package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

class WitTrainingRepository(private val api:ApiService,private val tokens:TokenManager){
    private fun auth()=tokens.accessToken()?.let{"Bearer $it"}
    suspend fun config():EmotionServerResult<WitConfigResponse>{val a=auth()?:return EmotionServerResult.Unauthorized;return request{api.getWitConfig(a)}}
    suspend fun action(v:WitActionRequest):EmotionServerResult<WitActionResponse>{val a=auth()?:return EmotionServerResult.Unauthorized;return request{api.witAction(a,v)}}
    suspend fun speech(token:String,voice:String):EmotionServerResult<ByteArray>{val a=auth()?:return EmotionServerResult.Unauthorized;return try{val r=api.getWitSpeech(a,WitTtsRequest(token,voice));val b=r.body()?.bytes();val t=r.headers()["Content-Type"].orEmpty().lowercase();when{r.isSuccessful&&b!=null&&b.size>12&&(t.startsWith("audio/")||t=="application/octet-stream")->EmotionServerResult.Success(b);r.code()==401->EmotionServerResult.Unauthorized;else->EmotionServerResult.Error(r.code(),message(r.errorBody()?.string(),"AI 음성을 재생하지 못했습니다."))}}catch(_:Exception){EmotionServerResult.Error(0,"음성 없이 글로 연습을 이어갈 수 있어요.")}}
    private suspend fun <T> request(block:suspend()->retrofit2.Response<T>): EmotionServerResult<T> = try{val r=block();val b=r.body();when{r.isSuccessful&&b!=null->EmotionServerResult.Success(b);r.code()==401->EmotionServerResult.Unauthorized;else->EmotionServerResult.Error(r.code(),message(r.errorBody()?.string(),"요청을 처리하지 못했습니다."))}}catch(_:SocketTimeoutException){EmotionServerResult.Error(0,"AI 답변이 늦어지고 있어요. 다시 시도해주세요.")}catch(_:IOException){EmotionServerResult.Error(0,"인터넷 연결을 확인해주세요.")}catch(_:Exception){EmotionServerResult.Error(-1,"응답을 처리하지 못했습니다.")}
    private fun message(raw:String?,fallback:String)=runCatching{JSONObject(raw.orEmpty()).optString("message")}.getOrNull().takeUnless{it.isNullOrBlank()}?:fallback
}
