package com.hackathon.afterlog.feature.report

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackathon.afterlog.feature.report.components.CinematicLoadingView
import com.hackathon.afterlog.feature.report.components.ErrorView
import com.hackathon.afterlog.feature.report.components.RawTextFallbackView
import com.hackathon.afterlog.feature.report.components.InvestigationReportView
import com.hackathon.afterlog.feature.report.debug.DebugConfig
import com.hackathon.afterlog.feature.result.GameResultViewModel
import com.hackathon.afterlog.feature.result.ResultUiState
import kotlinx.coroutines.delay

private const val TAG = "ReportDetailScreen"

@Composable
fun ReportDetailScreen(
    viewModel: GameResultViewModel = hiltViewModel(),
    sessionId: String = "last_session",
    onNavigateToVideo: (String) -> Unit = {}
) {
    Log.d(TAG, "ReportDetailScreen composable launched with sessionId: $sessionId")
    val uiState by viewModel.uiState.collectAsState()
    var showLoadingScreen by remember { mutableStateOf(true) }

    // This effect ensures a minimum loading time for cinematic effect
    // and triggers data loading or mock data injection.
    LaunchedEffect(sessionId) {
        Log.d(TAG, "LaunchedEffect for data loading triggered.")
        val minLoadingTime = 3000L
        val startTime = System.currentTimeMillis()

        // DEBUG MODE CHECK
        if (DebugConfig.USE_MOCK_DATA) {
            Log.w(TAG, "DEBUG MODE ACTIVE: Using Mock Data (${DebugConfig.ACTIVE_MOCK_DATA.headline})")
            viewModel.setMockData(DebugConfig.ACTIVE_MOCK_DATA)
        } else {
            // REAL LOAD
            Log.d(TAG, "Calling viewModel.loadSessionData...")
            viewModel.loadSessionData(sessionId)
            Log.d(TAG, "viewModel.loadSessionData call finished.")
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < minLoadingTime) {
            delay(minLoadingTime - elapsedTime)
        }

        Log.d(TAG, "Minimum loading time passed. Setting showLoadingScreen to false.")
        showLoadingScreen = false
    }

    var showBootSequence by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showBootSequence) {
            com.hackathon.afterlog.feature.report.components.BootSequenceAnimation {
                showBootSequence = false
            }
        } else if (showLoadingScreen || uiState is ResultUiState.Loading || uiState is ResultUiState.Analyzing) {
            val logCount = if (uiState is ResultUiState.Analyzing) (uiState as ResultUiState.Analyzing).logs.size else 0
            CinematicLoadingView(count = logCount)
        } else {
            when (val state = uiState) {
                is ResultUiState.Error -> {
                    ErrorView(state.message)
                }
                is ResultUiState.Success -> {
                    if (state.report != null) {
                        val isPlaying by viewModel.isPlaying.collectAsState()
                        val isTtsLoading by viewModel.isTtsLoading.collectAsState()

                        val videoPath = state.replayVideoPath
                            ?: state.logs
                                .find { it.filePath.contains("highlight") && java.io.File(it.filePath).exists() }
                                ?.filePath

                        InvestigationReportView(
                            report = state.report,
                            videoPath = videoPath,
                            isPlaying = isPlaying,
                            isTtsLoading = isTtsLoading,
                            isReplayGenerating = state.isReplayGenerating,
                            onPlayClick = {
                                val textToRead = """
                                    ${state.report.headline}. 
                                    ${state.report.summary}. 
                                    ${state.report.article}.
                                    Verdict: ${state.report.verdict}
                                """.trimIndent()
                                viewModel.toggleNarration(textToRead)
                            },
                            onVideoClick = onNavigateToVideo
                        )
                    } else {
                        RawTextFallbackView(state.rawText ?: "No evidence found.", state.logs)
                    }
                }
                is ResultUiState.Loading, is ResultUiState.Analyzing -> {
                    /* Do nothing */
                }
            }
        }
    }
}
