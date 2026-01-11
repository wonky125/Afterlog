package com.hackathon.afterlog.feature.result

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.local.entities.MediaType
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.data.model.TimelineEvent
import com.hackathon.afterlog.data.repository.GeminiRepository
import com.hackathon.afterlog.data.repository.LocalRepository
import com.hackathon.afterlog.data.repository.TtsRepository
import com.hackathon.afterlog.domain.MediaPipelineUseCase
import com.hackathon.afterlog.service.AudioPlayerManager
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
            try {
                _uiState.value = ResultUiState.Loading

                // 1. Fetch Logs from DB
                val logs = localRepository.getSessionLogs(sessionId)
                Log.d("GameResultVM", "Loaded logs for session $sessionId: ${logs.size} items")

                if (logs.isEmpty()) {
                    if (sessionId == "last_session") {
                        // ... (Mock logic - omitted)
                         _uiState.value = ResultUiState.Error("No data found (Mock Disabled)")
                         return@launch
                    } else {
                        Log.w("GameResultVM", "No logs found, aborting analysis.")
                        _uiState.value = ResultUiState.Error("No logs found for session $sessionId")
                        return@launch
                    }
                }

                // 2. Identify "Highlight" videos
                val videoFiles = logs
                    .filter { it.filePath.contains("highlight") }
                    .map { File(it.filePath) }
                    .filter { it.exists() }
                
                // 3. Find Audio File
                val audioLog = logs.firstOrNull { it.filePath.endsWith(".pcm") }
                val audioFile = audioLog?.let { File(it.filePath) }
                
                // 3. Trigger Gemini Analysis (REAL CONNECTION)
                _uiState.value = ResultUiState.Analyzing(logs)

                val contextData = "Session: $sessionId. Clues found: ${logs.size}. " +
                    "Highest noise detected: ${logs.maxByOrNull { it.decibel ?: 0 }?.decibel} dB."

                Log.d("GameResultVM", "Calling GeminiRepository.generateInvestigativeReport...")
                
                // Pass audioFile (even if PCM, it will just be a placeholder or processed in Repo phase 2)
                val rawResponse = try {
                    geminiRepository.generateInvestigativeReport(videoFiles, audioFile, contextData)
                } catch (e: Exception) {
                    Log.e("GameResultVM", "Failed to generate report", e)
                    _uiState.value = ResultUiState.Error("Failed to analyze media: ${e.message}")
                    return@launch
                }

                // 4. Safe JSON Parsing
                val parsedReport = parseGeminiResponse(rawResponse)

                val subtitlePath = logs
                    .lastOrNull { it.type == MediaType.SUBTITLE && File(it.filePath).exists() }
                    ?.filePath

                _uiState.value = ResultUiState.Success(
                    report = parsedReport,
                    rawText = if (parsedReport == null) rawResponse else null,
                    logs = logs,
                    replayVideoPath = null,
                    subtitlePath = subtitlePath,
                    isReplayGenerating = parsedReport != null
                )

                if (parsedReport != null) {
                    val replaySessionId = logs.firstOrNull()?.sessionId ?: sessionId
                    val narrationText = buildNarrationText(parsedReport)

                    viewModelScope.launch {
                        val replayResult = mediaPipelineUseCase.generateReplayWithNarration(
                            sessionId = replaySessionId,
                            narrationText = narrationText
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
                Log.e("GameResultVM", "Error in loadSessionData", e)
                _uiState.value = ResultUiState.Error("Error: ${e.message}")
            }
        }
    }

    private fun parseGeminiResponse(rawText: String): GeminiReport? {
        return try {
            // Remove markdown code blocks if present (Handle both json and no-lang variants)
            var cleanedJson = rawText.trim()
            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.replaceFirst("```json", "", true)
                    .replaceFirst("```", "", true)

                // Remove trailing backticks
                if (cleanedJson.endsWith("```")) {
                    cleanedJson = cleanedJson.substring(0, cleanedJson.lastIndexOf("```"))
                }
            }
            cleanedJson = cleanedJson.trim()
            
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
                narrationText = narrationText
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
