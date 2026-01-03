package com.hackathon.afterlog.data.repository

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.hackathon.afterlog.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.hackathon.afterlog.data.remote.GeminiFilesApiClient

@Singleton
class GeminiRepository @Inject constructor(
    private val filesApiClient: GeminiFilesApiClient
) {

    private val generativeModel = GenerativeModel(
        // Development: gemini-2.5-flash (Working & High Quota)
        // Production: gemini-3-pro-preview (Gemini 3 Pro requirement)
        modelName = "gemini-2.5-flash", 
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = com.google.ai.client.generativeai.type.generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 8192
        }
    )

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent("Hello, are you there? Reply with: 'Connection Success!'")
            return@withContext response.text ?: "Connection Error: Empty response"
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Connection test failed", e)
            return@withContext "Connection Failed: ${e.localizedMessage}"
        }
    }

    suspend fun generateInvestigativeReport(
        videoFiles: List<File>,
        audioFile: File?,
        contextData: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d("GeminiRepo", "Starting Noir Analysis for ${videoFiles.size} files")
            
            // 1. Extract Key Frames from Videos (Limit 12 frames total)
            val frames = extractKeyFrames(videoFiles, maxFrames = 12)
            
            if (frames.isEmpty()) {
                return@withContext """{"headline":"NO EVIDENCE FOUND","summary":"The scene was empty.","atmosphere":"Silence.","timeline":[],"verdict":"Case closed—nothing to report."}"""
            }

            // 2. Audio Upload (Real implementation)
            val audioData = audioFile?.let { uploadAudioToGemini(it) }

            // 3. Prepare Detailed Cinematic Prompt (JSON FORCED)
            val systemInstruction = """
                # PERSONA
                You are a hard-boiled investigative journalist working for "The Midnight Chronicle" in the 1920s. 
                Your writing style is noir—sharp, atmospheric, and dripping with cynical wit.
                
                # TASK
                Analyze the provided VIDEO FRAMES${if (audioData != null) " and AUDIO RECORDING" else ""} from a tabletop game session. 
                Reconstruct a crime-scene-style report documenting key events.
                
                # COPYRIGHT & TRADEMARK SAFETY RULES (CRITICAL)
                1. **No Trademarks**: Do NOT use specific copyrighted names, locations, or branding (e.g., "Arkham", "Cthulhu", "D&D", game titles, brand logos).
                2. **Masking Strategy**: Replace specific IP terms with generic, atmospheric descriptors:
                   - "Arkham" -> "The Dark City", "This God-forsaken town"
                   - Specific Monsters (e.g., "Cthulhu") -> "The Ancient Horror", "The Tentacled Beast"
                   - Specific Characters -> "The Missing Heiress", "The Private Eye"
                3. **Safety Check**: If unsure if a term is trademarked, describe its appearance or role instead of using the name.
                
                # RULES
                1. **Cross-Validation**: If audio is provided, match audio events (screams, gasps, dialogue) to visual changes in the frames.
                2. **No Hallucination**: If you cannot clearly identify something in a frame or audio, state "Unidentified" or "Unclear". DO NOT invent details.
                3. **Speaker Identification**: Label distinct voices as "Speaker A (Male/Female)", "Speaker B", etc. Match to visuals if possible.
                4. **Timestamps**: Estimate timestamps based on frame order (assume even spacing). Format: "MM:SS".
                5. **Noir Atmosphere**: Use evocative language—shadows, cold steel, whispers, cracking floorboards.
                
                # CONTEXT PROVIDED BY USER
                $contextData
            """.trimIndent()

            val outputSchema = """
                # OUTPUT FORMAT
                Respond with ONLY valid JSON. NO markdown code fences. NO explanation before or after.
                
                {
                  "headline": "ALL CAPS SENSATIONAL TITLE (max 60 chars) - NO TRADEMARKS",
                  "summary": "One-sentence hook describing the session's most dramatic moment (max 120 chars)",
                  "atmosphere": "Scene-setting description with noir metaphors (max 200 chars)",
                  "article": "2-3 paragraphs of narrative journalism. Tell the story of what happened during this game session as if writing for The Midnight Chronicle. Use vivid prose, dramatic pacing, and noir atmosphere. Avoid copyrighted terms. (400-600 words)",
                  "timeline": [
                    {
                      "timestamp": "MM:SS format",
                      "speaker": "Speaker A (Gender) or 'Environment' for non-human sounds",
                      "event": "Brief event title (max 50 chars)",
                      "description": "Detailed noir-style narration of what happened (max 150 chars)",
                      "decibel": 85
                    }
                  ],
                  "verdict": "Your cynical, journalist's final deduction about what really happened (max 200 chars)"
                }
                
                REQUIREMENTS:
                - article MUST be 2-3 paragraphs of flowing narrative prose.
                - timeline MUST contain 3-7 events.
                - Every event MUST have timestamp, speaker, event, description.
                - decibel is optional (omit if not inferrable from audio).
                - If no audio provided, focus entirely on visual analysis.
                
                BEGIN JSON OUTPUT:
            """.trimIndent()

            val prompt = "$systemInstruction\n\n$outputSchema"

            // 4. Send to Gemini
            val inputContent = content {
                frames.forEach { bitmap ->
                    image(bitmap)
                }
                if (audioData != null) {
                    // CRITICAL FIX: Pass as FileData, not text
                    fileData(uri = audioData.first, mimeType = audioData.second)
                }
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            
            // Cleanup bitmaps
            frames.forEach { it.recycle() }

            return@withContext response.text ?: """{"headline":"ANALYSIS FAILED","summary":"Output was empty","atmosphere":"","timeline":[],"verdict":"The typewriter jammed."}"""

        } catch (e: Exception) {
            Log.e("GeminiRepo", "Investigation failed", e)
            return@withContext """{"headline":"SYSTEM ERROR","summary":"${e.localizedMessage}","atmosphere":"","timeline":[],"verdict":"Investigation aborted."}"""
        }
    }

    /**
     * Uploads audio file to Gemini Files API and returns the file URI and MimeType.
     */
    private suspend fun uploadAudioToGemini(audioFile: File): Pair<String, String>? {
        // Determine MIME type based on file extension
        val mimeType = when (audioFile.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "pcm" -> "audio/L16"  // Raw PCM (16-bit)
            else -> "audio/mpeg"  // Default fallback
        }
        
        val uri = filesApiClient.uploadFile(audioFile, mimeType)
        return if (uri != null) {
            Pair(uri, mimeType)
        } else {
            null
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
