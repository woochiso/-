package com.example.data.remote.dto

data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val newPasswordConfirm: String
)

data class PasswordChangeResponse(
    val success: Boolean = false,
    val message: String? = null
)
