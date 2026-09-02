package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException

class CounselingRepository(private val api:ApiService,private val tokens:TokenManager){
    suspend fun sessions()=call{api.getCounseling(it)}
    suspend fun detail(sessionId:Long)=call{api.getCounseling(it,sessionId)}
    suspend fun send(request:CounselingMessageRequest)=call{api.sendCounselingMessage(it,request)}
    suspend fun deleteSession(sessionId:Long)=call{api.deleteCounselingSession(it,CounselingDeleteRequest(sessionId=sessionId))}
    suspend fun today(date:String)=call{api.getTodayEmotions(it,date)}
    suspend fun emotions()=call{api.getEmotionMaster(it)}
    suspend fun stories()=call{api.getStories(it,1,100)}
    private suspend fun<T>call(block:suspend(String)->retrofit2.Response<T>):EmotionServerResult<T>{
        val token=tokens.accessToken()?:return EmotionServerResult.Unauthorized
        return try{val response=block("Bearer $token");val body=response.body();when{response.isSuccessful&&body!=null->EmotionServerResult.Success(body);response.code()==401->EmotionServerResult.Unauthorized;else->{val message=runCatching{JSONObject(response.errorBody()?.string().orEmpty()).optString("message")}.getOrNull().takeUnless{it.isNullOrBlank()}?:if(response.code() in 500..599)"AI 상담 답변을 불러오지 못했습니다." else "상담 요청을 처리하지 못했습니다.";EmotionServerResult.Error(response.code(),message)}}}catch(_:SocketTimeoutException){EmotionServerResult.Error(0,"응답 시간이 길어지고 있습니다. 다시 시도해주세요.")}catch(_:JsonDataException){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}catch(_:JsonEncodingException){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}catch(_:IOException){EmotionServerResult.Error(0,"인터넷 연결을 확인해주세요.")}catch(_:Exception){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}
    }
}
