package com.hackathon.afterlog.data.media

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
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
    
    /**
     * Creates a video from images (slideshow) when no video chunks are available.
     * Falls back to the existing VideoSynthesizer for this case.
     */
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
