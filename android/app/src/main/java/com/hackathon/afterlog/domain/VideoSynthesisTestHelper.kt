package com.hackathon.afterlog.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.hackathon.afterlog.data.media.VideoSynthesizer
import com.hackathon.afterlog.data.repository.TtsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VideoSynthesisTestHelper: Utility for testing the video synthesis pipeline.
 * 
 * Generates dummy image and uses TTS to create test audio, then runs VideoSynthesizer.
 * This allows testing without real session data.
 * 
 * Usage: Call `runFullTest()` from any ViewModel or debug screen.
 */
@Singleton
class VideoSynthesisTestHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsRepository: TtsRepository,
    private val videoSynthesizer: VideoSynthesizer
) {
    
    /**
     * Runs a complete test of the video synthesis pipeline.
     * 
     * @return The generated MP4 file path, or error message
     */
    suspend fun runFullTest(): TestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🧪 Starting Video Synthesis Test...")
        
        try {
            // Step 1: Generate dummy image
            val testImage = generateTestImage()
            Log.d(TAG, "✅ Test image created: ${testImage.absolutePath}")
            
            // Step 2: Generate TTS audio
            val narrationText = "This is a test of the Afterlog video synthesis system. " +
                "The night was dark, and the investigators gathered around the table. " +
                "Something terrible was about to happen."
            
            Log.d(TAG, "🎙️ Generating TTS audio...")
            val audioFile = ttsRepository.synthesizeText(
                text = narrationText,
                filename = "test_narration.mp3"
            )
            
            if (audioFile == null || !audioFile.exists()) {
                return@withContext TestResult.Failure("TTS generation failed. Check GOOGLE_CLOUD_KEY in local.properties")
            }
            Log.d(TAG, "✅ TTS audio created: ${audioFile.length()} bytes")
            
            // Step 3: Run VideoSynthesizer
            Log.d(TAG, "🎞️ Synthesizing video...")
            val outputVideo = videoSynthesizer.synthesize(
                outputSessionId = "test_${System.currentTimeMillis()}",
                audioFile = audioFile,
                images = listOf(testImage),
                imageDurationSec = 5
            )
            
            if (outputVideo == null || !outputVideo.exists()) {
                return@withContext TestResult.Failure("Video synthesis failed. Check logs for details.")
            }
            
            Log.d(TAG, "🎉 TEST SUCCESS! Video size: ${outputVideo.length()} bytes")
            Log.d(TAG, "📁 Output path: ${outputVideo.absolutePath}")
            
            return@withContext TestResult.Success(outputVideo.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Test failed with exception", e)
            return@withContext TestResult.Failure("Exception: ${e.message}")
        }
    }
    
    /**
     * Generates a simple test image (dark background with text).
     */
    private fun generateTestImage(): File {
        val width = 1280
        val height = 720
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Dark noir background
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        
        // Draw title text
        val paint = Paint().apply {
            color = Color.parseColor("#E94560")
            textSize = 72f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("AFTERLOG", width / 2f, height / 2f - 50f, paint)
        
        // Draw subtitle
        paint.apply {
            color = Color.WHITE
            textSize = 36f
        }
        canvas.drawText("Video Synthesis Test", width / 2f, height / 2f + 30f, paint)
        
        // Draw timestamp
        paint.apply {
            color = Color.GRAY
            textSize = 24f
        }
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        canvas.drawText(timestamp, width / 2f, height / 2f + 80f, paint)
        
        // Save to file
        val imageFile = File(context.cacheDir, "test_image.jpg")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        
        return imageFile
    }
    
    sealed class TestResult {
        data class Success(val videoPath: String) : TestResult()
        data class Failure(val error: String) : TestResult()
    }
    
    companion object {
        private const val TAG = "SynthesisTest"
    }
}
