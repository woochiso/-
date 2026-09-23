package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.TtsUsageResponse

class TtsUsageRepository(private val api: ApiService, private val tokens: TokenManager) {
    suspend fun load(): TtsUsageResponse {
        val token = tokens.accessToken() ?: error("로그인이 필요합니다.")
        val response = api.getTtsUsage("Bearer $token")
        return response.body()?.takeIf { response.isSuccessful && it.success }
            ?: error(response.errorBody()?.string().orEmpty().ifBlank { "AI 음성 사용량을 불러오지 못했습니다." })
    }
}
