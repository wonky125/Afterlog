package com.hackathon.afterlog.data.repository

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.hackathon.afterlog.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.hackathon.afterlog.data.remote.GeminiFilesApiClient
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class GeminiRepository @Inject constructor(
    private val filesApiClient: GeminiFilesApiClient,
    @ApplicationContext private val context: Context
) {
    data class CaptionLine(val startMs: Long, val endMs: Long, val text: String)

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-pro-preview",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 4096
        }
    )

    private val retryableErrorHints = listOf(
        "503",
        "429",
        "unavailable",
        "overloaded",
        "resource_exhausted",
        "temporarily",
        "timeout"
    )
    
    private val json = Json { ignoreUnknownKeys = true }

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
            
            val frames = extractKeyFrames(videoFiles, intervalSec = 15)
            Log.d("GeminiRepo", "Extracted ${frames.size} frames")
            
            val audioData = audioFile?.let { uploadAudioToGemini(it) }

            // Only abort if BOTH frames and audio are missing
            if (frames.isEmpty() && audioData == null) {
                Log.w("GeminiRepo", "No evidence (video or audio) found. Aborting.")
                return@withContext """{"headline":"NO EVIDENCE FOUND","summary":"The scene was empty.","atmosphere":"Silence.","timeline":[],"verdict":"Case closed—nothing to report."}"""
            }

            val systemInstruction = """
                # PERSONA
                You are an investigative journalist in the 1920s, documenting a Lovecraftian mystery.
                Your communication style is noir, gritty, and atmospheric, using rich, descriptive language of that era.
                
                # TASK
                Analyze the provided VIDEO FRAMES${if (audioData != null) " and AUDIO RECORDING" else ""} from a crime scene. 
                Reconstruct a newspaper article documenting the investigation and the supernatural or criminal events discovered.
                
                # CONTENT RULES
                1. **Noir Atmosphere**: Use words like 'macabre', 'eldritch', 'unspeakable', 'shadows', 'archives', 'the fog'.
                2. **No Space Horror**: Avoid all science fiction, robots, spaceships, or technical AI terms.
                3. **ID Identification**: Label individuals as 'The Private Eye', 'The Witness', 'The Suspect', or 'Local Constable'.
                4. **Journalistic Flow**: The report should read like a front-page story in a gothic newspaper.
                5. **No Underscores in Headline**: The headline must be a standard human-readable newspaper headline.
                
                # CONTEXT PROVIDED BY USER
                $contextData
            """.trimIndent()

            val outputSchema = """
                # OUTPUT FORMAT
                Respond with ONLY valid JSON.
                {
                  "headline": "A gripping newspaper headline",
                  "summary": "The sub-headline or lead (max 120 chars)",
                  "atmosphere": "The sensory details of the scene (max 200 chars)",
                  "article": "A detailed 2-3 paragraph investigative report in Noir style. (400-600 words)",
                  "timeline": [
                    {
                      "timestamp": "MM:SS format",
                      "speaker": "The Detective or Witness ID",
                      "event": "Event description (max 50 chars)",
                      "description": "Flavor text describing what happened (max 150 chars)",
                      "decibel": 85
                    }
                  ],
                  "verdict": "Your ultimate deduction regarding the mystery (max 200 chars)"
                }
            """.trimIndent()

            val prompt = "$systemInstruction\n\n$outputSchema"

            val inputContent = content {
                frames.forEach { bitmap ->
                    image(bitmap)
                }
                if (audioData != null) {
                    fileData(uri = audioData.first, mimeType = audioData.second)
                }
                text(prompt)
            }

            val response = generateWithRetry(inputContent)
            
            frames.forEach { it.recycle() }

            return@withContext response.text ?: """{"headline":"ANALYSIS FAILED","summary":"Output was empty","atmosphere":"","timeline":[],"verdict":"The typewriter jammed."}"""

        } catch (e: Exception) {
            Log.e("GeminiRepo", "Investigation failed", e)
            if (isRetryableGeminiError(e)) {
                return@withContext """{"headline":"ANALYSIS DELAYED","summary":"Gemini is temporarily overloaded. Please retry.","atmosphere":"","article":"","timeline":[],"verdict":"Awaiting a clearer signal."}"""
            }
            // Return detailed error for debugging
            val errorType = e.javaClass.simpleName
            val errorMessage = e.message?.replace("\"", "'") ?: "Unknown error"
            return@withContext """{"headline":"SYSTEM ERROR ($errorType)","summary":"$errorMessage","atmosphere":"","timeline":[],"verdict":"Investigation aborted."}"""
        }
    }

    /**
     * Generates noir-style minimal captions (headline tone) for key events.
     * Output format:
     * {
     *   "events": [
     *     {"start_ms": 12000, "end_ms": 14000, "text": "운명의 굴림"}
     *   ]
     * }
     */
    suspend fun generateNoirCaptions(
        audioFile: File,
        eventHints: List<String> = emptyList()
    ): List<CaptionLine> = withContext(Dispatchers.IO) {
        try {
            val audioData = uploadAudioToGemini(audioFile) ?: return@withContext emptyList()

            val prompt = """
                You are a 1920s noir journalist writing ultra-short captions for a newsreel.
                Keep the mood: fate, evidence, betrayal, silence breaking.
                
                RULES:
                - Output JSON only: {"events":[{"start_ms":12000,"end_ms":14000,"text":"어둠이 갈라졌다"}]}
                - 5~10 events max.
                - text: 2~5 words (~12 chars), no slang, no modern internet words.
                - Use strong noir phrases: 운명, 침묵, 단서, 그림자, 배신, 결판.
                - Align to audio moments (scream, loud spike, tense dialogue).
                
                HINTS:
                ${eventHints.joinToString(separator = "; ")}
            """.trimIndent()

            val inputContent = content {
                fileData(uri = audioData.first, mimeType = audioData.second)
                text(prompt)
            }

            val response = generateWithRetry(inputContent)
            val raw = response.text ?: return@withContext emptyList()
            return@withContext parseCaptions(raw)
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Caption generation failed", e)
            return@withContext emptyList()
        }
    }

    private fun parseCaptions(raw: String): List<CaptionLine> {
        return try {
            var cleaned = raw.trim()
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            }
            val root = json.parseToJsonElement(cleaned).jsonObject
            val events = root["events"]?.jsonArray ?: return emptyList()
            events.mapNotNull { el ->
                runCatching {
                    val obj = el.jsonObject
                    val start = obj["start_ms"]?.jsonPrimitive?.longOrNull ?: return@runCatching null
                    val end = obj["end_ms"]?.jsonPrimitive?.longOrNull ?: return@runCatching null
                    val text = obj["text"]?.jsonPrimitive?.content ?: return@runCatching null
                    CaptionLine(startMs = start, endMs = end, text = text)
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Caption parse failed", e)
            emptyList()
        }
    }

    private suspend fun generateWithRetry(inputContent: com.google.ai.client.generativeai.type.Content)
        : com.google.ai.client.generativeai.type.GenerateContentResponse {
        val maxAttempts = 3
        var delayMs = 1000L
        var lastError: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return generativeModel.generateContent(inputContent)
            } catch (e: Exception) {
                lastError = e
                val isLastAttempt = attempt == maxAttempts - 1
                if (isLastAttempt || !isRetryableGeminiError(e)) {
                    throw e
                }
                Log.w("GeminiRepo", "Gemini overloaded/unavailable. Retrying in ${delayMs}ms", e)
                delay(delayMs)
                delayMs *= 2
            }
        }
        throw lastError ?: IllegalStateException("Gemini generateContent failed with unknown error.")
    }

    private fun isRetryableGeminiError(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val message = current.message?.lowercase() ?: ""
            if (retryableErrorHints.any { hint -> message.contains(hint) }) return true
            val simpleName = current.javaClass.simpleName.lowercase()
            if (simpleName.contains("serverexception") || simpleName.contains("apiexception")) {
                if (message.contains("5") || message.contains("unavailable")) return true
            }
            current = current.cause
        }
        return false
    }

    private suspend fun uploadAudioToGemini(audioFile: File): Pair<String, String>? {
        if (!audioFile.exists() || !audioFile.canRead()) {
            Log.e("GeminiRepo", "Audio file not accessible: ${audioFile.absolutePath}")
            return null
        }

        val mimeType = when (audioFile.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "pcm" -> {
                // Convert PCM to WAV before upload
                val wavFile = convertPcmToWav(audioFile)
                if (wavFile != null) {
                    val wavMimeType = "audio/wav"
                    Log.d("GeminiRepo", "Uploading converted audio: ${wavFile.name} ($wavMimeType)")
                    val uri = filesApiClient.uploadFile(wavFile, wavMimeType)
                    return if (uri != null) {
                        Log.d("GeminiRepo", "Audio upload SUCCESS. URI: $uri")
                        Pair(uri, wavMimeType)
                    } else {
                        Log.e("GeminiRepo", "Audio upload FAILED. URI is null.")
                        null
                    }
                } else {
                    Log.e("GeminiRepo", "PCM conversion failed, skipping upload.")
                    return null
                }
            }
            else -> {
                Log.w("GeminiRepo", "Unsupported audio format: ${audioFile.extension}")
                return null
            }
        }
        
        Log.d("GeminiRepo", "Uploading converted audio to Gemini: ${audioFile.name} ($mimeType)")
        val uri = filesApiClient.uploadFile(audioFile, mimeType) ?: return null
        
        Log.d("GeminiRepo", "Audio upload SUCCESS. URI: $uri")
        return Pair(uri, mimeType)

    }

    private fun extractKeyFrames(
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

    /**
     * Converts raw PCM (16-bit, 16kHz, Mono) to WAV for Gemini compatibility.
     */
    private fun convertPcmToWav(pcmFile: File): File? {
        return try {
            val wavFile = File(pcmFile.parent, pcmFile.nameWithoutExtension + ".wav")
            val dataLen = pcmFile.length()
            
            val sampleRate = 16000 // AppConstants.Audio.SAMPLE_RATE
            val channels = 1
            val byteRate = sampleRate * channels * 2
            
            val header = ByteArray(44)
            val totalDataLen = dataLen + 36

            
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
            
            header[12] = 'f'.code.toByte() // 'fmt ' chunk
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            
            header[20] = 1 // Format = PCM
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
            
            header[32] = 2.toByte() // block align (Mono 16-bit = 2 bytes per sample)
            header[33] = 0
            
            header[34] = 16 // bits per sample
            header[35] = 0
            
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            
            header[40] = (dataLen and 0xff).toByte()
            header[41] = ((dataLen shr 8) and 0xff).toByte()
            header[42] = ((dataLen shr 16) and 0xff).toByte()
            header[43] = ((dataLen shr 24) and 0xff).toByte()
            
            FileInputStream(pcmFile).use { input ->
                FileOutputStream(wavFile).use { out ->
                    out.write(header)
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                }
            }
            
            Log.d("GeminiRepo", "Converted PCM to WAV: ${wavFile.absolutePath} (${wavFile.length()} bytes)")
            wavFile
        } catch (e: Exception) {
            Log.e("GeminiRepo", "PCM to WAV conversion failed", e)
            null
        }
    }
}
