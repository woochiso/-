package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.HumorAnalysisResponse
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.HumorTrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class HumorTrainingUiState(
    val analyzing: Boolean = false,
    val speechLoading: Boolean = false,
    val result: HumorAnalysisResponse? = null,
    val speechBytes: ByteArray? = null,
    val error: String? = null,
    val speechError: String? = null,
    val requiresLogin: Boolean = false
)

class HumorTrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HumorTrainingRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(HumorTrainingUiState())
    val state = _state.asStateFlow()
    private var pending: Triple<File, Double, String>? = null

    fun analyze(file: File, duration: Double, inputMode: String) {
        if (_state.value.analyzing) return
        pending = Triple(file, duration, inputMode)
        viewModelScope.launch {
            _state.value = _state.value.copy(analyzing = true, error = null, speechError = null, speechBytes = null)
            when (val response = repository.analyze(file, duration, inputMode)) {
                is EmotionServerResult.Success -> {
                    _state.value = _state.value.copy(analyzing = false, result = response.value)
                    response.value.ttsToken?.takeIf { it.isNotBlank() }?.let(::loadSpeech)
                }
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(analyzing = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(analyzing = false, error = response.message)
            }
        }
    }

    fun retry() { pending?.let { analyze(it.first, it.second, it.third) } }
    fun reset() { pending = null; _state.value = HumorTrainingUiState() }
    fun clearSpeech() { _state.value = _state.value.copy(speechBytes = null) }

    private fun loadSpeech(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(speechLoading = true, speechError = null)
            when (val response = repository.speech(token)) {
                is EmotionServerResult.Success -> _state.value = _state.value.copy(speechLoading = false, speechBytes = response.value)
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(speechLoading = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(speechLoading = false, speechError = response.message)
            }
        }
    }
}

