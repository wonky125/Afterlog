package com.hackathon.afterlog.data.repository.gemini

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

object GeminiVideoUtils {
    fun extractKeyFrames(
        videoFiles: List<File>,
        intervalSec: Int = 15,
        maxTotalFrames: Int = 40
    ): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var totalFrames = 0
        
        try {
        // Process all video files to cover the full session
            for (videoFile in videoFiles) {
                if (totalFrames >= maxTotalFrames) break
                if (!videoFile.exists()) continue
                
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoFile.absolutePath)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    
                    if (durationMs > 0) {
                        // Extract 1 frame every intervalSec, but cap per video to limit token usage.
                        val totalPossibleFrames = (durationMs / (intervalSec * 1000)).toInt()
                        val remaining = maxTotalFrames - totalFrames
                        val frameCount = minOf(totalPossibleFrames, 20, remaining)
                        
                        // Use until to avoid overshooting duration
                        for (i in 0 until frameCount) {
                            val timeUs = i * intervalSec * 1000000L
                            
                            // Retrieve and scale down to reduce token usage/memory (e.g., 512x512 max)
                            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            
                            if (bitmap != null) {
                                // Simple resizing to save bandwidth/tokens (Gemini checks usually roughly 512px)
                                val scaledForGemini = Bitmap.createScaledBitmap(bitmap, 512, 512, true) 
                                if (bitmap != scaledForGemini) {
                                    bitmap.recycle()
                                }
                                bitmaps.add(scaledForGemini)
                                totalFrames++
                                if (totalFrames >= maxTotalFrames) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeminiRepo", "Error extracting from ${videoFile.name}", e)
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Frame extraction failed", e)
        }
        
        Log.d("GeminiRepo", "Extracted total ${bitmaps.size} frames for analysis")
        return bitmaps
    }
}
