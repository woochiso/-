package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.RecoverySessionRequest
import org.json.JSONObject
import java.io.IOException

class RecoveryRepository(private val api:ApiService,private val tokens:TokenManager){
    suspend fun overview():EmotionServerResult<com.example.data.remote.dto.RecoveryOverviewResponse> = call { api.getRecoveryOverview(it) }
    suspend fun save(request:RecoverySessionRequest):EmotionServerResult<com.example.data.remote.dto.RecoverySessionResponse> = call { api.saveRecoverySession(it,request) }
    private suspend fun <T> call(block:suspend(String)->retrofit2.Response<T>):EmotionServerResult<T>{
        val token=tokens.accessToken()?:return EmotionServerResult.Unauthorized
        return try{val response=block("Bearer $token");val body=response.body();when{response.isSuccessful&&body!=null->EmotionServerResult.Success(body);response.code()==401->EmotionServerResult.Unauthorized;else->{val message=runCatching{JSONObject(response.errorBody()?.string().orEmpty()).optString("message")}.getOrNull().takeUnless{it.isNullOrBlank()}?:if(response.code() in 500..599)"서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요." else "회복 활동 요청을 처리하지 못했습니다.";EmotionServerResult.Error(response.code(),message)}}}catch(_:IOException){EmotionServerResult.Error(0,"인터넷 연결을 확인해주세요.")}catch(_:Exception){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}
    }
}
