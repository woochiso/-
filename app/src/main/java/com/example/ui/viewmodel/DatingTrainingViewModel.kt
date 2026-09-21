package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.DatingTrainingRepository
import com.example.data.repository.EmotionServerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class DatingMessage(val role: String, val text: String, val ttsToken: String? = null)
enum class DatingStage { SETUP, CHAT, RESULT }
data class DatingTrainingUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val speechLoading: Boolean = false,
    val config: DatingConfigResponse? = null,
    val selectedCategory: String = "",
    val selectedStyle: String = "",
    val selectedVoice: String = "",
    val stage: DatingStage = DatingStage.SETUP,
    val practiceId: String? = null,
    val scenario: String = "",
    val turns: Int = 0,
    val maxTurns: Int = 12,
    val messages: List<DatingMessage> = emptyList(),
    val result: DatingActionResponse? = null,
    val speechBytes: ByteArray? = null,
    val speechToken: String? = null,
    val error: String? = null,
    val speechError: String? = null,
    val requiresLogin: Boolean = false
)

class DatingTrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatingTrainingRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(DatingTrainingUiState())
    val state = _state.asStateFlow()
    private var pendingAction: DatingActionRequest? = null

    init { loadConfig() }
    fun loadConfig() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        when (val r = repository.config()) {
            is EmotionServerResult.Success -> {
                val c = r.value
                _state.value = _state.value.copy(loading = false, config = c,
                    selectedCategory = _state.value.selectedCategory.ifBlank { c.categories.firstOrNull()?.id.orEmpty() },
                    selectedStyle = _state.value.selectedStyle.ifBlank { c.styles.firstOrNull()?.id.orEmpty() },
                    selectedVoice = _state.value.selectedVoice.ifBlank { c.voices.firstOrNull()?.id.orEmpty() }, maxTurns = c.maxTurns)
            }
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(loading = false, requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(loading = false, error = r.message)
        }
    }
    fun selectCategory(id: String) { _state.value = _state.value.copy(selectedCategory = id) }
    fun selectStyle(id: String) { _state.value = _state.value.copy(selectedStyle = id) }
    fun selectVoice(id: String) { _state.value = _state.value.copy(selectedVoice = id) }

    fun start() = execute(DatingActionRequest("start", UUID.randomUUID().toString(), category = _state.value.selectedCategory, style = _state.value.selectedStyle))
    fun reply(text: String) {
        val value = text.trim(); val s = _state.value
        if (value.isBlank() || s.practiceId == null || s.sending) return
        _state.value = s.copy(messages = s.messages + DatingMessage("user", value), error = null)
        execute(DatingActionRequest("reply", UUID.randomUUID().toString(), practiceId = s.practiceId, expectedTurn = s.turns, text = value))
    }
    fun finish() { _state.value.practiceId?.let { execute(DatingActionRequest("finish", UUID.randomUUID().toString(), practiceId = it)) } }
    fun retry() { pendingAction?.let(::execute) }
    fun newPractice() {
        val id = _state.value.practiceId
        if (id == null) resetToSetup() else execute(DatingActionRequest("finish_ack", UUID.randomUUID().toString(), practiceId = id))
    }
    fun clearSpeech() { _state.value = _state.value.copy(speechBytes = null) }

    private fun execute(request: DatingActionRequest) {
        if (_state.value.sending) return
        pendingAction = request
        viewModelScope.launch {
            _state.value = _state.value.copy(sending = true, error = null, speechError = null)
            when (val r = repository.action(request)) {
                is EmotionServerResult.Success -> applyResponse(request.action, r.value)
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(sending = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(sending = false, error = r.message)
            }
        }
    }
    private fun applyResponse(action: String, r: DatingActionResponse) {
        if (!r.success) { _state.value = _state.value.copy(sending = false, error = r.message ?: "요청을 처리하지 못했습니다."); return }
        when (action) {
            "start" -> _state.value = _state.value.copy(sending = false, stage = DatingStage.CHAT, practiceId = r.practiceId,
                scenario = r.scenario.orEmpty(), turns = r.turns, maxTurns = r.maxTurns,
                messages = listOf(DatingMessage("assistant", r.partnerLine.orEmpty(), r.ttsToken)))
            "reply" -> {
                val text = r.safetyMessage ?: r.partnerLine.orEmpty()
                _state.value = _state.value.copy(sending = false, stage = if (r.finished) DatingStage.RESULT else DatingStage.CHAT,
                    turns = r.turns, maxTurns = r.maxTurns, messages = _state.value.messages + DatingMessage("assistant", text, r.ttsToken))
            }
            "finish" -> _state.value = _state.value.copy(sending = false, stage = DatingStage.RESULT, result = r)
            "finish_ack", "discard" -> resetToSetup()
            else -> _state.value = _state.value.copy(sending = false)
        }
        r.ttsToken?.takeIf(String::isNotBlank)?.let(::loadSpeech)
    }
    fun replaySpeech(token: String) = loadSpeech(token)
    private fun loadSpeech(token: String) = viewModelScope.launch {
        _state.value = _state.value.copy(speechLoading = true, speechError = null, speechBytes = null, speechToken = token)
        when (val r = repository.speech(token, _state.value.selectedVoice)) {
            is EmotionServerResult.Success -> _state.value = _state.value.copy(speechLoading = false, speechBytes = r.value)
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(speechLoading = false, requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(speechLoading = false, speechError = r.message)
        }
    }
    private fun resetToSetup() { pendingAction = null; _state.value = _state.value.copy(sending = false, stage = DatingStage.SETUP, practiceId = null, scenario = "", turns = 0, messages = emptyList(), result = null, speechBytes = null, speechToken = null, error = null) }
}
