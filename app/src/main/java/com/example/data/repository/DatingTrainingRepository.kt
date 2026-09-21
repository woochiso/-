package com.example.data.repository

import android.util.Log
import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

class DatingTrainingRepository(private val api: ApiService, private val tokens: TokenManager) {
    private fun auth() = tokens.accessToken()?.let { "Bearer $it" }

    suspend fun config(): EmotionServerResult<DatingConfigResponse> {
        val auth = auth() ?: return EmotionServerResult.Unauthorized
        return runRequest { api.getDatingConfig(auth) }
    }

    suspend fun action(request: DatingActionRequest): EmotionServerResult<DatingActionResponse> {
        val auth = auth() ?: return EmotionServerResult.Unauthorized
        return runRequest { api.datingAction(auth, request) }
    }

    suspend fun speech(ttsToken: String, voice: String): EmotionServerResult<ByteArray> {
        val auth = auth() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = api.getDatingSpeech(auth, DatingTtsRequest(ttsToken, voice))
            val bytes = response.body()?.bytes()
            val type = response.headers()["Content-Type"].orEmpty().lowercase()
            when {
                response.isSuccessful && bytes != null && bytes.size > 12 && (type.startsWith("audio/") || type == "application/octet-stream") -> EmotionServerResult.Success(bytes)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> EmotionServerResult.Error(response.code(), errorMessage(response.errorBody()?.string(), "AI 음성을 재생하지 못했습니다."))
            }
        } catch (_: Exception) { EmotionServerResult.Error(0, "AI 음성을 재생하지 못했습니다. 글로 대화를 이어갈 수 있어요.") }
    }

    private suspend fun <T> runRequest(block: suspend () -> retrofit2.Response<T>): EmotionServerResult<T> = try {
        val response = block(); val body = response.body()
        when {
            response.isSuccessful && body != null -> EmotionServerResult.Success(body)
            response.code() == 401 -> EmotionServerResult.Unauthorized
            else -> {
                val raw = response.errorBody()?.string()
                val json = runCatching { JSONObject(raw.orEmpty()) }.getOrNull()
                Log.w("DATING_HTTP", "status=${response.code()} errorCode=${json?.optString("error_code").orEmpty()} stage=${json?.optJSONObject("diagnostic")?.optString("finish_reason").orEmpty()} keys=${json?.keys()?.asSequence()?.toList().orEmpty()}")
                EmotionServerResult.Error(response.code(), errorMessage(raw, "요청을 처리하지 못했습니다. 다시 시도해주세요."))
            }
        }
    } catch (_: SocketTimeoutException) { EmotionServerResult.Error(0, "상대의 답변이 늦어지고 있어요. 다시 시도해주세요.")
    } catch (_: IOException) { EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.")
    } catch (_: Exception) { EmotionServerResult.Error(-1, "응답을 처리하지 못했습니다.") }

    private fun errorMessage(raw: String?, fallback: String): String =
        runCatching { JSONObject(raw.orEmpty()).optString("message") }.getOrNull().takeUnless { it.isNullOrBlank() } ?: fallback
}
