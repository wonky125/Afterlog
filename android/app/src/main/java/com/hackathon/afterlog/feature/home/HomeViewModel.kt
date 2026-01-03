package com.hackathon.afterlog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackathon.afterlog.data.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository
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
    
    fun clearTestResult() {
        _testResult.value = null
    }
}
