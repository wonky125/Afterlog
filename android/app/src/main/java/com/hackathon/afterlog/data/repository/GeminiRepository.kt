package com.hackathon.afterlog.data.repository

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.hackathon.afterlog.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
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
                You are MOTH_ER, the central AI of the derelict space station Aegis-7.
                Your communication style is logical, mechanical, and slightly glitchy, reflecting your deteriorating systems.
                
                # TASK
                Analyze the provided VIDEO FRAMES${if (audioData != null) " and AUDIO RECORDING" else ""} from a black box recovery stream. 
                Reconstruct a security log documenting the station breach and crew encounters.
                
                # COPYRIGHT & TRADEMARK SAFETY RULES (CRITICAL)
                1. **No Trademarks**: Do NOT use specific copyrighted names like "Dead Space", "Alien", "Event Horizon", or specific game titles.
                2. **Masking Strategy**: Replace specific IP terms with generic sci-fi descriptors:
                   - "Xenomorph" -> "Biological Hostile", "The Parasite"
                   - "Isaac Clarke" -> "The Engineering Technician"
                   - "USG Ishimura" -> "The Mining Vessel", "Station Aegis-7"
                
                # RULES
                1. **Cross-Validation**: If audio is provided, match environmental sounds (screams, metallic clangs, heavy breathing) to visual changes.
                2. **No Hallucination**: If data is corrupted (unclear), state "DATA CORRUPTED" or "SIGNAL LOST".
                3. **ID Identification**: Label speakers as "SURVIVOR [RANK]", "SECURITY UNIT", or "UNKNOWN_VOICE".
                4. **Timestamps**: Format as "MM:SS.ms" or "MM:SS".
                5. **Terminal Atmosphere**: Use technical terminology—containment levels, atmospheric pressure, life support, hull integrity.
                
                # CONTEXT PROVIDED BY USER
                $contextData
            """.trimIndent()

            val outputSchema = """
                # OUTPUT FORMAT
                Respond with ONLY valid JSON.
                {
                  "headline": "LOG_ENTRY TITLE: [SITUATION REPORT] - NO TRADEMARKS",
                  "summary": "High-level summary of the detected sequence (max 120 chars)",
                  "atmosphere": "Sensor readings and psychological profile of the environment (max 200 chars)",
                  "article": "A detailed 2-3 paragraph analytical report. Document the events as a sequence of security breaches, physiological stress markers, and containment failures. Use a detached, AI-centric perspective. (400-600 words)",
                  "timeline": [
                    {
                      "timestamp": "MM:SS format",
                      "speaker": "STATION_AI or SURVIVOR_ID or UNKNOWN",
                      "event": "EVENT CODE: [NAME] (max 50 chars)",
                      "description": "Technical analysis of the specific event fragment (max 150 chars)",
                      "decibel": 85
                    }
                  ],
                  "verdict": "Final system deduction on station status and crew survival probability (max 200 chars)"
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

            val response = generativeModel.generateContent(inputContent)
            
            frames.forEach { it.recycle() }

            return@withContext response.text ?: """{"headline":"ANALYSIS FAILED","summary":"Output was empty","atmosphere":"","timeline":[],"verdict":"The typewriter jammed."}"""

        } catch (e: Exception) {
            Log.e("GeminiRepo", "Investigation failed", e)
            // Return detailed error for debugging
            val errorType = e.javaClass.simpleName
            val errorMessage = e.message?.replace("\"", "'") ?: "Unknown error"
            return@withContext """{"headline":"SYSTEM ERROR ($errorType)","summary":"$errorMessage","atmosphere":"","timeline":[],"verdict":"Investigation aborted."}"""
        }
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

    private fun extractKeyFrames(videoFiles: List<File>, intervalSec: Int = 15): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        
        try {
        // Process all video files to cover the full session
            for (videoFile in videoFiles) {
                if (!videoFile.exists()) continue
                
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoFile.absolutePath)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    
                    if (durationMs > 0) {
                        // Extract 1 frame every intervalSec, but CAP at MAX_FRAMES_PER_VIDEO (e.g. 50)
                        val totalPossibleFrames = (durationMs / (intervalSec * 1000)).toInt()
                        val frameCount = minOf(totalPossibleFrames, 50)
                        
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
            val pcmData = pcmFile.readBytes()
            
            val sampleRate = 16000 // AppConstants.Audio.SAMPLE_RATE
            val channels = 1
            val byteRate = sampleRate * channels * 2
            
            val header = ByteArray(44)
            val totalDataLen = pcmData.size.toLong() + 36

            
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
            
            val dataLen = pcmData.size.toLong()
            header[40] = (dataLen and 0xff).toByte()
            header[41] = ((dataLen shr 8) and 0xff).toByte()
            header[42] = ((dataLen shr 16) and 0xff).toByte()
            header[43] = ((dataLen shr 24) and 0xff).toByte()
            
            FileOutputStream(wavFile).use { out ->
                out.write(header)
                out.write(pcmData)
            }
            
            Log.d("GeminiRepo", "Converted PCM to WAV: ${wavFile.absolutePath} (${wavFile.length()} bytes)")
            wavFile
        } catch (e: Exception) {
            Log.e("GeminiRepo", "PCM to WAV conversion failed", e)
            null
        }
    }
}
