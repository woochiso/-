package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.PasswordChangeRequest
import java.io.IOException
import org.json.JSONObject

sealed interface PasswordChangeResult {
    data class Success(val message: String) : PasswordChangeResult
    data class ValidationError(val message: String) : PasswordChangeResult
    data object Unauthorized : PasswordChangeResult
    data class Failure(val message: String) : PasswordChangeResult
}

class PasswordChangeRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun change(request: PasswordChangeRequest): PasswordChangeResult {
        val token = tokenManager.accessToken()
        if (token.isNullOrBlank()) return PasswordChangeResult.Unauthorized
        return try {
            val response = apiService.changePassword("Bearer $token", request)
            val body = response.body()
            val errorMessage = response.errorBody()?.string()?.let(::safeMessage)
            when {
                response.isSuccessful && body?.success == true ->
                    PasswordChangeResult.Success(body.message ?: "비밀번호가 변경되었습니다.")
                response.code() == 401 -> PasswordChangeResult.Unauthorized
                response.code() in listOf(400, 409, 422) ->
                    PasswordChangeResult.ValidationError(errorMessage ?: "입력값을 확인해주세요.")
                else -> PasswordChangeResult.Failure(
                    errorMessage ?: "비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해주세요."
                )
            }
        } catch (_: IOException) {
            PasswordChangeResult.Failure("인터넷 연결을 확인해주세요.")
        } catch (_: Exception) {
            PasswordChangeResult.Failure("비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해주세요.")
        }
    }

    private fun safeMessage(json: String): String? = runCatching {
        JSONObject(json).optString("message").takeIf(String::isNotBlank)
    }.getOrNull()
}
