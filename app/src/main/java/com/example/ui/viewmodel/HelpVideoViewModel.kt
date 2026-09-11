package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.HelpVideo
import com.example.data.repository.HelpVideoRepository
import com.example.data.repository.HelpVideoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HelpVideoUiState(
    val loading: Boolean = false,
    val videos: List<HelpVideo> = emptyList(),
    val error: String? = null
)

class HelpVideoViewModel : ViewModel() {
    private val repository = HelpVideoRepository(RetrofitClient.apiService)
    private val _state = MutableStateFlow(HelpVideoUiState())
    val state: StateFlow<HelpVideoUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.load()) {
                is HelpVideoResult.Success -> _state.value = HelpVideoUiState(videos = result.videos)
                is HelpVideoResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
