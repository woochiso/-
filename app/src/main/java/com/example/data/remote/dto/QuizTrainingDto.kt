package com.example.data.remote.dto

import com.squareup.moshi.Json

data class QuizOptionDto(
    val id: String = "",
    val title: String = "",
    val description: String = ""
)

data class QuizConfigResponse(
    val success: Boolean = false,
    val categories: List<QuizOptionDto> = emptyList(),
    val difficulties: List<QuizOptionDto> = emptyList(),
    val voices: List<QuizOptionDto> = emptyList(),
    @Json(name = "default_difficulty") val defaultDifficulty: String = "EASY",
    @Json(name = "default_voice") val defaultVoice: String = "female_caster",
    @Json(name = "question_count") val questionCount: Int = 10,
    @Json(name = "time_limit") val timeLimit: Int = 20,
    val message: String? = null
)

data class QuizQuestionDto(
    val number: Int = 0,
    val total: Int = 10,
    val question: String = "",
    val options: List<String> = emptyList()
)

data class QuizActionRequest(
    val action: String,
    @Json(name = "requestId") val requestId: String,
    val category: String? = null,
    val difficulty: String? = null,
    @Json(name = "gameId") val gameId: String? = null,
    val answer: Int? = null
)

data class QuizActionResponse(
    val success: Boolean = false,
    @Json(name = "game_id") val gameId: String? = null,
    val score: Int = 0,
    val question: QuizQuestionDto? = null,
    val correct: Boolean? = null,
    @Json(name = "timed_out") val timedOut: Boolean = false,
    @Json(name = "correct_index") val correctIndex: Int? = null,
    @Json(name = "correct_answer") val correctAnswer: String? = null,
    val explanation: String? = null,
    val finished: Boolean = false,
    val total: Int? = null,
    @Json(name = "tts_token") val ttsToken: String? = null,
    val message: String? = null
)

data class QuizTtsRequest(
    @Json(name = "ttsToken") val ttsToken: String,
    val voice: String
)
