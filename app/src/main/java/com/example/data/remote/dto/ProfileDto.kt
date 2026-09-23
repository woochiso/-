package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val user: ProfileUser? = null,
    val usage: ProfileUsage? = null,
    val tts: TtsUsageResponse? = null
)

@JsonClass(generateAdapter = true)
data class ProfileUsage(
    @Json(name = "joined_at") val joinedAt: String? = null,
    @Json(name = "emotion_record_days") val emotionRecordDays: Int = 0,
    @Json(name = "emotion_total_count") val emotionTotalCount: Int = 0,
    @Json(name = "story_count") val storyCount: Int = 0,
    @Json(name = "counseling_count") val counselingCount: Int = 0,
    @Json(name = "recovery_count") val recoveryCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ProfileUser(
    val id: Long,
    val email: String,
    val nickname: String,
    val birthYear: Int? = null,
    val gender: String = "NO_ANSWER",
    val occupation: String? = null,
    val maritalStatus: String = "OTHER",
    val children: Int = 0,
    val role: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    val nickname: String,
    val birthYear: Int?,
    val gender: String,
    val occupation: String,
    val maritalStatus: String,
    val children: Int
)
