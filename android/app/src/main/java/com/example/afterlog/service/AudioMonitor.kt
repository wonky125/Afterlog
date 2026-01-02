package com.example.afterlog.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.afterlog.data.local.entities.MediaType
import com.example.afterlog.data.repository.LocalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalRepository,
    private val timeManager: TimeManager,
    private val videoManager: VideoManager
) {
    companion object {
        private const val TAG = "AudioMonitor"
        private const val SCREAM_THRESHOLD_DB = 50.0
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 1024
        private const val MOCK_DELAY_MS = 50L
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    private var currentScope: CoroutineScope? = null

    // File Output
    private var pcmFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

    // Config
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startMonitoring(sessionId: String, scope: CoroutineScope) {
        if (isRecording) {
            Log.w(TAG, "Already recording, skipping start")
            return
        }

        currentScope = scope

        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted!")
            return
        }

        Log.w(TAG, "Starting MOCK Audio Monitor (Hardware bypassed)")

        try {
            // Setup File with proper error handling
            val audioDir = getAudioDirectory()
            if (audioDir == null) {
                Log.e(TAG, "Failed to get audio directory")
                return
            }

            pcmFile = File(audioDir, "audio_${sessionId}.pcm")
            
            // Ensure parent directory exists
            pcmFile?.parentFile?.let { parentDir ->
                if (!parentDir.exists() && !parentDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: ${parentDir.absolutePath}")
                    return
                }
            }

            // Create file output stream
            fileOutputStream = try {
                FileOutputStream(pcmFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create FileOutputStream", e)
                cleanup()
                return
            }
            
            // SETUP HARDWARE AUDIO RECORD
            try {
                // Ensure Min Buffer Size
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
                if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Invalid buffer size")
                    return
                }
                
                audioRecord = AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    minBufferSize * 2
                )
                
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize")
                    cleanup()
                    return
                }
                
                audioRecord?.startRecording()
                Log.d(TAG, "Hardware AudioRecord started")
                
            } catch (e: Exception) {
                 Log.e(TAG, "Critical hardware error: ${e.message}")
                 cleanup()
                 return
            }

            isRecording = true

            // Log Start Entity
            scope.launch(Dispatchers.IO) {
                try {
                    repository.logMedia(
                        sessionId = sessionId,
                        type = MediaType.AUDIO_CHUNK,
                        filePath = pcmFile?.absolutePath ?: "",
                        decibel = null,
                        timestamp = timeManager.getCurrentTime()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log media start", e)
                }
            }

            // Start Hardware Polling loop
            recordingJob = scope.launch(Dispatchers.IO) {
                runHardwareAudioLoop(sessionId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting Audio Monitor", e)
            cleanup()
        }
    }

    private fun getAudioDirectory(): File? {
        return try {
            // Use internal files dir (more reliable than external)
            val internalDir = context.filesDir
            File(internalDir, "session_media").apply {
                if (!exists() && !mkdirs()) {
                    Log.e(TAG, "Failed to create session_media directory")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio directory", e)
            null
        }
    }

    private suspend fun CoroutineScope.runHardwareAudioLoop(sessionId: String) {
        val buffer = ByteArray(BUFFER_SIZE)
        var lastScreamTime = 0L
        
        try {
            while (isActive && isRecording) {
                val readBytes = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                
                if (readBytes > 0) {
                     // 1. Write to File
                    try {
                        fileOutputStream?.write(buffer, 0, readBytes)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing to file", e)
                        break
                    }

                    // 2. dB Calculation
                    // PCM 16bit = 2 bytes per sample
                    var sum = 0.0
                    val shortCount = readBytes / 2
                    for (i in 0 until shortCount) {
                        // Little Endian conversion
                        val low = buffer[i * 2].toInt()
                        val high = buffer[i * 2 + 1].toInt()
                        val sample = (high shl 8) or (low and 0xFF)
                        val sampleVal = if (sample < 32768) sample else sample - 65536
                        sum += sampleVal.toDouble() * sampleVal.toDouble()
                    }
                    
                    if (shortCount > 0) {
                        val rms = kotlin.math.sqrt(sum / shortCount)
                        val db = if (rms > 0) 20 * kotlin.math.log10(rms) else 0.0
                        
                        if (db > SCREAM_THRESHOLD_DB) {
                             val now = timeManager.getCurrentTime()
                             if (now - lastScreamTime > 5000) {
                                 lastScreamTime = now
                                 // Main thread callback? No, handleScreamEvent handles threading.
                                 handleScreamEvent(sessionId, db.toInt())
                             }
                        }
                    }
                } else {
                    // Read error or silence
                    if (readBytes < 0) {
                         Log.w(TAG, "AudioRecord read error: $readBytes")
                    }
                    delay(10) // Prevents busy loop on error
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in hardware audio loop", e)
        } finally {
            Log.d(TAG, "Hardware audio loop ended")
        }
    }

    fun handleScreamEvent(sessionId: String, db: Int) {
        Log.i(TAG, "🔊 SCREAM DETECTED! dB: $db")
        
        videoManager.saveBufferForEvent(sessionId)
        
        currentScope?.launch(Dispatchers.IO) {
            try {
                repository.logMedia(
                    sessionId = sessionId,
                    type = MediaType.SCREAM_EVENT,
                    filePath = "SCREAM_MARKER",
                    decibel = db,
                    timestamp = timeManager.getCurrentTime()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log scream event", e)
            }
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping audio monitoring...")
        cleanup()
    }

    private fun cleanup() {
        try {
            isRecording = false
            
            // Cancel job first
            recordingJob?.cancel()
            recordingJob = null

            // Stop and release audio record
            audioRecord?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioRecord", e)
                }
                release()
            }
            audioRecord = null

            // Close file stream
            fileOutputStream?.apply {
                try {
                    flush()
                    close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing FileOutputStream", e)
                }
            }
            fileOutputStream = null

            // Log final file info
            pcmFile?.let { file ->
                if (file.exists()) {
                    Log.d(TAG, "Audio file saved: ${file.absolutePath}, Size: ${file.length()} bytes")
                } else {
                    Log.w(TAG, "Audio file does not exist after cleanup")
                }
            }

            pcmFile = null
            currentScope = null

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
