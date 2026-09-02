package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonDataException

sealed interface EmotionServerResult<out T> {
    data class Success<T>(val value: T) : EmotionServerResult<T>
    data class Error(val status: Int, val message: String) : EmotionServerResult<Nothing>
    data object Unauthorized : EmotionServerResult<Nothing>
}

class EmotionServerRepository(private val api: ApiService, private val tokens: TokenManager) {
    suspend fun master() = call { api.getEmotionMaster(it) }
    suspend fun toggleFavorite(emotionId: Long, favorite: Boolean? = null) =
        call { api.toggleEmotionFavorite(it, EmotionFavoriteToggleRequest(emotionId, favorite)) }
    suspend fun favorites() = call { api.getFavoriteEmotions(it) }
    suspend fun today(date: String) = call { api.getTodayEmotions(it, date) }
    suspend fun saveToday(request: TodayEmotionSaveRequest) = call { api.saveTodayEmotions(it, request) }
    suspend fun addToday(request: TodayEmotionAddRequest) = call { api.addTodayEmotion(it, request) }
    suspend fun correctToday(request: TodayEmotionCorrectionRequest) = call { api.correctTodayEmotion(it, request) }
    suspend fun storyOptions(emotionId: Long, recordDate: String) =
        call { api.getEmotionStoryOptions(it, emotionId, recordDate) }
    suspend fun linkStory(request: EmotionStoryLinkRequest) = call { api.linkEmotionStory(it, request) }
    suspend fun graph(period: String?, startDate: String? = null, endDate: String? = null) =
        call { api.getEmotionGraph(it, period, startDate, endDate) }
    suspend fun storyGraph(storyId: Long, period: String, startDate: String? = null, endDate: String? = null) =
        call("STORY_GRAPH_RESPONSE") { api.getStoryGraph(it, storyId, period, startDate, endDate) }
    suspend fun storyInsights(
        categoryCode: String?, emotionId: Long?, period: String,
        startDate: String? = null, endDate: String? = null, storyId: Long? = null
    ) = call { api.getEmotionStoryInsights(it, categoryCode, emotionId, period, startDate, endDate, storyId) }

    private suspend fun <T> call(
        debugTag: String? = null,
        block: suspend (String) -> Response<T>
    ): EmotionServerResult<T> {
        val token = tokens.accessToken() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = block("Bearer $token")
            val body = response.body()
            if (BuildConfig.DEBUG && debugTag != null) {
                Log.d("WOOCHISO_EMOTION", "$debugTag status=${response.code()} contentType=${response.headers()["Content-Type"]}")
            }
            when {
                response.isSuccessful && body != null -> EmotionServerResult.Success(body)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> EmotionServerResult.Error(response.code(), errorMessage(response))
            }
        } catch (error: IOException) {
            if (BuildConfig.DEBUG && debugTag != null) Log.d("WOOCHISO_EMOTION", "$debugTag networkError=${error.javaClass.simpleName}")
            EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.")
        } catch (error: JsonDataException) {
            if (BuildConfig.DEBUG && debugTag != null) Log.d("WOOCHISO_EMOTION", "$debugTag parseError=${error.javaClass.simpleName}")
            EmotionServerResult.Error(-1, "서버 응답 형식을 확인하지 못했습니다. 잠시 후 다시 시도해주세요.")
        } catch (error: Exception) {
            if (BuildConfig.DEBUG && debugTag != null) Log.d("WOOCHISO_EMOTION", "$debugTag unexpectedError=${error.javaClass.simpleName}")
            EmotionServerResult.Error(-1, "서버 응답 형식을 확인하지 못했습니다. 잠시 후 다시 시도해주세요.")
        }
    }

    private fun errorMessage(response: Response<*>): String {
        val serverMessage = runCatching {
            JSONObject(response.errorBody()?.string().orEmpty()).optString("message")
        }.getOrNull().takeUnless { it.isNullOrBlank() }
        return serverMessage ?: when (response.code()) {
            400 -> "입력 정보를 확인해주세요."
            401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
            404 -> "요청한 정보를 찾을 수 없습니다."
            in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            else -> "요청을 처리하지 못했습니다."
        }
    }
}
