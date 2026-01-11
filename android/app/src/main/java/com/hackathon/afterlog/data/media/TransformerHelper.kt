package com.hackathon.afterlog.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper object for Media3 Transformer operations.
 * 
 * Provides low-level utilities for building media sequences and executing transformations.
 * Used internally by VideoStitcher.
 */
@UnstableApi
internal object TransformerHelper {
    private const val TAG = "TransformerHelper"

    /**
     * Executes the Transformer export operation asynchronously.
     */
    suspend fun executeTransformation(
        context: Context,
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
        Handler(Looper.getMainLooper()).post {
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
     * Builds a video sequence from multiple files for concatenation.
     */
    fun buildVideoSequence(videoFiles: List<File>): EditedMediaItemSequence {
        val editedItems = videoFiles.map { file ->
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            EditedMediaItem.Builder(mediaItem).build()
        }
        return EditedMediaItemSequence(editedItems)
    }
    
    /**
     * Builds an audio-only sequence for overlay.
     */
    fun buildAudioSequence(audioFile: File): EditedMediaItemSequence {
        val mediaItem = MediaItem.fromUri(audioFile.toURI().toString())
        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true) // Keep only audio track
            .build()
        return EditedMediaItemSequence(listOf(editedItem))
    }

    /**
     * Builds an audio-only sequence clipped to a specific window.
     */
    fun buildClippedAudioSequence(
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

    /**
     * Builds an audio sequence from multiple clips within a single audio file.
     */
    fun buildAudioSequenceFromClips(
        audioFile: File,
        audioStartOffsetMs: Long,
        clips: List<VideoStitcher.AudioClip>
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

    /**
     * Extracts the duration of a media file in milliseconds.
     */
    fun extractDurationMs(file: File): Long? {
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
}
