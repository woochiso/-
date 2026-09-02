package com.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EmotionCategoryDto(
    val categoryId: Long,
    val categoryCode: String,
    val categoryHanja: String,
    val categoryName: String,
    val categoryLabel: String,
    val description: String? = null,
    val colorCode: String
)

@JsonClass(generateAdapter = true)
data class EmotionMasterDto(
    val emotionId: Long,
    val emotionName: String,
    val description: String? = null,
    val categoryId: Long,
    val categoryCode: String,
    val categoryLabel: String,
    val colorCode: String,
    val favorite: Boolean
)

@JsonClass(generateAdapter = true)
data class EmotionMasterResponse(
    val success: Boolean,
    val categories: List<EmotionCategoryDto> = emptyList(),
    val emotions: List<EmotionMasterDto> = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EmotionFavoriteToggleRequest(val emotionId: Long, val favorite: Boolean? = null)

@JsonClass(generateAdapter = true)
data class EmotionFavoriteToggleResponse(
    val success: Boolean,
    val emotionId: Long,
    val favorite: Boolean,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class FavoriteEmotionServerDto(
    val emotionId: Long,
    val emotionName: String,
    val categoryId: Long,
    val categoryCode: String,
    val categoryLabel: String,
    val categoryHanja: String,
    val colorCode: String,
    val todayCount: Int,
    val totalCount: Int
)

@JsonClass(generateAdapter = true)
data class FavoriteEmotionsResponse(
    val success: Boolean,
    val recordDate: String,
    val favorites: List<FavoriteEmotionServerDto> = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class TodayEmotionRecordDto(val emotionId: Long, val todayCount: Int)

@JsonClass(generateAdapter = true)
data class TodayEmotionResponse(
    val success: Boolean,
    val recordDate: String,
    val records: List<TodayEmotionRecordDto> = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class TodayEmotionSaveRequest(
    val recordDate: String,
    val records: List<TodayEmotionRecordDto>
)

@JsonClass(generateAdapter = true)
data class TodayEmotionSaveResponse(
    val success: Boolean,
    val message: String? = null,
    val recordDate: String,
    val favorites: List<FavoriteEmotionServerDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TodayEmotionAddRequest(
    val emotionId: Long,
    val addCount: Int,
    val requestToken: String
)

@JsonClass(generateAdapter = true)
data class TodayEmotionAddResponse(
    val success: Boolean,
    val emotionId: Long,
    val addedCount: Int,
    val newTotal: Int,
    val delta: Int,
    val eventType: String? = null,
    val occurredAt: String? = null,
    val idempotencyReplayed: Boolean = false,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class TodayEmotionCorrectionRequest(
    val emotionId: Long,
    val newTotal: Int,
    val requestToken: String
)

@JsonClass(generateAdapter = true)
data class TodayEmotionCorrectionResponse(
    val success: Boolean,
    val emotionId: Long,
    val newTotal: Int,
    val delta: Int,
    val eventType: String? = null,
    val occurredAt: String? = null,
    val idempotencyReplayed: Boolean = false,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EmotionStoryRecordDto(
    val dailyRecordId: Long,
    val recordDate: String,
    val emotionId: Long,
    val emotionName: String,
    val categoryCode: String,
    val categoryLabel: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class EmotionStoryOptionDto(val storyId: Long, val title: String, val period: String)

@JsonClass(generateAdapter = true)
data class EmotionStorySelectedDto(val storyId: Long, val linkStrength: Int)

@JsonClass(generateAdapter = true)
data class EmotionStoryOptionsResponse(
    val success: Boolean,
    val record: EmotionStoryRecordDto? = null,
    val stories: List<EmotionStoryOptionDto> = emptyList(),
    val selected: EmotionStorySelectedDto? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EmotionStoryLinkRequest(
    val emotionId: Long,
    val storyId: Long,
    val recordDate: String,
    val linkStrength: Int
)

@JsonClass(generateAdapter = true)
data class EmotionStoryLinkResponse(
    val success: Boolean,
    val message: String? = null,
    val selected: EmotionStorySelectedDto? = null
)

@JsonClass(generateAdapter = true)
data class EmotionGraphPeriodDto(val type: String, val startDate: String, val endDate: String)

@JsonClass(generateAdapter = true)
data class EmotionGraphCategoryDto(
    val categoryId: Long, val categoryCode: String, val categoryHanja: String,
    val categoryName: String, val categoryLabel: String, val colorCode: String,
    val totalCount: Int, val percentage: Double, val averageLinkStrength: Double? = null
)

@JsonClass(generateAdapter = true)
data class EmotionGraphEmotionDto(
    val emotionId: Long, val emotionName: String, val categoryId: Long? = null,
    val categoryCode: String, val categoryLabel: String, val categoryHanja: String? = null,
    val colorCode: String, val totalCount: Int, val percentage: Double? = null,
    val averageLinkStrength: Double? = null
)

@JsonClass(generateAdapter = true)
data class EmotionGraphResponse(
    val success: Boolean,
    val period: EmotionGraphPeriodDto? = null,
    val totalCount: Int = 0,
    val categories: List<EmotionGraphCategoryDto> = emptyList(),
    val emotions: List<EmotionGraphEmotionDto> = emptyList(),
    val dailySeriesIncluded: Boolean = false,
    val dailySeries: List<Map<String, Any?>> = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class StoryGraphStoryDto(
    val storyId: Long,
    val title: String,
    val periodLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class StoryGraphResponse(
    val success: Boolean,
    val story: StoryGraphStoryDto? = null,
    val period: EmotionGraphPeriodDto? = null,
    val totalCount: Int = 0,
    val categories: List<EmotionGraphCategoryDto>? = emptyList(),
    val emotions: List<EmotionGraphEmotionDto>? = emptyList(),
    val dailySeriesIncluded: Boolean = false,
    val dailySeries: List<Map<String, Any?>>? = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EmotionRelatedStoryEmotionDto(
    val emotionId: Long,
    val emotionName: String,
    val categoryCode: String,
    val categoryLabel: String,
    val colorCode: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class EmotionRelatedStoryDto(
    val storyId: Long,
    val title: String,
    val summary: String,
    val storyCreatedAt: String,
    val recordDate: String,
    val totalCount: Int,
    val emotions: List<EmotionRelatedStoryEmotionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EmotionStoryInsightSelectionDto(
    val type: String,
    val categoryCode: String? = null,
    val emotionId: Long? = null,
    val label: String = "",
    val startDate: String,
    val endDate: String
)

@JsonClass(generateAdapter = true)
data class EmotionStoryInsightSummaryDto(
    val storyCount: Int = 0,
    val connectionCount: Int = 0,
    val totalCount: Int = 0,
    val limited: Boolean = false
)

@JsonClass(generateAdapter = true)
data class EmotionStoryInsightsResponse(
    val success: Boolean,
    val selection: EmotionStoryInsightSelectionDto? = null,
    val summary: EmotionStoryInsightSummaryDto = EmotionStoryInsightSummaryDto(),
    val stories: List<EmotionRelatedStoryDto> = emptyList(),
    val message: String? = null
)
