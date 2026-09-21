package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.RecoveryInsightDataDto
import com.example.data.repository.AiEmotionAnalysisRepository
import com.example.data.repository.EmotionServerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiEmotionAnalysisUiState(
    val loading: Boolean = false,
    val data: RecoveryInsightDataDto? = null,
    val error: String? = null,
    val requiresLogin: Boolean = false,
    val period: String = "30d",
    val customStart: String? = null,
    val customEnd: String? = null,
    val selectedSessionId: String? = null,
    val aiLoading: Boolean = false,
    val aiText: String? = null,
    val aiError: String? = null,
    val aiCached: Boolean = false
)

class AiEmotionAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AiEmotionAnalysisRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(AiEmotionAnalysisUiState())
    val state = _state.asStateFlow()

    fun load() = loadInsight()

    fun selectPeriod(period: String, start: String? = null, end: String? = null) {
        if (period == _state.value.period && start == _state.value.customStart && end == _state.value.customEnd) return
        _state.value = _state.value.copy(period = period, customStart = start, customEnd = end, data = null, aiText = null, aiError = null)
        loadInsight()
    }

    fun selectSession(sessionId: String) { _state.value = _state.value.copy(selectedSessionId = sessionId) }

    fun requestAiAnalysis(force: Boolean = false) {
        val snapshot = _state.value
        val data = snapshot.data ?: return
        if (snapshot.aiLoading || data.comparableRecords < data.minimumPatternRecords) return
        viewModelScope.launch {
            _state.value = _state.value.copy(aiLoading = true, aiError = null)
            when (val result = repository.recoveryAi(snapshot.period, snapshot.customStart, snapshot.customEnd, force)) {
                is EmotionServerResult.Success -> _state.value = _state.value.copy(aiLoading = false, aiText = result.value.analysis?.analysisText, aiCached = result.value.cached, aiError = result.value.message?.takeIf { result.value.analysis == null })
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(aiLoading = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(aiLoading = false, aiError = result.message)
            }
        }
    }

    private fun loadInsight() {
        val snapshot = _state.value
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.recoveryInsight(snapshot.period, snapshot.customStart, snapshot.customEnd)) {
                is EmotionServerResult.Success -> {
                    val data = result.value.data
                    if (!result.value.success || data == null) _state.value = _state.value.copy(loading = false, error = result.value.message ?: "AI 감정분석 데이터를 불러오지 못했습니다.")
                    else {
                        val selected = _state.value.selectedSessionId?.takeIf { id -> data.recentSessions.any { it.sessionId == id } } ?: data.recentSessions.firstOrNull()?.sessionId
                        _state.value = _state.value.copy(loading = false, data = data, selectedSessionId = selected)
                        if (data.comparableRecords >= data.minimumPatternRecords) requestAiAnalysis(false)
                    }
                }
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(loading = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}
