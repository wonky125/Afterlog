package com.example.afterlog.service

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.afterlog.data.local.entities.MediaType
import com.example.afterlog.data.repository.LocalRepository
import com.example.afterlog.service.TimeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalRepository,
    private val timeManager: TimeManager
) {
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var captureJob: Job? = null
    private var isCapturing = false

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                
                // Preview is not needed for background capture, just ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )
                Log.d("CameraManager", "Camera bound to lifecycle")

            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startCapturing(sessionId: String, scope: CoroutineScope) {
        if (isCapturing) return
        isCapturing = true
        Log.d("CameraManager", "Started explicit capturing loop")

        captureJob = scope.launch(Dispatchers.IO) {
            while (isActive && isCapturing) {
                takePicture(sessionId)
                delay(5000) // 5 seconds interval
            }
        }
    }

    fun stopCapturing() {
        isCapturing = false
        captureJob?.cancel()
        Log.d("CameraManager", "Stopped capturing loop")
    }

    private fun takePicture(sessionId: String) {
        val imageCapture = imageCapture ?: return

        // Create file with consolidated TimeManager
        val timestamp = timeManager.getCurrentTime()
        val photoFile = File(
            context.getExternalFilesDir(null),
            "session_media/${sessionId}_${timestamp}.jpg"
        )
        
        // Ensure directory exists
        photoFile.parentFile?.mkdirs()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraManager", "Photo capture succeeded: ${photoFile.absolutePath}")
                    // Save to DB via Repository
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.logMedia(
                            sessionId = sessionId,
                            type = MediaType.IMAGE,
                            filePath = photoFile.absolutePath,
                            decibel = null,
                            timestamp = timestamp // Pass explicit timestamp
                        )
                    }
                }
            }
        )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
