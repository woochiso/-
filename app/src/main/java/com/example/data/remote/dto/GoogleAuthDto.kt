package com.example.data.remote.dto

data class GoogleLoginRequest(val idToken: String)
data class GoogleProfileDto(val email: String, val name: String? = null, val picture: String? = null)
data class ConsentVersionsDto(val terms: String, val privacyCollection: String)
data class GoogleAuthResponse(
    val success: Boolean,
    val token: String? = null,
    val user: LoginUserDto? = null,
    val requiresProfile: Boolean = false,
    val googleProfile: GoogleProfileDto? = null,
    val consentVersions: ConsentVersionsDto? = null,
    val message: String? = null
)
data class GoogleSignupRequest(
    val idToken: String,
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
