package com.hackathon.afterlog.data.media

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.common.C
import androidx.media3.common.MediaItem.ClippingConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import com.google.common.collect.ImmutableList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            val videoSequence = buildVideoSequence(validVideos)
            
            // Build audio sequence if provided
            val audioSequence = narrationAudio?.let { buildAudioSequence(it) }
            
            // Create composition with multiple sequences if needed
            val sequences = mutableListOf(videoSequence)
            if (audioSequence != null) {
                sequences.add(audioSequence)
            }
            
            val composition = Composition.Builder(sequences).build()
            
            // Execute transformation with cleanup guarantee
            val result = try {
                executeTransformation(composition, outputFile)
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

            val result = executeTransformation(composition, outputFile)

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

        val cues = parseSrtToCues(srtContent)
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

        val playableAudio = ensurePlayableAudioFile(audioFile)
        Log.d(TAG, "Stitch (with clipped audio): ${validVideos.size} videos, audio=${playableAudio.name}, window=$audioClipStartMs-$audioClipEndMs")

        val outputFile = File(context.filesDir, "stitched_replay_$outputSessionId.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            val videoSequence = buildVideoSequence(validVideos)
            Log.d(TAG, "videoChunks=${videoChunks.size}, audio=${playableAudio.name}, clip=$audioClipStartMs-$audioClipEndMs")
            val audioSequence = buildClippedAudioSequence(playableAudio, audioClipStartMs, audioClipEndMs)

            val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()

            val result = executeTransformation(composition, outputFile)

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

        val playableAudio = ensurePlayableAudioFile(audioFile)
        Log.d(TAG, "Stitch (audio segments): videos=${validVideos.size}, clips=${audioClips.size}, startOffset=$audioStartOffsetMs, audio=${playableAudio.name}")

        val outputFile = File(context.filesDir, "stitched_replay_$outputSessionId.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            val videoSequence = buildVideoSequence(validVideos)
            val audioSequence = buildAudioSequenceFromClips(playableAudio, audioStartOffsetMs, audioClips)

            val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()
            val result = executeTransformation(composition, outputFile)

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

        val durationMs = extractDurationMs(inputVideo)
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

        val result = executeTransformation(composition, outputFile)
        if (result.isSuccess && outputFile.exists()) {
            Log.d(TAG, "Trimmed video to ${maxDurationMs}ms: ${outputFile.absolutePath}")
            Result.success(outputFile)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Video trim failed."))
        }
    }
    
    /**
     * Builds a video sequence from multiple files for concatenation.
     */
    private fun buildVideoSequence(videoFiles: List<File>): EditedMediaItemSequence {
        val editedItems = videoFiles.map { file ->
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            EditedMediaItem.Builder(mediaItem).build()
        }
        return EditedMediaItemSequence(editedItems)
    }
    
    /**
     * Builds an audio-only sequence for overlay.
     */
    private fun buildAudioSequence(audioFile: File): EditedMediaItemSequence {
        val mediaItem = MediaItem.fromUri(audioFile.toURI().toString())
        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true) // Keep only audio track
            .build()
        return EditedMediaItemSequence(listOf(editedItem))
    }

    /**
     * Builds an audio-only sequence clipped to a specific window.
     */
    private fun buildClippedAudioSequence(
        audioFile: File,
        startMs: Long,
        endMs: Long
    ): EditedMediaItemSequence {
        val clipping = ClippingConfiguration.Builder()
            .setStartPositionMs(startMs.coerceAtLeast(0))
            .setEndPositionMs(if (endMs > 0) endMs else C.TIME_END_OF_SOURCE)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(audioFile.toURI().toString())
            .setClippingConfiguration(clipping)
            .build()

        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .build()

        return EditedMediaItemSequence(listOf(editedItem))
    }

    private fun buildAudioSequenceFromClips(
        audioFile: File,
        audioStartOffsetMs: Long,
        clips: List<AudioClip>
    ): EditedMediaItemSequence {
        val editedItems = clips.mapNotNull { clip ->
            val rawStart = clip.startMs - audioStartOffsetMs
            val trimMs = if (rawStart < 0) -rawStart else 0L
            val clipStart = rawStart.coerceAtLeast(0)
            val clipDuration = (clip.durationMs - trimMs).coerceAtLeast(0)
            val clipEnd = clipStart + clipDuration

            if (clipDuration <= 0) {
                Log.w(TAG, "Skipping audio clip: start=${clip.startMs} duration=${clip.durationMs}")
                return@mapNotNull null
            }

            val clipping = ClippingConfiguration.Builder()
                .setStartPositionMs(clipStart)
                .setEndPositionMs(clipEnd)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(audioFile.toURI().toString())
                .setClippingConfiguration(clipping)
                .build()

            EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)
                .build()
        }

        return EditedMediaItemSequence(editedItems)
    }

    private fun extractDurationMs(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read duration: ${file.name}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release retriever", e)
            }
        }
    }
    
    /**
     * Executes the Transformer export operation asynchronously.
     */
    private suspend fun executeTransformation(
        composition: Composition,
        outputFile: File
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Log.d(TAG, "Transformation completed successfully")
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }
                
                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    Log.e(TAG, "Transformation failed: ${exportException.message}")
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            })
            .build()
        
        // Start export on main thread (Transformer requirement)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                transformer.start(composition, outputFile.absolutePath)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
        
        continuation.invokeOnCancellation {
            transformer.cancel()
        }
    }

    private fun ensurePlayableAudioFile(audioFile: File): File {
        if (audioFile.extension.equals("pcm", ignoreCase = true)) {
            val wavFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}_converted.wav")
            return convertPcmToWav(audioFile, wavFile) ?: audioFile
        }
        return audioFile
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File): File? {
        return try {
            if (wavFile.exists()) wavFile.delete()

            val sampleRate = 16000
            val channels = 1
            val byteRate = sampleRate * channels * 2
            val dataLen = pcmFile.length()
            val maxDataLen = 0xFFFFFFFFL - 36
            if (dataLen <= 0L || dataLen > maxDataLen) {
                Log.e(TAG, "PCM file too large for WAV header: $dataLen")
                return null
            }
            val totalDataLen = dataLen + 36

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()

            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()

            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()

            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0

            header[20] = 1
            header[21] = 0

            header[22] = channels.toByte()
            header[23] = 0

            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()

            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()

            header[32] = (channels * 2).toByte()
            header[33] = 0

            header[34] = 16
            header[35] = 0

            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()

            header[40] = (dataLen and 0xff).toByte()
            header[41] = ((dataLen shr 8) and 0xff).toByte()
            header[42] = ((dataLen shr 16) and 0xff).toByte()
            header[43] = ((dataLen shr 24) and 0xff).toByte()

            FileOutputStream(wavFile).use { out ->
                out.write(header)
                FileInputStream(pcmFile).use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                }
            }

            Log.d(TAG, "PCM converted to WAV: ${wavFile.absolutePath}")
            wavFile
        } catch (e: Exception) {
            Log.e(TAG, "PCM to WAV conversion failed", e)
            null
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

    private class CaptionTextOverlay(
        cues: List<CaptionCue>
    ) : TextOverlay() {
        private data class PreparedCue(val startMs: Long, val endMs: Long, val text: SpannableString)

        private val emptyText = buildInvisibleSpan()
        private val preparedCues = cues.mapNotNull { cue ->
            val text = cue.text.trim()
            if (text.isBlank()) {
                null
            } else {
                PreparedCue(
                    startMs = cue.startMs,
                    endMs = cue.endMs,
                    text = buildSpan(text)
                )
            }
        }.sortedBy { it.startMs }
        private var lastIndex = -1

        private val overlaySettings = OverlaySettings.Builder()
            .setBackgroundFrameAnchor(0f, -0.6f)
            .setOverlayFrameAnchor(0f, -1f)
            .build()

        override fun getText(presentationTimeUs: Long): SpannableString {
            val timeMs = presentationTimeUs / 1000
            val cue = findCue(timeMs) ?: return emptyText
            return cue.text
        }

        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
            return overlaySettings
        }

        private fun findCue(timeMs: Long): PreparedCue? {
            val idx = lastIndex
            if (idx in preparedCues.indices) {
                val current = preparedCues[idx]
                if (timeMs in current.startMs..current.endMs) {
                    return current
                }
            }

            for (i in preparedCues.indices) {
                val cue = preparedCues[i]
                if (timeMs in cue.startMs..cue.endMs) {
                    lastIndex = i
                    return cue
                }
            }
            return null
        }

        private fun buildSpan(text: String): SpannableString {
            val spanText = SpannableString(text)
            if (text.isNotEmpty()) {
                spanText.setSpan(
                    ForegroundColorSpan(Color.WHITE),
                    0,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spanText.setSpan(
                    BackgroundColorSpan(Color.argb(160, 0, 0, 0)),
                    0,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spanText.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spanText.setSpan(
                    RelativeSizeSpan(0.85f),
                    0,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spanText
        }

        private fun buildInvisibleSpan(): SpannableString {
            val spanText = SpannableString(".")
            spanText.setSpan(
                ForegroundColorSpan(Color.TRANSPARENT),
                0,
                spanText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanText.setSpan(
                BackgroundColorSpan(Color.TRANSPARENT),
                0,
                spanText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spanText
        }
    }

    private fun parseSrtToCues(srtContent: String): List<CaptionCue> {
        val normalized = srtContent.replace("\r", "").trimStart('\uFEFF')
        if (normalized.isBlank()) return emptyList()

        val cues = mutableListOf<CaptionCue>()
        val blocks = normalized.split(Regex("\\n\\s*\\n"))
        for (block in blocks) {
            val lines = block.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex == -1) continue

            val timeLine = lines[timeLineIndex]
            val parts = timeLine.split("-->")
            if (parts.size < 2) continue

            val startMs = parseSrtTimestamp(parts[0].trim())
            val endPart = parts[1].trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            val endMs = parseSrtTimestamp(endPart)

            val textLines = lines.drop(timeLineIndex + 1)
            val text = cleanSrtText(textLines.joinToString(" "))
            if (startMs != null && endMs != null && text.isNotBlank()) {
                cues.add(
                    CaptionCue(
                        startMs = startMs.coerceAtLeast(0L),
                        endMs = maxOf(endMs, startMs + 500L),
                        text = text
                    )
                )
            }
        }
        return cues
    }

    private fun cleanSrtText(text: String): String {
        return text
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\{\\\\.*?\\}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseSrtTimestamp(value: String): Long? {
        val match = Regex("(\\d{2}):(\\d{2}):(\\d{2})([.,](\\d{1,3}))?").find(value) ?: return null
        val hours = match.groupValues[1]
        val minutes = match.groupValues[2]
        val seconds = match.groupValues[3]
        val millisPart = match.groupValues[5]
        val millis = when (millisPart.length) {
            0 -> 0
            1 -> millisPart.toInt() * 100
            2 -> millisPart.toInt() * 10
            else -> millisPart.take(3).toInt()
        }
        return hours.toLong() * 3_600_000L +
            minutes.toLong() * 60_000L +
            seconds.toLong() * 1_000L +
            millis.toLong()
    }
}
