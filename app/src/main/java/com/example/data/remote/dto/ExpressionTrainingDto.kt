package com.example.data.remote.dto

import com.squareup.moshi.Json

data class ExpressionOptionDto(val id: String = "", val title: String = "", val description: String = "")
data class ExpressionConfigResponse(
    val success: Boolean = false,
    val categories: List<ExpressionOptionDto> = emptyList(),
    val voices: List<ExpressionOptionDto> = emptyList(),
    @Json(name = "default_voice") val defaultVoice: String = "DEFAULT",
    val message: String? = null
)
data class ExpressionActionRequest(
    val action: String,
    @Json(name = "requestId") val requestId: String,
    val category: String? = null,
    @Json(name = "practiceId") val practiceId: String? = null,
    @Json(name = "inputMode") val inputMode: String = "text",
    val text: String? = null
)
data class ExpressionActionResponse(
    val success: Boolean = false,
    @Json(name = "practice_id") val practiceId: String? = null,
    val category: String? = null,
    @Json(name = "category_description") val categoryDescription: String? = null,
    val scenario: String? = null,
    @Json(name = "partner_line") val partnerLine: String? = null,
    val transcript: String? = null,
    val risk: Boolean = false,
    @Json(name = "safety_message") val safetyMessage: String? = null,
    val reaction: String? = null,
    val strength: String? = null,
    val suggestion: String? = null,
    val example: String? = null,
    @Json(name = "tts_token") val ttsToken: String? = null,
    val message: String? = null
)
data class ExpressionTtsRequest(@Json(name = "ttsToken") val ttsToken: String, val voice: String)
