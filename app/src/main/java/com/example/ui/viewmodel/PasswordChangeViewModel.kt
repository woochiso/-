package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.PasswordChangeRequest
import com.example.data.repository.PasswordChangeRepository
import com.example.data.repository.PasswordChangeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasswordChangeUiState(
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val requiresLogin: Boolean = false
)

class PasswordChangeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PasswordChangeRepository(RetrofitClient.apiService, TokenManager(application))
    private val _state = MutableStateFlow(PasswordChangeUiState())
    val state: StateFlow<PasswordChangeUiState> = _state.asStateFlow()

    fun change(request: PasswordChangeRequest) {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null, message = null) }
            _state.value = when (val result = repository.change(request)) {
                is PasswordChangeResult.Success -> PasswordChangeUiState(message = result.message)
                is PasswordChangeResult.ValidationError -> PasswordChangeUiState(error = result.message)
                PasswordChangeResult.Unauthorized -> PasswordChangeUiState(
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요.", requiresLogin = true
                )
                is PasswordChangeResult.Failure -> PasswordChangeUiState(error = result.message)
            }
        }
    }

    fun reset() { _state.value = PasswordChangeUiState() }
}
