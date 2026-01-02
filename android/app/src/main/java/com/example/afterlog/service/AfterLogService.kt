package com.example.afterlog.service

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
import com.example.afterlog.MainActivity
import com.example.afterlog.R
import com.example.afterlog.data.repository.LocalRepository
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
        const val ACTION_SIMULATE_SCREAM = "com.example.afterlog.action.SIMULATE_SCREAM"
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
        // Do not wait for DB or coroutines.
        startForegroundServiceWithNotification("Initializing...")

        // 1. Handle Simulation Actions independently
        if (intent.action == ACTION_SIMULATE_SCREAM) {
            handleSimulateAction()
            return START_STICKY
        }

        // 2. Handle Session Initialization
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId.isNullOrEmpty()) {
            handleNewSession()
        } else {
            handleResumeSession(sessionId)
        }

        return START_STICKY
    }

    private fun handleSimulateAction() {
        Log.d(TAG, "Simulating Scream Event!")
        val targetSession = currentSessionId ?: "unknown_session"
        videoManager.saveBufferForEvent(targetSession)
        showToast("Simulation: Scream Event Triggered!")
        // Update notification to show we are recording/active
        startForegroundServiceWithNotification("Recording active...")
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
        // Update Notification to "Recording" state
        startForegroundServiceWithNotification("Listening for screams...")

        // Acquire WakeLock (Safety: 1 hour timeout to prevent infinite battery drain)
        acquireWakeLock()

        // NOTE: CameraManager (ImageCapture) disabled - conflicts with VideoManager binding
        // TODO: Post-hackathon: Merge into unified CameraUseCaseManager
        Log.d(TAG, "ImageCapture disabled (Video-only mode)")

        var isAudioStarted = false
        var isVideoStarted = false

        // Start Audio Monitoring
        // Start Audio Monitoring
        // Re-enabled: New AudioRecord-based implementation (Stable)
        // Start Audio Monitoring
        // Re-enabled: Separate Stream Architecture
        // AudioMonitor handles Full Audio Log (PCM).
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1000) // Small delay to let CameraX init first
                audioMonitor.startMonitoring(sessionId, this)
                withContext(Dispatchers.Main) {
                    isAudioStarted = true
                    Log.d(TAG, "Audio monitoring started (Separate Stream)")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Audio Init Failed", t)
                withContext(Dispatchers.Main) {
                     showToast("Audio Error: ${t.message}")
                }
            }
        }
        // Log.d(TAG, "AudioMonitor skipped (Consolidated into Video)")
        // showToast("Audio Disabled (Emulator Mode)") // Removed

        // Start Video Recording (Rolling Buffer)
        // RE-ENABLED: With Mock Audio, Video is safe to run.
        try {
            videoManager.bindCamera(this)
            // Launch in a safe scope to prevent app crash
            lifecycleScope.launch(kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
                 Log.e(TAG, "Video Coroutine Crash", e)
                 showToast("Video Crash: ${e.message}")
            }) {
                videoManager.startRecordingLoop(sessionId, this)
            }
            isVideoStarted = true
            Log.d(TAG, "Video recording loop started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
            showToast("Video Error: ${e.message}")
        }
        // showToast("Video Disabled (Audio Test Mode)")

        // 5. Error Handling Policy: Warn user if partial failure
        // 5. Error Handling Policy: Warn user if partial failure
        /*
        if (!isAudioStarted && !isVideoStarted) {
            Log.e(TAG, "Both Audio and Video failed to start! Stopping service.")
            showToast("Critical Error: Recording failed to start.")
            stopSelf()
            return
        } else if (!isAudioStarted || !isVideoStarted) {
            showToast("Warning: Recording started partially (Check logs).")
        }
        */

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
                // API 29: startForeground takes 3 args, but specific types might be optional or different.
                // Using 0 (manifest type) is safest if we don't need location.
                // However, Q introduced usage of types. 'camera' type was added in R?
                // Checking docs: 'camera' type added in Android 11 (API 30).
                // So on Q (29), we CANNOT pass FOREGROUND_SERVICE_TYPE_CAMERA.
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
        cameraManager.shutdown()
        
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
                acquire(4 * 60 * 60 * 1000L) // 2. Increased timeout to 4 hours (Safe for long sessions)
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WakeLock", e)
        }
    }
}
