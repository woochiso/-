package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiAnalysisCategoryDto(
    @Json(name = "category_code") val code: String,
    @Json(name = "category_hanja") val hanja: String,
    @Json(name = "category_name") val name: String,
    @Json(name = "category_label") val label: String,
    val color: String,
    @Json(name = "total_count") val totalCount: Int,
    val values: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiAnalysisTopEmotionDto(
    @Json(name = "emotion_id") val emotionId: Long,
    @Json(name = "emotion_name") val emotionName: String,
    @Json(name = "category_code") val categoryCode: String? = null,
    @Json(name = "total_count") val totalCount: Int
)

@JsonClass(generateAdapter = true)
data class AiAnalysisChangeDto(
    @Json(name = "category_code") val categoryCode: String,
    @Json(name = "category_label") val categoryLabel: String,
    val color: String,
    val current: Int,
    val previous: Int,
    val percent: Double? = null,
    @Json(name = "is_new") val isNew: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AiAnalysisStoryDto(
    @Json(name = "story_id") val storyId: Long,
    @Json(name = "story_title") val storyTitle: String,
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "recent_count") val recentCount: Int = 0,
    @Json(name = "past_count") val pastCount: Int = 0,
    val emotions: List<AiAnalysisStoryEmotionDto> = emptyList(),
    @Json(name = "recovery_trend") val recoveryTrend: String = ""
)

@JsonClass(generateAdapter = true)
data class AiAnalysisStoryEmotionDto(
    @Json(name = "emotion_name") val emotionName: String,
    @Json(name = "total_count") val totalCount: Int
)

@JsonClass(generateAdapter = true)
data class AiAnalysisPatternsDto(
    @Json(name = "top_emotions") val topEmotions: List<AiAnalysisTopEmotionDto> = emptyList(),
    @Json(name = "category_changes") val categoryChanges: List<AiAnalysisChangeDto> = emptyList(),
    @Json(name = "story_impacts") val storyImpacts: List<AiAnalysisStoryDto> = emptyList(),
    @Json(name = "summary_messages") val summaryMessages: List<String> = emptyList(),
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "unique_emotions") val uniqueEmotions: Int = 0
)

@JsonClass(generateAdapter = true)
data class AiAnalysisResultDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "positive_ratio") val positiveRatio: Double = 0.0,
    @Json(name = "negative_ratio") val negativeRatio: Double = 0.0,
    @Json(name = "other_ratio") val otherRatio: Double = 0.0,
    val messages: List<String> = emptyList(),
    val advice: String = ""
)

@JsonClass(generateAdapter = true)
data class AiAnalysisPeakCategoryDto(val label: String, val name: String, val count: Int)

@JsonClass(generateAdapter = true)
data class AiAnalysisHourlySummaryDto(
    @Json(name = "peak_index") val peakIndex: Int? = null,
    @Json(name = "peak_label") val peakLabel: String? = null,
    @Json(name = "peak_total") val peakTotal: Int = 0,
    @Json(name = "peak_category") val peakCategory: AiAnalysisPeakCategoryDto? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisSeriesDto(
    @Json(name = "category_code") val categoryCode: String,
    @Json(name = "category_label") val categoryLabel: String,
    @Json(name = "category_name") val categoryName: String,
    val color: String,
    val values: List<Int> = emptyList(),
    @Json(name = "total_count") val totalCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AiAnalysisHourlyDto(
    val unit: String,
    @Json(name = "source_table") val sourceTable: String,
    val labels: List<String> = emptyList(),
    val series: List<AiAnalysisSeriesDto> = emptyList(),
    @Json(name = "has_data") val hasData: Boolean = false,
    @Json(name = "collection_start") val collectionStart: String? = null,
    val summary: AiAnalysisHourlySummaryDto
)

@JsonClass(generateAdapter = true)
data class AiEmotionAnalysisResponse(
    val success: Boolean,
    val period: String,
    val granularity: String,
    @Json(name = "graph_unit") val graphUnit: String,
    @Json(name = "period_label") val periodLabel: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    val dates: List<String> = emptyList(),
    val categories: List<AiAnalysisCategoryDto> = emptyList(),
    val analysis: AiAnalysisResultDto,
    val patterns: AiAnalysisPatternsDto,
    val hourly: AiAnalysisHourlyDto
)

@JsonClass(generateAdapter = true)
data class AiAnalysisSummaryRequest(val period: String, val startDate: String? = null, val endDate: String? = null, val force: Boolean = false)

@JsonClass(generateAdapter = true)
data class AiAnalysisTextDto(
    @Json(name = "analysis_text") val analysisText: String,
    @Json(name = "model_name") val modelName: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisSummaryResponse(
    val success: Boolean,
    val exists: Boolean = false,
    val cached: Boolean = false,
    val analysis: AiAnalysisTextDto? = null,
    val message: String? = null
)

// 홈페이지 RecoveryInsightService의 결과를 그대로 전달받는 Android 전용 DTO.
// 서버 버전 차이에도 화면이 종료되지 않도록 신규 컬렉션은 모두 안전한 기본값을 둔다.
@JsonClass(generateAdapter = true)
data class RecoveryInsightPeriodDto(
    val label: String = "",
    @Json(name = "start_date") val startDate: String = "",
    @Json(name = "end_date") val endDate: String = ""
)

@JsonClass(generateAdapter = true)
data class RecoveryEmotionChangeDto(
    @Json(name = "category_code") val categoryCode: String = "",
    @Json(name = "category_label") val categoryLabel: String = "",
    @Json(name = "category_hanja") val categoryHanja: String = "",
    val before: Int = 0,
    val after: Int = 0,
    val delta: Int = 0,
    @Json(name = "average_delta") val averageDelta: Double = 0.0,
    @Json(name = "same_direction_count") val sameDirectionCount: Int = 0,
    @Json(name = "same_direction_ratio") val sameDirectionRatio: Int = 0,
    @Json(name = "dominant_direction") val dominantDirection: String = "same",
    @Json(name = "repeated_change") val repeatedChange: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RecoveryInsightSessionDto(
    @Json(name = "session_id") val sessionId: String = "",
    @Json(name = "activity_name") val activityName: String = "",
    @Json(name = "recorded_at") val recordedAt: String = "",
    val changes: List<RecoveryEmotionChangeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RecoveryActivityPatternDto(
    @Json(name = "activity_id") val activityId: Long = 0,
    @Json(name = "activity_name") val activityName: String = "",
    val count: Int = 0,
    @Json(name = "enough_data") val enoughData: Boolean = false,
    @Json(name = "has_repeated_change") val hasRepeatedChange: Boolean = false,
    @Json(name = "favorable_rate") val favorableRate: Int = 0,
    @Json(name = "emotion_changes") val emotionChanges: List<RecoveryEmotionChangeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StoryRecallDto(
    @Json(name = "story_id") val storyId: Long = 0,
    @Json(name = "story_title") val storyTitle: String = "",
    @Json(name = "current_count") val currentCount: Int = 0,
    @Json(name = "previous_count") val previousCount: Int = 0,
    @Json(name = "daily_average") val dailyAverage: Double = 0.0,
    @Json(name = "previous_daily_average") val previousDailyAverage: Double? = null,
    @Json(name = "change_percent") val changePercent: Double? = null,
    @Json(name = "previous_data_available") val previousDataAvailable: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RecoveryLongTermDto(
    val available: Boolean = false,
    @Json(name = "period_days") val periodDays: Int = 0,
    val stories: List<StoryRecallDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RecoveryInsightDataDto(
    val period: RecoveryInsightPeriodDto = RecoveryInsightPeriodDto(),
    @Json(name = "minimum_pattern_records") val minimumPatternRecords: Int = 3,
    @Json(name = "comparable_records") val comparableRecords: Int = 0,
    @Json(name = "recent_sessions") val recentSessions: List<RecoveryInsightSessionDto> = emptyList(),
    @Json(name = "overall_changes") val overallChanges: List<RecoveryEmotionChangeDto> = emptyList(),
    @Json(name = "activity_patterns") val activityPatterns: List<RecoveryActivityPatternDto> = emptyList(),
    @Json(name = "long_term") val longTerm: RecoveryLongTermDto = RecoveryLongTermDto()
)

@JsonClass(generateAdapter = true)
data class RecoveryInsightResponse(
    val success: Boolean = false,
    @Json(name = "period_key") val periodKey: String = "30d",
    val data: RecoveryInsightDataDto? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class RecoveryAiRequest(
    val period: String,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null,
    val force: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RecoveryAiResponse(
    val success: Boolean = false,
    val cached: Boolean = false,
    @Json(name = "insufficient_data") val insufficientData: Boolean = false,
    val analysis: AiAnalysisTextDto? = null,
    val message: String? = null
)
