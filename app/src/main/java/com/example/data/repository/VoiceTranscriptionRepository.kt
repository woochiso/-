package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.VoiceTranscriptionResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class VoiceTranscriptionRepository(
    private val api: ApiService,
    private val tokens: TokenManager
) {
    suspend fun transcribe(file: File): EmotionServerResult<VoiceTranscriptionResponse> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            val audio = MultipartBody.Part.createFormData(
                "audio",
                file.name,
                file.asRequestBody("audio/mp4".toMediaType())
            )
            val response = api.transcribeVoice("Bearer $token", audio)
            val body = response.body()
            when {
                response.isSuccessful && body?.ok == true && body.text.isNotBlank() -> EmotionServerResult.Success(body)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> EmotionServerResult.Error(response.code(), body?.message ?: "음성을 텍스트로 변환하지 못했습니다.")
            }
        } catch (_: Exception) {
            EmotionServerResult.Error(0, "음성을 텍스트로 변환하지 못했습니다. 인터넷 연결을 확인해주세요.")
        }
    }
}
