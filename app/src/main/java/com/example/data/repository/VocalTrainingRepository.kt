package com.example.data.repository

import android.util.Log
import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.VocalAnalysisResponse
import com.example.data.remote.dto.VocalTtsRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

class VocalTrainingRepository(private val api: ApiService, private val tokens: TokenManager) {
    suspend fun analyze(file: File, duration: Double, mode: String, mixFile: File?): EmotionServerResult<VocalAnalysisResponse> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            Log.d("VOCAL_FLOW", "repository analyze entered")
            Log.d("VOCAL_DEBUG", "source=${if (file.name.startsWith("vocal-upload")) "picked" else "recorded"}")
            Log.d("VOCAL_DEBUG", "filePath=${file.absolutePath} fileName=${file.name} extension=${file.extension}")
            Log.d("VOCAL_DEBUG", "exists=${file.exists()} size=${file.length()} resolverMime=audio/wav")
            Log.d("VOCAL_DEBUG", "requestMime=audio/wav multipartField=audio multipartFilename=${file.name}")
            Log.d("VOCAL_AI_UPLOAD", "filename=${file.name} extension=${file.extension.lowercase()} androidMime=audio/wav requestMime=audio/wav size=${file.length()} field=audio")
            mixFile?.let { Log.d("MR_DEBUG", "uploadFile=${it.name} size=${it.length()} mime=audio/wav multipartField=mix_audio") }
            Log.d("VOCAL_FLOW", "request ready url=https://woochiso.com/api/training/vocal/analyze.php method=POST")
            val response = api.analyzeVocal(
                "Bearer $token",
                MultipartBody.Part.createFormData("audio", "android-vocal.wav", file.asRequestBody(WAV)),
                mixFile?.let { MultipartBody.Part.createFormData("mix_audio", "android-vocal-mix.wav", it.asRequestBody(WAV)) },
                duration.toString().toRequestBody(TEXT),
                "upload".toRequestBody(TEXT),
                mode.toRequestBody(TEXT),
                (if (mixFile != null) "1" else "0").toRequestBody(TEXT),
                (if (mixFile != null) "1" else "0").toRequestBody(TEXT),
                (if (mixFile != null) "1" else "0").toRequestBody(TEXT),
                (if (mixFile != null) "1" else "0").toRequestBody(TEXT),
                "1".toRequestBody(TEXT),
                (if (mixFile != null) "1" else "0").toRequestBody(TEXT),
                "1".toRequestBody(TEXT),
                "android-pcm-wav-1.0".toRequestBody(TEXT)
            )
            val body = response.body()
            val errorBody = response.errorBody()?.string()
            Log.d("VOCAL_HTTP_URL", "https://woochiso.com/api/training/vocal/analyze.php")
            Log.d("VOCAL_HTTP_STATUS", "status=${response.code()} contentType=${response.headers()["Content-Type"]} serverVersion=${response.headers()["X-Woochiso-Vocal-Version"] ?: "missing"}")
            if (!response.isSuccessful) Log.d("VOCAL_HTTP_BODY", errorBody.orEmpty())
            Log.d("VOCAL_HTTP", "status=${response.code()} contentType=${response.headers()["Content-Type"]} success=${response.isSuccessful && body?.success == true}")
            if (!response.isSuccessful) {
                Log.d("VOCAL_HTTP", "errorBody=${(body?.message ?: parse(errorBody)).take(300)}")
            }
            when {
                response.isSuccessful && body?.success == true -> EmotionServerResult.Success(body)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> EmotionServerResult.Error(response.code(), body?.message ?: parse(errorBody))
            }
        } catch (_: SocketTimeoutException) { EmotionServerResult.Error(0, "보컬 분석 시간이 길어지고 있습니다. 다시 시도해주세요.")
        } catch (_: IOException) { EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.")
        } catch (_: Exception) { EmotionServerResult.Error(-1, "AI 보컬 분석 결과를 처리하지 못했습니다.") }
    }

    suspend fun speech(tokenValue: String): EmotionServerResult<ByteArray> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = api.getVocalSpeech("Bearer $token", VocalTtsRequest(tokenValue))
            if (response.code() == 401) return EmotionServerResult.Unauthorized
            val body = response.body()
            val type = response.headers()["Content-Type"].orEmpty().lowercase()
            if (response.isSuccessful && body != null && (type.startsWith("audio/") || type == "application/octet-stream"))
                EmotionServerResult.Success(body.bytes())
            else EmotionServerResult.Error(response.code(), "AI 음성 피드백을 재생하지 못했습니다.")
        } catch (_: Exception) { EmotionServerResult.Error(0, "AI 음성 피드백을 재생하지 못했습니다.") }
    }

    private fun parse(raw: String?) = runCatching { JSONObject(raw.orEmpty()).optString("message") }.getOrNull()
        .takeUnless { it.isNullOrBlank() } ?: "음성 분석을 완료하지 못했어요. 다시 시도해주세요."
    private companion object {
        val TEXT = "text/plain".toMediaType()
        val WAV = "audio/wav".toMediaType()
    }
}
