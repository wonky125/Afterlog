package com.hackathon.afterlog.service

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.data.local.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val timeManager: TimeManager,
    private val cameraUseCaseManager: CameraUseCaseManager,
    private val fileManager: FileManager
) {
    private var activeRecording: Recording? = null
    // Executor removed (using Coroutines)
    private var isCapturing = false
    private var currentSessionId: String? = null

    // Rolling Buffer: Stores paths of recent temp files
    // Capacity = 6 (3 minutes worth of 30s chunks)
    private val tempBuffer = ConcurrentLinkedDeque<File>()
    private val bufferMutex = Mutex() // Thread-safe buffer access
    private val bufferCapacity = 6
    private val chunkDurationMillis = 30_000L
    private var currentScope: CoroutineScope? = null // Store for lifecycle-aware operations

    // bindCamera REMOVED -> handled by CameraUseCaseManager

    @SuppressLint("MissingPermission") // Video only, no audio
    fun startRecordingLoop(sessionId: String, scope: CoroutineScope) {
        if (isCapturing) return
        isCapturing = true
        currentSessionId = sessionId
        currentScope = scope // Store for saveBufferForEvent
        
        Log.d("VideoManager", "Starting video loop for session: $sessionId")

        scope.launch(Dispatchers.IO) {
            // Wait for VideoCapture to be bound (check via manager)
            var items = 0
            while (cameraUseCaseManager.getVideoCapture() == null && isActive && isCapturing) {
                if (items % 10 == 0) Log.d("VideoManager", "Waiting for camera binding...")
                delay(200)
                items++
                // Timeout after 10 seconds
                if (items > 50) {
                    Log.w("VideoManager", "Camera binding timeout! Switching to Mock Mode.")
                    runMockVideoLoop(sessionId, this)
                    return@launch
                }
            }

            var nextRecordTime = System.currentTimeMillis()

            while (isActive && isCapturing) {
                val now = System.currentTimeMillis()

                if (now >= nextRecordTime) {
                    withContext(Dispatchers.Main) {
                         recordChunk(sessionId)
                    }
                    nextRecordTime += chunkDurationMillis
                    
                    // Wait until next chunk start time
                    delay((nextRecordTime - System.currentTimeMillis()).coerceAtLeast(0))
                    
                    withContext(Dispatchers.Main) {
                        stopCurrentRecording()
                    }
                } else {
                    delay(100)
                }
            }
        }
    }

    private suspend fun runMockVideoLoop(sessionId: String, scope: CoroutineScope) {
        val random = java.util.Random()
        Log.i("VideoManager", "Starting Mock Video Loop")

        while (scope.isActive && isCapturing) {
            val timestamp = timeManager.getCurrentTime()
            val tempFile = fileManager.getTempVideoFile(sessionId, timestamp)
            
            // Create dummy file
            try {
                // fileManager.createDummyVideoFile(tempFile) // Removed: Method does not exist
                // We'll just write randomness here for simplicity
                java.io.FileOutputStream(tempFile).use { out ->
                    val bytes = ByteArray(1024 * 100) // 100KB dummy
                    random.nextBytes(bytes)
                    out.write(bytes)
                }
                Log.d("VideoManager", "Created Mock Video Chunk: ${tempFile.name}")
                addToBuffer(tempFile)
                
            } catch (e: Exception) {
                Log.e("VideoManager", "Failed to create mock video", e)
            }

            delay(chunkDurationMillis)
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordChunk(sessionId: String) {
        val videoCapture = cameraUseCaseManager.getVideoCapture() ?: return
        
        val timestamp = timeManager.getCurrentTime()
        val tempFile = fileManager.getTempVideoFile(sessionId, timestamp)
        
        val outputOptions = FileOutputOptions.Builder(tempFile).build()
        
        try {
            activeRecording = videoCapture.output
                .prepareRecording(context, outputOptions)
                // .withAudioEnabled() // DISABLED: Silent Video Only (Separate Stream Arch)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when(recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d("VideoManager", "Started chunk: ${tempFile.name}")
                            // Do NOT add to buffer here. Only add fully finalized files.
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                Log.d("VideoManager", "Finalized chunk: ${tempFile.name}")
                                // Add to buffer ONLY when file is complete and safe (MP4 header written)
                                currentScope?.launch { addToBuffer(tempFile) } 
                            } else {
                                // If error, file might be partial.
                                Log.e("VideoManager", "Video capture error: ${recordEvent.error}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
             Log.e("VideoManager", "Failed to start recording", e)
        }
    }

    private fun stopCurrentRecording() {
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            Log.e("VideoManager", "Error stopping recording", e)
        }
        activeRecording = null
    }

    private suspend fun addToBuffer(file: File) {
        bufferMutex.withLock {
            tempBuffer.addLast(file)
            // Trim buffer if exceeding capacity
            while (tempBuffer.size > bufferCapacity) {
                val oldFile = tempBuffer.pollFirst()
                oldFile?.delete()
                Log.d("VideoManager", "Deleted old chunk: ${oldFile?.name}")
            }
        }
    }

    /**
     * Triggered by AudioMonitor (Scream Event).
     * Moves all current buffer files to permanent storage as VIDEO_HIGHLIGHT.
     */
    fun saveBufferForEvent(sessionId: String) {
        Log.i("VideoManager", "Saving buffer for Scream Event!")
        
        // Copy logic (Snapshot of current buffer)
        // PROTECT WITH MUTEX to prevent ConcurrentModificationException
        // We launch a coroutine to do the copy I/O work
        currentScope?.launch(Dispatchers.IO) {
            val snapshot = bufferMutex.withLock { 
                 tempBuffer.toList() 
            }
            
            snapshot.forEach { tempFile ->
                if (tempFile.exists()) {
                    val timestamp = timeManager.getCurrentTime()
                    val permFile = fileManager.getHighlightVideoFile(sessionId, tempFile.name)
                    
                    try {
                        tempFile.copyTo(permFile, overwrite = true)
                        
                        // Register to DB as VIDEO_HIGHLIGHT for stitching
                        repository.logMedia(
                            sessionId = sessionId,
                            type = MediaType.VIDEO_HIGHLIGHT, 
                            filePath = permFile.absolutePath,
                            decibel = null,
                            timestamp = timestamp
                        )
                        Log.d("VideoManager", "Saved VIDEO_HIGHLIGHT: ${permFile.name}")
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
        // Clean up temp files? No, keep rolling buffer until end of session or explicit clear.
        Log.d("VideoManager", "Video recording stopped")
        // Cleanup old buffer files to save space
        activeRecording = null
        // Note: We don't delete files here to allow post-session analysis if needed, 
        // but for safety we can clear the memory reference.
        tempBuffer.clear()
    }
}
