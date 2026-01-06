package com.hackathon.afterlog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.afterlog.data.repository.GeminiRepository
import com.hackathon.afterlog.domain.VideoSynthesisTestHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn

@OptIn(UnstableApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val videoTestHelper: VideoSynthesisTestHelper,
    private val mediaPipelineUseCase: com.hackathon.afterlog.domain.MediaPipelineUseCase
) : ViewModel() {

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    fun testGeminiConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = "Testing..."
            val result = geminiRepository.testConnection()
            _testResult.value = result
            _isTesting.value = false
        }
    }
    
    /**
     * Tests the video synthesis pipeline with auto-generated dummy data.
     * Creates test image + TTS audio -> runs FFmpeg -> produces MP4
     */
    fun testVideoSynthesis() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = "🎬 Testing Video Synthesis..."
            
            when (val result = videoTestHelper.runFullTest()) {
                is VideoSynthesisTestHelper.TestResult.Success -> {
                    _testResult.value = "✅ Video created!\n${result.videoPath}"
                }
                is VideoSynthesisTestHelper.TestResult.Failure -> {
                    _testResult.value = "❌ Failed: ${result.error}"
                }
            }
            
            _isTesting.value = false
        }
    }
    
    fun clearTestResult() {
        _testResult.value = null
    }

    /**
     * Triggers the full media pipeline for the specified session (default: "last_session").
     */
    fun generateReplay(sessionId: String = "last_session") {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = "🔍 Analyzing Session: $sessionId..."
            
            val result = mediaPipelineUseCase.generateReplay(sessionId)
            
            result.fold(
                onSuccess = { videoFile ->
                    _testResult.value = "✅ Replay Ready!\n${videoFile.absolutePath}"
                },
                onFailure = { error ->
                    _testResult.value = "❌ Replay Generation Failed.\n${error.localizedMessage}"
                }
            )
            
            _isTesting.value = false
        }
    }
}
