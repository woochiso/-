package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException

class MealRepository(private val api: ApiService, private val tokens: TokenManager) {
    private fun auth() = tokens.accessToken()?.let { "Bearer $it" }

    suspend fun records() = call("meal/index.php") { api.getMeals(it, 30) }
    suspend fun detail(id: Long) = call("meal/detail.php") { api.getMeal(it, id) }
    suspend fun analyze(file: File) = call("meal/analyze.php") { auth -> api.analyzeMealPhoto(auth, file.part()) }
    suspend fun nutrition(request: MealFoodsRequest) = call("meal/nutrition.php") { api.calculateMealNutrition(it, request) }
    suspend fun search(query: String) = call("meal/foods.php") { api.searchMealFoods(it, query) }
    suspend fun favorites() = call("meal/favorites.php") { api.getMealFavorites(it) }
    suspend fun toggleFavorite(id: Long, favorite: Boolean) = call("meal/favorites.php") { api.toggleMealFavorite(it, MealFavoriteToggleRequest(id, favorite)) }
    suspend fun quickRecord(templateId: Long?, foodId: Long?) = call("meal/quick-record.php") { api.quickRecordMeal(it, MealQuickRecordRequest(templateId, foodId)) }
    suspend fun calendar(month: String) = call("meal/calendar.php") { api.getMealCalendar(it, month) }
    suspend fun graph(period: String) = call("meal/graph.php") { api.getMealGraph(it, period) }
    suspend fun analysis(period: String) = call("meal/analysis.php") { api.getMealAiAnalysis(it, period) }
    suspend fun delete(id: Long) = call("meal/delete.php") { api.deleteMeal(it, MealDeleteRequest(id)) }

    suspend fun save(file: File?, recordId: Long?, date: String, time: String, type: String, water: Int, memo: String, mealType: String, representative: String, foods: List<MealFoodDto>): EmotionServerResult<MealActionResponse> {
        val payloadJson = JSONObject().apply {
            if (recordId != null) put("mealRecordId", recordId)
            put("meal_date", date); put("meal_time", time); put("meal_type", type)
            put("drinking_water_ml", water); put("memo", memo)
            put("analysis_meal_type", mealType); put("representative_food", representative)
            put("foods", JSONArray().apply { foods.forEach { food -> put(JSONObject().apply {
                put("name", food.name); put("amount", food.amount); put("unit", food.unit)
                put("included", food.included); put("portion_type", food.portionType)
                put("portion_level", food.portionLevel); put("count", food.count)
            }) } })
        }
        val endpoint = if (recordId == null) "meal/save.php" else "meal/update.php"
        if (BuildConfig.DEBUG) {
            val debugPayload = JSONObject(payloadJson.toString()).apply {
                put("memo", "[redacted:length=${memo.length}]")
            }
            val image = JSONObject().apply {
                put("present", file != null)
                put("filename", file?.name ?: JSONObject.NULL)
                put("extension", file?.extension ?: JSONObject.NULL)
                put("size", file?.length() ?: 0L)
            }
            Log.d(
                "MEAL_SAVE_REQUEST",
                "endpoint=$endpoint selected_candidate=${representative.take(200)} " +
                    "image=$image request_body=$debugPayload"
            )
        }
        val payload = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        return call(endpoint) { auth -> if (recordId == null) api.saveMeal(auth, file?.part(), payload) else api.updateMeal(auth, file?.part(), payload) }
    }

    private fun File.part(): MultipartBody.Part {
        val mime = when (extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        return MultipartBody.Part.createFormData("meal_image", name, asRequestBody(mime.toMediaType()))
    }

    private suspend fun <T> call(block: suspend (String) -> retrofit2.Response<T>): EmotionServerResult<T> {
        return call("meal", block)
    }

    private suspend fun <T> call(endpoint: String, block: suspend (String) -> retrofit2.Response<T>): EmotionServerResult<T> {
        val bearer = auth() ?: return EmotionServerResult.Unauthorized
        return try {
            val response = block(bearer)
            val body = response.body()
            when {
                response.isSuccessful && body != null -> EmotionServerResult.Success(body)
                response.code() == 401 -> EmotionServerResult.Unauthorized
                else -> {
                    val raw = response.errorBody()?.string().orEmpty()
                    val message = runCatching { JSONObject(raw).optString("message") }.getOrNull().takeUnless { it.isNullOrBlank() }
                        ?: if (response.code() in 500..599) "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요." else "식단 요청을 처리하지 못했습니다."
                    EmotionServerResult.Error(response.code(), message)
                }
            }
        } catch (e: SocketTimeoutException) {
            if (BuildConfig.DEBUG) Log.e("MEAL_API", "endpoint=$endpoint exception=${e.javaClass.name} message=${e.message}")
            EmotionServerResult.Error(0, "서버 응답이 지연되고 있습니다. 다시 시도해주세요.")
        } catch (e: JsonDataException) {
            if (BuildConfig.DEBUG) Log.e("MEAL_API", "endpoint=$endpoint json=${e.javaClass.name} message=${e.message}", e)
            if (BuildConfig.DEBUG && endpoint in setOf("meal/save.php", "meal/update.php")) Log.e("MEAL_SAVE_RESPONSE", "parsing_exception=${e.javaClass.name} message=${e.message}", e)
            EmotionServerResult.Error(-1, "서버 응답을 처리하지 못했습니다.")
        } catch (e: JsonEncodingException) {
            if (BuildConfig.DEBUG) Log.e("MEAL_API", "endpoint=$endpoint json=${e.javaClass.name} message=${e.message}", e)
            if (BuildConfig.DEBUG && endpoint in setOf("meal/save.php", "meal/update.php")) Log.e("MEAL_SAVE_RESPONSE", "parsing_exception=${e.javaClass.name} message=${e.message}", e)
            EmotionServerResult.Error(-1, "서버 응답을 처리하지 못했습니다.")
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Log.e("MEAL_API", "endpoint=$endpoint exception=${e.javaClass.name} message=${e.message}")
            if (BuildConfig.DEBUG && endpoint in setOf("meal/save.php", "meal/update.php")) Log.e("MEAL_SAVE_RESPONSE", "network_exception=${e.javaClass.name} message=${e.message}")
            EmotionServerResult.Error(0, "인터넷 연결을 확인해주세요.")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("MEAL_API", "endpoint=$endpoint exception=${e.javaClass.name} message=${e.message}", e)
            if (BuildConfig.DEBUG && endpoint in setOf("meal/save.php", "meal/update.php")) Log.e("MEAL_SAVE_RESPONSE", "exception=${e.javaClass.name} message=${e.message}", e)
            EmotionServerResult.Error(-1, "서버 응답을 처리하지 못했습니다.")
        }
    }
}
