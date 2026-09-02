package com.example.data.repository

import com.example.auth.AuthSession
import com.example.auth.TokenManager
import com.example.data.remote.AuthRemoteDataSource
import com.example.data.remote.RemoteLoginResult
import com.example.data.remote.ApiService
import com.example.data.remote.dto.GoogleAuthResponse
import com.example.data.remote.dto.GoogleLoginRequest
import com.example.data.remote.dto.GoogleSignupRequest
import com.example.data.remote.dto.RegistrationRequest
import com.example.data.remote.dto.EmailAvailabilityRequest
import com.example.data.remote.dto.NicknameAvailabilityRequest
import com.example.data.remote.dto.ForgotPasswordRequest
import java.io.IOException
import org.json.JSONObject

sealed interface LoginResult {
    data class Success(val session: AuthSession) : LoginResult
    data object InvalidCredentials : LoginResult
    data object NoInternet : LoginResult
    data object ServerUnavailable : LoginResult
    data object InvalidResponse : LoginResult
    data object ServerError : LoginResult
}

sealed interface GoogleAuthResult {
    data class Success(val session: AuthSession) : GoogleAuthResult
    data class RequiresProfile(val response: GoogleAuthResponse) : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}

class AuthRepository(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenManager: TokenManager,
    private val apiService: ApiService? = null
) {
    fun currentSession(): AuthSession? = tokenManager.loadSession()

    fun updateCurrentUser(email: String, nickname: String?, grade: String?): AuthSession? {
        val current = tokenManager.loadSession() ?: return null
        tokenManager.updateUserProfile(email, nickname, grade)
        return current.copy(email = email, nickname = nickname, grade = grade)
    }

    suspend fun login(email: String, password: String): LoginResult {
        return when (val result = remoteDataSource.login(email, password)) {
            is RemoteLoginResult.Success -> {
                val token = result.response.token
                val user = result.response.user
                if (token.isNullOrBlank() || user == null) {
                    LoginResult.ServerError
                } else {
                    tokenManager.saveSession(
                        token = token,
                        userId = user.id,
                        email = user.email,
                        nickname = user.nickname,
                        grade = user.grade
                    )
                    LoginResult.Success(
                        AuthSession(
                            userId = user.id,
                            email = user.email,
                            nickname = user.nickname,
                            grade = user.grade
                        )
                    )
                }
            }
            RemoteLoginResult.InvalidCredentials -> LoginResult.InvalidCredentials
            RemoteLoginResult.NoInternet -> LoginResult.NoInternet
            RemoteLoginResult.ServerUnavailable -> LoginResult.ServerUnavailable
            RemoteLoginResult.InvalidResponse -> LoginResult.InvalidResponse
            RemoteLoginResult.ServerError -> LoginResult.ServerError
        }
    }

    suspend fun googleLogin(idToken: String): GoogleAuthResult = googleRequest { api -> api.googleLogin(GoogleLoginRequest(idToken)) }

    suspend fun completeGoogleSignup(request: GoogleSignupRequest): GoogleAuthResult = googleRequest { api -> api.completeGoogleSignup(request) }

    suspend fun register(request: RegistrationRequest): Result<AuthSession> = runCatching {
        val api = requireNotNull(apiService)
        val response = api.register(request)
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) {
            val serverMessage = runCatching { JSONObject(response.errorBody()?.string().orEmpty()).optString("message") }.getOrNull()
            error(serverMessage?.takeIf { it.isNotBlank() } ?: body?.message ?: "회원가입을 처리하지 못했습니다.")
        }
        val token = body.token ?: error("회원가입 인증정보를 확인하지 못했습니다.")
        val user = body.user ?: error("회원정보를 확인하지 못했습니다.")
        tokenManager.saveSession(token, user.id, user.email, user.nickname, user.grade)
        AuthSession(user.id, user.email, user.nickname, user.grade)
    }.recoverCatching { exception ->
        when (exception) {
            is IOException -> error("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.")
            is IllegalStateException -> throw exception
            else -> error("회원가입 처리 중 오류가 발생했습니다.")
        }
    }

    suspend fun isEmailAvailable(email: String): Result<Boolean> = runCatching {
        val response = requireNotNull(apiService).checkEmail(EmailAvailabilityRequest(email))
        val body = response.body()
        if (!response.isSuccessful || body?.ok != true) {
            error(body?.message ?: "이메일 확인에 실패했습니다.")
        }
        body.available
    }

    suspend fun isNicknameAvailable(nickname: String): Result<Boolean> = runCatching {
        val response = requireNotNull(apiService).checkNickname(NicknameAvailabilityRequest(nickname))
        val body = response.body()
        if (!response.isSuccessful || body?.ok != true) {
            error(body?.message ?: "닉네임 확인에 실패했습니다.")
        }
        body.available
    }

    suspend fun requestPasswordReset(email: String): Result<String> = runCatching {
        val response = requireNotNull(apiService).forgotPassword(ForgotPasswordRequest(email))
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) {
            error(body?.message ?: "잠시 후 다시 시도해주세요.")
        }
        body.message ?: "입력하신 이메일이 등록되어 있다면 비밀번호 재설정 안내를 보내드렸습니다."
    }.recoverCatching { exception ->
        when (exception) {
            is IOException -> error("인터넷 연결을 확인해주세요.")
            is IllegalStateException -> throw exception
            else -> error("잠시 후 다시 시도해주세요.")
        }
    }

    private suspend fun googleRequest(call: suspend (ApiService) -> retrofit2.Response<GoogleAuthResponse>): GoogleAuthResult {
        val api = apiService ?: return GoogleAuthResult.Error("Google 로그인을 사용할 수 없습니다.")
        return try {
            val response = call(api)
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                if (body.requiresProfile) GoogleAuthResult.RequiresProfile(body) else {
                    val token = body.token; val user = body.user
                    if (token.isNullOrBlank() || user == null) GoogleAuthResult.Error("서버 응답을 처리할 수 없습니다.") else {
                        tokenManager.saveSession(token, user.id, user.email, user.nickname, user.grade)
                        GoogleAuthResult.Success(AuthSession(user.id, user.email, user.nickname, user.grade))
                    }
                }
            } else GoogleAuthResult.Error(body?.message ?: "Google 로그인을 처리하지 못했습니다.")
        } catch (_: IOException) { GoogleAuthResult.Error("인터넷 연결을 확인해주세요.") }
        catch (_: Exception) { GoogleAuthResult.Error("Google 로그인을 처리하지 못했습니다.") }
    }

    fun clearSessionForLogout(): String? {
        val token = tokenManager.accessToken()
        tokenManager.clearSession()
        return token
    }

    suspend fun revokeToken(token: String) {
        if (token.isNotBlank()) runCatching { apiService?.logout("Bearer $token") }
    }
}
