package com.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StoryDto(val storyId: Long, val title: String, val content: String? = null,
    val period: String, val age: Int? = null, val year: Int? = null,
    val eventCategory: String, val relatedPerson: String, val importance: Int,
    val currentImpact: Int, val currentStatus: String, val createdAt: String, val updatedAt: String)

@JsonClass(generateAdapter = true)
data class StoryPagination(val page: Int, val limit: Int, val total: Int, val hasMore: Boolean)
@JsonClass(generateAdapter = true)
data class StoryListResponse(val success: Boolean, val stories: List<StoryDto> = emptyList(), val pagination: StoryPagination? = null, val message: String? = null)
@JsonClass(generateAdapter = true)
data class StoryResponse(val success: Boolean, val story: StoryDto? = null, val message: String? = null)
@JsonClass(generateAdapter = true)
data class StoryActionResponse(val success: Boolean, val message: String? = null)
@JsonClass(generateAdapter = true)
data class StoryWriteRequest(val title: String, val content: String, val period: String,
    val age: Int?, val year: Int?, val eventCategory: String, val relatedPerson: String,
    val importance: Int, val currentImpact: Int, val currentStatus: String)
@JsonClass(generateAdapter = true)
data class StoryUpdateRequest(val storyId: Long, val title: String, val content: String,
    val period: String, val age: Int?, val year: Int?, val eventCategory: String,
    val relatedPerson: String, val importance: Int, val currentImpact: Int, val currentStatus: String)
@JsonClass(generateAdapter = true)
data class StoryDeleteRequest(val storyId: Long)
