package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.ProfileUpdateRequest
import com.example.data.remote.dto.ProfileUser
import java.io.IOException
import org.json.JSONObject

sealed interface ProfileResult {
    data class Success(val user: ProfileUser, val message: String? = null) : ProfileResult
    data class ValidationError(val message: String) : ProfileResult
    data class Conflict(val message: String) : ProfileResult
    data object Unauthorized : ProfileResult
    data class Failure(val message: String) : ProfileResult
}

class ProfileRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun load(): ProfileResult = execute("PROFILE_REQUEST") { authorization ->
        apiService.profile(authorization)
    }

    suspend fun update(request: ProfileUpdateRequest): ProfileResult = execute("PROFILE_UPDATE") { authorization ->
        apiService.updateProfile(authorization, request)
    }

    private suspend fun execute(
        debugEvent: String,
        request: suspend (String) -> retrofit2.Response<com.example.data.remote.dto.ProfileResponse>
    ): ProfileResult {
        val token = tokenManager.accessToken()
        if (token.isNullOrBlank()) return ProfileResult.Unauthorized
        if (BuildConfig.DEBUG) Log.d(TAG, debugEvent)

        return try {
            val response = request("Bearer $token")
            val body = response.body()
            val errorMessage = response.errorBody()?.string()?.let(::safeMessage)
            val result = when {
                response.isSuccessful && body?.success == true && body.user != null ->
                    ProfileResult.Success(body.user, body.message)
                response.code() == 401 -> ProfileResult.Unauthorized
                response.code() == 409 -> ProfileResult.Conflict(errorMessage ?: "이미 사용 중인 닉네임입니다.")
                response.code() == 400 || response.code() == 415 ->
                    ProfileResult.ValidationError(errorMessage ?: "입력값을 확인해주세요.")
                else -> ProfileResult.Failure(errorMessage ?: "서버 요청을 처리하지 못했습니다.")
            }
            if (BuildConfig.DEBUG) {
                val success = result is ProfileResult.Success
                Log.d(TAG, if (success) "${debugEvent}_SUCCESS" else "${debugEvent}_FAILED status=${response.code()}")
            }
            result
        } catch (_: IOException) {
            if (BuildConfig.DEBUG) Log.d(TAG, "${debugEvent}_FAILED status=network")
            ProfileResult.Failure("인터넷 연결을 확인해주세요.")
        } catch (_: Exception) {
            if (BuildConfig.DEBUG) Log.d(TAG, "${debugEvent}_FAILED status=unknown")
            ProfileResult.Failure("회원정보를 처리하지 못했습니다.")
        }
    }

    private fun safeMessage(json: String): String? = runCatching {
        JSONObject(json).optString("message").takeIf(String::isNotBlank)
    }.getOrNull()

    private companion object { const val TAG = "WOOCHISO_PROFILE" }
}
