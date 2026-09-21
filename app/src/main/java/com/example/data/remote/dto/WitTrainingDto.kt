package com.example.data.remote.dto

import com.squareup.moshi.Json

data class WitOptionDto(val id:String="",val title:String="",val description:String="")
data class WitConfigResponse(val success:Boolean=false,val categories:List<WitOptionDto> = emptyList(),val voices:List<WitOptionDto> = emptyList(),@Json(name="default_voice") val defaultVoice:String="DEFAULT",val message:String?=null)
data class WitActionRequest(val action:String,@Json(name="requestId") val requestId:String,val category:String?=null,@Json(name="customSituation") val customSituation:String?=null,@Json(name="practiceId") val practiceId:String?=null,val text:String?=null)
data class WitScoresDto(val wit:Int=0,val situation:Int=0,val naturalness:Int=0,val consideration:Int=0,val quickness:Int=0)
data class WitActionResponse(val success:Boolean=false,@Json(name="practice_id") val practiceId:String?=null,val category:String?=null,@Json(name="category_title") val categoryTitle:String?=null,@Json(name="category_description") val categoryDescription:String?=null,val scenario:String?=null,@Json(name="character_line") val characterLine:String?=null,val context:String?=null,val transcript:String?=null,val risk:Boolean=false,@Json(name="safety_message") val safetyMessage:String?=null,@Json(name="character_response") val characterResponse:String?=null,val scores:WitScoresDto?=null,@Json(name="overall_score") val overallScore:Int?=null,val strength:String?=null,@Json(name="better_phrase") val betterPhrase:String?=null,@Json(name="alternative_phrase") val alternativePhrase:String?=null,@Json(name="tts_token") val ttsToken:String?=null,val message:String?=null)
data class WitTtsRequest(@Json(name="ttsToken") val ttsToken:String,val voice:String)
