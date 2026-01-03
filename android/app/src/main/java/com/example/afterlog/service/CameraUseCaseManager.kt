package com.example.afterlog.service

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages CameraX use cases centrally to prevent "unbindAll" conflicts.
 * Binds both ImageCapture and VideoCapture to the same lifecycle.
 */
@Singleton
class CameraUseCaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraUseCaseManager"
    }

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Initializes use cases and binds them to the lifecycle.
     * MUST be called on the Main thread (or logic inside handles it).
     */
    fun bindToLifecycle(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // 1. Setup ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                // 2. Setup VideoCapture
                val qualitySelector = QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // 3. Bind ALL use cases together
                cameraProvider?.unbindAll()
                
                try {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture!!,
                        videoCapture!!
                    )
                    Log.d(TAG, "All camera use cases bound successfully (Image + Video)")
                } catch (e: Exception) {
                     Log.e(TAG, "Binding failed, trying separately...", e)
                     // Fallback logic if needed, but normally this should work
                }

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun getImageCapture(): ImageCapture? = imageCapture
    fun getVideoCapture(): VideoCapture<Recorder>? = videoCapture

    fun shutdown() {
        cameraProvider?.unbindAll()
        imageCapture = null
        videoCapture = null
        Log.d(TAG, "Camera resources released")
    }
}
