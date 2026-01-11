package com.hackathon.afterlog.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.hackathon.afterlog.BuildConfig
import com.hackathon.afterlog.data.model.HighlightSegment
import com.hackathon.afterlog.data.remote.GeminiFilesApiClient
import com.hackathon.afterlog.data.repository.gemini.GeminiAudioUtils
import com.hackathon.afterlog.data.repository.gemini.GeminiLogUtils
import com.hackathon.afterlog.data.repository.gemini.GeminiParsers
import com.hackathon.afterlog.data.repository.gemini.GeminiPromptBuilder
import com.hackathon.afterlog.data.repository.gemini.GeminiRetryPolicy
import com.hackathon.afterlog.data.repository.gemini.GeminiVideoUtils
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val filesApiClient: GeminiFilesApiClient,
    @ApplicationContext private val context: Context
) {
    data class CaptionLine(val startMs: Long, val endMs: Long, val text: String)

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-pro-preview",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 4096
        }
    )

    private val retryPolicy = GeminiRetryPolicy(generativeModel)
    private val audioUtils = GeminiAudioUtils(filesApiClient, context)
    private val promptBuilder = GeminiPromptBuilder
    private val parsers = GeminiParsers
    private val videoUtils = GeminiVideoUtils
    private val logUtils = GeminiLogUtils

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent("Hello, are you there? Reply with: 'Connection Success!'")
            return@withContext response.text ?: "Connection Error: Empty response"
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Connection test failed", e)
            return@withContext "Connection Failed: ${e.localizedMessage}"
        }
    }

    suspend fun generateInvestigativeReport(
        videoFiles: List<File>,
        audioFile: File?,
        contextData: String
    ): String = withContext(Dispatchers.IO) {
        val frames = videoUtils.extractKeyFrames(videoFiles, intervalSec = 15, maxTotalFrames = 24)
        try {
            Log.d("GeminiRepo", "Starting Noir Analysis for ${videoFiles.size} files")
            
            Log.d("GeminiRepo", "Extracted ${frames.size} frames")
            
            val audioData = audioFile?.let { audioUtils.uploadAudioToGemini(it) }

            // Only abort if BOTH frames and audio are missing
            if (frames.isEmpty() && audioData == null) {
                Log.w("GeminiRepo", "No evidence (video or audio) found. Aborting.")
                return@withContext """{"headline":"NO EVIDENCE FOUND","summary":"The scene was empty.","atmosphere":"Silence.","timeline":[],"highlight_segments":[],"verdict":"Case closed?”nothing to report."}"""
            }

            val prompt = promptBuilder.buildInvestigativePrompt(contextData, audioData != null, compact = false)
            val inputContent = buildInputContent(frames, audioData, prompt)
            val response = try {
                retryPolicy.generateWithRetry(inputContent)
            } catch (e: Exception) {
                if (retryPolicy.isMaxTokensError(e)) {
                    Log.w("GeminiRepo", "Max tokens hit. Retrying with reduced inputs (8 frames, no audio).", e)
                    val reducedFrames = frames.take(8)
                    val compactPrompt = promptBuilder.buildInvestigativePrompt(
                        contextData,
                        includeAudio = false,
                        compact = true
                    )
                    val compactInput = buildInputContent(reducedFrames, audioData = null, compactPrompt)
                    retryPolicy.generateWithRetry(compactInput)
                } else {
                    throw e
                }
            }
            val rawText = response.text
            if (rawText.isNullOrBlank()) {
                Log.w("GeminiRepo", "Gemini response empty")
                return@withContext """{"headline":"ANALYSIS FAILED","summary":"Output was empty","atmosphere":"","timeline":[],"highlight_segments":[],"verdict":"The typewriter jammed."}"""
            }

            Log.d("GeminiRepo", "Gemini response received (${rawText.length} chars)")
            logUtils.logLongMessage("GeminiRepo", "Gemini JSON", rawText)
            logUtils.logHighlightSegmentsFromRaw(rawText)
            return@withContext rawText

        } catch (e: Exception) {
            Log.e("GeminiRepo", "Investigation failed", e)
            if (retryPolicy.isRetryableGeminiError(e)) {
                return@withContext """{"headline":"ANALYSIS DELAYED","summary":"Gemini is temporarily overloaded. Please retry.","atmosphere":"","article":"","timeline":[],"highlight_segments":[],"verdict":"Awaiting a clearer signal."}"""
            }
            // Return detailed error for debugging
            val errorType = e.javaClass.simpleName
            val errorMessage = e.message?.replace("\"", "'") ?: "Unknown error"
            return@withContext """{"headline":"SYSTEM ERROR ($errorType)","summary":"$errorMessage","atmosphere":"","timeline":[],"highlight_segments":[],"verdict":"Investigation aborted."}"""
        } finally {
            frames.forEach { it.recycle() }
        }
    }

    suspend fun generateHighlightSegmentsForWindow(
        videoFiles: List<File>,
        audioFile: File?,
        contextData: String,
        windowStartSec: Double,
        windowEndSec: Double,
        windowLabel: String
    ): List<HighlightSegment> = withContext(Dispatchers.IO) {
        val frames = videoUtils.extractKeyFrames(videoFiles, intervalSec = 30, maxTotalFrames = 12)
        val sliceFile = audioFile?.let {
            audioUtils.createAudioSliceForGemini(it, windowStartSec, windowEndSec, windowLabel)
        }
        val audioData = sliceFile?.let { audioUtils.uploadAudioToGemini(it) }

        if (frames.isEmpty() && audioData == null) {
            Log.w("GeminiRepo", "Segment highlight skipped (no frames/audio): $windowLabel")
            sliceFile?.delete()
            return@withContext emptyList()
        }

        try {
            val prompt = promptBuilder.buildHighlightOnlyPrompt(
                contextData = contextData,
                includeAudio = audioData != null,
                windowStartSec = windowStartSec,
                windowEndSec = windowEndSec,
                windowLabel = windowLabel
            )
            val inputContent = buildInputContent(frames, audioData, prompt)
            val response = retryPolicy.generateWithRetry(inputContent)
            val rawText = response.text ?: return@withContext emptyList()
            val segments = parsers.parseHighlightSegmentsFromRaw(rawText)
            if (segments.isEmpty()) {
                Log.d("GeminiRepo", "Segment highlight empty: $windowLabel")
            } else {
                Log.d("GeminiRepo", "Segment highlight ok: $windowLabel (${segments.size})")
            }
            return@withContext segments
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Segment highlight analysis failed: $windowLabel", e)
            return@withContext emptyList()
        } finally {
            frames.forEach { it.recycle() }
            sliceFile?.delete()
        }
    }

    /**
     * Generates noir-style minimal captions (headline tone) for key events.
     * Output format:
     * {
     *   "events": [
     *     {"start_ms": 12000, "end_ms": 14000, "text": "The dice are cast"}
     *   ]
     * }
     */
    suspend fun generateNoirCaptions(
        audioFile: File,
        eventHints: List<String> = emptyList()
    ): List<CaptionLine> = withContext(Dispatchers.IO) {
        try {
            val audioData = audioUtils.uploadAudioToGemini(audioFile)
            if (audioData == null) {
                Log.w("GeminiRepo", "Audio upload failed, skipping caption generation.")
                return@withContext emptyList()
            }

            val prompt = promptBuilder.buildNoirCaptionPrompt(eventHints)

            val inputContent = content {
                fileData(uri = audioData.first, mimeType = audioData.second)
                text(prompt)
            }

            val response = retryPolicy.generateWithRetry(inputContent)
            val raw = response.text ?: return@withContext emptyList()
            return@withContext parsers.parseCaptions(raw)
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Caption generation failed", e)
            return@withContext emptyList()
        }
    }

    fun generateFallbackHighlights(
        totalDurationSec: Double,
        segmentCount: Int = 3,
        segmentLengthSec: Double = 15.0
    ): List<HighlightSegment> {
        if (totalDurationSec < segmentLengthSec) {
            return listOf(
                HighlightSegment(
                    startSec = 0.0,
                    endSec = totalDurationSec,
                    reason = "Session overview"
                )
            )
        }

        val count = segmentCount.coerceIn(1, 5)
        val interval = totalDurationSec / (count + 1)

        return (1..count).map { i ->
            val centerSec = interval * i
            val startSec = (centerSec - segmentLengthSec / 2).coerceAtLeast(0.0)
            val endSec = (startSec + segmentLengthSec).coerceAtMost(totalDurationSec)
            HighlightSegment(
                startSec = startSec,
                endSec = endSec,
                reason = "Auto-selected moment ${i}"
            )
        }
    }

    private fun buildInputContent(
        frames: List<Bitmap>,
        audioData: Pair<String, String>?,
        prompt: String
    ): com.google.ai.client.generativeai.type.Content {
        return content {
            frames.forEach { bitmap ->
                image(bitmap)
            }
            if (audioData != null) {
                fileData(uri = audioData.first, mimeType = audioData.second)
            }
            text(prompt)
        }
    }

}
