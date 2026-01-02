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
import kotlinx.coroutines.launch
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

        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)

        if (sessionId == null) {
            // Check for simulation action
            if (intent?.action == ACTION_SIMULATE_SCREAM) {
                Log.d(TAG, "Simulating Scream Event!")
                videoManager.saveBufferForEvent(currentSessionId ?: "unknown_session")
                return START_STICKY
            }

            Log.e(TAG, "No session ID provided, starting new session")
            lifecycleScope.launch {
                currentSessionId = repository.startNewSession()
                startRecording(currentSessionId!!)
            }
        } else {
            currentSessionId = sessionId
            startRecording(sessionId)
        }

        return START_STICKY
    }

    private fun startRecording(sessionId: String) {
        // Start as foreground service
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Acquire WakeLock
        acquireWakeLock()

        // NOTE: CameraManager (ImageCapture) disabled - conflicts with VideoManager binding
        // TODO: Post-hackathon: Merge into unified CameraUseCaseManager
        Log.d(TAG, "ImageCapture disabled (Video-only mode)")

        // Start Audio Monitoring
        try {
            audioMonitor.startMonitoring(sessionId, lifecycleScope)
            Log.d(TAG, "Audio monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio monitoring", e)
        }

        // Start Video Recording (Rolling Buffer)
        try {
            videoManager.bindCamera(this)
            videoManager.startRecordingLoop(sessionId, lifecycleScope)
            Log.d(TAG, "Video recording loop started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
        }

        Log.i(TAG, "Recording started for session: $sessionId")
    }

    override fun onDestroy() {
        Log.d(TAG, "AfterLogService destroying")
        
        // Stop all recording components
        audioMonitor.stopMonitoring()
        videoManager.stopRecording()
        cameraManager.stopCapturing() // Re-enabled for proper cleanup
        cameraManager.shutdown() // Release executor
        
        // End session in database
        currentSessionId?.let { sessionId ->
            lifecycleScope.launch {
                repository.endSession(sessionId)
                Log.d(TAG, "Session ended: $sessionId")
            }
        }

        // Release WakeLock
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

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AfterLog Recording")
            .setContentText("Recording your session...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with proper icon
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
                acquire(60 * 60 * 1000L) // 1 hour max (failsafe)
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
