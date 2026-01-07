package com.hackathon.afterlog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hackathon.afterlog.MainActivity
import com.hackathon.afterlog.R
import com.hackathon.afterlog.data.repository.LocalRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AfterLogService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "afterlog_recording_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_SESSION_ID = "session_id"
        const val ACTION_SIMULATE_SCREAM = "com.hackathon.afterlog.action.SIMULATE_SCREAM"
        private const val TAG = "AfterLogService"
    }

    @Inject
    lateinit var repository: LocalRepository

    @Inject
    lateinit var cameraManager: CameraManager

    @Inject
    lateinit var audioMonitor: AudioMonitor

    @Inject
    lateinit var videoManager: VideoManager

    @Inject
    lateinit var timeManager: TimeManager

    @Inject
    lateinit var cameraUseCaseManager: CameraUseCaseManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentSessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "AfterLogService created at ${timeManager.getCurrentTime()}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent == null) {
            Log.e(TAG, "Service started with null intent, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        // *CRITICAL FIX*: Start Foreground IMMEDIATELY to prevent crash (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundServiceWithNotification("Initializing AfterLog...")
        }

        // 1. Handle Simulation Actions independently
        if (intent.action == ACTION_SIMULATE_SCREAM) {
            handleSimulateAction()
            return START_STICKY
        }

        // 2. Handle Session Initialization
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId.isNullOrEmpty()) {
            Log.w(TAG, "No sessionId provided; creating a new session in service")
            handleNewSession()
        } else {
            handleResumeSession(sessionId)
        }

        return START_STICKY
    }

    private fun handleSimulateAction() {
        val targetSession = currentSessionId
        if (targetSession == null) {
            Log.e(TAG, "Cannot simulate scream: No active session")
            showToast("Error: No active session. Please start recording first.")
            return
        }
        
        Log.d(TAG, "Simulating Scream Event for session: $targetSession")
        videoManager.saveBufferForEvent(targetSession)
        showToast("Simulation: Scream Event Triggered!")
        updateNotification("Recording active (Event triggered!)")
    }

    private fun handleNewSession() {
        Log.i(TAG, "Starting new session...")
        lifecycleScope.launch {
            val newSessionId = repository.startNewSession()
            handleResumeSession(newSessionId)
        }
    }

    private fun handleResumeSession(sessionId: String) {
        currentSessionId = sessionId
        startRecording(sessionId)
    }

    private fun startRecording(sessionId: String) {
        // Update Notification to "Recording" state using UPDATE method, not startForeground
        updateNotification("Listening for screams...")

        // Acquire WakeLock (Safety: 1 hour timeout to prevent infinite battery drain)
        acquireWakeLock()

        // Bind unified camera use cases
        Log.d(TAG, "Binding Camera Use Cases...")
        lifecycleScope.launch(Dispatchers.Main) {
             cameraUseCaseManager.bindToLifecycle(this@AfterLogService)
        }

        // Start Audio Monitoring
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(500) // Small delay to let system settle
                audioMonitor.startMonitoring(sessionId, this) { db ->
                    // UI and Notification feedback on detection
                    showToast("Scream Detected! ($db dB)")
                    updateNotification("Recording active (Scream Detected!)")
                }
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Audio monitoring started")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Audio Init Failed", t)
                withContext(Dispatchers.Main) {
                     showToast("Audio Error: ${t.message}")
                }
            }
        }

        // Start Video Recording (Rolling Buffer)
        try {
            // No need to bind explicitly here, CameraUseCaseManager handles it.
            lifecycleScope.launch(kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
                 Log.e(TAG, "Video Coroutine Crash", e)
                 showToast("Video Crash: ${e.message}")
            }) {
                videoManager.startRecordingLoop(sessionId, this)
            }
            Log.d(TAG, "Video recording loop started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
            showToast("Video Error: ${e.message}")
        }

        // Start Photo Timelapse (if needed, CameraManager logic)
        cameraManager.startCapturing(sessionId, lifecycleScope)

        Log.i(TAG, "Recording started for session: $sessionId")
    }

    private fun startForegroundServiceWithNotification(contentText: String) {
        val notification = createNotification(contentText)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: Require explicit types
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29: startForeground takes 3 args, but specific types might be optional
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
             Log.e(TAG, "Failed to start foreground service", e)
             showToast("Error starting service: ${e.message}")
             stopSelf()
        }
    }

    // Helper to update notification without restarting foreground service
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showToast(message: String) {
        // Must run on Main Thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "AfterLogService destroying")
        
        // Stop all recording components
        audioMonitor.stopMonitoring()
        videoManager.stopRecording()
        cameraManager.stopCapturing()
        
        cameraManager.stopCapturing()
        cameraManager.shutdown()
        
        cameraUseCaseManager.shutdown()
        
        // End session in database
        currentSessionId?.let { sessionId ->
            lifecycleScope.launch {
                repository.endSession(sessionId)
                Log.d(TAG, "Session ended: $sessionId")
            }
        }

        // Release WakeLock (Critical cleanup)
        releaseWakeLock()
        
        // Stop foreground service properly
        stopForeground(STOP_FOREGROUND_REMOVE)

        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AfterLog Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when AfterLog is recording your session"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AfterLog Recording")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification_record)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AfterLog::RecordingWakeLock"
            ).apply {
                acquire(2 * 60 * 60 * 1000L) // 2 hours (Safety reduced from 4h)
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d(TAG, "WakeLock released")
                } else {
                     Log.w(TAG, "WakeLock already released")
                }
            }
        } catch (e: RuntimeException) {
            // release() double call safe guard across threads
            Log.e(TAG, "WakeLock release failed (already released?)", e)
        } finally {
            wakeLock = null
        }
    }
}
