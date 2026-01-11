package com.hackathon.afterlog.feature.result

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.data.model.HighlightSegment
import com.hackathon.afterlog.data.model.TimelineEvent
import com.hackathon.afterlog.data.repository.GeminiRepository
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.data.repository.TtsRepository
import com.hackathon.afterlog.domain.MediaPipelineUseCase
import com.hackathon.afterlog.feature.report.debug.DebugConfig
import com.hackathon.afterlog.service.AudioPlayerManager
import com.hackathon.afterlog.service.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import com.hackathon.afterlog.data.util.GeminiJsonUtils
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val localRepository: LocalRepository,
    private val ttsRepository: TtsRepository,
    private val mediaPipelineUseCase: MediaPipelineUseCase,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    // Expose playing state directly from the manager
    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying

    private var currentAudioFile: File? = null
    private val _isTtsLoading = MutableStateFlow(false)
    val isTtsLoading: StateFlow<Boolean> = _isTtsLoading.asStateFlow()

    fun toggleNarration(text: String) {
        if (isTtsLoading.value) return // Prevent race condition (double clicks)

        viewModelScope.launch {
            if (isPlaying.value) {
                audioPlayerManager.stop()
            } else {
                if (currentAudioFile == null || !currentAudioFile!!.exists()) {
                    _isTtsLoading.value = true
                    // Combine headline + summary + verdict for a good narration flow
                    // The text passed in might be just the summary, but let's assume we want a full report read.
                    // For now, let's use the passed text.
                    val generatedFile = ttsRepository.synthesizeText(text)
                    _isTtsLoading.value = false

                    if (generatedFile == null) {
                         Log.e("GameResultVM", "TTS Synthesis failed - received null file")
                         // Ideally we would set an error state here, or a one-shot event
                         return@launch
                    }
                    currentAudioFile = generatedFile
                }
                currentAudioFile?.let { audioPlayerManager.playFile(it) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
        currentAudioFile?.delete()
    }

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadSessionData(sessionId: String) {
        viewModelScope.launch {
            analyzeSession(sessionId, allowMockData = true)
        }
    }

    fun reanalyzeSession(sessionId: String) {
        viewModelScope.launch {
            analyzeSession(sessionId, allowMockData = false)
        }
    }

    private suspend fun analyzeSession(
        sessionId: String,
        allowMockData: Boolean
    ) {
        try {
            _uiState.value = ResultUiState.Loading

            if (allowMockData && DebugConfig.USE_MOCK_DATA) {
                Log.w("GameResultVM", "Debug mock data enabled; skipping live Gemini analysis.")
                setMockData(DebugConfig.ACTIVE_MOCK_DATA)
                return
            }

            // 1. Fetch Logs from DB
            val logs = localRepository.getSessionLogs(sessionId)
            Log.d("GameResultVM", "Loaded logs for session $sessionId: ${logs.size} items")

            if (logs.isEmpty()) {
                if (sessionId == "last_session") {
                    _uiState.value = ResultUiState.Error("No data found (Mock Disabled)")
                    return
                } else {
                    Log.w("GameResultVM", "No logs found, aborting analysis.")
                    _uiState.value = ResultUiState.Error("No logs found for session $sessionId")
                    return
                }
            }

            val resolvedSessionId = logs.firstOrNull()?.sessionId ?: sessionId

            // 2. Identify videos by MediaType (not filename)
            val videoFiles = logs
                .filter { it.type == MediaType.VIDEO_HIGHLIGHT || it.type == MediaType.VIDEO_CHUNK }
                .map { File(it.filePath) }
                .filter { it.exists() }

            // 3. Find Audio File
            val audioLog = logs.firstOrNull { it.filePath.endsWith(".pcm") }
            val audioFile = audioLog?.let { File(it.filePath) }
            val sessionStart = localRepository.getSessionStartTime(resolvedSessionId)

            // 3. Trigger Gemini Analysis (REAL CONNECTION)
            _uiState.value = ResultUiState.Analyzing(logs)

            val contextData = "Session: $resolvedSessionId. Clues found: ${logs.size}. " +
                "Highest noise detected: ${logs.maxByOrNull { it.decibel ?: 0 }?.decibel} dB."

            Log.d("GameResultVM", "Calling GeminiRepository.generateInvestigativeReport...")

            // Pass audioFile (even if PCM, it will just be a placeholder or processed in Repo phase 2)
            var rawResponse = try {
                geminiRepository.generateInvestigativeReport(videoFiles, audioFile, contextData)
            } catch (e: Exception) {
                Log.e("GameResultVM", "Failed to generate report", e)
                _uiState.value = ResultUiState.Error("Failed to analyze media: ${e.message}")
                return
            }

            // 4. Safe JSON Parsing
            var parsedReport = parseGeminiResponse(rawResponse)
            if (parsedReport == null) {
                Log.w("GameResultVM", "Report parse failed; retrying once")
                rawResponse = try {
                    geminiRepository.generateInvestigativeReport(videoFiles, audioFile, contextData)
                } catch (e: Exception) {
                    Log.e("GameResultVM", "Retry failed to generate report", e)
                    rawResponse
                }
                parsedReport = parseGeminiResponse(rawResponse)
            }

            val forceHighlightFallback = parsedReport == null
            val segmentedHighlights = buildSegmentedHighlights(
                sessionId = resolvedSessionId,
                logs = logs,
                audioFile = audioFile,
                sessionStart = sessionStart,
                contextData = contextData,
                allowShortSession = forceHighlightFallback
            )
            val highlightsForReplay = when {
                segmentedHighlights.isNotEmpty() -> segmentedHighlights
                parsedReport != null -> parsedReport.highlightSegments
                else -> emptyList()
            }
            val reportForReplay = if (parsedReport != null) {
                parsedReport.copy(highlightSegments = highlightsForReplay)
            } else {
                Log.w("GameResultVM", "Report still invalid; using highlight-only replay fallback")
                buildFallbackReport(highlightsForReplay)
            }

            val subtitlePath = logs
                .lastOrNull { it.type == MediaType.SUBTITLE && File(it.filePath).exists() }
                ?.filePath

            _uiState.value = ResultUiState.Success(
                report = reportForReplay,
                rawText = if (parsedReport == null) rawResponse else null,
                logs = logs,
                replayVideoPath = null,
                subtitlePath = subtitlePath,
                isReplayGenerating = reportForReplay != null
            )

            if (reportForReplay != null) {
                val narrationText = buildNarrationText(reportForReplay)

                viewModelScope.launch {
                    val replayResult = mediaPipelineUseCase.generateReplayWithNarration(
                        sessionId = resolvedSessionId,
                        narrationText = narrationText,
                        highlightSegments = reportForReplay.highlightSegments
                    )

                    val assets = replayResult.getOrNull()
                    _uiState.update { state ->
                        if (state is ResultUiState.Success) {
                            if (assets != null) {
                                state.copy(
                                    replayVideoPath = assets.videoFile.absolutePath,
                                    subtitlePath = assets.subtitleFile?.absolutePath,
                                    isReplayGenerating = false
                                )
                            } else {
                                state.copy(isReplayGenerating = false)
                            }
                        } else {
                            state
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GameResultVM", "Error in analyzeSession", e)
            _uiState.value = ResultUiState.Error("Error: ${e.message}")
        }
    }

    private suspend fun buildSegmentedHighlights(
        sessionId: String,
        logs: List<MediaLogEntity>,
        audioFile: File?,
        sessionStart: Long?,
        contextData: String,
        allowShortSession: Boolean
    ): List<HighlightSegment> {
        if (audioFile == null || !audioFile.exists()) {
            Log.w("GameResultVM", "Segmented highlights skipped: audio file missing ($sessionId)")
            return emptyList()
        }
        if (sessionStart == null) {
            Log.w("GameResultVM", "Segmented highlights skipped: sessionStart missing ($sessionId)")
            return emptyList()
        }

        val durationSec = estimateAudioDurationSec(audioFile)
        if (durationSec == null) {
            Log.w("GameResultVM", "Segmented highlights skipped: audio duration unknown ($sessionId)")
            return emptyList()
        }
        if (durationSec <= HIGHLIGHT_SEGMENT_SECONDS && !allowShortSession) {
            return emptyList()
        }

        if (allowShortSession && durationSec <= HIGHLIGHT_SEGMENT_SECONDS) {
            Log.d("GameResultVM", "Using single-window highlight fallback (${formatTime(durationSec)})")
        }
        val segmentCount = if (durationSec <= HIGHLIGHT_SEGMENT_SECONDS) {
            1
        } else {
            ceil(durationSec / HIGHLIGHT_SEGMENT_SECONDS).toInt().coerceAtLeast(1)
        }
        val videoLogs = logs.filter {
            it.type == MediaType.VIDEO_CHUNK || it.type == MediaType.VIDEO_HIGHLIGHT
        }
        val collected = mutableListOf<HighlightSegment>()
        val chunkDurationMs = AppConstants.Video.CHUNK_DURATION_MS

        for (index in 0 until segmentCount) {
            val startSec = index * HIGHLIGHT_SEGMENT_SECONDS
            val endSec = min(durationSec, startSec + HIGHLIGHT_SEGMENT_SECONDS)
            if (endSec <= startSec) continue

            val windowStartMs = sessionStart + (startSec * 1000).toLong()
            val windowEndMs = sessionStart + (endSec * 1000).toLong()
            val windowVideos = videoLogs.filter { log ->
                val logStart = log.timestamp
                val logEnd = logStart + chunkDurationMs
                logEnd >= windowStartMs && logStart <= windowEndMs
            }.map { File(it.filePath) }
                .filter { it.exists() }

            val windowLabel = "segment ${index + 1}/$segmentCount ${formatTime(startSec)}-${formatTime(endSec)}"
            val windowHighlights = geminiRepository.generateHighlightSegmentsForWindow(
                videoFiles = windowVideos,
                audioFile = audioFile,
                contextData = contextData,
                windowStartSec = startSec,
                windowEndSec = endSec,
                windowLabel = windowLabel
            )

            if (windowHighlights.isNotEmpty()) {
                Log.d(
                    "GameResultVM",
                    "Segmented highlights $windowLabel: ${windowHighlights.size} segments"
                )
            }

            windowHighlights.forEach { segment ->
                val adjusted = segment.copy(
                    startSec = segment.startSec + startSec,
                    endSec = segment.endSec + startSec
                )
                collected.add(adjusted)
            }
        }

        // Fallback: Generate default segments if Gemini returned nothing
        if (collected.isEmpty()) {
            Log.w("GameResultVM", "Gemini returned no highlights, using fallback segments ($sessionId)")
            return geminiRepository.generateFallbackHighlights(durationSec)
        }

        val merged = mergeHighlightSegments(collected, durationSec)
        if (merged.isNotEmpty()) {
            Log.d(
                "GameResultVM",
                "Segmented highlight merge complete: ${merged.size} segments ($sessionId)"
            )
        }
        return merged
    }

    private fun estimateAudioDurationSec(audioFile: File): Double? {
        val dataLength = when (audioFile.extension.lowercase()) {
            "wav" -> (audioFile.length() - WAV_HEADER_BYTES).coerceAtLeast(0L)
            "pcm" -> audioFile.length()
            else -> {
                Log.w("GameResultVM", "Unknown audio extension: ${audioFile.extension}")
                return null
            }
        }
        if (dataLength <= 0L) return null
        val bytesPerSecond = AppConstants.Audio.SAMPLE_RATE * PCM_BYTES_PER_SAMPLE
        return dataLength / bytesPerSecond.toDouble()
    }

    private fun mergeHighlightSegments(
        segments: List<HighlightSegment>,
        maxDurationSec: Double
    ): List<HighlightSegment> {
        if (segments.isEmpty()) return emptyList()
        val sorted = segments.sortedBy { it.startSec }
        val merged = mutableListOf<HighlightSegment>()
        var current = sorted.first()

        for (next in sorted.drop(1)) {
            if (next.startSec <= current.endSec + HIGHLIGHT_MERGE_GAP_SEC) {
                val newEnd = max(current.endSec, next.endSec)
                val reason = mergeReason(current.reason, next.reason)
                current = current.copy(endSec = newEnd, reason = reason)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        return merged.map { segment ->
            val clampedStart = segment.startSec.coerceIn(0.0, maxDurationSec)
            val clampedEnd = segment.endSec.coerceIn(clampedStart, maxDurationSec)
            segment.copy(startSec = clampedStart, endSec = clampedEnd)
        }
    }

    private fun mergeReason(first: String, second: String): String {
        if (first.isBlank()) return second
        if (second.isBlank()) return first
        return if (first == second) first else "$first / $second"
    }

    private fun formatTime(seconds: Double): String {
        val totalSeconds = seconds.toLong().coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val secs = totalSeconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    private fun buildFallbackReport(
        highlights: List<HighlightSegment>
    ): GeminiReport {
        return GeminiReport(
            headline = "SYSTEM ERROR",
            summary = "Analysis failed; replay uses highlights only.",
            atmosphere = "",
            article = "The investigation stalled. Highlights are reconstructed from the captured evidence.",
            timeline = emptyList(),
            verdict = "Investigation aborted.",
            highlightSegments = highlights
        )
    }

    companion object {
        private const val HIGHLIGHT_SEGMENT_SECONDS = 15 * 60.0
        private const val HIGHLIGHT_MERGE_GAP_SEC = 1.0
        private const val WAV_HEADER_BYTES = 44L
        private const val PCM_BYTES_PER_SAMPLE = 2
    }

    private fun parseGeminiResponse(rawText: String): GeminiReport? {
        return try {
            val cleanedJson = GeminiJsonUtils.cleanMarkdownJson(rawText)
            json.decodeFromString<GeminiReport>(cleanedJson)

        } catch (e: SerializationException) {
            Log.e("GameResultVM", "JSON parsing failed, fallback to raw", e)
            null
        } catch (e: Exception) {
            Log.e("GameResultVM", "Unexpected parsing error", e)
            null
        }
    }

    private fun buildNarrationText(report: GeminiReport): String {
        return """
            ${report.headline}.
            ${report.summary}.
            ${report.article}.
            Verdict: ${report.verdict}
        """.trimIndent()
    }

    fun regenerateReplay(sessionId: String) {
        val currentState = _uiState.value
        if (currentState !is ResultUiState.Success) return
        val report = currentState.report ?: return
        if (currentState.isReplayGenerating) return

        val actualSessionId = currentState.logs.firstOrNull()?.sessionId ?: sessionId
        val narrationText = buildNarrationText(report)

        _uiState.value = currentState.copy(isReplayGenerating = true)
        viewModelScope.launch {
            val replayResult = mediaPipelineUseCase.generateReplayWithNarration(
                sessionId = actualSessionId,
                narrationText = narrationText,
                highlightSegments = report.highlightSegments
            )

            val assets = replayResult.getOrNull()
            _uiState.update { state ->
                if (state is ResultUiState.Success) {
                    if (assets != null) {
                        state.copy(
                            replayVideoPath = assets.videoFile.absolutePath,
                            subtitlePath = assets.subtitleFile?.absolutePath,
                            isReplayGenerating = false
                        )
                    } else {
                        state.copy(isReplayGenerating = false)
                    }
                } else {
                    state
                }
            }
        }
    }

    /**
     * For Debug Mode: Directly inject mock data into the UI state
     */
    fun setMockData(mockReport: GeminiReport) {
        // Use a dummy log list for the UI state if needed, or empty
        val mockLogs = emptyList<MediaLogEntity>()
        
        _uiState.value = ResultUiState.Success(
            report = mockReport,
            rawText = null,
            logs = mockLogs,
            replayVideoPath = null,
            subtitlePath = null,
            isReplayGenerating = false
        )
    }
}

sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Analyzing(val logs: List<MediaLogEntity>) : ResultUiState()
    data class Success(
        val report: GeminiReport?,
        val rawText: String?,
        val logs: List<MediaLogEntity>,
        val replayVideoPath: String?,
        val subtitlePath: String?,
        val isReplayGenerating: Boolean
    ) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}
