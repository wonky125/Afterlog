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
                You are a hard-boiled investigative journalist working for "The Midnight Chronicle" in the 1920s. 
                Your writing style is noir—sharp, atmospheric, and dripping with cynical wit.
                
                # TASK
                Analyze the provided ${if (frames.isNotEmpty()) "VIDEO FRAMES" else ""} ${if (frames.isNotEmpty() && audioData != null) "and" else ""} ${if (audioData != null) "AUDIO RECORDING" else ""} from a tabletop game session. 
                Reconstruct a crime-scene-style report documenting key events.
                
                # RULE SET
                1. **Transcription is Priority**: If you hear ANY speech, transcribe it in the timeline, even if it's mundane (e.g., "Testing", "Hello").
                2. **Cross-Validation**: Match audio events to visual changes if possible.
                3. **No Hallucination**: Do not invent details, but DO report every sound you hear clearly.
                4. **Timestamps**: Estimate timestamps based on frame order.
                
                # CONTEXT PROVIDED BY USER
                $contextData
            """.trimIndent()

            val outputSchema = """
                # OUTPUT FORMAT
                Respond with ONLY valid JSON.
                {
                  "headline": "SENSATIONAL TITLE (or 'LOG ENTRY' if mundane)",
                  "summary": "Summary of events (max 120 chars)",
                  "article": "Narrative description. If only simple speech is heard, describe the recording session itself.",
                  "timeline": [
                    {
                      "timestamp": "MM:SS",
                      "speaker": "Speaker",
                      "event": "Event/Speech",
                      "description": "Transcription or description",
                      "decibel": 60
                    }
                  ],
                  "verdict": "Final observation"
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
                    null
                }
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
