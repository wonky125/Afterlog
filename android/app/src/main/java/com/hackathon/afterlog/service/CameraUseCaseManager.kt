package com.hackathon.afterlog.service

import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
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
    private var preview: Preview? = null
    private var previewSurfaceProvider: Preview.SurfaceProvider? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var orientationListener: OrientationEventListener? = null
    private var currentRotation: Int = Surface.ROTATION_0

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
                
                // 2. Setup Preview
                preview = Preview.Builder()
                    .build()
                previewSurfaceProvider?.let { provider ->
                    preview?.setSurfaceProvider(provider)
                }

                // 3. Setup VideoCapture
                val qualitySelector = QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                currentRotation = getDisplayRotation()
                updateTargetRotation(currentRotation)
                startOrientationListener()
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // 4. Bind ALL use cases together
                cameraProvider?.unbindAll()
                
                try {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture!!,
                        videoCapture!!,
                        preview!!
                    )
                    Log.d(TAG, "All camera use cases bound successfully (Image + Video + Preview)")
                } catch (e: Exception) {
                    Log.e(TAG, "Binding failed, falling back without Preview", e)
                    try {
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageCapture!!,
                            videoCapture!!
                        )
                        Log.d(TAG, "Camera bound without Preview (Image + Video)")
                    } catch (fallbackError: Exception) {
                        Log.e(TAG, "Fallback binding failed, trying Video only", fallbackError)
                        try {
                            cameraProvider?.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                videoCapture!!
                            )
                            Log.d(TAG, "Camera bound with Video only")
                        } catch (videoOnlyError: Exception) {
                            Log.e(TAG, "Video-only binding failed", videoOnlyError)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun getImageCapture(): ImageCapture? = imageCapture
    fun getVideoCapture(): VideoCapture<Recorder>? = videoCapture

    fun setPreviewSurfaceProvider(surfaceProvider: Preview.SurfaceProvider?) {
        previewSurfaceProvider = surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)
    }

    private fun startOrientationListener() {
        if (orientationListener != null) return
        orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = resolveRotation(orientation)
                if (rotation != currentRotation) {
                    currentRotation = rotation
                    updateTargetRotation(rotation)
                }
            }
        }.also { listener ->
            if (listener.canDetectOrientation()) {
                listener.enable()
            }
        }
    }

    private fun updateTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        preview?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
    }

    private fun resolveRotation(orientation: Int): Int {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return currentRotation
        return when {
            orientation in 315..359 || orientation in 0..44 -> Surface.ROTATION_0
            orientation in 45..134 -> Surface.ROTATION_90
            orientation in 135..224 -> Surface.ROTATION_180
            else -> Surface.ROTATION_270
        }
    }

    private fun getDisplayRotation(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val rotation = windowManager?.defaultDisplay?.rotation
        if (rotation == null) {
            Log.w(TAG, "Display rotation unavailable, defaulting to ROTATION_0")
        }
        return rotation ?: Surface.ROTATION_0
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        imageCapture = null
        videoCapture = null
        preview = null
        previewSurfaceProvider = null
        orientationListener?.disable()
        orientationListener = null
        Log.d(TAG, "Camera resources released")
    }
}
