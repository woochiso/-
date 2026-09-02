package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.AiEmotionAnalysisResponse
import com.example.data.repository.AiEmotionAnalysisRepository
import com.example.data.repository.EmotionServerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AiAnalysisTab { OVERALL, HOURLY, PATTERN, STORY }

data class AiEmotionAnalysisUiState(
    val loading: Boolean = false,
    val data: AiEmotionAnalysisResponse? = null,
    val error: String? = null,
    val requiresLogin: Boolean = false,
    val tab: AiAnalysisTab = AiAnalysisTab.OVERALL,
    val period: String = "30d",
    val timelineUnit: String = "hour",
    val visibleCategories: Set<String> = emptySet(),
    val aiLoading: Boolean = false,
    val aiText: String? = null,
    val aiError: String? = null,
    val aiCached: Boolean = false
)

class AiEmotionAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AiEmotionAnalysisRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(AiEmotionAnalysisUiState())
    val state = _state.asStateFlow()

    fun load() = loadStatistics(loadCachedAi = true)

    fun selectTab(tab: AiAnalysisTab) { _state.value = _state.value.copy(tab = tab) }

    fun selectPeriod(period: String) {
        if (_state.value.period == period) return
        _state.value = _state.value.copy(period = period, aiText = null, aiError = null, aiCached = false)
        loadStatistics(loadCachedAi = true)
    }

    fun selectTimelineUnit(unit: String) {
        if (_state.value.timelineUnit == unit || unit !in setOf("hour", "day", "week", "month")) return
        _state.value = _state.value.copy(timelineUnit = unit)
        loadStatistics(loadCachedAi = false)
    }

    fun toggleCategory(code: String) {
        val current = _state.value.visibleCategories
        _state.value = _state.value.copy(visibleCategories = if (code in current) current - code else current + code)
    }

    fun requestAiAnalysis(force: Boolean = false) {
        val period = _state.value.period
        if (_state.value.aiLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(aiLoading = true, aiError = null)
            when (val result = repository.createSummary(period, force)) {
                is EmotionServerResult.Success -> _state.value = _state.value.copy(aiLoading = false, aiText = result.value.analysis?.analysisText, aiCached = result.value.cached, aiError = result.value.message?.takeIf { result.value.analysis == null })
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(aiLoading = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(aiLoading = false, aiError = result.message)
            }
        }
    }

    private fun loadStatistics(loadCachedAi: Boolean) {
        val snapshot = _state.value
        val graphUnit = snapshot.timelineUnit
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.analysis(snapshot.period, "day", graphUnit)) {
                is EmotionServerResult.Success -> {
                    val codes = result.value.categories.map { it.code }.toSet()
                    _state.value = _state.value.copy(loading = false, data = result.value, visibleCategories = _state.value.visibleCategories.ifEmpty { codes })
                    if (loadCachedAi) loadCachedSummary(snapshot.period)
                }
                EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(loading = false, requiresLogin = true)
                is EmotionServerResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }

    private suspend fun loadCachedSummary(period: String) {
        when (val result = repository.cachedSummary(period)) {
            is EmotionServerResult.Success -> _state.value = _state.value.copy(aiText = result.value.analysis?.analysisText, aiCached = result.value.exists, aiError = null)
            EmotionServerResult.Unauthorized -> _state.value = _state.value.copy(requiresLogin = true)
            is EmotionServerResult.Error -> _state.value = _state.value.copy(aiError = result.message)
        }
    }
}
