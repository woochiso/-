package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CounselingSessionDto(
    @Json(name="session_id") val sessionId:Long,
    val title:String,
    val category:String?=null,
    val status:String,
    @Json(name="message_count") val messageCount:Int,
    @Json(name="created_at") val createdAt:String,
    @Json(name="last_message_at") val lastMessageAt:String
)

@JsonClass(generateAdapter = true)
data class CounselingMessageDto(
    @Json(name="message_id") val messageId:Long,
    val role:String,
    val content:String,
    @Json(name="created_at") val createdAt:String
)

@JsonClass(generateAdapter = true)
data class CounselingCrisisDto(
    val level:String = "NONE",
    @Json(name="show_card") val showCard:Boolean = false
)

@JsonClass(generateAdapter = true)
data class CounselingResponse(
    val success:Boolean,
    val sessions:List<CounselingSessionDto> = emptyList(),
    val session:CounselingSessionDto? = null,
    val messages:List<CounselingMessageDto> = emptyList(),
    @Json(name="session_id") val sessionId:Long? = null,
    @Json(name="message_id") val messageId:Long? = null,
    @Json(name="ai_success") val aiSuccess:Boolean? = null,
    @Json(name="assistant_available") val assistantAvailable:Boolean? = null,
    val crisis:CounselingCrisisDto? = null,
    val duplicate:Boolean = false,
    val message:String? = null
)

@JsonClass(generateAdapter = true)
data class CounselingMessageRequest(
    val action:String = "message",
    @Json(name="sessionId") val sessionId:Long? = null,
    val category:String = "FREE_TALK",
    val message:String,
    @Json(name="client_message_id") val clientMessageId:String,
    @Json(name="include_emotion_context") val includeEmotionContext:Boolean = false,
    @Json(name="story_id") val storyId:Long? = null
)

@JsonClass(generateAdapter = true)
data class CounselingDeleteRequest(
    val action:String = "delete",
    @Json(name="sessionId") val sessionId:Long
)

@JsonClass(generateAdapter = true)
data class CounselingTtsRequest(
    @Json(name="sessionId") val sessionId: Long,
    @Json(name="messageId") val messageId: Long,
    @Json(name="counselorVoice") val counselorVoice: String = "DEFAULT"
)
