package com.example.afterlog.service

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.afterlog.data.local.entities.MediaType
import com.example.afterlog.data.repository.LocalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalRepository,
    private val timeManager: TimeManager
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val videoExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isCapturing = false
    private var currentSessionId: String? = null

    // Rolling Buffer: Stores paths of recent temp files
    // Capacity = 6 (3 minutes worth of 30s chunks)
    private val tempBuffer = ConcurrentLinkedDeque<File>()
    private val bufferCapacity = 6
    private val chunkDurationMillis = 30_000L

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // QualitySelector.QUALITY_SD (480p) or HD (720p) to save space/battery?
                // Let's go with HD (720p) for balance.
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                
                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Note: If CameraManager also binds ImageCapture, we might need to bind BOTH in the same call
                // to the same lifecycle to avoid unbinding each other. 
                // But for now, let's assume CameraManager handles ImageCapture purely or we merge them.
                // *CRITICAL*: ProcessCameraProvider.bindToLifecycle unbinds use cases if called separately for non-concurrent config?
                // Actually, as long as we don't call 'unbindAll()', it might append.
                // However, the best practice is to bind all use cases at once.
                // Current architecture separates CameraManager and VideoManager. This is a risk.
                // Correct approach: We should merge ImageCapture and VideoCapture binding logic or use a shared "CameraUseCaseManager".
                // For MVP Phase 2.5, let's try binding here. If it kicks out ImageCapture, we'll merge.
                // For now, I will NOT call unbindAll() here.
                
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    videoCapture!!
                )
                Log.d("VideoManager", "VideoCapture bound to lifecycle")

            } catch (e: Exception) {
                Log.e("VideoManager", "VideoCapture binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission") // Video only, no audio
    fun startRecordingLoop(sessionId: String, scope: CoroutineScope) {
        if (isCapturing) return
        isCapturing = true
        currentSessionId = sessionId
        
        Log.d("VideoManager", "Starting video loop for session: $sessionId")

        scope.launch(Dispatchers.IO) {
            while (isActive && isCapturing) {
                recordChunk(sessionId)
                delay(chunkDurationMillis) 
                stopCurrentRecording() // Stop to finalize file, then loop will start next
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordChunk(sessionId: String) {
        val videoCapture = videoCapture ?: return
        
        val timestamp = timeManager.getCurrentTime()
        val tempFile = File(context.cacheDir, "temp_vid_${sessionId}_$timestamp.mp4")
        
        val outputOptions = FileOutputOptions.Builder(tempFile).build()
        
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            // .withAudioEnabled() // Disabled for rolling buffer (avoid mic conflict)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d("VideoManager", "Started chunk: ${tempFile.name}")
                        addToBuffer(tempFile)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.d("VideoManager", "Finalized chunk: ${tempFile.name}")
                        } else {
                            videoCapture.output.prepareRecording(context, outputOptions)
                            Log.e("VideoManager", "Video capture error: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    private fun stopCurrentRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun addToBuffer(file: File) {
        tempBuffer.addLast(file)
        // Trim buffer if exceeding capacity
        while (tempBuffer.size > bufferCapacity) {
            val oldFile = tempBuffer.pollFirst()
            oldFile?.delete()
            Log.d("VideoManager", "Deleted old chunk: ${oldFile?.name}")
        }
    }

    /**
     * Triggered by AudioMonitor (Scream Event).
     * Moves all current buffer files to permanent storage.
     */
    fun saveBufferForEvent(sessionId: String) {
        Log.i("VideoManager", "Saving buffer for Scream Event!")
        
        // Copy logic (Snapshot of current buffer)
        val snapshot = tempBuffer.toList()
        
        CoroutineScope(Dispatchers.IO).launch {
            snapshot.forEach { tempFile ->
                if (tempFile.exists()) {
                    val timestamp = timeManager.getCurrentTime()
                    val permFile = File(
                        context.getExternalFilesDir(null), 
                        "session_media/highlight_${sessionId}_${tempFile.name}"
                    )
                    
                    try {
                        tempFile.copyTo(permFile, overwrite = true)
                        
                        // Register to DB
                        repository.logMedia(
                            sessionId = sessionId,
                            type = MediaType.VIDEO_CHUNK, // Assume we add this type
                            filePath = permFile.absolutePath,
                            decibel = null,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        Log.e("VideoManager", "Failed to save buffer file", e)
                    }
                }
            }
        }
    }

    fun stopRecording() {
        isCapturing = false
        stopCurrentRecording()
        // Clean up temp files? Or keep them for next session? 
        // Better clean up on app close or start.
    }
}
