package com.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val user: LoginUserDto? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginUserDto(
    val id: Long,
    val email: String,
    val nickname: String? = null,
    val grade: String? = null
)
