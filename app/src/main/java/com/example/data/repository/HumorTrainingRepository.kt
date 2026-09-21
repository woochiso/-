package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.HumorAnalysisResponse
import com.example.data.remote.dto.HumorTtsRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

class HumorTrainingRepository(private val api: ApiService, private val tokens: TokenManager) {
    suspend fun analyze(file: File, durationSeconds: Double, inputMode: String): EmotionServerResult<HumorAnalysisResponse> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            val mediaType = if (file.extension.equals("webm", true)) "audio/webm" else "audio/mp4"
            val part = MultipartBody.Part.createFormData("audio", file.name, file.asRequestBody(mediaType.toMediaType()))
            val response = api.analyzeHumor(
                "Bearer $token",
                part,
                durationSeconds.toString().toRequestBody("text/plain".toMediaType()),
                inputMode.toRequestBody("text/plain".toMediaType())
            )
            val body = response.body()
            when {
                response.isSuccessful && body != null && body.success -> EmotionServerResult.Success(body)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> EmotionServerResult.Error(response.code(), body?.message ?: errorMessage(response.errorBody()?.string()))
            }
        } catch (_: SocketTimeoutException) {
            EmotionServerResult.Error(0, "AI 분석 시간이 길어지고 있습니다. 다시 시도해주세요.")
        } catch (_: IOException) {
            EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.")
        } catch (_: Exception) {
            EmotionServerResult.Error(-1, "AI 분석 결과를 처리하지 못했습니다.")
        }
    }

    suspend fun speech(ttsToken: String): EmotionServerResult<ByteArray> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = api.getHumorSpeech("Bearer $token", HumorTtsRequest(ttsToken))
            val type = response.headers()["Content-Type"].orEmpty().lowercase()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val bytes = body.bytes()
                if ((type.startsWith("audio/") || type == "application/octet-stream") && bytes.size > 12) {
                    EmotionServerResult.Success(bytes)
                } else EmotionServerResult.Error(response.code(), "AI 음성을 재생하지 못했습니다.")
            } else if (response.code() == 401) EmotionServerResult.Unauthorized
            else EmotionServerResult.Error(response.code(), errorMessage(response.errorBody()?.string()))
        } catch (_: Exception) {
            EmotionServerResult.Error(0, "AI 음성을 재생하지 못했습니다. 결과는 글로 확인해주세요.")
        }
    }

    private fun errorMessage(raw: String?): String =
        runCatching { JSONObject(raw.orEmpty()).optString("message") }.getOrNull()
            .takeUnless { it.isNullOrBlank() } ?: "요청을 처리하지 못했습니다. 다시 시도해주세요."
}

