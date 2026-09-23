package com.example.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AuthSession(
    val userId: Long,
    val email: String,
    val nickname: String?,
    val grade: String?
)

class TokenManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    fun saveSession(
        token: String,
        userId: Long,
        email: String,
        nickname: String?,
        grade: String?
    ) {
        preferences.edit()
            .putString(KEY_TOKEN, encrypt(token))
            .putString(KEY_USER_ID, encrypt(userId.toString()))
            .putString(KEY_EMAIL, encrypt(email))
            .putString(KEY_NICKNAME, nickname?.let(::encrypt))
            .putString(KEY_GRADE, grade?.let(::encrypt))
            .apply()
    }

    fun loadSession(): AuthSession? {
        return try {
            val token = decrypt(preferences.getString(KEY_TOKEN, null))
            val userId = decrypt(preferences.getString(KEY_USER_ID, null))?.toLongOrNull()
            val email = decrypt(preferences.getString(KEY_EMAIL, null))

            if (token.isNullOrBlank() || userId == null || email.isNullOrBlank()) {
                null
            } else {
                AuthSession(
                    userId = userId,
                    email = email,
                    nickname = decrypt(preferences.getString(KEY_NICKNAME, null)),
                    grade = decrypt(preferences.getString(KEY_GRADE, null))
                )
            }
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    fun accessToken(): String? = try {
        decrypt(preferences.getString(KEY_TOKEN, null))
    } catch (_: Exception) {
        clearSession()
        null
    }

    fun updateUserProfile(email: String, nickname: String?, grade: String?) {
        preferences.edit()
            .putString(KEY_EMAIL, encrypt(email))
            .putString(KEY_NICKNAME, nickname?.let(::encrypt))
            .putString(KEY_GRADE, grade?.let(::encrypt))
            .apply()
    }

    fun saveLastLoginEmail(email: String) {
        preferences.edit()
            .putString(KEY_LAST_LOGIN_EMAIL, encrypt(email))
            .apply()
    }

    fun lastLoginEmail(): String = try {
        decrypt(preferences.getString(KEY_LAST_LOGIN_EMAIL, null)).orEmpty()
    } catch (_: Exception) {
        preferences.edit().remove(KEY_LAST_LOGIN_EMAIL).apply()
        ""
    }

    fun clearSession() {
        preferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_NICKNAME)
            .remove(KEY_GRADE)
            .apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null

        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_SIZE_BYTES)
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALIAS = "woochiso_auth_key_v1"
        const val PREFERENCES_NAME = "woochiso_secure_auth"
        const val IV_SIZE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val KEY_TOKEN = "access_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_NICKNAME = "nickname"
        const val KEY_GRADE = "grade"
        const val KEY_LAST_LOGIN_EMAIL = "last_login_email"
    }
}
