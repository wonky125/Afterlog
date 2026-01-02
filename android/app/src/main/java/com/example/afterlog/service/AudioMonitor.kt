package com.example.afterlog.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.afterlog.data.local.entities.MediaType
import com.example.afterlog.data.repository.LocalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

@Singleton
class AudioMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalRepository,
    private val timeManager: TimeManager,
    private val videoManager: VideoManager
) {
    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    
    // Config - Lowered to 50 for emulator testing (production: 80)
    private val screamThresholdDb = 50.0

    fun startMonitoring(sessionId: String, scope: CoroutineScope) {
        if (isRecording) return

        try {
            val audioFile = File(context.getExternalFilesDir(null), "session_media/audio_${sessionId}.m4a")
            audioFile.parentFile?.mkdirs()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            Log.d("AudioMonitor", "Full audio recording started: ${audioFile.absolutePath}")
            
            // Log the audio file chunk entry
            scope.launch {
                repository.logMedia(
                    sessionId = sessionId,
                    type = MediaType.AUDIO_CHUNK,
                    filePath = audioFile.absolutePath,
                    decibel = null,
                    timestamp = timeManager.getCurrentTime()
                )
            }

            // Polling for Screams
            recordingJob = scope.launch(Dispatchers.IO) {
                while (isActive && isRecording) {
                    val maxAmplitude = mediaRecorder?.maxAmplitude ?: 0
                    if (maxAmplitude > 0) {
                        val db = 20 * log10(maxAmplitude.toDouble())
                        // Debug log (enabled for testing)
                        if (db > 40) Log.d("AudioMonitor", "Current dB: $db")

                        // Use configurable threshold (lowered to 50 for testing, default 80)
                        if (db > screamThresholdDb) {
                            // Send Broadcast to UI (Explicit)
                            val intent = android.content.Intent("com.example.afterlog.SCREAM_DETECTED")
                            intent.putExtra("db", db.toInt())
                            intent.setPackage(context.packageName) // Fix: Explicit Intent for Android 8+
                            context.sendBroadcast(intent)

                            handleScreamEvent(sessionId, db.toInt())
                            // Debounce
                            delay(5000) 
                        }
                    }
                    delay(100) // Poll every 100ms
                }
            }

        } catch (e: Exception) {
            Log.e("AudioMonitor", "Error starting MediaRecorder", e)
            cleanup()
        }
    }

    private fun handleScreamEvent(sessionId: String, db: Int) {
        Log.i("AudioMonitor", "SCREAM DETECTED! dB: $db")
        
        // Trigger Video Buffer Save
        videoManager.saveBufferForEvent(sessionId)
        
        // Use GlobalScope for fire-and-forget DB operation
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            repository.logMedia(
                sessionId = sessionId,
                type = MediaType.SCREAM_EVENT,
                filePath = "TIMESTAMP_MARKER", // Just a marker
                decibel = db,
                timestamp = timeManager.getCurrentTime()
            )
        }
    }

    fun stopMonitoring() {
        cleanup()
    }

    private fun cleanup() {
        try {
            isRecording = false
            recordingJob?.cancel()
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("AudioMonitor", "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            Log.d("AudioMonitor", "Audio monitoring stopped")
        }
    }
}
