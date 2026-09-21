package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

class ExpressionTrainingRepository(private val api: ApiService, private val tokens: TokenManager) {
    private fun auth() = tokens.accessToken()?.let { "Bearer $it" }
    suspend fun config(): EmotionServerResult<ExpressionConfigResponse> {
        val auth = auth() ?: return EmotionServerResult.Unauthorized
        return request { api.getExpressionConfig(auth) }
    }
    suspend fun action(value: ExpressionActionRequest): EmotionServerResult<ExpressionActionResponse> {
        val auth = auth() ?: return EmotionServerResult.Unauthorized
        return request { api.expressionAction(auth, value) }
    }
    suspend fun speech(token: String, voice: String): EmotionServerResult<ByteArray> {
        val auth = auth() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = api.getExpressionSpeech(auth, ExpressionTtsRequest(token, voice)); val bytes = response.body()?.bytes()
            val type = response.headers()["Content-Type"].orEmpty().lowercase()
            when {
                response.isSuccessful && bytes != null && bytes.size > 12 && (type.startsWith("audio/") || type == "application/octet-stream") -> EmotionServerResult.Success(bytes)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> EmotionServerResult.Error(response.code(), message(response.errorBody()?.string(), "AI 음성을 재생하지 못했습니다."))
            }
        } catch (_: Exception) { EmotionServerResult.Error(0, "AI 음성을 재생하지 못했습니다. 결과는 글로 확인해주세요.") }
    }
    private suspend fun <T> request(block: suspend () -> retrofit2.Response<T>): EmotionServerResult<T> = try {
        val response = block(); val body = response.body()
        when {
            response.isSuccessful && body != null -> EmotionServerResult.Success(body)
            response.code() == 401 -> EmotionServerResult.Unauthorized
            else -> EmotionServerResult.Error(response.code(), message(response.errorBody()?.string(), "요청을 처리하지 못했습니다."))
        }
    } catch (_: SocketTimeoutException) { EmotionServerResult.Error(0, "AI 답변이 늦어지고 있어요. 다시 시도해주세요.")
    } catch (_: IOException) { EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.")
    } catch (_: Exception) { EmotionServerResult.Error(-1, "응답을 처리하지 못했습니다.") }
    private fun message(raw: String?, fallback: String) = runCatching { JSONObject(raw.orEmpty()).optString("message") }.getOrNull().takeUnless { it.isNullOrBlank() } ?: fallback
}
