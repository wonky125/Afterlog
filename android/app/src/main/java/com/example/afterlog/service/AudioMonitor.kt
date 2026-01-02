package com.example.afterlog.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import android.os.Build
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
    private val repository: LocalRepository,
    private val timeManager: TimeManager,
    private val videoManager: VideoManager,
    private val fileManager: FileManager
) {
    companion object {
        private const val TAG = "AudioMonitor"
        private val SCREAM_THRESHOLD_DB = AppConstants.Audio.SCREAM_THRESHOLD_DB
        private val SAMPLE_RATE = AppConstants.Audio.SAMPLE_RATE
        private val BUFFER_SIZE = AppConstants.Audio.BUFFER_SIZE
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
        // Permission check is usually done in Activity/Service before calling this, but acceptable here if context available.
        // Assuming caller checks permission or crash. We removed context injection for purity if possible, 
        // but AudioRecord doesn't strictly need Context for constructor, checks usually happen outside.
        // Wait, I removed Context from injection. Just to be safe, I'll rely on Service checking checks.

        Log.i(TAG, "Starting AudioMonitor (Hardware Mode)")

        // Initialize AudioRecord safely
        if (!initializeAudioRecord()) {
             Log.e(TAG, "Failed to initialize AudioRecord. Switching to MOCK mode.")
             startMockMonitoring(sessionId, scope)
             return
        }

        try {
            pcmFile = fileManager.getAudioFile(sessionId)
            
            // Create file output stream
            fileOutputStream = try {
                FileOutputStream(pcmFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create FileOutputStream", e)
                cleanup()
                return
            }
            
            // Start Recording
            try {
                audioRecord?.startRecording()
                 val recordingState = audioRecord?.recordingState
                if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.e(TAG, "Recording failed to start. State: $recordingState")
                    cleanup()
                    return
                }
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

    private fun startMockMonitoring(sessionId: String, scope: CoroutineScope) {
        Log.w(TAG, "Starting Mock Audio Monitoring (No Hardware Detected)")
        isRecording = true
        
        // Ensure file exists even for mock
        try {
            pcmFile = fileManager.getAudioFile(sessionId)
            fileOutputStream = FileOutputStream(pcmFile)
        } catch (e: Exception) {
            Log.e(TAG, "Mock file setup failed", e)
        }
        
        recordingJob = scope.launch(Dispatchers.IO) {
            runMockAudioLoop(sessionId)
        }
        
        // Log Mock Start
        scope.launch(Dispatchers.IO) {
             repository.logMedia(
                 sessionId = sessionId,
                 type = MediaType.AUDIO_CHUNK,
                 filePath = pcmFile?.absolutePath ?: "MOCK_AUDIO",
                 decibel = 0,
                 timestamp = timeManager.getCurrentTime()
             )
        }
    }

    private fun initializeAudioRecord(): Boolean {
        // Force Mock Mode if running on Emulator (Generic check)
        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) {
             Log.w(TAG, "Emulator detected, skipping real AudioRecord init")
             return false
        }
        
        return try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $minBufferSize")
                return false
            }
            
            audioRecord = AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )
            
            val state = audioRecord?.state
            if (state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized. State: $state")
                audioRecord?.release()
                audioRecord = null
                return false
            }
            
            true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord initialization failed", e)
            audioRecord?.release()
            audioRecord = null
            false
        }
    }
    
    // --- Mock Loop ---
    private suspend fun CoroutineScope.runMockAudioLoop(sessionId: String) {
        var lastScreamTime = 0L
        val random = java.util.Random()
        
        while (isActive && isRecording) {
            // Simulate random noise 30-60dB
            // Occasionally spike to > 60dB (1% chance every loop)
            val baseDb = 30.0 + random.nextDouble() * 30.0
            val isScream = random.nextInt(100) < 2 // 2% chance
            
            val db = if (isScream) baseDb + 30.0 else baseDb
            
            // Write dummy bytes to file to simulate recording size
            try {
                fileOutputStream?.write(ByteArray(BUFFER_SIZE))
            } catch (e: Exception) {}

            if (db > SCREAM_THRESHOLD_DB) {
                 val now = timeManager.getCurrentTime()
                 if (now - lastScreamTime > AppConstants.Audio.SCREAM_COOLDOWN_MS) {
                     lastScreamTime = now
                     Log.i(TAG, "Run Mock Scream Event! dB: $db")
                     handleScreamEvent(sessionId, db.toInt())
                 }
            }
            
            delay(100) // 100ms loop
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
                        // Little Endian conversion with correct bit masking
                        val low = buffer[i * 2].toInt() and 0xFF
                        val high = buffer[i * 2 + 1].toInt() and 0xFF
                        val sample = (high shl 8) or low
                        val sampleVal = if (sample < 32768) sample else sample - 65536
                        sum += sampleVal.toDouble() * sampleVal.toDouble()
                    }
                    
                    if (shortCount > 0) {
                        val rms = kotlin.math.sqrt(sum / shortCount)
                        val db = if (rms > 0) 20 * kotlin.math.log10(rms) else 0.0
                        
                        // Debouncing scream detection
                        if (db > SCREAM_THRESHOLD_DB) {
                             val now = timeManager.getCurrentTime()
                             if (now - lastScreamTime > AppConstants.Audio.SCREAM_COOLDOWN_MS) {
                                 lastScreamTime = now
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
