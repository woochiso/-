package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import java.io.IOException

class AiEmotionAnalysisRepository(private val api: ApiService, private val tokens: TokenManager) {
    suspend fun recoveryInsight(period: String, startDate: String?, endDate: String?) =
        call { api.getRecoveryInsight(it, period, startDate, endDate) }
    suspend fun recoveryAi(period: String, startDate: String?, endDate: String?, force: Boolean) =
        call { api.getRecoveryAiInsight(it, RecoveryAiRequest(period, startDate, endDate, force)) }
    suspend fun analysis(period: String, granularity: String, graphUnit: String) = call { api.getAiEmotionAnalysis(it, period, granularity, graphUnit) }
    suspend fun cachedSummary(period: String) = call { api.getAiEmotionAnalysisSummary(it, period) }
    suspend fun createSummary(period: String, force: Boolean) = call { api.createAiEmotionAnalysisSummary(it, AiAnalysisSummaryRequest(period, force = force)) }

    private suspend fun <T> call(block: suspend (String) -> retrofit2.Response<T>): EmotionServerResult<T> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = block("Bearer $token")
            val body = response.body()
            when {
                response.isSuccessful && body != null -> EmotionServerResult.Success(body)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> {
                    val message = runCatching { JSONObject(response.errorBody()?.string().orEmpty()).optString("message") }.getOrNull().takeUnless { it.isNullOrBlank() }
                        ?: if (response.code() in 500..599) "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요." else "감정 분석 요청을 처리하지 못했습니다."
                    EmotionServerResult.Error(response.code(), message)
                }
            }
        } catch (_: IOException) { EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.") }
        catch (_: Exception) { EmotionServerResult.Error(-1, "서버 응답을 처리하지 못했습니다.") }
    }
}
