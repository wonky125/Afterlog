package com.hackathon.afterlog.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.util.Log
import com.hackathon.afterlog.data.media.VideoSynthesizer
import com.hackathon.afterlog.data.media.VideoStitcher
import com.hackathon.afterlog.data.repository.GeminiRepository
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.data.repository.TtsRepository
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.model.HighlightSegment
import com.hackathon.afterlog.data.model.PerspectiveGuideConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.hackathon.afterlog.data.util.GeminiJsonUtils
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs

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

    data class ReplayAssets(
        val videoFile: File,
        val subtitleFile: File?
    )

    data class SubtitlePackage(
        val file: File,
        val cues: List<VideoStitcher.CaptionCue>
    )
    
    /**
     * Generates a cinematic replay MP4 for a given session.
     * 
     * @param sessionId The game session ID
     * @return Result<File> containing the generated MP4 file or error info
     */
    suspend fun generateReplay(sessionId: String): Result<ReplayAssets> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting replay generation for session: $sessionId")
        
        try {
            // Step 1: Fetch media from database
            val videos = localRepository.getVideosBySession(sessionId)
            val audioFile = localRepository.getAudioFileBySession(sessionId)
            val sessionStart = localRepository.getSessionStartTime(sessionId)

            validateTimelineAlignment(sessionId, videos, audioFile, sessionStart)
            
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
            val geminiHighlights = extractHighlightSegmentsFromJson(reportJson)
            Log.d(TAG, "Gemini highlight segments: ${geminiHighlights.size}")
            
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
            
            val selectedSegments = resolveHighlightSegments(
                sessionId = sessionId,
                videos = videos,
                sessionStart = sessionStart,
                geminiHighlights = geminiHighlights,
                audioLog = audioFile
            )
            val finalVideo = buildFinalVideo(
                sessionId = sessionId,
                videos = videos,
                narrationAudio = narrationAudio,
                audioLog = audioFile,
                sessionStart = sessionStart,
                selectedHighlightSegments = selectedSegments
            )

            if (finalVideo == null || !finalVideo.exists()) {
                Log.e(TAG, "Video generation failed (both stitcher and synthesizer)")
                return@withContext Result.failure(IllegalStateException("Final video generation failed. Check logs."))
            }
            
            val subtitlePackage = createSubtitlePackage(
                sessionId = sessionId,
                audioLog = audioFile,
                fallbackAudio = narrationAudio,
                sessionStart = sessionStart,
                highlightSegments = selectedSegments
            )
            val outputVideo = burnInSubtitlesIfNeeded(sessionId, finalVideo, subtitlePackage)
            val cappedVideo = trimReplayIfNeeded(sessionId, outputVideo)
            Log.d(TAG, "🎉 Replay generation complete: ${cappedVideo.absolutePath}")
            return@withContext Result.success(ReplayAssets(cappedVideo, subtitlePackage?.file))
            
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline failed", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Generates a cinematic replay MP4 using an already-built narration text.
     */
    suspend fun generateReplayWithNarration(
        sessionId: String,
        narrationText: String,
        highlightSegments: List<HighlightSegment> = emptyList()
    ): Result<ReplayAssets> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting replay generation (narration provided) for session: $sessionId")

        try {
            if (narrationText.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Narration text is empty."))
            }

            val videos = localRepository.getVideosBySession(sessionId)
            val audioFile = localRepository.getAudioFileBySession(sessionId)
            val sessionStart = localRepository.getSessionStartTime(sessionId)

            validateTimelineAlignment(sessionId, videos, audioFile, sessionStart)

            if (videos.isEmpty() && audioFile == null) {
                return@withContext Result.failure(IllegalStateException("No media found for session $sessionId."))
            }

            val narrationAudio = ttsRepository.synthesizeText(
                text = narrationText,
                filename = "narration_$sessionId.mp3"
            )

            if (narrationAudio == null || !narrationAudio.exists()) {
                return@withContext Result.failure(IllegalStateException("TTS Synthesis failed."))
            }

            val selectedSegments = resolveHighlightSegments(
                sessionId = sessionId,
                videos = videos,
                sessionStart = sessionStart,
                geminiHighlights = highlightSegments,
                audioLog = audioFile
            )
            val finalVideo = buildFinalVideo(
                sessionId = sessionId,
                videos = videos,
                narrationAudio = narrationAudio,
                audioLog = audioFile,
                sessionStart = sessionStart,
                selectedHighlightSegments = selectedSegments
            )
            if (finalVideo == null || !finalVideo.exists()) {
                return@withContext Result.failure(IllegalStateException("Final video generation failed."))
            }

            val subtitlePackage = createSubtitlePackage(
                sessionId = sessionId,
                audioLog = audioFile,
                fallbackAudio = narrationAudio,
                sessionStart = sessionStart,
                highlightSegments = selectedSegments
            )
            val outputVideo = burnInSubtitlesIfNeeded(sessionId, finalVideo, subtitlePackage)
            val cappedVideo = trimReplayIfNeeded(sessionId, outputVideo)
            Log.d(TAG, "Replay generation (narration) complete: ${cappedVideo.absolutePath}")
            return@withContext Result.success(ReplayAssets(cappedVideo, subtitlePackage?.file))
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline failed (narration provided)", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Extracts narration text from Gemini's JSON response.
     * Tries multiple fields: "article", "summary", "headline"
     */
    private fun extractNarrationFromJson(jsonString: String): String {
        return try {
            val validJson = GeminiJsonUtils.cleanMarkdownJson(jsonString)
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

    private suspend fun createSubtitlePackage(
        sessionId: String,
        audioLog: MediaLogEntity?,
        fallbackAudio: File?,
        sessionStart: Long?,
        highlightSegments: List<VideoSegment>
    ): SubtitlePackage? {
        val useNarrationAudio = highlightSegments.isEmpty() && fallbackAudio != null
        val audioSource = when {
            useNarrationAudio -> fallbackAudio
            audioLog != null -> File(audioLog.filePath)
            else -> fallbackAudio
        }
        if (audioSource == null || !audioSource.exists()) {
            Log.w(TAG, "Subtitle generation skipped: no audio source for $sessionId")
            return null
        }

        val eventHints = buildCaptionHints(sessionId, sessionStart)
        val captions = geminiRepository.generateNoirCaptions(audioSource, eventHints)
        val cleanedCaptions = captions.mapNotNull { line ->
            val text = line.text.replace("\n", " ").trim()
            if (text.isBlank()) null else GeminiRepository.CaptionLine(line.startMs, line.endMs, text)
        }

        val sortedCaptions = cleanedCaptions.sortedBy { it.startMs }

        val audioOffsetMs = if (useNarrationAudio) {
            0L
        } else {
            computeAudioOffsetMs(audioLog, sessionStart)
        }
        val baseCues = sortedCaptions.map { line ->
            val rawStart = line.startMs.coerceAtLeast(0L)
            val rawEnd = maxOf(line.endMs, rawStart + 1500L)
            val startMs = rawStart + audioOffsetMs
            val endMs = rawEnd + audioOffsetMs
            VideoStitcher.CaptionCue(startMs = startMs, endMs = endMs, text = line.text)
        }

        val fallbackCues = buildFallbackCuesFromSegments(highlightSegments)

        if (baseCues.isEmpty() && fallbackCues.isEmpty()) {
            Log.d(TAG, "No captions generated for session $sessionId")
            return null
        }

        val canMapToSegments = highlightSegments.isNotEmpty() && !useNarrationAudio && audioLog != null && sessionStart != null
        val mappedCues = if (baseCues.isNotEmpty() && canMapToSegments) {
            remapCuesToSegments(baseCues, highlightSegments)
        } else {
            baseCues
        }

        val finalCues = when {
            mappedCues.isNotEmpty() -> mappedCues
            fallbackCues.isNotEmpty() -> fallbackCues
            else -> emptyList()
        }

        if (finalCues.isEmpty()) {
            Log.d(TAG, "Caption mapping yielded zero cues for session $sessionId")
            return null
        }

        val srtContent = buildSrtTextFromCues(finalCues)
        if (srtContent.isBlank()) {
            Log.w(TAG, "Generated SRT content is empty for session $sessionId")
            return null
        }
        val subtitleFile = File(context.filesDir, "replay_${sessionId}.srt")
        subtitleFile.parentFile?.mkdirs()
        subtitleFile.writeText(srtContent)
        localRepository.deleteLogsBySessionAndType(sessionId, MediaType.SUBTITLE)
        localRepository.logMedia(
            sessionId = sessionId,
            type = MediaType.SUBTITLE,
            filePath = subtitleFile.absolutePath
        )

        Log.d(TAG, "Subtitle saved: ${subtitleFile.name}")
        return SubtitlePackage(subtitleFile, finalCues)
    }

    private suspend fun buildCaptionHints(
        sessionId: String,
        sessionStart: Long?
    ): List<String> {
        val logs = localRepository.getSessionLogs(sessionId)
        val screams = logs.filter { it.type == MediaType.SCREAM_EVENT && it.decibel != null }
        if (screams.isEmpty()) {
            return emptyList()
        }

        return screams
            .sortedBy { it.timestamp }
            .take(8)
            .mapNotNull { event ->
                val decibel = event.decibel ?: return@mapNotNull null
                val relativeMs = sessionStart?.let { start -> (event.timestamp - start).coerceAtLeast(0) }
                val timeLabel = relativeMs?.let { formatSrtTimestamp(it).substring(0, 8) }
                    ?: "t=${event.timestamp}"
                "$timeLabel ${decibel}dB spike"
            }
    }

    private fun buildSrtText(captions: List<GeminiRepository.CaptionLine>): String {
        return buildString {
            var counter = 1
            captions.forEach { line ->
                val cleanedText = line.text.replace("\n", " ").trim()
                if (cleanedText.isBlank()) return@forEach
                val startMs = line.startMs.coerceAtLeast(0L)
                val endMs = maxOf(line.endMs, startMs + 1500L)
                append("${counter++}\n")
                append("${formatSrtTimestamp(startMs)} --> ${formatSrtTimestamp(endMs)}\n")
                append(cleanedText)
                append("\n\n")
            }
        }
    }

    private fun extractHighlightSegmentsFromJson(jsonString: String): List<HighlightSegment> {
        return try {
            val validJson = GeminiJsonUtils.cleanMarkdownJson(jsonString)
            val jsonObj = json.parseToJsonElement(validJson).jsonObject
            val segments = jsonObj["highlight_segments"]?.jsonArray ?: return emptyList()
            segments.mapNotNull { element ->
                val obj = element.jsonObject
                val start = obj["start_sec"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["start_timestamp"]?.jsonPrimitive?.doubleOrNull
                val end = obj["end_sec"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["end_timestamp"]?.jsonPrimitive?.doubleOrNull
                if (start == null || end == null || end <= start) {
                    return@mapNotNull null
                }
                val reason = obj["reason"]?.jsonPrimitive?.content ?: ""
                HighlightSegment(startSec = start, endSec = end, reason = reason)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse highlight segments from Gemini JSON", e)
            emptyList()
        }
    }

    private fun buildSrtTextFromCues(cues: List<VideoStitcher.CaptionCue>): String {
        return buildString {
            var counter = 1
            cues.sortedBy { it.startMs }.forEach { cue ->
                val cleanedText = cue.text.replace("\n", " ").trim()
                if (cleanedText.isBlank()) return@forEach
                val startMs = cue.startMs.coerceAtLeast(0L)
                val endMs = maxOf(cue.endMs, startMs + 500L)
                append("${counter++}\n")
                append("${formatSrtTimestamp(startMs)} --> ${formatSrtTimestamp(endMs)}\n")
                append(cleanedText)
                append("\n\n")
            }
        }
    }

    private fun computeAudioOffsetMs(audioLog: MediaLogEntity?, sessionStart: Long?): Long {
        return if (audioLog != null && sessionStart != null) {
            (audioLog.timestamp - sessionStart).coerceAtLeast(0L)
        } else {
            0L
        }
    }

    private fun remapCuesToSegments(
        cues: List<VideoStitcher.CaptionCue>,
        segments: List<VideoSegment>
    ): List<VideoStitcher.CaptionCue> {
        if (segments.isEmpty()) return cues

        val orderedSegments = segments.sortedBy { it.startMs }
        val orderedCues = cues.sortedBy { it.startMs }
        val remapped = mutableListOf<VideoStitcher.CaptionCue>()
        var timelineOffset = 0L

        orderedSegments.forEach { segment ->
            val segDuration = segment.durationMs.coerceAtLeast(0L)
            val segStart = segment.startMs
            val segEnd = segStart + segDuration

            orderedCues.forEach { cue ->
                if (cue.endMs <= segStart || cue.startMs >= segEnd) return@forEach
                val clipStart = maxOf(cue.startMs, segStart)
                val clipEnd = minOf(cue.endMs, segEnd)
                if (clipEnd <= clipStart) return@forEach

                val newStart = timelineOffset + (clipStart - segStart)
                val newEnd = timelineOffset + (clipEnd - segStart)
                remapped.add(cue.copy(startMs = newStart, endMs = newEnd))
            }

            timelineOffset += segDuration
        }

        return remapped.sortedBy { it.startMs }
    }

    private fun buildFallbackCuesFromSegments(
        segments: List<VideoSegment>
    ): List<VideoStitcher.CaptionCue> {
        if (segments.isEmpty()) return emptyList()

        val phrases = listOf(
            "FATE STIRS",
            "SILENCE BREAKS",
            "A CLUE EMERGES",
            "SHADOWS SHIFT",
            "VERDICT NEARS"
        )
        val orderedSegments = segments.sortedBy { it.startMs }
        val fallback = mutableListOf<VideoStitcher.CaptionCue>()
        var timelineOffset = 0L

        orderedSegments.forEachIndexed { index, segment ->
            val duration = segment.durationMs.coerceAtLeast(0L)
            if (duration < 500L) {
                timelineOffset += duration
                return@forEachIndexed
            }
            val startMs = timelineOffset + (duration / 2)
            val endMs = minOf(timelineOffset + duration, startMs + 1500L)
            if (endMs > startMs) {
                fallback.add(
                    VideoStitcher.CaptionCue(
                        startMs = startMs,
                        endMs = endMs,
                        text = phrases[index % phrases.size]
                    )
                )
            }
            timelineOffset += duration
        }

        return fallback
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val totalMs = ms.coerceAtLeast(0L)
        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val seconds = (totalMs % 60_000) / 1000
        val milliseconds = totalMs % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }

    private fun formatSeconds(seconds: Double): String {
        return String.format(Locale.US, "%.3f", seconds)
    }

    private fun formatMsAsSeconds(ms: Long): String {
        return formatSeconds(ms / 1000.0)
    }

    private fun adjustHighlightsForAudioOffset(
        sessionId: String,
        highlights: List<HighlightSegment>,
        audioLog: MediaLogEntity?,
        sessionStart: Long?
    ): List<HighlightSegment> {
        if (highlights.isEmpty()) return highlights
        if (audioLog == null || sessionStart == null) {
            Log.w(TAG, "Audio offset unavailable; using raw highlight seconds ($sessionId)")
            return highlights
        }

        val offsetMs = computeAudioOffsetMs(audioLog, sessionStart)
        if (offsetMs <= 0L) {
            return highlights
        }

        val offsetSec = offsetMs / 1000.0
        Log.d(TAG, "Applying audio offset ${formatSeconds(offsetSec)}s to Gemini highlights ($sessionId)")
        return highlights.map { segment ->
            val start = segment.startSec + offsetSec
            val end = segment.endSec + offsetSec
            if (end <= start) {
                segment.copy(startSec = start, endSec = start)
            } else {
                segment.copy(startSec = start, endSec = end)
            }
        }
    }

    private fun logHighlightSegments(
        sessionId: String,
        highlights: List<HighlightSegment>,
        label: String
    ) {
        if (highlights.isEmpty()) {
            Log.d(TAG, "$label: none ($sessionId)")
            return
        }

        val formatted = highlights.mapIndexed { index, segment ->
            "${index + 1}:${formatSeconds(segment.startSec)}-${formatSeconds(segment.endSec)}"
        }.joinToString(", ")

        Log.d(TAG, "$label (${highlights.size}): $formatted ($sessionId)")
    }

    private suspend fun burnInSubtitlesIfNeeded(
        sessionId: String,
        baseVideo: File,
        subtitlePackage: SubtitlePackage?
    ): File {
        if (subtitlePackage == null) {
            return baseVideo
        }

        val burnResult = if (subtitlePackage.file.exists()) {
            videoStitcher.burnInSubtitlesFromSrt(
                inputVideo = baseVideo,
                subtitleFile = subtitlePackage.file,
                outputSessionId = sessionId
            )
        } else {
            videoStitcher.burnInSubtitles(
                inputVideo = baseVideo,
                cues = subtitlePackage.cues,
                outputSessionId = sessionId
            )
        }

        val burnedVideo = burnResult.getOrNull()
        return if (burnedVideo != null && burnedVideo.exists()) {
            burnedVideo
        } else {
            Log.w(TAG, "Subtitle burn-in failed; falling back to base video.")
            baseVideo
        }
    }

    private suspend fun trimReplayIfNeeded(
        sessionId: String,
        baseVideo: File
    ): File {
        val trimResult = videoStitcher.trimVideoToMaxDuration(
            inputVideo = baseVideo,
            maxDurationMs = MAX_REPLAY_DURATION_MS,
            outputSessionId = sessionId
        )
        val trimmedVideo = trimResult.getOrNull()
        return if (trimmedVideo != null && trimmedVideo.exists()) {
            trimmedVideo
        } else {
            Log.w(TAG, "Replay trim skipped or failed; using base video.")
            baseVideo
        }
    }

    private suspend fun buildFinalVideo(
        sessionId: String,
        videos: List<MediaLogEntity>,
        narrationAudio: File,
        audioLog: MediaLogEntity?,
        sessionStart: Long?,
        selectedHighlightSegments: List<VideoSegment>
    ): File? {
        val highlightVideos = videos.filter { it.type == MediaType.VIDEO_HIGHLIGHT }
        val allVideoFiles = videos.map { File(it.filePath) }.filter { it.exists() }

        Log.d(TAG, "Hybrid Mode: ${highlightVideos.size} highlights, ${allVideoFiles.size} total videos")

        Log.d(TAG, "Segments: highlights=${highlightVideos.size}, totalVideoFiles=${allVideoFiles.size}")

        if (highlightVideos.isNotEmpty()) {
            val segments = if (selectedHighlightSegments.isNotEmpty()) {
                selectedHighlightSegments
            } else {
                buildVideoSegments(highlightVideos, sessionStart)
            }
            Log.d(
                TAG,
                "Video segments=${segments.size}, sessionStart=$sessionStart"
            )
            val highlightFiles = segments.map { it.file }.filter { it.exists() }
            if (highlightFiles.isNotEmpty()) {
                val pcmFile = audioLog?.let { File(it.filePath) }?.takeIf { it.exists() }

                val audioStartOffsetMs = if (audioLog != null && sessionStart != null) {
                    (audioLog.timestamp - sessionStart).coerceAtLeast(0)
                } else {
                    null
                }

                val stitchResult = if (pcmFile != null && segments.isNotEmpty() && audioStartOffsetMs != null) {
                    val clips = segments.map {
                        VideoStitcher.AudioClip(startMs = it.startMs, durationMs = it.durationMs)
                    }
                    Log.d(TAG, "Audio clips=${clips.size} startOffset=$audioStartOffsetMs pcmPath=${pcmFile.absolutePath}")
                    videoStitcher.stitchVideosWithAudioSegments(
                        videoChunks = highlightFiles,
                        audioFile = pcmFile,
                        audioStartOffsetMs = audioStartOffsetMs,
                        audioClips = clips,
                        outputSessionId = sessionId
                    )
                } else {
                    videoStitcher.stitchVideos(
                        videoChunks = highlightFiles,
                        narrationAudio = narrationAudio,
                        outputSessionId = sessionId
                    )
                }

                return stitchResult.getOrNull()
            } else {
                Log.w(TAG, "Highlight logs present but no valid files; falling back to non-highlight path.")
            }
        }

        if (allVideoFiles.isNotEmpty()) {
            Log.d(TAG, "Using VideoSynthesizer slideshow (no highlights)")
            val keyframes = extractKeyframesFromVideos(allVideoFiles)

            return if (keyframes.isEmpty()) {
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

        Log.w(TAG, "No video files available for replay (image fallback disabled)")
        return null
    }

    private suspend fun resolveHighlightSegments(
        sessionId: String,
        videos: List<MediaLogEntity>,
        sessionStart: Long?,
        geminiHighlights: List<HighlightSegment>,
        audioLog: MediaLogEntity?
    ): List<VideoSegment> {
        val adjustedHighlights = adjustHighlightsForAudioOffset(
            sessionId = sessionId,
            highlights = geminiHighlights,
            audioLog = audioLog,
            sessionStart = sessionStart
        )
        logHighlightSegments(sessionId, adjustedHighlights, "Gemini highlight windows (sec)")
        if (adjustedHighlights.isNotEmpty()) {
            val trimmed = buildSegmentsFromGeminiHighlights(
                sessionId = sessionId,
                videos = videos,
                sessionStart = sessionStart,
                geminiHighlights = adjustedHighlights
            )
            if (trimmed.isNotEmpty()) {
                return capSegmentsToMaxDuration(trimmed)
            }
        }

        val highlightVideos = videos.filter { it.type == MediaType.VIDEO_HIGHLIGHT }
        if (highlightVideos.isEmpty()) {
            return emptyList()
        }
        val segments = buildVideoSegments(highlightVideos, sessionStart)
        val screamEvents = localRepository.getSessionLogs(sessionId)
            .filter { it.type == MediaType.SCREAM_EVENT && it.decibel != null }
        val trimmedSegments = if (screamEvents.isEmpty()) {
            segments
        } else {
            trimSegmentsAroundScreams(sessionId, segments, screamEvents)
        }
        return selectSegmentsByImportance(sessionId, trimmedSegments)
    }

/**
     * Extracts keyframes from video files using MediaMetadataRetriever.
     * Takes the frame at 1 second mark (or 0 if short).
     */
    private fun extractKeyframesFromVideos(videoFiles: List<File>): List<File> {
        val extractedImages = mutableListOf<File>()
        for (video in videoFiles) {
            if (!video.exists()) continue
            
            val retriever = MediaMetadataRetriever()
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
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }
        }
        
        return extractedImages
    }
    
    private data class VideoSegment(
        val file: File,
        val startMs: Long,
        val durationMs: Long,
        val absoluteStartMs: Long
    )

    private data class RankedSegment(
        val segment: VideoSegment,
        val score: Float
    )

    private fun buildVideoSegments(
        highlights: List<MediaLogEntity>,
        sessionStart: Long?
    ): List<VideoSegment> {
        val segments = mutableListOf<VideoSegment>()
        for (log in highlights) {
            val file = File(log.filePath)
            if (!file.exists()) continue

            val durationMs = try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to release retriever for ${file.name}", e)
                    }
                }
            } catch (e: Exception) {
                0L
            }
            val normalizedDurationMs = if (durationMs > 0L) {
                durationMs
            } else {
                Log.w(TAG, "Segment duration missing; defaulting to $DEFAULT_SEGMENT_DURATION_MS ms for ${file.name}")
                DEFAULT_SEGMENT_DURATION_MS
            }

            val startMs = if (sessionStart != null) {
                (log.timestamp - sessionStart).coerceAtLeast(0)
            } else {
                0L
            }

            segments.add(
                VideoSegment(
                    file = file,
                    startMs = startMs,
                    durationMs = normalizedDurationMs,
                    absoluteStartMs = log.timestamp
                )
            )
        }
        return segments.sortedBy { it.startMs }
    }

    private suspend fun buildSegmentsFromGeminiHighlights(
        sessionId: String,
        videos: List<MediaLogEntity>,
        sessionStart: Long?,
        geminiHighlights: List<HighlightSegment>
    ): List<VideoSegment> {
        val baseLogs = videos.filter { it.type == MediaType.VIDEO_CHUNK }
            .ifEmpty { videos.filter { it.type == MediaType.VIDEO_HIGHLIGHT } }
        if (baseLogs.isEmpty()) {
            Log.w(TAG, "Gemini highlight mapping skipped: no base video chunks ($sessionId)")
            return emptyList()
        }

        val baseSegments = buildVideoSegments(baseLogs, sessionStart)
        if (baseSegments.isEmpty()) {
            Log.w(TAG, "Gemini highlight mapping skipped: no base segments ($sessionId)")
            return emptyList()
        }

        val orderedHighlights = geminiHighlights
            .mapNotNull { highlight ->
                val startMs = (highlight.startSec * 1000.0).toLong().coerceAtLeast(0L)
                val endMs = (highlight.endSec * 1000.0).toLong().coerceAtLeast(0L)
                if (endMs <= startMs) null else Pair(startMs, endMs)
            }
            .sortedBy { it.first }
        if (orderedHighlights.isEmpty()) {
            Log.w(TAG, "Gemini highlight mapping skipped: no valid highlight windows ($sessionId)")
            return emptyList()
        }

        val clippedSegments = mutableListOf<VideoSegment>()
        for ((startMs, endMs) in orderedHighlights) {
            val windowStart = if (sessionStart != null) startMs else startMs
            val windowEnd = if (sessionStart != null) endMs else endMs
            var matched = false

            baseSegments.forEach { segment ->
                val segStart = segment.startMs
                val segEnd = segStart + segment.durationMs.coerceAtLeast(0L)
                val overlapStart = maxOf(segStart, windowStart)
                val overlapEnd = minOf(segEnd, windowEnd)
                val overlapDuration = overlapEnd - overlapStart
                if (overlapDuration <= 0L) return@forEach

                matched = true
                val clipStartMs = overlapStart - segStart
                val clipEndMs = clipStartMs + overlapDuration
                val trimResult = videoStitcher.trimVideoToWindow(
                    inputVideo = segment.file,
                    startMs = clipStartMs,
                    endMs = clipEndMs,
                    outputSessionId = sessionId,
                    outputLabel = "gemini_${overlapStart}_${segment.file.nameWithoutExtension}"
                )
                val trimmedFile = trimResult.getOrNull()
                if (trimmedFile == null || !trimmedFile.exists()) {
                    return@forEach
                }

                Log.d(
                    TAG,
                    "Gemini match: file=${segment.file.name} clip=${formatMsAsSeconds(clipStartMs)}-${formatMsAsSeconds(clipEndMs)}s " +
                        "session=${formatMsAsSeconds(overlapStart)}-${formatMsAsSeconds(overlapEnd)}s output=${trimmedFile.name}"
                )
                clippedSegments.add(
                    VideoSegment(
                        file = trimmedFile,
                        startMs = overlapStart,
                        durationMs = overlapDuration,
                        absoluteStartMs = segment.absoluteStartMs + clipStartMs
                    )
                )
            }

            if (!matched) {
                Log.w(
                    TAG,
                    "Gemini window unmatched: ${formatMsAsSeconds(windowStart)}-${formatMsAsSeconds(windowEnd)}s ($sessionId)"
                )
            }
        }

        Log.d(TAG, "Gemini highlight mapping complete: ${clippedSegments.size} clips ($sessionId)")
        return clippedSegments.sortedBy { it.startMs }
    }

    private fun capSegmentsToMaxDuration(
        segments: List<VideoSegment>
    ): List<VideoSegment> {
        if (segments.isEmpty()) return segments
        var total = 0L
        val capped = mutableListOf<VideoSegment>()
        for (segment in segments.sortedBy { it.startMs }) {
            val duration = segment.durationMs.coerceAtLeast(0L)
            if (total + duration <= MAX_REPLAY_DURATION_MS || capped.isEmpty()) {
                capped.add(segment)
                total += duration
            }
            if (total >= MAX_REPLAY_DURATION_MS) break
        }
        return capped
    }

    private suspend fun selectSegmentsByImportance(
        sessionId: String,
        segments: List<VideoSegment>
    ): List<VideoSegment> {
        if (segments.isEmpty()) return segments

        val totalDuration = segments.sumOf { it.durationMs.coerceAtLeast(0L) }
        if (totalDuration <= MAX_REPLAY_DURATION_MS) {
            return segments.sortedBy { it.startMs }
        }

        val logs = localRepository.getSessionLogs(sessionId)
        val screamEvents = logs.filter { it.type == MediaType.SCREAM_EVENT && it.decibel != null }
        val guide = localRepository.getPerspectiveGuide(sessionId)

        val ranked = segments.map { segment ->
            val score = computeSegmentScore(segment, screamEvents, guide)
            RankedSegment(segment = segment, score = score)
        }

        val sorted = ranked.sortedWith(
            compareByDescending<RankedSegment> { it.score }
                .thenBy { it.segment.startMs }
        )

        var total = 0L
        val selected = mutableListOf<VideoSegment>()
        for (entry in sorted) {
            val duration = entry.segment.durationMs.coerceAtLeast(0L)
            if (selected.isEmpty() || total + duration <= MAX_REPLAY_DURATION_MS) {
                selected.add(entry.segment)
                total += duration
            }
            if (total >= MAX_REPLAY_DURATION_MS) break
        }

        if (selected.isEmpty()) {
            return segments.sortedBy { it.startMs }.take(1)
        }

        return selected.sortedBy { it.startMs }
    }

    private suspend fun computeSegmentScore(
        segment: VideoSegment,
        screamEvents: List<MediaLogEntity>,
        guide: PerspectiveGuideConfig?
    ): Float {
        val midpoint = segment.absoluteStartMs + (segment.durationMs / 2)
        val bestEventScore = screamEvents.mapNotNull { event ->
            val decibel = event.decibel ?: return@mapNotNull null
            val distanceMs = abs(event.timestamp - midpoint)
            if (distanceMs > SEGMENT_EVENT_WINDOW_MS) return@mapNotNull null
            val distancePenalty = (distanceMs / 1000f) * SEGMENT_EVENT_DISTANCE_PENALTY
            (decibel.toFloat() - distancePenalty).coerceAtLeast(0f)
        }.maxOrNull()

        if (bestEventScore != null) {
            return bestEventScore
        }

        val motionScore = computeMotionScore(segment.file, guide)
        return motionScore * MOTION_SCORE_WEIGHT
    }

    private suspend fun computeMotionScore(
        file: File,
        guide: PerspectiveGuideConfig?
    ): Float = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0f
        val retriever = MediaMetadataRetriever()
        var firstFrame: Bitmap? = null
        var secondFrame: Bitmap? = null
        var firstScaled: Bitmap? = null
        var secondScaled: Bitmap? = null
        try {
            retriever.setDataSource(file.absolutePath)
            val durationMs =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: return@withContext 0f
            if (durationMs <= 0L) return@withContext 0f

            val durationUs = durationMs * 1000L
            val firstUs = minOf(500_000L, durationUs / 3)
            val secondUs = minOf(2_000_000L, (durationUs * 2) / 3)

            firstFrame = retriever.getFrameAtTime(firstUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            secondFrame = retriever.getFrameAtTime(secondUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (firstFrame == null || secondFrame == null) return@withContext 0f

            val targetWidth = 160
            val localFirstScaled = scaleForSampling(firstFrame, targetWidth)
            val localSecondScaled = scaleForSampling(secondFrame, targetWidth)
            firstScaled = localFirstScaled
            secondScaled = localSecondScaled
            val bounds = computeRoiBounds(guide, localFirstScaled.width, localFirstScaled.height)
            if (bounds.width <= 0 || bounds.height <= 0) return@withContext 0f

            val step = maxOf(1, minOf(bounds.width, bounds.height) / 32)
            var diffSum = 0f
            var count = 0
            for (y in bounds.top until bounds.bottom step step) {
                for (x in bounds.left until bounds.right step step) {
                    val c1 = localFirstScaled.getPixel(x, y)
                    val c2 = localSecondScaled.getPixel(x, y)
                    diffSum += abs(luminance(c1) - luminance(c2))
                    count++
                }
            }

            if (count == 0) 0f else diffSum / count
        } catch (e: Exception) {
            Log.e(TAG, "Motion scoring failed for ${file.name}", e)
            0f
        } finally {
            if (firstScaled != null && firstScaled !== firstFrame) {
                firstScaled?.recycle()
            }
            if (secondScaled != null && secondScaled !== secondFrame) {
                secondScaled?.recycle()
            }
            try { retriever.release() } catch (e: Exception) {}
            firstFrame?.recycle()
            secondFrame?.recycle()
        }
    }

    private data class RoiBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
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
        guide: PerspectiveGuideConfig?,
        width: Int,
        height: Int
    ): RoiBounds {
        if (guide == null) {
            return RoiBounds(0, 0, width, height)
        }

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

    private fun validateTimelineAlignment(
        sessionId: String,
        videos: List<MediaLogEntity>,
        audioLog: MediaLogEntity?,
        sessionStart: Long?
    ) {
        if (sessionStart == null) {
            Log.w(TAG, "Timeline validation skipped: sessionStart missing ($sessionId)")
            return
        }

        val videoStart = videos.minByOrNull { it.timestamp }?.timestamp
        val audioStart = audioLog?.timestamp
        if (videoStart == null || audioStart == null) {
            Log.w(
                TAG,
                "Timeline validation skipped: missing videoStart=$videoStart audioStart=$audioStart ($sessionId)"
            )
            return
        }

        val videoOffsetMs = (videoStart - sessionStart).coerceAtLeast(0L)
        val audioOffsetMs = (audioStart - sessionStart).coerceAtLeast(0L)
        val driftMs = abs(videoOffsetMs - audioOffsetMs)
        if (driftMs > TIMESTAMP_DRIFT_WARN_MS) {
            Log.w(
                TAG,
                "Timeline drift warning: videoOffset=$videoOffsetMs audioOffset=$audioOffsetMs drift=$driftMs ($sessionId)"
            )
        } else {
            Log.d(
                TAG,
                "Timeline alignment OK: videoOffset=$videoOffsetMs audioOffset=$audioOffsetMs drift=$driftMs ($sessionId)"
            )
        }
    }

    private suspend fun trimSegmentsAroundScreams(
        sessionId: String,
        segments: List<VideoSegment>,
        screamEvents: List<MediaLogEntity>
    ): List<VideoSegment> {
        if (segments.isEmpty() || screamEvents.isEmpty()) return segments

        val trimmed = mutableListOf<VideoSegment>()
        for (segment in segments) {
            val screamEvent = findBestScreamEventForSegment(segment, screamEvents)
            if (screamEvent == null) {
                trimmed.add(segment)
                continue
            }
            trimmed.add(trimSegmentAroundEvent(sessionId, segment, screamEvent))
        }
        return trimmed
    }

    private fun findBestScreamEventForSegment(
        segment: VideoSegment,
        screamEvents: List<MediaLogEntity>
    ): MediaLogEntity? {
        val start = segment.absoluteStartMs
        val end = start + segment.durationMs.coerceAtLeast(0L)
        val midpoint = start + (segment.durationMs / 2)
        val candidates = screamEvents.filter { event ->
            event.decibel != null && event.timestamp in start..end
        }
        if (candidates.isEmpty()) return null
        return candidates.maxWithOrNull(
            compareBy<MediaLogEntity> { it.decibel ?: 0 }
                .thenBy { -abs(it.timestamp - midpoint) }
        )
    }

    private suspend fun trimSegmentAroundEvent(
        sessionId: String,
        segment: VideoSegment,
        screamEvent: MediaLogEntity
    ): VideoSegment {
        val eventOffsetMs = screamEvent.timestamp - segment.absoluteStartMs
        if (eventOffsetMs < 0 || eventOffsetMs > segment.durationMs) {
            return segment
        }

        val clipStartMs = (eventOffsetMs - SCREAM_EVENT_PRE_MS).coerceAtLeast(0L)
        val clipEndMs = (eventOffsetMs + SCREAM_EVENT_POST_MS)
            .coerceAtMost(segment.durationMs)
        val clipDurationMs = (clipEndMs - clipStartMs).coerceAtLeast(0L)
        if (clipDurationMs <= 0L) {
            return segment
        }
        if (clipStartMs == 0L && clipEndMs >= segment.durationMs) {
            return segment
        }

        val trimResult = videoStitcher.trimVideoToWindow(
            inputVideo = segment.file,
            startMs = clipStartMs,
            endMs = clipEndMs,
            outputSessionId = sessionId,
            outputLabel = "scream_${screamEvent.timestamp}_${segment.file.nameWithoutExtension}"
        )
        val trimmedFile = trimResult.getOrNull()
        if (trimmedFile == null || !trimmedFile.exists()) {
            return segment
        }

        val newStartMs = segment.startMs + clipStartMs
        val newAbsoluteStartMs = segment.absoluteStartMs + clipStartMs
        return VideoSegment(
            file = trimmedFile,
            startMs = newStartMs,
            durationMs = clipDurationMs,
            absoluteStartMs = newAbsoluteStartMs
        )
    }
    
    companion object {
        private const val TAG = "MediaPipeline"
        private const val MAX_REPLAY_DURATION_MS = 4 * 60_000L
        private const val SEGMENT_EVENT_WINDOW_MS = 90_000L
        private const val SEGMENT_EVENT_DISTANCE_PENALTY = 0.5f
        private const val MOTION_SCORE_WEIGHT = 2.0f
        private const val DEFAULT_SEGMENT_DURATION_MS = 30_000L
        private const val SCREAM_EVENT_PRE_MS = 15_000L
        private const val SCREAM_EVENT_POST_MS = 15_000L
        private const val TIMESTAMP_DRIFT_WARN_MS = 2_000L
    }
}
