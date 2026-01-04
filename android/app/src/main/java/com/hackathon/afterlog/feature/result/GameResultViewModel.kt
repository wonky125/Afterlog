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
import com.hackathon.afterlog.service.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadSessionData(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = ResultUiState.Loading

            // 1. Fetch Logs from DB
            // Assuming we have a getSessionLogs function in LocalRepository
            // Use dummy data or implement retrieval if needed
             val logs = localRepository.getSessionLogs(sessionId)
             // val logs = emptyList<MediaLog>() // Placeholder

            if (logs.isEmpty()) {
                if (sessionId == "last_session") {
                    // DEBUG: Inject Mock Data for UI Verification
                    Log.d("GameResultVM", "No real logs found, using MOCK DATA for verification.")
                    val mockReport = GeminiReport(
                        headline = "THE MIDNIGHT WHISPER",
                        summary = "A phantom voice was recorded in the living room exactly at 03:00 AM, coincident with a sudden temperature drop.",
                        atmosphere = "Chilling, suspenseful, and undeniably supernatural.",
                        article = "The clock struck three when the first whisper echoed through the empty halls of the old manor. Our investigators—seasoned veterans of the unexplained—froze in place as the temperature plummeted. What happened next would shake even the most hardened skeptic.\n\nMs. Adams was the first to hear it: a faint plea for help, disembodied and desperate. The recording equipment captured every chilling syllable. By dawn, the team had collected evidence that defied all rational explanation.\n\nThis reporter has covered many cases in The Dark City, but none quite like this. The voice on that recording speaks to something beyond our understanding—a cry from the other side that demands to be heard.",
                        timeline = listOf(
                            TimelineEvent("02:59:55", "Environment", "Silence", "Ambient noise level drops significantly.", 30),
                            TimelineEvent("03:00:00", "Unknown Entity", "Whisper", "A faint voice says 'Help me'.", 45),
                            TimelineEvent("03:00:05", "User", "Gasp", "User reacts to the sound.", 60)
                        ),
                        verdict = "High Probability of Class A Apparition."
                    )

                    val mockLogs = listOf(
                         MediaLogEntity(
                             sessionId = "mock",
                             type = MediaType.AUDIO_SICK,
                             filePath = "mock_audio.mp3",
                             timestamp = System.currentTimeMillis(),
                             decibel = 45
                         ),
                         MediaLogEntity(
                             sessionId = "mock",
                             type = MediaType.VIDEO_HIGHLIGHT,
                             filePath = "mock_video.mp4",
                             timestamp = System.currentTimeMillis() + 5000,
                             decibel = 60
                         )
                    )

                    _uiState.value = ResultUiState.Success(
                        report = mockReport,
                        rawText = null,
                        logs = mockLogs
                    )
                    return@launch
                } else {
                    _uiState.value = ResultUiState.Error("No logs found for session $sessionId")
                    return@launch
                }
            }

            // 2. Identify "Highlight" videos
            // Videos saved in 'session_media/highlight_...'
            val videoFiles = logs
                .filter { it.filePath.contains("highlight") }
                .map { File(it.filePath) }
                .filter { it.exists() }

            if (videoFiles.isEmpty()) {
                 _uiState.value = ResultUiState.Success(
                     report = null,
                     rawText = "No video evidence collected.",
                     logs = logs
                 )
                 return@launch
            }

            // Find Audio File
            val audioFile = logs
                .firstOrNull { it.filePath.endsWith(".pcm") } // Currently PCM based on AudioMonitor
                ?.let { File(it.filePath) }

            // 3. Trigger Gemini Analysis (REAL CONNECTION)
            _uiState.value = ResultUiState.Analyzing(logs)

            val contextData = "Session: $sessionId. Clues found: ${logs.size}. " +
                "Highest noise detected: ${logs.maxByOrNull { it.decibel ?: 0 }?.decibel} dB."

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

            _uiState.value = ResultUiState.Success(
                report = parsedReport,
                rawText = if (parsedReport == null) rawResponse else null,
                logs = logs
            )
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
}

sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Analyzing(val logs: List<MediaLogEntity>) : ResultUiState()
    data class Success(
        val report: GeminiReport?,
        val rawText: String?,
        val logs: List<MediaLogEntity>
    ) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}
