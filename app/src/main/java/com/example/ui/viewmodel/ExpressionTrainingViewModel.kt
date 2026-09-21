package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.ExpressionTrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ExpressionStage { CHOOSE, PRACTICE, RESULT }
data class ExpressionTrainingUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val speechLoading: Boolean = false,
    val config: ExpressionConfigResponse? = null,
    val stage: ExpressionStage = ExpressionStage.CHOOSE,
    val category: String = "",
    val categoryTitle: String = "",
    val voice: String = "DEFAULT",
    val practiceId: String? = null,
    val categoryDescription: String = "",
    val scenario: String = "",
    val partnerLine: String = "",
    val inputText: String = "",
    val result: ExpressionActionResponse? = null,
    val speechBytes: ByteArray? = null,
    val speechToken: String? = null,
    val error: String? = null,
    val speechError: String? = null,
    val requiresLogin: Boolean = false
)

class ExpressionTrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpressionTrainingRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(ExpressionTrainingUiState())
    val state = _state.asStateFlow()
    private var pending: ExpressionActionRequest? = null
    init { loadConfig() }

    fun loadConfig() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        when (val r = repository.config()) {
            is EmotionServerResult.Success -> _state.value = _state.value.copy(loading = false, config = r.value, voice = r.value.defaultVoice)
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(loading = false, requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(loading = false, error = r.message)
        }
    }
    fun selectVoice(value: String) { _state.value = _state.value.copy(voice = value) }
    fun selectCategory(id: String) {
        val title = _state.value.config?.categories?.firstOrNull { it.id == id }?.title.orEmpty()
        execute(ExpressionActionRequest("scenario", UUID.randomUUID().toString(), category = id), title)
    }
    fun updateText(value: String) { if (value.length <= 2000) _state.value = _state.value.copy(inputText = value, error = null) }
    fun submit() {
        val s = _state.value; val text = s.inputText.trim()
        if (s.sending || s.practiceId == null || text.isBlank()) return
        execute(ExpressionActionRequest("reply", UUID.randomUUID().toString(), practiceId = s.practiceId, text = text))
    }
    fun retry() { pending?.let { execute(it, _state.value.categoryTitle) } }
    fun sameCategory() { if (_state.value.category.isNotBlank()) selectCategory(_state.value.category) }
    fun chooseOther() { pending = null; _state.value = _state.value.copy(stage = ExpressionStage.CHOOSE, category = "", categoryTitle = "", practiceId = null, scenario = "", partnerLine = "", inputText = "", result = null, speechBytes = null, speechToken = null, error = null) }
    fun clearSpeech() { _state.value = _state.value.copy(speechBytes = null) }
    fun replaySpeech(token: String) = loadSpeech(token)

    private fun execute(request: ExpressionActionRequest, categoryTitle: String = _state.value.categoryTitle) {
        if (_state.value.sending) return
        pending = request
        viewModelScope.launch {
            _state.value = _state.value.copy(sending = true, error = null, speechError = null)
            when (val r = repository.action(request)) {
                is EmotionServerResult.Success -> apply(request.action, r.value, categoryTitle)
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(sending = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(sending = false, error = r.message)
            }
        }
    }
    private fun apply(action: String, r: ExpressionActionResponse, categoryTitle: String) {
        if (!r.success) { _state.value = _state.value.copy(sending = false, error = r.message ?: "요청을 처리하지 못했습니다."); return }
        if (action == "scenario") {
            _state.value = _state.value.copy(sending = false, stage = ExpressionStage.PRACTICE, category = r.category.orEmpty(), categoryTitle = categoryTitle,
                practiceId = r.practiceId, categoryDescription = r.categoryDescription.orEmpty(), scenario = r.scenario.orEmpty(), partnerLine = r.partnerLine.orEmpty(), inputText = "", result = null)
        } else {
            _state.value = _state.value.copy(sending = false, stage = ExpressionStage.RESULT, result = r)
        }
        r.ttsToken?.takeIf(String::isNotBlank)?.let(::loadSpeech)
    }
    private fun loadSpeech(token: String) = viewModelScope.launch {
        _state.value = _state.value.copy(speechLoading = true, speechError = null, speechBytes = null, speechToken = token)
        when (val r = repository.speech(token, _state.value.voice)) {
            is EmotionServerResult.Success -> _state.value = _state.value.copy(speechLoading = false, speechBytes = r.value)
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(speechLoading = false, requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(speechLoading = false, speechError = r.message)
        }
    }
}
