package com.hackathon.afterlog.domain

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.hackathon.afterlog.data.media.VideoSynthesizer
import com.hackathon.afterlog.data.media.VideoStitcher
import com.hackathon.afterlog.data.repository.GeminiRepository
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.data.repository.TtsRepository
import com.hackathon.afterlog.data.local.entities.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.media3.common.util.UnstableApi

/**
 * MediaPipelineUseCase: Orchestrates the full "Cinematic Replay" generation pipeline.
 * 
 * Hybrid Strategy (per docs/implementation/06_media_synthesis.md):
 * - If VIDEO_HIGHLIGHT chunks exist (80dB+ scream events): Stitch real video clips
 * - Otherwise: Fall back to image slideshow from keyframes
 * 
 * Flow:
 * 1. Fetch session media (videos, images, audio) from local DB
 * 2. Analyze with Gemini AI (15sec interval frames for dense analysis)
 * 3. Synthesize TTS narration from report
 * 4. Combine narration + video/images into final MP4
 * 
 * Follows CODING_STANDARDS.md: Business logic in domain layer, all I/O on Dispatchers.IO
 */
@UnstableApi
@Singleton
class MediaPipelineUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiRepository: GeminiRepository,
    private val ttsRepository: TtsRepository,
    private val videoSynthesizer: VideoSynthesizer,
    private val videoStitcher: VideoStitcher,
    private val localRepository: LocalRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Generates a cinematic replay MP4 for a given session.
     * 
     * @param sessionId The game session ID
     * @return Result<File> containing the generated MP4 file or error info
     */
    suspend fun generateReplay(sessionId: String): Result<File> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🎬 Starting replay generation for session: $sessionId")
        
        try {
            // Step 1: Fetch media from database
            val videos = localRepository.getVideosBySession(sessionId)
            val audioFile = localRepository.getAudioFileBySession(sessionId)
            
            if (videos.isEmpty() && audioFile == null) {
                Log.w(TAG, "No media found for session $sessionId")
                return@withContext Result.failure(IllegalStateException("No media found for session $sessionId. Please record at least one video segment."))
            }
            
            Log.d(TAG, "📦 Fetched ${videos.size} videos, audio present: ${audioFile != null}")
            
            // Step 2: Analyze with Gemini
            Log.d(TAG, "🤖 Analyzing session with Gemini...")
            val reportJson = geminiRepository.generateInvestigativeReport(
                videoFiles = videos.map { File(it.filePath) },
                audioFile = audioFile?.let { File(it.filePath) },
                contextData = "Board game session recorded by Afterlog"
            )
            
            if (reportJson.isBlank()) {
                Log.e(TAG, "Gemini analysis returned empty result")
                return@withContext Result.failure(IllegalStateException("Gemini analysis failed: Empty response."))
            }
            
            Log.d(TAG, "✅ Gemini analysis complete")
            
            // Step 3: Extract narration text from JSON
            val narrationText = extractNarrationFromJson(reportJson)
            if (narrationText.isBlank()) {
                Log.e(TAG, "Failed to extract narration from Gemini response")
                return@withContext Result.failure(IllegalStateException("Could not extract narration from AI response. JSON: $reportJson"))
            }
            
            Log.d(TAG, "📝 Narration text: ${narrationText.take(100)}...")
            
            // Step 4: Generate TTS audio
            Log.d(TAG, "🎙️ Generating TTS narration...")
            val narrationAudio = ttsRepository.synthesizeText(
                text = narrationText,
                filename = "narration_$sessionId.mp3"
            )
            
            if (narrationAudio == null || !narrationAudio.exists()) {
                Log.e(TAG, "TTS synthesis failed")
                return@withContext Result.failure(IllegalStateException("TTS Synthesis failed. Check API key."))
            }
            
            Log.d(TAG, "✅ TTS complete: ${narrationAudio.length()} bytes")
            
            // Step 5: Determine video generation strategy (Hybrid Mode)
            // Priority: VIDEO_HIGHLIGHT clips > VIDEO_CHUNK > Image Slideshow
            val highlightVideos = videos.filter { it.type == MediaType.VIDEO_HIGHLIGHT }
            val allVideoFiles = videos.map { File(it.filePath) }.filter { it.exists() }
            
            Log.d(TAG, "📊 Hybrid Mode: ${highlightVideos.size} highlights, ${allVideoFiles.size} total videos")
            
            val finalVideo: File? = when {
                // Case A: We have highlight clips -> Stitch real video with TTS overlay
                highlightVideos.isNotEmpty() -> {
                    Log.d(TAG, "🎬 Using VideoStitcher for ${highlightVideos.size} highlight clips")
                    val highlightFiles = highlightVideos.map { File(it.filePath) }.filter { it.exists() }
                    
                    val stitchResult = videoStitcher.stitchVideos(
                        videoChunks = highlightFiles,
                        narrationAudio = narrationAudio,
                        outputSessionId = sessionId
                    )
                    
                    stitchResult.getOrNull()
                }
                
                // Case B: No highlights, but we have video chunks -> Extract keyframes for slideshow
                allVideoFiles.isNotEmpty() -> {
                    Log.d(TAG, "🖼️ Using VideoSynthesizer slideshow (no highlights)")
                    val keyframes = extractKeyframesFromVideos(allVideoFiles)
                    
                    if (keyframes.isEmpty()) {
                        Log.w(TAG, "No keyframes extracted from video chunks")
                        null
                    } else {
                        videoSynthesizer.synthesize(
                            outputSessionId = sessionId,
                            audioFile = narrationAudio,
                            images = keyframes,
                            imageDurationSec = 5
                        )
                    }
                }
                
                // Case C: No video at all -> Should not reach here due to earlier check
                else -> null
            }
            
            if (finalVideo == null || !finalVideo.exists()) {
                Log.e(TAG, "Video generation failed (both stitcher and synthesizer)")
                return@withContext Result.failure(IllegalStateException("Final video generation failed. Check logs."))
            }
            
            Log.d(TAG, "🎉 Replay generation complete: ${finalVideo.absolutePath}")
            return@withContext Result.success(finalVideo)
            
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline failed", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Extracts narration text from Gemini's JSON response.
     * Tries multiple fields: "article", "summary", "headline"
     */
    private fun extractNarrationFromJson(jsonString: String): String {
        return try {
            val validJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
                
            val jsonObj = json.parseToJsonElement(validJson).jsonObject
            
            // Priority: article > summary > headline
            jsonObj["article"]?.jsonPrimitive?.content
                ?: jsonObj["summary"]?.jsonPrimitive?.content
                ?: jsonObj["headline"]?.jsonPrimitive?.content
                ?: ""
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini JSON: $jsonString", e)
            ""
        }
    }
    
    /**
     * Extracts keyframes from video files using MediaMetadataRetriever.
     * Takes the frame at 1 second mark (or 0 if short).
     */
    private fun extractKeyframesFromVideos(videoFiles: List<File>): List<File> {
        val extractedImages = mutableListOf<File>()
        val retriever = MediaMetadataRetriever()
        
        for (video in videoFiles) {
            if (!video.exists()) continue
            
            try {
                retriever.setDataSource(video.absolutePath)
                // Get frame at 1 second (1000000 microseconds)
                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) 
                    ?: retriever.getFrameAtTime(0) // Fallback to 0 if 1s fails
                
                if (bitmap != null) {
                    val imageFile = File(context.cacheDir, "keyframe_${video.nameWithoutExtension}.jpg")
                    FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    extractedImages.add(imageFile)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract keyframe from ${video.name}", e)
            }
        }
        
        try { retriever.release() } catch (e: Exception) {}
        
        return extractedImages
    }
    
    companion object {
        private const val TAG = "MediaPipeline"
    }
}
