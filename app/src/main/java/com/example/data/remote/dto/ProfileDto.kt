package com.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val user: ProfileUser? = null
)

@JsonClass(generateAdapter = true)
data class ProfileUser(
    val id: Long,
    val email: String,
    val nickname: String,
    val phone: String? = null,
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
    val phone: String,
    val birthYear: Int?,
    val gender: String,
    val occupation: String,
    val maritalStatus: String,
    val children: Int
)
