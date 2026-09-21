package com.example.data.remote.dto

import com.squareup.moshi.Json

data class HumorAnalysisResponse(
    val success: Boolean = false,
    val score: Int? = null,
    val highlight: String? = null,
    @Json(name = "delivery_feedback") val deliveryFeedback: String? = null,
    val suggestion: String? = null,
    val transcript: String? = null,
    @Json(name = "audio_analysis_available") val audioAnalysisAvailable: Boolean = false,
    @Json(name = "audio_analysis_notice") val audioAnalysisNotice: String? = null,
    @Json(name = "tts_token") val ttsToken: String? = null,
    val message: String? = null
)

data class HumorTtsRequest(@Json(name = "ttsToken") val ttsToken: String)

