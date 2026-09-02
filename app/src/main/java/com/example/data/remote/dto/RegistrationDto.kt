package com.example.data.remote.dto

data class RegistrationRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val phoneNumber: String,
    val nickname: String,
    val birthYear: Int,
    val gender: String,
    val occupation: String,
    val occupationOther: String? = null,
    val maritalStatus: String,
    val childrenCount: Int,
    val purpose: String,
    val termsAgreed: Boolean,
    val privacyAgreed: Boolean
)

data class RegistrationResponse(
    val success: Boolean,
    val token: String? = null,
    val user: LoginUserDto? = null,
    val message: String? = null
)

data class EmailAvailabilityRequest(val email: String)

data class EmailAvailabilityResponse(
    val ok: Boolean,
    val available: Boolean,
    val message: String? = null
)

data class NicknameAvailabilityRequest(val nickname: String)

data class NicknameAvailabilityResponse(
    val ok: Boolean,
    val available: Boolean,
    val message: String? = null
)

data class SimpleActionResponse(
    val success: Boolean,
    val message: String? = null
)

data class ForgotPasswordRequest(val email: String)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String? = null
)
