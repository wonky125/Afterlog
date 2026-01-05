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
            
            val frames = extractKeyFrames(videoFiles, maxFrames = 12)
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
            return@withContext """{"headline":"SYSTEM ERROR","summary":"${e.localizedMessage}","atmosphere":"","timeline":[],"verdict":"Investigation aborted."}"""
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
                // Convert PCM to WAV before upload to ensure correct headers/format for Gemini
                val wavFile = convertPcmToWav(audioFile)
                if (wavFile != null) {
                    return uploadAudioToGemini(wavFile)
                }
                "audio/L16" 
            }
            else -> {
                Log.w("GeminiRepo", "Unsupported audio format: ${audioFile.extension}")
                return null
            }
        }
        
        Log.d("GeminiRepo", "Uploading converted audio to Gemini: ${audioFile.name} ($mimeType)")
        val uri = filesApiClient.uploadFile(audioFile, mimeType)
        
        return if (uri != null) {
            Log.d("GeminiRepo", "Audio upload SUCCESS. URI: $uri")
            Pair(uri, mimeType)
        } else {
            Log.e("GeminiRepo", "Audio upload FAILED. URI is null.")
            null
        }
    }

    private fun extractKeyFrames(videoFiles: List<File>, maxFrames: Int): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        
        try {
            val targetFile = videoFiles.lastOrNull() 
            if (targetFile == null) {
                return emptyList()
            }
            
            retriever.setDataSource(targetFile.absolutePath)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            if (durationMs > 0) {
                val interval = durationMs / (maxFrames + 1)
                for (i in 1..maxFrames) {
                    val timeUs = (interval * i) * 1000
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                         bitmaps.add(bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Frame extraction failed", e)
        } finally {
            retriever.release()
        }
        
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
            val bitrate = sampleRate * channels * 16
            
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
            
            header[32] = (channels * 16 / 8).toByte() // block align (Should be 2 for Mono 16-bit)
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
