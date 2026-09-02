package com.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecoveryCategoryDto(val categoryCode:String,val categoryHanja:String,val categoryName:String,val categoryLabel:String,val colorCode:String)

@JsonClass(generateAdapter = true)
data class RecoverySessionDto(
    val recoverySessionId:String,val status:String,
    val beforeEmotions:Map<String,Int> = emptyMap(),val afterEmotions:Map<String,Int>? = null,
    val startedAt:String? = null,val completedAt:String? = null
)

@JsonClass(generateAdapter = true)
data class RecoveryActivityDto(
    val activityId:Long,val activityCode:String,val activityName:String,val iconText:String,
    val shortAction:String,val description:String,val recommendedMinutes:String,val expectedEffect:String,
    val todaySession:RecoverySessionDto? = null
)

@JsonClass(generateAdapter = true)
data class RecoveryRecordDto(
    val recoverySessionId:String,val activityId:Long,val activityName:String,val iconText:String,
    val beforeEmotions:Map<String,Int> = emptyMap(),val afterEmotions:Map<String,Int> = emptyMap(),
    val memo:String = "",val startedAt:String,val completedAt:String
)

@JsonClass(generateAdapter = true)
data class RecoveryOverviewResponse(
    val success:Boolean,val todayCount:Int = 0,val activities:List<RecoveryActivityDto> = emptyList(),
    val categories:List<RecoveryCategoryDto> = emptyList(),val recentRecords:List<RecoveryRecordDto> = emptyList(),
    val message:String? = null
)

@JsonClass(generateAdapter = true)
data class RecoverySessionRequest(
    val action:String,val activityId:Long,val recoverySessionId:String,
    val emotions:Map<String,Int>,val memo:String = ""
)

@JsonClass(generateAdapter = true)
data class RecoverySessionResponse(
    val success:Boolean,val recoverySessionId:String,val status:String,
    val beforeEmotions:Map<String,Int> = emptyMap(),val afterEmotions:Map<String,Int>? = null,
    val alreadyExists:Boolean = false,val alreadyCompleted:Boolean = false,val message:String? = null
)
