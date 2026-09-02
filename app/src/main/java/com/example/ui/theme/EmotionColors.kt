package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** 우치소 7감정 공통 색상. 감정 이름이 아니라 categoryCode/ID를 기준으로 사용한다. */
object EmotionColors {
    const val ANGER_ARGB = 0xFFF44336L
    const val JOY_ARGB = 0xFFFF9800L
    const val LOVE_ARGB = 0xFFEC407AL
    const val PLEASURE_ARGB = 0xFF2EAF5DL
    const val DESIRE_ARGB = 0xFF16A6B6L
    const val SADNESS_ARGB = 0xFF2F6FE4L
    const val HATE_ARGB = 0xFF7E3FC7L

    val ANGER = Color(ANGER_ARGB)
    val JOY = Color(JOY_ARGB)
    val LOVE = Color(LOVE_ARGB)
    val PLEASURE = Color(PLEASURE_ARGB)
    val DESIRE = Color(DESIRE_ARGB)
    val SADNESS = Color(SADNESS_ARGB)
    val HATE = Color(HATE_ARGB)

    fun forCategoryCode(code: String?, fallback: Color = Color(0xFF74AFDD)): Color =
        when (code?.uppercase()) {
            "ANGER" -> ANGER
            "JOY" -> JOY
            "LOVE" -> LOVE
            "PLEASURE" -> PLEASURE
            "DESIRE" -> DESIRE
            "SADNESS", "SORROW" -> SADNESS
            "HATE", "HATRED", "DISLIKE" -> HATE
            else -> fallback
        }
}
