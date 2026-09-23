package com.example.data.remote.dto

import com.squareup.moshi.Json

data class VoiceTranscriptionResponse(
    val ok: Boolean = false,
    val text: String = "",
    val message: String? = null,
    @Json(name = "error_code") val errorCode: String? = null
)
