package com.hackathon.afterlog.data.media

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VideoStitcher: Concatenates multiple video chunks and overlays TTS audio.
 * 
 * Uses AndroidX Media3 Transformer for video editing operations.
 * This replaces the need for FFmpeg-Kit.
 * 
 * Architecture: data/media layer (per CODING_STANDARDS.md package structure)
 */
@UnstableApi
@Singleton
class VideoStitcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AudioClip(val startMs: Long, val durationMs: Long)
    data class CaptionCue(val startMs: Long, val endMs: Long, val text: String)

    companion object {
        private const val TAG = "VideoStitcher"
    }
    
    /**
     * Concatenates video clips and optionally overlays narration audio.
     * 
     * @param videoChunks List of video files to stitch together (in order)
     * @param narrationAudio Optional TTS narration audio to overlay
     * @param outputSessionId Session ID for output file naming
     * @return Result containing the output MP4 file or error
     */
    suspend fun stitchVideos(
        videoChunks: List<File>,
        narrationAudio: File? = null,
        outputSessionId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        
        if (videoChunks.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No video chunks provided for stitching")
            )
        }
        
        // Validate all video files exist
        val validVideos = videoChunks.filter { it.exists() && it.length() > 0 }
        if (validVideos.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No valid video files found")
            )
        }
        
        Log.d(TAG, "Starting stitch: ${validVideos.size} videos, audio=${narrationAudio?.name}")
        
        val outputFile = File(context.filesDir, "stitched_replay_$outputSessionId.mp4")
        if (outputFile.exists()) outputFile.delete()
        
        try {
            // Build video sequence (concatenation)
            val videoSequence = TransformerHelper.buildVideoSequence(validVideos)
            
            // Build audio sequence if provided
            val audioSequence = narrationAudio?.let { TransformerHelper.buildAudioSequence(it) }
            
            // Create composition with multiple sequences if needed
            val sequences = mutableListOf(videoSequence)
            if (audioSequence != null) {
                sequences.add(audioSequence)
            }
            
            val composition = Composition.Builder(sequences).build()
            
            // Execute transformation with cleanup guarantee
            val result = try {
                TransformerHelper.executeTransformation(context, composition, outputFile)
            } finally {
                // Ensure cleanup even if transformation fails
                try {
                    sequences.clear()
                } catch (e: Exception) {
                    Log.w(TAG, "Cleanup warning", e)
                }
            }
            
            if (result.isSuccess && outputFile.exists()) {
                Log.d(TAG, "✅ Stitch complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                Result.success(outputFile)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown stitching error"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Stitching failed", e)
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Burns subtitle cues into the video stream and returns a new MP4.
     */
    suspend fun burnInSubtitles(
        inputVideo: File,
        cues: List<CaptionCue>,
        outputSessionId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!inputVideo.exists()) {
            return@withContext Result.failure(
                IllegalArgumentException("Input video missing: ${inputVideo.absolutePath}")
            )
        }

        val normalizedCues = cues.mapNotNull { cue ->
            val text = cue.text.replace("\n", " ").trim()
            if (text.isBlank()) {
                null
            } else {
                val startMs = cue.startMs.coerceAtLeast(0L)
                val endMs = maxOf(cue.endMs, startMs + 1500L)
                CaptionCue(startMs = startMs, endMs = endMs, text = text)
            }
        }

        if (normalizedCues.isEmpty()) {
            Log.d(TAG, "No subtitle cues provided; skipping burn-in.")
            return@withContext Result.success(inputVideo)
        }

        val outputFile = File(context.filesDir, "replay_${outputSessionId}_burned.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            val overlay = CaptionTextOverlay(normalizedCues)
            val overlayEffect = OverlayEffect(ImmutableList.of(overlay))
            val effects = Effects(emptyList(), listOf(overlayEffect))

            val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(inputVideo.toURI().toString()))
                .setEffects(effects)
                .build()
            val composition = Composition.Builder(EditedMediaItemSequence(editedItem)).build()

            val result = TransformerHelper.executeTransformation(context, composition, outputFile)

            if (result.isSuccess && outputFile.exists()) {
                Log.d(TAG, "Subtitle burn-in complete: ${outputFile.absolutePath}")
                Result.success(outputFile)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Subtitle burn-in failed."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Subtitle burn-in failed", e)
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Burns subtitles into the video stream using an SRT file as the source of truth.
     */
    suspend fun burnInSubtitlesFromSrt(
        inputVideo: File,
        subtitleFile: File,
        outputSessionId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!subtitleFile.exists()) {
            return@withContext Result.failure(
                IllegalArgumentException("Subtitle file missing: ${subtitleFile.absolutePath}")
            )
        }

        val srtContent = runCatching { subtitleFile.readText() }.getOrNull().orEmpty()
        if (srtContent.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Subtitle file is empty: ${subtitleFile.absolutePath}")
            )
        }

        val cues = SubtitleProcessor.parseSrtToCues(srtContent)
        if (cues.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("No cues parsed from SRT: ${subtitleFile.absolutePath}")
            )
        }

        burnInSubtitles(inputVideo, cues, outputSessionId)
    }

    /**
     * Concatenates video clips and overlays a clipped portion of a long audio file (e.g., PCM).
     * Audio is clipped to [audioClipStartMs, audioClipEndMs] before overlay.
     */
    suspend fun stitchVideosWithAudioClip(
        videoChunks: List<File>,
        audioFile: File,
        audioClipStartMs: Long,
        audioClipEndMs: Long,
        outputSessionId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        
        if (videoChunks.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No video chunks provided for stitching")
            )
        }
        
        val validVideos = videoChunks.filter { it.exists() && it.length() > 0 }
        if (validVideos.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No valid video files found")
            )
        }

        val playableAudio = AudioConverter.ensurePlayableAudioFile(audioFile)
        Log.d(TAG, "Stitch (with clipped audio): ${validVideos.size} videos, audio=${playableAudio.name}, window=$audioClipStartMs-$audioClipEndMs")

        val outputFile = File(context.filesDir, "stitched_replay_$outputSessionId.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            val videoSequence = TransformerHelper.buildVideoSequence(validVideos)
            Log.d(TAG, "videoChunks=${videoChunks.size}, audio=${playableAudio.name}, clip=$audioClipStartMs-$audioClipEndMs")
            val audioSequence = TransformerHelper.buildClippedAudioSequence(playableAudio, audioClipStartMs, audioClipEndMs)

            val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()

            val result = TransformerHelper.executeTransformation(context, composition, outputFile)

            if (result.isSuccess && outputFile.exists()) {
                Log.d(TAG, "✅ Stitch (clipped audio) complete: ${outputFile.absolutePath}")
                Result.success(outputFile)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown stitching error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stitching (clipped audio) failed", e)
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Concatenates video clips and overlays audio clips aligned to each segment.
     * The audio file is a single continuous recording; clips are pulled per segment.
     */
    suspend fun stitchVideosWithAudioSegments(
        videoChunks: List<File>,
        audioFile: File,
        audioStartOffsetMs: Long,
        audioClips: List<AudioClip>,
        outputSessionId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (videoChunks.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No video chunks provided for stitching")
            )
        }

        val validVideos = videoChunks.filter { it.exists() && it.length() > 0 }
        if (validVideos.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No valid video files found")
            )
        }

        if (audioClips.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No audio clips provided for stitching")
            )
        }

        val playableAudio = AudioConverter.ensurePlayableAudioFile(audioFile)
        Log.d(TAG, "Stitch (audio segments): videos=${validVideos.size}, clips=${audioClips.size}, startOffset=$audioStartOffsetMs, audio=${playableAudio.name}")

        val outputFile = File(context.filesDir, "stitched_replay_$outputSessionId.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            val videoSequence = TransformerHelper.buildVideoSequence(validVideos)
            val audioSequence = TransformerHelper.buildAudioSequenceFromClips(playableAudio, audioStartOffsetMs, audioClips)

            val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()
            val result = TransformerHelper.executeTransformation(context, composition, outputFile)

            if (result.isSuccess && outputFile.exists()) {
                Log.d(TAG, "Stitch (audio segments) complete: ${outputFile.absolutePath}")
                Result.success(outputFile)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown stitching error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stitching (audio segments) failed", e)
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Trims a video to a max duration using Transformer clipping.
     */
    suspend fun trimVideoToMaxDuration(
        inputVideo: File,
        maxDurationMs: Long,
        outputSessionId: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!inputVideo.exists()) {
            return@withContext Result.failure(
                IllegalArgumentException("Input video missing: ${inputVideo.absolutePath}")
            )
        }
        if (maxDurationMs <= 0) {
            return@withContext Result.success(inputVideo)
        }

        val durationMs = TransformerHelper.extractDurationMs(inputVideo)
        if (durationMs == null || durationMs <= maxDurationMs) {
            return@withContext Result.success(inputVideo)
        }

        val suffix = if (inputVideo.nameWithoutExtension.contains("_burned")) {
            "_burned_trimmed"
        } else {
            "_trimmed"
        }
        val outputFile = File(context.filesDir, "replay_${outputSessionId}$suffix.mp4")
        if (outputFile.exists()) outputFile.delete()

        val clipping = ClippingConfiguration.Builder()
            .setStartPositionMs(0)
            .setEndPositionMs(maxDurationMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(inputVideo.toURI().toString())
            .setClippingConfiguration(clipping)
            .build()

        val editedItem = EditedMediaItem.Builder(mediaItem).build()
        val composition = Composition.Builder(EditedMediaItemSequence(editedItem)).build()

        val result = TransformerHelper.executeTransformation(context, composition, outputFile)
        if (result.isSuccess && outputFile.exists()) {
            Log.d(TAG, "Trimmed video to ${maxDurationMs}ms: ${outputFile.absolutePath}")
            Result.success(outputFile)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Video trim failed."))
        }
    }

    /**
     * Trims a video to a specific time window.
     */
    suspend fun trimVideoToWindow(
        inputVideo: File,
        startMs: Long,
        endMs: Long,
        outputSessionId: String,
        outputLabel: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!inputVideo.exists()) {
            return@withContext Result.failure(
                IllegalArgumentException("Input video missing: ${inputVideo.absolutePath}")
            )
        }

        val safeStart = startMs.coerceAtLeast(0L)
        var safeEnd = endMs
        if (safeEnd <= safeStart) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid trim window: start=$startMs end=$endMs")
            )
        }

        val durationMs = TransformerHelper.extractDurationMs(inputVideo)
        if (durationMs != null) {
            if (safeStart == 0L && safeEnd >= durationMs) {
                return@withContext Result.success(inputVideo)
            }
            safeEnd = safeEnd.coerceAtMost(durationMs)
            if (safeEnd <= safeStart) {
                return@withContext Result.failure(
                    IllegalArgumentException("Trim window out of bounds: start=$safeStart end=$safeEnd")
                )
            }
        }

        val safeLabel = outputLabel
            ?.replace(Regex("[^A-Za-z0-9_-]"), "_")
            ?.take(40)
            .orEmpty()
        val suffix = if (safeLabel.isNotBlank()) {
            "_${safeLabel}_clip_${safeStart}_${safeEnd}"
        } else {
            "_clip_${safeStart}_${safeEnd}"
        }
        val outputFile = File(context.filesDir, "replay_${outputSessionId}$suffix.mp4")
        if (outputFile.exists()) outputFile.delete()

        val clipping = ClippingConfiguration.Builder()
            .setStartPositionMs(safeStart)
            .setEndPositionMs(safeEnd)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(inputVideo.toURI().toString())
            .setClippingConfiguration(clipping)
            .build()

        val editedItem = EditedMediaItem.Builder(mediaItem).build()
        val composition = Composition.Builder(EditedMediaItemSequence(editedItem)).build()

        val result = TransformerHelper.executeTransformation(context, composition, outputFile)
        if (result.isSuccess && outputFile.exists()) {
            Log.d(TAG, "Trimmed video window $safeStart-$safeEnd: ${outputFile.absolutePath}")
            Result.success(outputFile)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Video trim window failed."))
        }
    }
    
    /**
     * Creates a video from images (slideshow) when no video chunks are available.
     * Falls back to the existing VideoSynthesizer for this case.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun createImageSlideshow(
        images: List<File>,
        narrationAudio: File,
        outputSessionId: String,
        imageDurationSec: Int = 5
    ): Result<File> = withContext(Dispatchers.IO) {
        // Delegate to VideoSynthesizer for image-based video creation
        // This keeps the existing working logic for the fallback case
        Result.failure(UnsupportedOperationException("Use VideoSynthesizer for image slideshows"))
    }
}
