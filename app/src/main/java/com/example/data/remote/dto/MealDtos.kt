package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MealFoodDto(
    val name: String,
    val amount: String = "1",
    val unit: String = "인분",
    val included: Boolean = true,
    @Json(name = "portion_type") val portionType: String = "LEVEL",
    @Json(name = "portion_level") val portionLevel: String? = "normal",
    val count: Int? = null,
    val status: String? = null,
    @Json(name = "needs_confirmation") val needsConfirmation: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MealNutritionDto(
    val available: Boolean = false,
    val source: String? = null,
    val reliability: String? = null,
    // PHP encodes an empty associative array as [] and a populated one as {}.
    // Keep the wire value flexible so both valid server shapes can be consumed.
    val summary: Any? = null,
    val states: Any? = null,
    val message: String? = null
) {
    fun summaryValue(key: String): Double? =
        ((summary as? Map<*, *>)?.get(key) as? Number)?.toDouble()

    fun stateEntries(): List<Pair<String, String>> =
        (states as? Map<*, *>)?.mapNotNull { (key, value) ->
            val name = key as? String ?: return@mapNotNull null
            val label = value as? String ?: return@mapNotNull null
            name to label
        }.orEmpty()
}

@JsonClass(generateAdapter = true)
data class MealRecordDto(
    @Json(name = "meal_record_id") val id: Long,
    @Json(name = "meal_date") val date: String,
    @Json(name = "meal_time") val time: String? = null,
    @Json(name = "meal_type") val type: String,
    @Json(name = "food_name") val foodName: String,
    val memo: String? = null,
    @Json(name = "drinking_water_ml") val waterMl: Int = 0,
    @Json(name = "has_image") val hasImage: Boolean = false,
    @Json(name = "image_url") val imageUrl: String? = null,
    val foods: List<MealFoodDto> = emptyList(),
    val nutrition: MealNutritionDto? = null
)

@JsonClass(generateAdapter = true)
data class MealListResponse(val success: Boolean, val records: List<MealRecordDto> = emptyList(), val message: String? = null)

@JsonClass(generateAdapter = true)
data class MealRepresentativeCandidateDto(
    val name: String,
    val confidence: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class MealAnalyzeResponse(
    val success: Boolean,
    @Json(name = "meal_type") val mealType: String = "single",
    @Json(name = "representative_food") val representativeFood: String = "",
    @Json(name = "representative_candidates") val representativeCandidates: List<MealRepresentativeCandidateDto> = emptyList(),
    @Json(name = "uncertain_items") val uncertainItems: List<String> = emptyList(),
    val foods: List<MealFoodDto> = emptyList(),
    val nutrition: MealNutritionDto? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MealFoodsRequest(
    val foods: List<MealFoodDto>,
    @Json(name = "representative_food") val representativeFood: String,
    @Json(name = "meal_type") val mealType: String = "single",
    @Json(name = "drinking_water_ml") val waterMl: Int = 0
)

@JsonClass(generateAdapter = true)
data class MealNutritionResponse(val success: Boolean, val nutrition: MealNutritionDto? = null, val message: String? = null)

@JsonClass(generateAdapter = true)
data class MealActionResponse(val success: Boolean, @Json(name = "meal_record_id") val recordId: Long? = null, val message: String? = null)

@JsonClass(generateAdapter = true)
data class MealDeleteRequest(@Json(name = "mealRecordId") val recordId: Long)

@JsonClass(generateAdapter = true)
data class MealSearchItemDto(
    @Json(name = "food_id") val id: Long,
    @Json(name = "food_name") val name: String,
    @Json(name = "category_code") val category: String? = null,
    @Json(name = "portion_type") val portionType: String = "LEVEL",
    @Json(name = "is_favorite") val isFavorite: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MealSearchResponse(val success: Boolean, val foods: List<MealSearchItemDto> = emptyList(), val message: String? = null)

@JsonClass(generateAdapter = true)
data class MealFavoriteToggleRequest(@Json(name = "foodId") val foodId: Long, val favorite: Boolean)

@JsonClass(generateAdapter = true)
data class MealFavoriteTemplateDto(
    @Json(name = "template_id") val id: Long,
    @Json(name = "template_name") val name: String,
    val foods: List<MealFoodDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MealFavoritesResponse(
    val success: Boolean,
    val foods: List<MealSearchItemDto> = emptyList(),
    val templates: List<MealFavoriteTemplateDto> = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MealQuickRecordRequest(
    @Json(name = "templateId") val templateId: Long? = null,
    @Json(name = "foodId") val foodId: Long? = null,
    @Json(name = "mealType") val mealType: String = "snack"
)

@JsonClass(generateAdapter = true)
data class MealCalendarResponse(val success: Boolean, val month: String, val records: List<MealRecordDto> = emptyList(), val message: String? = null)

@JsonClass(generateAdapter = true)
data class MealNutrientGraphDto(
    val key: String,
    val label: String,
    @Json(name = "display_state") val displayState: String,
    val valid: Int,
    val average: Double? = null,
    @Json(name = "bubble_size") val bubbleSize: Int = 92
)

@JsonClass(generateAdapter = true)
data class MealGraphResponse(
    val success: Boolean,
    val nickname: String? = null,
    @Json(name = "period_label") val periodLabel: String = "7일",
    @Json(name = "record_count") val recordCount: Int = 0,
    val nutrients: List<MealNutrientGraphDto> = emptyList(),
    // Older API versions returned names only. The current API returns name/count objects.
    @Json(name = "frequent_foods") val frequentFoods: List<Any> = emptyList(),
    val message: String? = null
) {
    fun bubbleScale(valid: Int): Float {
        val values = nutrients.take(7).map { it.valid }
        val minimum = values.minOrNull() ?: return 0.5f
        val maximum = values.maxOrNull() ?: return 0.5f
        return if (minimum == maximum) 0.5f else ((valid - minimum).toFloat() / (maximum - minimum)).coerceIn(0f, 1f)
    }

    fun frequentFoodItems(): List<MealFrequentFoodDto> = frequentFoods.mapNotNull { value ->
        when (value) {
            is String -> MealFrequentFoodDto(value, null)
            is Map<*, *> -> {
                val name = value["food_name"] as? String ?: return@mapNotNull null
                MealFrequentFoodDto(name, (value["record_count"] as? Number)?.toInt())
            }
            else -> null
        }
    }.take(20)
}

data class MealFrequentFoodDto(val name: String, val count: Int?)

@JsonClass(generateAdapter = true)
data class MealAiAnalysisResponse(
    val success: Boolean,
    val summary: String = "",
    val patterns: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    @Json(name = "one_line") val oneLine: String = "",
    val message: String? = null
)
