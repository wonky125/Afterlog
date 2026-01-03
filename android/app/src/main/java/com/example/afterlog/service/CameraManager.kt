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
import com.example.afterlog.data.local.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    private val repository: LocalRepository,
    private val timeManager: TimeManager,
    private val cameraUseCaseManager: CameraUseCaseManager,
    private val fileManager: FileManager
) {
    private var cameraExecutor: ExecutorService? = null
    private var captureJob: Job? = null
    private var isCapturing = false
    private var currentScope: CoroutineScope? = null

    // Removed bindCamera(), now handled by CameraUseCaseManager elsewhere

    private fun ensureExecutor(): ExecutorService {
        if (cameraExecutor?.isShutdown != false) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        return cameraExecutor!!
    }

    fun startCapturing(sessionId: String, scope: CoroutineScope) {
        currentScope = scope
        if (isCapturing) return
        isCapturing = true
        Log.d("CameraManager", "Started explicit capturing loop")

        captureJob = scope.launch(Dispatchers.IO) {
            while (isActive && isCapturing) {
                takePicture(sessionId)
                delay(AppConstants.Camera.TIMELAPSE_INTERVAL_MS) 
            }
        }
    }

    fun stopCapturing() {
        isCapturing = false
        captureJob?.cancel()
        currentScope = null
        Log.d("CameraManager", "Stopped capturing loop")
    }

    private fun takePicture(sessionId: String) {
        val imageCapture = cameraUseCaseManager.getImageCapture()
        
        // Use TimeManager for consolidated time
        val timestamp = timeManager.getCurrentTime()
        val photoFile = fileManager.getImageFile(sessionId, timestamp)

        if (imageCapture == null) {
            Log.w("CameraManager", "ImageCapture is null, using Mock Camera")
            saveMockImage(sessionId, photoFile, timestamp)
            return
        }

        // Output Options
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        try {
            imageCapture.takePicture(
                outputOptions,
                ensureExecutor(), // Use safe executor getter
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CameraManager", "Photo capture failed: ${exc.message}", exc)
                        // Fallback to mock on error
                        saveMockImage(sessionId, photoFile, timestamp)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d("CameraManager", "Photo capture succeeded: ${photoFile.absolutePath}")
                        logImageToDb(sessionId, photoFile.absolutePath, timestamp)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CameraManager", "Capture request failed", e)
            saveMockImage(sessionId, photoFile, timestamp)
        }
    }

    private fun saveMockImage(sessionId: String, file: File, timestamp: Long) {
        currentScope?.launch(Dispatchers.IO) {
            try {
                // Create a simple black bitmap
                val bitmap = android.graphics.Bitmap.createBitmap(640, 480, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.DKGRAY)
                
                // Add text timestamp
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                }
                canvas.drawText("MOCK CAM", 50f, 100f, paint)
                canvas.drawText("$timestamp", 50f, 160f, paint)

                // Save to file
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                Log.d("CameraManager", "Mock photo saved: ${file.absolutePath}")
                logImageToDb(sessionId, file.absolutePath, timestamp)
                
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to save mock image", e)
            }
        }
    }

    private fun logImageToDb(sessionId: String, path: String, timestamp: Long) {
        currentScope?.launch(Dispatchers.IO) {
            try {
                repository.logMedia(
                    sessionId = sessionId,
                    type = MediaType.IMAGE,
                    filePath = path,
                    decibel = null,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                Log.e("CameraManager", "Failed to log image", e)
            }
        }
    }

    fun shutdown() {
        cameraExecutor?.shutdownNow()
        cameraExecutor = null
    }
}
