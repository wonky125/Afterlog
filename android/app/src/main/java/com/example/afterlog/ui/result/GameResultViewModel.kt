package com.example.afterlog.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afterlog.data.local.entities.MediaLogEntity
import com.example.afterlog.data.repository.GeminiRepository
import com.example.afterlog.data.repository.LocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val localRepository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadSessionData(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = ResultUiState.Loading
            
            // 1. Fetch Logs from DB
            // Assuming we have a getSessionLogs function in LocalRepository
            // Use dummy data or implement retrieval if needed
             val logs = localRepository.getSessionLogs(sessionId)
             // val logs = emptyList<MediaLog>() // Placeholder

            if (logs.isEmpty()) {
                _uiState.value = ResultUiState.Error("No logs found for session $sessionId")
                return@launch
            }

            // 2. Identify "Highlight" videos
            // Videos saved in 'session_media/highlight_...'
            val videoFiles = logs
                .filter { it.filePath.contains("highlight") }
                .map { File(it.filePath) }
                .filter { it.exists() }

            if (videoFiles.isEmpty()) {
                 _uiState.value = ResultUiState.Success(
                     report = "No video evidence collected.",
                     logs = logs
                 )
                 return@launch
            }

            // 3. Trigger Gemini Analysis (MOCK MODE)
            // User requested to NOT connect Gemini yet.
            _uiState.value = ResultUiState.Analyzing(logs)
            
            // SIMULATED DELAY & RESPONSE
            kotlinx.coroutines.delay(2000) 
            
            val mockReport = """
                Headline: THE MOCK REPORT
                Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date())}
                
                Observation: 
                [MOCK] This is a dummy analysis. 
                The system detected ${logs.size} evidence logs.
                Real Gemini AI connection is currently DISABLED as per request.
                
                Conclusion:
                The recording stability has been verified. 
                Proceed to enable AI when ready.
            """.trimIndent()
            
            // Real call disabled:
            // val report = geminiRepository.generateInvestigativeReport(...)
            
            _uiState.value = ResultUiState.Success(mockReport, logs)
        }
    }
}

sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Analyzing(val logs: List<MediaLogEntity>) : ResultUiState()
    data class Success(val report: String, val logs: List<MediaLogEntity>) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}
