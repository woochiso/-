package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.ProfileUpdateRequest
import com.example.data.remote.dto.ProfileUsage
import com.example.data.remote.dto.ProfileUser
import com.example.data.remote.dto.TtsUsageResponse
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ProfileResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val user: ProfileUser? = null,
    val usage: ProfileUsage? = null,
    val tts: TtsUsageResponse? = null,
    val message: String? = null,
    val error: String? = null,
    val requiresLogin: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(RetrofitClient.apiService, TokenManager(application))
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            applyResult(repository.load(), saving = false)
        }
    }

    fun updateProfile(request: ProfileUpdateRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            applyResult(repository.update(request), saving = true)
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null, error = null) }

    private fun applyResult(result: ProfileResult, saving: Boolean) {
        _uiState.update { current ->
            when (result) {
                is ProfileResult.Success -> current.copy(
                    isLoading = false, isSaving = false, user = result.user,
                    usage = result.usage ?: current.usage,
                    tts = result.tts ?: current.tts,
                    message = if (saving) result.message ?: "회원정보가 저장되었습니다." else null,
                    error = null, requiresLogin = false
                )
                ProfileResult.Unauthorized -> current.copy(
                    isLoading = false, isSaving = false, requiresLogin = true,
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요."
                )
                is ProfileResult.ValidationError -> current.copy(isLoading = false, isSaving = false, error = result.message)
                is ProfileResult.Conflict -> current.copy(isLoading = false, isSaving = false, error = result.message)
                is ProfileResult.Failure -> current.copy(isLoading = false, isSaving = false, error = result.message)
            }
        }
    }
}
