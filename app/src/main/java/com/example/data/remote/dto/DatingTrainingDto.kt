package com.example.data.remote.dto

import com.squareup.moshi.Json

data class DatingOptionDto(
    val id: String = "",
    val title: String = "",
    val description: String = ""
)

data class DatingConfigResponse(
    val success: Boolean = false,
    val categories: List<DatingOptionDto> = emptyList(),
    val styles: List<DatingOptionDto> = emptyList(),
    val voices: List<DatingOptionDto> = emptyList(),
    @Json(name = "max_turns") val maxTurns: Int = 12,
    val message: String? = null
)

data class DatingActionRequest(
    val action: String,
    @Json(name = "requestId") val requestId: String,
    val category: String? = null,
    val style: String? = null,
    @Json(name = "practiceId") val practiceId: String? = null,
    @Json(name = "expectedTurn") val expectedTurn: Int? = null,
    val text: String? = null
)

data class DatingActionResponse(
    val success: Boolean = false,
    @Json(name = "practice_id") val practiceId: String? = null,
    val turns: Int = 0,
    @Json(name = "max_turns") val maxTurns: Int = 12,
    val scenario: String? = null,
    @Json(name = "partner_line") val partnerLine: String? = null,
    val transcript: String? = null,
    @Json(name = "tts_token") val ttsToken: String? = null,
    val finished: Boolean = false,
    val risk: Boolean = false,
    @Json(name = "safety_message") val safetyMessage: String? = null,
    @Json(name = "practice_score") val practiceScore: Int? = null,
    @Json(name = "natural_point") val naturalPoint: String? = null,
    @Json(name = "practice_point") val practicePoint: String? = null,
    @Json(name = "alternative_phrase") val alternativePhrase: String? = null,
    @Json(name = "conversation_tip") val conversationTip: String? = null,
    val message: String? = null
)

data class DatingTtsRequest(
    @Json(name = "ttsToken") val ttsToken: String,
    val voice: String
)
