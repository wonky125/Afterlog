package com.hackathon.afterlog.data.repository.gemini

import android.content.Context
import android.util.Log
import com.hackathon.afterlog.data.remote.GeminiFilesApiClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min

class GeminiAudioUtils(
    private val filesApiClient: GeminiFilesApiClient,
    private val context: Context
) {
    suspend fun uploadAudioToGemini(audioFile: File): Pair<String, String>? {
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
                        if (!wavFile.delete()) {
                            Log.w("GeminiRepo", "Failed to delete temp WAV: ${wavFile.absolutePath}")
                        }
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

    fun createAudioSliceForGemini(
        audioFile: File,
        windowStartSec: Double,
        windowEndSec: Double,
        windowLabel: String
    ): File? {
        return try {
            val extension = audioFile.extension.lowercase()
            val dataOffset = when (extension) {
                "wav" -> WAV_HEADER_BYTES
                "pcm" -> 0L
                else -> {
                    Log.w("GeminiRepo", "Unsupported audio format for slicing: $extension")
                    return null
                }
            }
            val totalBytes = audioFile.length() - dataOffset
            if (totalBytes <= 0L) return null

            val bytesPerSecond = AUDIO_SAMPLE_RATE * PCM_BYTES_PER_SAMPLE
            val totalSeconds = totalBytes / bytesPerSecond.toDouble()
            val safeStart = windowStartSec.coerceAtLeast(0.0).coerceAtMost(totalSeconds)
            val safeEnd = windowEndSec.coerceAtLeast(safeStart).coerceAtMost(totalSeconds)
            val startByte = (safeStart * bytesPerSecond).toLong()
            val endByte = (safeEnd * bytesPerSecond).toLong()
            val segmentLen = endByte - startByte
            if (segmentLen <= 0L) return null

            val outputFile = File(
                context.cacheDir,
                "gemini_audio_${audioFile.nameWithoutExtension}_${safeStart.toInt()}_${safeEnd.toInt()}.wav"
            )
            val header = buildWavHeader(segmentLen)

            FileInputStream(audioFile).use { input ->
                input.channel.position(dataOffset + startByte)
                FileOutputStream(outputFile).use { out ->
                    out.write(header)
                    val buffer = ByteArray(16 * 1024)
                    var remaining = segmentLen
                    while (remaining > 0) {
                        val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }

            Log.d("GeminiRepo", "Audio slice created ($windowLabel): ${outputFile.name}")
            outputFile
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Audio slice failed ($windowLabel)", e)
            null
        }
    }

    private fun convertPcmToWav(pcmFile: File): File? {
        return try {
            val wavFile = File(pcmFile.parent, pcmFile.nameWithoutExtension + ".wav")
            val dataLen = pcmFile.length()
            val maxDataLen = 0xFFFFFFFFL - 36
            if (dataLen <= 0L || dataLen > maxDataLen) {
                Log.e("GeminiRepo", "PCM file too large for WAV header: $dataLen")
                return null
            }
            val header = buildWavHeader(dataLen)

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

    private fun buildWavHeader(dataLen: Long): ByteArray {
        val channels = AUDIO_CHANNELS
        val byteRate = AUDIO_SAMPLE_RATE * channels * PCM_BYTES_PER_SAMPLE
        val header = ByteArray(WAV_HEADER_BYTES.toInt())
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

        header[24] = (AUDIO_SAMPLE_RATE and 0xff).toByte()
        header[25] = ((AUDIO_SAMPLE_RATE shr 8) and 0xff).toByte()
        header[26] = ((AUDIO_SAMPLE_RATE shr 16) and 0xff).toByte()
        header[27] = ((AUDIO_SAMPLE_RATE shr 24) and 0xff).toByte()

        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        header[32] = (channels * PCM_BYTES_PER_SAMPLE).toByte()
        header[33] = 0

        header[34] = (PCM_BYTES_PER_SAMPLE * 8).toByte()
        header[35] = 0

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        header[40] = (dataLen and 0xff).toByte()
        header[41] = ((dataLen shr 8) and 0xff).toByte()
        header[42] = ((dataLen shr 16) and 0xff).toByte()
        header[43] = ((dataLen shr 24) and 0xff).toByte()

        return header
    }

    private companion object {
        private const val AUDIO_SAMPLE_RATE = 16000
        private const val AUDIO_CHANNELS = 1
        private const val PCM_BYTES_PER_SAMPLE = 2
        private const val WAV_HEADER_BYTES = 44L
    }
}
