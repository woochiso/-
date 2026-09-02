package com.example.ui.theme

import androidx.compose.ui.graphics.Color

private val serverHexColor = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

fun parseServerColor(hex: String?, fallback: Color = Color(0xFF74AFDD)): Color {
    val value = hex?.trim() ?: return fallback
    if (!serverHexColor.matches(value)) return fallback
    return runCatching {
        val digits = value.removePrefix("#")
        val argb = if (digits.length == 6) "FF$digits" else digits
        Color(argb.toLong(16))
    }.getOrDefault(fallback)
}
