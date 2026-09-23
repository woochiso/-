package com.example.data.remote.dto

import com.squareup.moshi.Json

data class TtsUsageResponse(
    // Standalone /api/tts/usage.php includes success; nested /api/me.php tts status does not.
    val success: Boolean = true,
    val grade: String,
    val limited: Boolean,
    val limit: Int?,
    val used: Int,
    val remaining: Int?,
    @Json(name="reset_timezone") val resetTimezone: String = "Asia/Seoul",
    val message: String? = null
)
