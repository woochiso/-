package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
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
    suspend fun speech(sessionId:Long,messageId:Long)=speech(CounselingTtsRequest(sessionId=sessionId,messageId=messageId))
    suspend fun greetingSpeech()=speech(CounselingTtsRequest(mode="intro",introKey="WELCOME"))
    private suspend fun speech(request:CounselingTtsRequest):EmotionServerResult<ByteArray>{
        val token=tokens.accessToken()?:return EmotionServerResult.Unauthorized
        return try{
            if(BuildConfig.DEBUG)Log.d("WOOCHISO_TTS","TTS_REQUEST mode=${request.mode} sessionId=${request.sessionId} messageId=${request.messageId} bearer=true")
            if(BuildConfig.DEBUG)Log.d("WOOCHISO_TTS","AUDIO_DOWNLOAD_START url=${BuildConfig.WOOCHISO_API_BASE_URL}counseling/tts.php")
            val response=api.getCounselingSpeech("Bearer $token",request)
            val body=response.body()
            val contentType=response.headers()["Content-Type"].orEmpty().lowercase()
            if(BuildConfig.DEBUG){
                Log.d("WOOCHISO_TTS","TTS_HTTP_STATUS status=${response.code()}")
                Log.d("WOOCHISO_TTS","TTS_RESPONSE_TYPE contentType=$contentType contentLength=${response.headers()["Content-Length"]}")
            }
            if(response.isSuccessful&&body!=null){
                val bytes=body.bytes()
                val isAudioType=contentType.startsWith("audio/")||contentType=="application/octet-stream"
                val isAudioBytes=isRecognizedAudio(bytes)
                if(isAudioType&&isAudioBytes){
                    if(BuildConfig.DEBUG)Log.d("WOOCHISO_TTS","AUDIO_DOWNLOAD_SUCCESS bytes=${bytes.size} format=${audioFormat(bytes)}")
                    EmotionServerResult.Success(bytes)
                }else{
                    val safeBody=bytes.toString(Charsets.UTF_8).take(500).replace(Regex("[\\r\\n]+")," ")
                    val serverMessage=runCatching{JSONObject(safeBody).optString("message")}.getOrNull().takeUnless{it.isNullOrBlank()}
                    if(BuildConfig.DEBUG)Log.e("WOOCHISO_TTS","TTS_SERVER_ERROR expectedAudio=true bytes=${bytes.size} body=$safeBody")
                    EmotionServerResult.Error(response.code(),serverMessage?:"AI 답변 음성을 불러오지 못했습니다.")
                }
            }
            else if(response.code()==401) EmotionServerResult.Unauthorized
            else{
                val safeBody=response.errorBody()?.string().orEmpty().take(200).replace(Regex("[\\r\\n]+")," ")
                if(BuildConfig.DEBUG)Log.e("WOOCHISO_TTS","http failure status=${response.code()} body=$safeBody")
                EmotionServerResult.Error(response.code(),"AI 답변 음성을 불러오지 못했습니다.")
            }
        }catch(error:SocketTimeoutException){if(BuildConfig.DEBUG)Log.e("WOOCHISO_TTS","timeout",error);EmotionServerResult.Error(0,"음성 생성 시간이 길어지고 있습니다.")}
        catch(error:IOException){if(BuildConfig.DEBUG)Log.e("WOOCHISO_TTS","audio download/network failure",error);EmotionServerResult.Error(0,"인터넷 연결을 확인해주세요.")}
        catch(error:Exception){if(BuildConfig.DEBUG)Log.e("WOOCHISO_TTS","unexpected failure",error);EmotionServerResult.Error(-1,"AI 답변 음성을 재생하지 못했습니다.")}
    }

    private fun isRecognizedAudio(bytes:ByteArray):Boolean=
        bytes.size>=12&&bytes[0]==0x52.toByte()&&bytes[1]==0x49.toByte()&&bytes[2]==0x46.toByte()&&bytes[3]==0x46.toByte()&&bytes[8]==0x57.toByte()&&bytes[9]==0x41.toByte()&&bytes[10]==0x56.toByte()&&bytes[11]==0x45.toByte() ||
        bytes.size>=3&&bytes[0]==0x49.toByte()&&bytes[1]==0x44.toByte()&&bytes[2]==0x33.toByte() ||
        bytes.size>=2&&(bytes[0].toInt() and 0xFF)==0xFF&&(bytes[1].toInt() and 0xE0)==0xE0

    private fun audioFormat(bytes:ByteArray):String=when{
        bytes.size>=12&&bytes[0]==0x52.toByte()->"wav"
        else->"mp3"
    }
    suspend fun today(date:String)=call{api.getTodayEmotions(it,date)}
    suspend fun emotions()=call{api.getEmotionMaster(it)}
    suspend fun stories()=call{api.getStories(it,1,100)}
    private suspend fun<T>call(block:suspend(String)->retrofit2.Response<T>):EmotionServerResult<T>{
        val token=tokens.accessToken()?:return EmotionServerResult.Unauthorized
        return try{val response=block("Bearer $token");val body=response.body();when{response.isSuccessful&&body!=null->EmotionServerResult.Success(body);response.code()==401->EmotionServerResult.Unauthorized;else->{val message=runCatching{JSONObject(response.errorBody()?.string().orEmpty()).optString("message")}.getOrNull().takeUnless{it.isNullOrBlank()}?:if(response.code() in 500..599)"AI 상담 답변을 불러오지 못했습니다." else "상담 요청을 처리하지 못했습니다.";EmotionServerResult.Error(response.code(),message)}}}catch(_:SocketTimeoutException){EmotionServerResult.Error(0,"응답 시간이 길어지고 있습니다. 다시 시도해주세요.")}catch(_:JsonDataException){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}catch(_:JsonEncodingException){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}catch(_:IOException){EmotionServerResult.Error(0,"인터넷 연결을 확인해주세요.")}catch(_:Exception){EmotionServerResult.Error(-1,"서버 응답을 처리하지 못했습니다.")}
    }
}
