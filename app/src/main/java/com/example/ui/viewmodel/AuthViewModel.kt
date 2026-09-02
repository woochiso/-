package com.example.ui.viewmodel

import android.app.Application
import android.util.Patterns
import android.util.Log
import com.example.BuildConfig
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthSession
import com.example.auth.TokenManager
import com.example.data.remote.AuthRemoteDataSource
import com.example.data.remote.RetrofitClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.LoginResult
import com.example.data.repository.GoogleAuthResult
import com.example.data.remote.dto.GoogleProfileDto
import com.example.data.remote.dto.ConsentVersionsDto
import com.example.data.remote.dto.GoogleSignupRequest
import com.example.data.remote.dto.RegistrationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val session: AuthSession? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val googleSignup: GoogleSignupState? = null,
    val emailCheckEmail: String = "",
    val isCheckingEmail: Boolean = false,
    val isEmailAvailable: Boolean? = null,
    val emailCheckMessage: String? = null,
    val nicknameCheckNickname: String = "",
    val isCheckingNickname: Boolean = false,
    val isNicknameAvailable: Boolean? = null,
    val nicknameCheckMessage: String? = null,
    val passwordResetSent: Boolean = false
)

data class GoogleSignupState(val idToken: String, val profile: GoogleProfileDto, val consentVersions: ConsentVersionsDto?)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(
        remoteDataSource = AuthRemoteDataSource(RetrofitClient.apiService),
        tokenManager = TokenManager(application),
        apiService = RetrofitClient.apiService
    )

    private val savedSession = repository.currentSession()
    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = savedSession != null,
            session = savedSession
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        if (BuildConfig.DEBUG) {
            val emailDomain = cleanEmail.substringAfter('@', missingDelimiterValue = "unknown")
                .takeIf { it != cleanEmail && it.isNotBlank() } ?: "unknown"
            Log.d(
                AUTH_DEBUG_TAG,
                "AUTH_INPUT " +
                    "emailLength=${email.length} " +
                    "emailDomain=$emailDomain " +
                    "emailTrimmed=${email == cleanEmail} " +
                    "emailHasLineBreak=${email.contains('\n') || email.contains('\r')} " +
                    "passwordLength=${password.length} " +
                    "passwordEmpty=${password.isEmpty()} " +
                    "passwordLeadingWhitespace=${password.firstOrNull()?.isWhitespace() == true} " +
                    "passwordTrailingWhitespace=${password.lastOrNull()?.isWhitespace() == true} " +
                    "passwordHasLineBreak=${password.contains('\n') || password.contains('\r')}"
            )
        }
        val emailError = when {
            cleanEmail.isBlank() -> "이메일을 입력해주세요."
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> "올바른 이메일 주소를 입력해주세요."
            else -> null
        }
        val passwordError = if (password.isBlank()) "비밀번호를 입력해주세요." else null

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emailError = null,
                    passwordError = null,
                    errorMessage = null
                )
            }

            when (val result = repository.login(cleanEmail, password)) {
                is LoginResult.Success -> {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        session = result.session
                    )
                }
                LoginResult.InvalidCredentials -> showError("이메일 또는 비밀번호가 올바르지 않습니다.")
                LoginResult.NoInternet -> showError("인터넷 연결을 확인해주세요.")
                LoginResult.ServerUnavailable -> showError("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.")
                LoginResult.InvalidResponse -> showError("서버 응답을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.")
                LoginResult.ServerError -> showError("로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(emailError = null, passwordError = null, errorMessage = null)
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank() || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.googleLogin(idToken)) {
                is GoogleAuthResult.Success -> _uiState.value = AuthUiState(isLoggedIn = true, session = result.session)
                is GoogleAuthResult.RequiresProfile -> {
                    val profile = result.response.googleProfile
                    if (profile == null) showError("Google 계정 정보를 확인하지 못했습니다.")
                    else _uiState.update { it.copy(isLoading = false, googleSignup = GoogleSignupState(idToken, profile, result.response.consentVersions)) }
                }
                is GoogleAuthResult.Error -> showError(result.message)
            }
        }
    }

    fun completeGoogleSignup(request: GoogleSignupRequest) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.completeGoogleSignup(request)) {
                is GoogleAuthResult.Success -> _uiState.value = AuthUiState(isLoggedIn = true, session = result.session)
                is GoogleAuthResult.Error -> showError(result.message)
                is GoogleAuthResult.RequiresProfile -> showError("가입 정보를 다시 확인해주세요.")
            }
        }
    }

    fun cancelGoogleSignup() { _uiState.update { it.copy(googleSignup = null, errorMessage = null) } }

    fun register(request: RegistrationRequest, onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.register(request).fold(
                onSuccess = { session -> _uiState.value = AuthUiState(isLoggedIn = true, session = session); onSuccess() },
                onFailure = { showError(it.message ?: "회원가입을 처리하지 못했습니다.") }
            )
        }
    }

    fun resetEmailCheck(email: String) {
        _uiState.update {
            it.copy(
                emailCheckEmail = email.trim().lowercase(),
                isCheckingEmail = false,
                isEmailAvailable = null,
                emailCheckMessage = null
            )
        }
    }

    fun checkEmail(email: String) {
        val normalized = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) return
        _uiState.update {
            it.copy(
                emailCheckEmail = normalized,
                isCheckingEmail = true,
                isEmailAvailable = null,
                emailCheckMessage = "이메일을 확인하고 있습니다."
            )
        }
        viewModelScope.launch {
            repository.isEmailAvailable(normalized).fold(
                onSuccess = { available ->
                    _uiState.update { current ->
                        if (current.emailCheckEmail != normalized) current else current.copy(
                            isCheckingEmail = false,
                            isEmailAvailable = available,
                            emailCheckMessage = if (available) "사용 가능한 이메일입니다." else "이미 가입된 이메일입니다."
                        )
                    }
                },
                onFailure = {
                    _uiState.update { current ->
                        if (current.emailCheckEmail != normalized) current else current.copy(
                            isCheckingEmail = false,
                            isEmailAvailable = null,
                            emailCheckMessage = "이메일 확인에 실패했습니다. 다시 시도해주세요."
                        )
                    }
                }
            )
        }
    }

    fun resetNicknameCheck(nickname: String) {
        _uiState.update {
            it.copy(
                nicknameCheckNickname = nickname.trim(),
                isCheckingNickname = false,
                isNicknameAvailable = null,
                nicknameCheckMessage = null
            )
        }
    }

    fun checkNickname(nickname: String) {
        val normalized = nickname.trim()
        if (normalized.length !in 2..20 || !normalized.matches(Regex("^[가-힣A-Za-z0-9_ ]+$"))) return
        _uiState.update {
            it.copy(
                nicknameCheckNickname = normalized,
                isCheckingNickname = true,
                isNicknameAvailable = null,
                nicknameCheckMessage = "닉네임을 확인하고 있습니다."
            )
        }
        viewModelScope.launch {
            repository.isNicknameAvailable(normalized).fold(
                onSuccess = { available ->
                    _uiState.update { current ->
                        if (current.nicknameCheckNickname != normalized) current else current.copy(
                            isCheckingNickname = false,
                            isNicknameAvailable = available,
                            nicknameCheckMessage = if (available) "사용 가능한 닉네임입니다." else "이미 사용 중인 닉네임입니다."
                        )
                    }
                },
                onFailure = {
                    _uiState.update { current ->
                        if (current.nicknameCheckNickname != normalized) current else current.copy(
                            isCheckingNickname = false,
                            isNicknameAvailable = null,
                            nicknameCheckMessage = "닉네임 확인에 실패했습니다. 다시 시도해주세요."
                        )
                    }
                }
            )
        }
    }

    fun requestPasswordReset(email: String) {
        val normalized = email.trim().lowercase()
        if (_uiState.value.isLoading || !Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, passwordResetSent = false) }
            repository.requestPasswordReset(normalized).fold(
                onSuccess = { _uiState.update { state -> state.copy(isLoading = false, passwordResetSent = true, errorMessage = null) } },
                onFailure = { showError(it.message ?: "잠시 후 다시 시도해주세요.") }
            )
        }
    }

    fun resetPasswordResetState() {
        _uiState.update { it.copy(isLoading = false, errorMessage = null, passwordResetSent = false) }
    }
    fun googleLoginFailed() { showError("Google 로그인을 완료하지 못했습니다. 다시 시도해주세요.") }

    /** Debug UI 작업용 메모리 세션. 서버와 TokenManager에는 접근하지 않는다. */
    fun openDebugPreview() {
        if (!BuildConfig.DEBUG) return

        _uiState.value = AuthUiState(
            isLoggedIn = true,
            session = AuthSession(
                userId = 0L,
                email = "test@woochiso.com",
                nickname = "테스트 사용자",
                grade = "FREE"
            )
        )
    }

    fun logout() {
        val token = repository.clearSessionForLogout()
        _uiState.value = AuthUiState()
        if (!token.isNullOrBlank()) viewModelScope.launch { repository.revokeToken(token) }
    }

    fun updateCurrentUser(email: String, nickname: String?, grade: String?) {
        val updated = repository.updateCurrentUser(email, nickname, grade) ?: return
        _uiState.update { it.copy(isLoggedIn = true, session = updated) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private companion object {
        const val AUTH_DEBUG_TAG = "WOOCHISO_AUTH_DEBUG"
    }
}
