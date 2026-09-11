package com.example.data.remote.dto

data class HelpVideosResponse(
    val success: Boolean,
    val videos: List<HelpVideo> = emptyList(),
    val message: String? = null
)

data class HelpVideo(
    val id: Long,
    val title: String,
    val description: String = "",
    val youtubeUrl: String,
    val type: String,
    val sortOrder: Int,
    val thumbnailUrl: String? = null
)
