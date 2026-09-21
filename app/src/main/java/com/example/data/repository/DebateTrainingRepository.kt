package com.example.data.repository
import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import java.io.IOException
class DebateTrainingRepository(private val api:ApiService,private val tokens:TokenManager){
 private fun auth()=tokens.accessToken()?.let{"Bearer $it"}
 suspend fun config():EmotionServerResult<DebateConfigResponse>{val a=auth()?:return EmotionServerResult.Unauthorized;return req{api.getDebateConfig(a)}}
 suspend fun action(v:DebateActionRequest):EmotionServerResult<DebateActionResponse>{val a=auth()?:return EmotionServerResult.Unauthorized;return req{api.debateAction(a,v)}}
 suspend fun speech(t:String,v:String):EmotionServerResult<ByteArray>{val a=auth()?:return EmotionServerResult.Unauthorized;return try{val r=api.getDebateSpeech(a,DebateTtsRequest(t,v));val b=r.body()?.bytes();val type=r.headers()["Content-Type"].orEmpty();if(r.isSuccessful&&b!=null&&b.size>12&&type.startsWith("audio/"))EmotionServerResult.Success(b)else if(r.code()==401)EmotionServerResult.Unauthorized else EmotionServerResult.Error(r.code(),msg(r.errorBody()?.string(),"음성을 재생하지 못했습니다."))}catch(_:Exception){EmotionServerResult.Error(0,"음성 없이 글로 토론을 계속할 수 있어요.")}}
 private suspend fun <T> req(block:suspend () -> retrofit2.Response<T>):EmotionServerResult<T> = try{val r=block();val b=r.body();if(r.isSuccessful&&b!=null)EmotionServerResult.Success(b)else if(r.code()==401)EmotionServerResult.Unauthorized else EmotionServerResult.Error(r.code(),msg(r.errorBody()?.string(),"요청을 처리하지 못했습니다."))}catch(_:IOException){EmotionServerResult.Error(0,"인터넷 연결을 확인해주세요.")}catch(_:Exception){EmotionServerResult.Error(-1,"응답을 처리하지 못했습니다.")}
 private fun msg(raw:String?,fallback:String)=runCatching{JSONObject(raw.orEmpty()).optString("message")}.getOrNull().takeUnless{it.isNullOrBlank()}?:fallback
}
