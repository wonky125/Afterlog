package com.example.afterlog.data.repository

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.afterlog.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", // Updated model for multimodal tasks
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateInvestigativeReport(videoFiles: List<File>, contextData: String): String = withContext(Dispatchers.IO) {
        try {
            // 1. Extract Key Frames from Videos (Limit 10 frames total to save token/bandwidth)
            val frames = extractKeyFrames(videoFiles, maxFrames = 10)
            
            if (frames.isEmpty()) {
                return@withContext "Error: No visual evidence found in the recordings."
            }

            // 2. Prepare Prompt
            // We act as a detective analyzing CCTV footage.
            val prompt = """
                You are an expert investigative journalist and detective.
                Analyze these frames extracted from a security camera footage (AfterLog).
                
                Context: $contextData
                
                Your Task:
                1. Describe the key events visible in the sequence.
                2. Identify any potential threats, anomalies, or suspicious objects.
                3. Infer the emotional state of any persons visible.
                4. Provide a dramatic, noir-style summary of this 3-minute segment.
                
                Output Format:
                Headline: [Catchy Title]
                Time: [Estimated Time]
                Observation: [Detailed analysis]
                Conclusion: [Your deduction]
            """.trimIndent()

            // 3. Send to Gemini
            val inputContent = content {
                frames.forEach { bitmap ->
                    image(bitmap)
                }
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            
            // Cleanup bitmaps to free memory
            frames.forEach { it.recycle() }

            return@withContext response.text ?: "Case Unsolved: The AI remained silent."

        } catch (e: Exception) {
            Log.e("GeminiRepo", "Analysis failed", e)
            return@withContext "Analysis Error: ${e.localizedMessage}"
        }
    }

    private fun extractKeyFrames(videoFiles: List<File>, maxFrames: Int): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        
        try {
            // Distribute frames across all available video files
            // For MVP, we just take the last (most recent) video file which contains the "event"
            val targetFile = videoFiles.lastOrNull() ?: return emptyList()
            
            retriever.setDataSource(targetFile.absolutePath)
            
            // Get duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            if (durationMs > 0) {
                // Extract 'maxFrames' evenly spaced
                val interval = durationMs / (maxFrames + 1)
                for (i in 1..maxFrames) {
                    val timeUs = (interval * i) * 1000 // Microseconds
                    // OPTION_CLOSEST_SYNC is faster
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bitmap?.let { bitmaps.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Frame extraction failed", e)
        } finally {
            retriever.release()
        }
        
        return bitmaps
    }
}
