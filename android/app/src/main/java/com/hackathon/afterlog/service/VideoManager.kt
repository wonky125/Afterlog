package com.hackathon.afterlog.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.model.PerspectiveGuideConfig
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.data.local.FileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
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
    private var perspectiveGuide: PerspectiveGuideConfig? = null

    // Rolling Buffer: Stores paths of recent temp files
    // Capacity = 6 (3 minutes worth of 30s chunks)
    private val tempBuffer = ConcurrentLinkedDeque<File>()
    private val bufferMutex = Mutex() // Thread-safe buffer access
    private val bufferCapacity = 6
    private val chunkDurationMillis = 30_000L
    private var currentScope: CoroutineScope? = null // Store for lifecycle-aware operations
    
    // Centered Highlight Logic
    private var pendingHighlightCount = 0
    private val highlightMutex = Mutex()
    private var lastMotionHighlightMs = 0L
    private val motionMutex = Mutex()
    private val roiHighlightCooldownMs = 60_000L
    private val roiMotionThreshold = 18f
    private val highlightIndex = ConcurrentHashMap.newKeySet<String>()

    fun setPerspectiveGuide(config: PerspectiveGuideConfig) {
        perspectiveGuide = config
        Log.d("VideoManager", "Perspective guide applied: ${config.toSerializedString()}")
    }

    // bindCamera REMOVED -> handled by CameraUseCaseManager

    @SuppressLint("MissingPermission") // Video only, no audio
    fun startRecordingLoop(sessionId: String, scope: CoroutineScope) {
        if (isCapturing) return
        isCapturing = true
        currentSessionId = sessionId
        currentScope = scope // Store for saveBufferForEvent
        highlightIndex.clear()
        
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
        Log.i("VideoManager", "Starting Mock Video Loop")

        while (scope.isActive && isCapturing) {
            val timestamp = timeManager.getCurrentTime()
            val imageFile = fileManager.getImageFile(sessionId, timestamp)
            try {
                createMockImage(imageFile, timestamp)
                repository.logMedia(
                    sessionId = sessionId,
                    type = MediaType.IMAGE,
                    filePath = imageFile.absolutePath,
                    decibel = null,
                    timestamp = timestamp
                )
                Log.d("VideoManager", "Created mock image: ${imageFile.name}")
            } catch (e: Exception) {
                Log.e("VideoManager", "Failed to create mock image", e)
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
                                val scope = currentScope
                                if (scope != null && scope.isActive) {
                                    scope.launch {
                                        handleNewChunk(tempFile)
                                    }
                                }
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

    private suspend fun handleNewChunk(file: File) {
        val sessionId = currentSessionId ?: return
        
        // Check if this chunk is part of a "future" highlight window
        val isPending = highlightMutex.withLock {
            if (pendingHighlightCount > 0) {
                pendingHighlightCount--
                true
            } else {
                false
            }
        }

        val roiHighlight = shouldPromoteToHighlight(file)

        if (isPending || roiHighlight) {
            saveAsHighlight(sessionId, file)
        }

        addToBuffer(file)
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

    private data class RoiBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    private suspend fun shouldPromoteToHighlight(file: File): Boolean = withContext(Dispatchers.IO) {
        val guide = perspectiveGuide ?: return@withContext false
        if (!file.exists()) return@withContext false

        val now = timeManager.getCurrentTime()
        val onCooldown = motionMutex.withLock {
            now - lastMotionHighlightMs < roiHighlightCooldownMs
        }
        if (onCooldown) return@withContext false

        val retriever = MediaMetadataRetriever()
        var firstFrame: Bitmap? = null
        var secondFrame: Bitmap? = null
        try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) return@withContext false

            val durationUs = durationMs * 1000L
            val firstUs = minOf(500_000L, durationUs / 3)
            val secondUs = minOf(2_000_000L, (durationUs * 2) / 3)

            firstFrame = retriever.getFrameAtTime(firstUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            secondFrame = retriever.getFrameAtTime(secondUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (firstFrame == null || secondFrame == null) {
                return@withContext false
            }

            val score = computeMotionScore(firstFrame, secondFrame, guide)
            if (score >= roiMotionThreshold) {
                val accepted = motionMutex.withLock {
                    val stillOk = now - lastMotionHighlightMs >= roiHighlightCooldownMs
                    if (stillOk) {
                        lastMotionHighlightMs = now
                    }
                    stillOk
                }
                if (accepted) {
                    Log.d("VideoManager", "ROI motion highlight detected (score=$score) for ${file.name}")
                }
                return@withContext accepted
            }
            false
        } catch (e: Exception) {
            Log.e("VideoManager", "ROI motion check failed", e)
            false
        } finally {
            try { retriever.release() } catch (e: Exception) {}
            firstFrame?.recycle()
            secondFrame?.recycle()
        }
    }

    private fun computeMotionScore(
        firstFrame: Bitmap,
        secondFrame: Bitmap,
        guide: PerspectiveGuideConfig
    ): Float {
        var firstScaled: Bitmap? = null
        var secondScaled: Bitmap? = null
        return try {
            val targetWidth = 160
            val localFirstScaled = scaleForSampling(firstFrame, targetWidth)
            val localSecondScaled = scaleForSampling(secondFrame, targetWidth)
            firstScaled = localFirstScaled
            secondScaled = localSecondScaled

            val bounds = computeRoiBounds(guide, localFirstScaled.width, localFirstScaled.height)
            if (bounds.width <= 0 || bounds.height <= 0) {
                0f
            } else {
                val step = maxOf(1, minOf(bounds.width, bounds.height) / 32)
                var diffSum = 0f
                var count = 0

                for (y in bounds.top until bounds.bottom step step) {
                    for (x in bounds.left until bounds.right step step) {
                        val c1 = localFirstScaled.getPixel(x, y)
                        val c2 = localSecondScaled.getPixel(x, y)
                        val luma1 = luminance(c1)
                        val luma2 = luminance(c2)
                        diffSum += kotlin.math.abs(luma1 - luma2)
                        count++
                    }
                }

                if (count > 0) diffSum / count else 0f
            }
        } finally {
            if (firstScaled != null && firstScaled !== firstFrame) {
                firstScaled?.recycle()
            }
            if (secondScaled != null && secondScaled !== secondFrame) {
                secondScaled?.recycle()
            }
        }
    }

    private fun scaleForSampling(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width <= targetWidth) {
            return bitmap
        }
        val scaledHeight = (bitmap.height * (targetWidth / bitmap.width.toFloat()))
            .toInt()
            .coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, scaledHeight, true)
    }

    private fun computeRoiBounds(
        guide: PerspectiveGuideConfig,
        width: Int,
        height: Int
    ): RoiBounds {
        val minX = guide.points.minOf { it.x }.coerceIn(0f, 1f)
        val maxX = guide.points.maxOf { it.x }.coerceIn(0f, 1f)
        val minY = guide.points.minOf { it.y }.coerceIn(0f, 1f)
        val maxY = guide.points.maxOf { it.y }.coerceIn(0f, 1f)

        val left = (minX * width).toInt().coerceIn(0, width - 1)
        val right = (maxX * width).toInt().coerceIn(left + 1, width)
        val top = (minY * height).toInt().coerceIn(0, height - 1)
        val bottom = (maxY * height).toInt().coerceIn(top + 1, height)

        return RoiBounds(left = left, top = top, right = right, bottom = bottom)
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color).toFloat()
        val g = Color.green(color).toFloat()
        val b = Color.blue(color).toFloat()
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun saveAsHighlight(sessionId: String, file: File) {
        currentScope?.launch(Dispatchers.IO) {
            if (!file.exists()) return@launch
            
            val parsedTimestamp = extractTimestampFromFileName(file.name)
            val timestamp = parsedTimestamp ?: timeManager.getCurrentTime()
            Log.d("VideoManager", "Highlight timestamp=$timestamp parsed=$parsedTimestamp file=${file.name}")
            val permFile = fileManager.getHighlightVideoFile(sessionId, file.name)
            val highlightKey = permFile.absolutePath
            if (!highlightIndex.add(highlightKey)) {
                Log.d("VideoManager", "Highlight already saved, skipping duplicate: ${permFile.name}")
                return@launch
            }
            
            try {
                file.copyTo(permFile, overwrite = true)
                
                // Register to DB as VIDEO_HIGHLIGHT for stitching
                repository.logMedia(
                    sessionId = sessionId,
                    type = MediaType.VIDEO_HIGHLIGHT, 
                    filePath = permFile.absolutePath,
                    decibel = null,
                    timestamp = timestamp
                )
                val guideText = perspectiveGuide?.toSerializedString() ?: "unset"
                Log.d("VideoManager", "Saved VIDEO_HIGHLIGHT: ${permFile.name} guide=$guideText")
            } catch (e: Exception) {
                Log.e("VideoManager", "Failed to copy highlight file", e)
                highlightIndex.remove(highlightKey)
            }
        }
    }

    private fun extractTimestampFromFileName(fileName: String): Long? {
        val regex = Regex("temp_vid_.*_(\\d+)\\.mp4$")
        return regex.find(fileName)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun createMockImage(file: File, timestamp: Long) {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.DKGRAY)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
        }
        canvas.drawText("MOCK VIDEO", 40f, 80f, paint)
        canvas.drawText("$timestamp", 40f, 130f, paint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
    }

    /**
     * Triggered by AudioMonitor (Scream Event).
     * Saves the last 1.5m (3 chunks) as highlights AND sets flags to capture the next 1.5m.
     */
    fun saveBufferForEvent(sessionId: String) {
        Log.i("VideoManager", "🎬 Triggering Centered Highlight (1.5m pre / 1.5m post)")
        
        // 1. Capture past 1.5 minutes (Pre-event context)
        currentScope?.launch(Dispatchers.IO) {
            val snapshot = bufferMutex.withLock { 
                 // Take the last 3 chunks from the 6-chunk buffer (most recent 90s)
                 tempBuffer.toList().takeLast(3)
            }
            
            snapshot.forEach { file ->
                saveAsHighlight(sessionId, file)
            }
        }

        // 2. Schedule capture of next 1.5 minutes (Post-event reaction)
        currentScope?.launch {
            highlightMutex.withLock {
                pendingHighlightCount = 3 // Next 3 chunks (90s) will be saved
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
        val scope = currentScope
        if (scope != null && scope.isActive) {
            scope.launch(Dispatchers.IO) { fileManager.clearTempFiles() }
        } else {
            fileManager.clearTempFiles()
        }
        // Note: We don't delete files here to allow post-session analysis if needed, 
        // but for safety we can clear the memory reference.
        tempBuffer.clear()
        highlightIndex.clear()
    }
}
