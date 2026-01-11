package com.hackathon.afterlog.data.media

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper object for audio format conversion.
 * 
 * Handles PCM to WAV conversion for Media3 Transformer compatibility.
 * Used internally by VideoStitcher.
 */
internal object AudioConverter {
    private const val TAG = "AudioConverter"

    /**
     * Ensures the audio file is in a playable format for Media3.
     * Converts PCM files to WAV format.
     */
    fun ensurePlayableAudioFile(audioFile: File): File {
        if (audioFile.extension.equals("pcm", ignoreCase = true)) {
            val wavFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}_converted.wav")
            return convertPcmToWav(audioFile, wavFile) ?: audioFile
        }
        return audioFile
    }

    /**
     * Converts a raw PCM file to WAV format with proper headers.
     * 
     * Assumes 16kHz sample rate, mono channel, 16-bit PCM.
     */
    private fun convertPcmToWav(pcmFile: File, wavFile: File): File? {
        return try {
            if (wavFile.exists()) wavFile.delete()

            val sampleRate = 16000
            val channels = 1
            val byteRate = sampleRate * channels * 2
            val dataLen = pcmFile.length()
            val maxDataLen = 0xFFFFFFFFL - 36
            if (dataLen <= 0L || dataLen > maxDataLen) {
                Log.e(TAG, "PCM file too large for WAV header: $dataLen")
                return null
            }
            val totalDataLen = dataLen + 36

            val header = ByteArray(44)
            // RIFF header
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()

            // File size - 8
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()

            // WAVE header
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            // fmt subchunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()

            // Subchunk1 size (16 for PCM)
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0

            // Audio format (1 = PCM)
            header[20] = 1
            header[21] = 0

            // Number of channels
            header[22] = channels.toByte()
            header[23] = 0

            // Sample rate
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()

            // Byte rate
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()

            // Block align
            header[32] = (channels * 2).toByte()
            header[33] = 0

            // Bits per sample
            header[34] = 16
            header[35] = 0

            // data subchunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()

            // Data size
            header[40] = (dataLen and 0xff).toByte()
            header[41] = ((dataLen shr 8) and 0xff).toByte()
            header[42] = ((dataLen shr 16) and 0xff).toByte()
            header[43] = ((dataLen shr 24) and 0xff).toByte()

            FileOutputStream(wavFile).use { out ->
                out.write(header)
                FileInputStream(pcmFile).use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                }
            }

            Log.d(TAG, "PCM converted to WAV: ${wavFile.absolutePath}")
            wavFile
        } catch (e: Exception) {
            Log.e(TAG, "PCM to WAV conversion failed", e)
            null
        }
    }
}
