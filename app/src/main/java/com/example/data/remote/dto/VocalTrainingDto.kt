package com.example.data.remote.dto

import com.squareup.moshi.Json

data class VocalScoresDto(
    @Json(name = "pitch_stability") val pitchStability: Int = 0,
    @Json(name = "rhythm_stability") val rhythmStability: Int = 0,
    val breath: Int = 0,
    val pronunciation: Int = 0,
    val dynamics: Int = 0,
    val emotion: Int = 0,
    val naturalness: Int = 0
)

data class VocalAnalysisResponse(
    val success: Boolean = false,
    @Json(name = "performance_mode") val performanceMode: String? = null,
    @Json(name = "overall_score") val overallScore: Int? = null,
    val scores: VocalScoresDto? = null,
    val strength: String? = null,
    @Json(name = "vocal_point") val vocalPoint: String? = null,
    @Json(name = "next_challenge") val nextChallenge: String? = null,
    val transcript: String? = null,
    @Json(name = "tts_token") val ttsToken: String? = null,
    val message: String? = null
)

data class VocalTtsRequest(@Json(name = "ttsToken") val ttsToken: String, val voice: String = "DEFAULT")
