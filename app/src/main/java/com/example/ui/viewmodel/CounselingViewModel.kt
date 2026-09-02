package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.CounselingRepository
import com.example.data.repository.EmotionServerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class CounselingStage{START,TODAY,STORIES,CHAT,HISTORY}
data class CounselingUiState(
    val loading:Boolean=false,val sending:Boolean=false,val stage:CounselingStage=CounselingStage.START,
    val sessions:List<CounselingSessionDto> = emptyList(),val messages:List<CounselingMessageDto> = emptyList(),
    val today:List<Pair<String,Int>> = emptyList(),val stories:List<StoryDto> = emptyList(),val selectedStory:StoryDto?=null,
    val sessionId:Long?=null,val includeEmotion:Boolean=false,val input:String="",val error:String?=null,val aiError:String?=null,val requiresLogin:Boolean=false,
    val deletingSessionId:Long?=null,val notice:String?=null
)

class CounselingViewModel(application:Application):AndroidViewModel(application){
    private val repository=CounselingRepository(RetrofitClient.apiService,TokenManager(application))
    private val _state=MutableStateFlow(CounselingUiState())
    val state=_state.asStateFlow()
    private var pending:CounselingMessageRequest?=null
    private var localId=-1L

    fun load(){viewModelScope.launch{_state.value=_state.value.copy(loading=true,error=null);when(val result=repository.sessions()){is EmotionServerResult.Success->_state.value=_state.value.copy(loading=false,sessions=result.value.sessions);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(loading=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(loading=false,error=result.message)}}}
    fun setInput(value:String){if(value.length<=5000)_state.value=_state.value.copy(input=value)}
    fun freeTalk(){_state.value=_state.value.copy(stage=CounselingStage.CHAT,sessionId=null,messages=listOf(greeting("안녕하세요. 오늘 어떤 이야기를 나누고 싶으세요?")),includeEmotion=false,selectedStory=null,aiError=null)}
    fun chooseToday(){viewModelScope.launch{_state.value=_state.value.copy(loading=true,error=null);val date=LocalDate.now(ZoneId.of("Asia/Seoul")).toString();val todayResult=repository.today(date);val masterResult=repository.emotions();if(todayResult is EmotionServerResult.Unauthorized||masterResult is EmotionServerResult.Unauthorized){_state.value=_state.value.copy(loading=false,requiresLogin=true);return@launch};if(todayResult is EmotionServerResult.Error){_state.value=_state.value.copy(loading=false,error=todayResult.message);return@launch};if(masterResult is EmotionServerResult.Error){_state.value=_state.value.copy(loading=false,error=masterResult.message);return@launch};val records=(todayResult as EmotionServerResult.Success).value.records;val names=(masterResult as EmotionServerResult.Success).value.emotions.associate{it.emotionId to it.emotionName};_state.value=_state.value.copy(loading=false,stage=CounselingStage.TODAY,today=records.filter{it.todayCount>0}.map{(names[it.emotionId]?:"감정") to it.todayCount})}}
    fun startToday(){_state.value=_state.value.copy(stage=CounselingStage.CHAT,sessionId=null,messages=listOf(greeting("오늘 기록한 감정을 참고해서 이야기해볼까요? 어떤 감정부터 나누고 싶으세요?")),includeEmotion=true,selectedStory=null,aiError=null)}
    fun chooseStory(){viewModelScope.launch{_state.value=_state.value.copy(loading=true,error=null);when(val result=repository.stories()){is EmotionServerResult.Success->_state.value=_state.value.copy(loading=false,stage=CounselingStage.STORIES,stories=result.value.stories);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(loading=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(loading=false,error=result.message)}}}
    fun selectStory(story:StoryDto){_state.value=_state.value.copy(selectedStory=story)}
    fun startStory(){val story=_state.value.selectedStory?:return;_state.value=_state.value.copy(stage=CounselingStage.CHAT,sessionId=null,messages=listOf(greeting("‘${story.title}’ 사연을 참고해서 이야기해볼게요. 지금 가장 먼저 나누고 싶은 부분은 무엇인가요?")),includeEmotion=false,aiError=null)}
    fun openSession(id:Long){viewModelScope.launch{_state.value=_state.value.copy(loading=true,error=null);when(val result=repository.detail(id)){is EmotionServerResult.Success->_state.value=_state.value.copy(loading=false,stage=CounselingStage.CHAT,sessionId=id,messages=result.value.messages,includeEmotion=false,selectedStory=null,aiError=null);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(loading=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(loading=false,error=result.message)}}}
    fun newCounseling(){pending=null;_state.value=_state.value.copy(stage=CounselingStage.START,sessionId=null,messages=emptyList(),input="",includeEmotion=false,selectedStory=null,aiError=null)}
    fun navigateBack():Boolean{
        if(_state.value.stage==CounselingStage.START)return false
        newCounseling()
        return true
    }
    fun deleteSession(id:Long){if(_state.value.deletingSessionId!=null)return;viewModelScope.launch{_state.value=_state.value.copy(deletingSessionId=id,error=null);when(val result=repository.deleteSession(id)){is EmotionServerResult.Success->{val deletingCurrent=_state.value.sessionId==id;_state.value=_state.value.copy(deletingSessionId=null,sessions=_state.value.sessions.filterNot{it.sessionId==id},stage=if(deletingCurrent)CounselingStage.START else _state.value.stage,sessionId=if(deletingCurrent)null else _state.value.sessionId,messages=if(deletingCurrent)emptyList() else _state.value.messages,notice="상담 기록을 삭제했습니다.")};EmotionServerResult.Unauthorized->_state.value=_state.value.copy(deletingSessionId=null,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(deletingSessionId=null,error="상담 기록을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.")}}}
    fun consumeNotice(){_state.value=_state.value.copy(notice=null)}
    fun send(){val text=_state.value.input.trim();if(text.isEmpty()||_state.value.sending)return;val request=CounselingMessageRequest(sessionId=_state.value.sessionId,message=text,clientMessageId=UUID.randomUUID().toString(),includeEmotionContext=_state.value.includeEmotion,storyId=_state.value.selectedStory?.storyId);pending=request;_state.value=_state.value.copy(input="",messages=_state.value.messages+CounselingMessageDto(localId--,"USER",text,""));execute(request)}
    fun retry(){pending?.let(::execute)}
    fun finish(){if(_state.value.sending)return;setInput("지금까지 나눈 이야기를 진단이나 평가 없이 핵심 내용과 작은 실천 한 가지로 짧게 정리해줘.");send()}
    private fun execute(request:CounselingMessageRequest){viewModelScope.launch{_state.value=_state.value.copy(sending=true,aiError=null);when(val result=repository.send(request)){is EmotionServerResult.Success->{val id=result.value.sessionId?:request.sessionId;if(id==null){_state.value=_state.value.copy(sending=false,aiError="상담 응답을 확인하지 못했습니다.");return@launch};pending=request.copy(sessionId=id);when(val detail=repository.detail(id)){is EmotionServerResult.Success->{val hasAssistant=detail.value.messages.lastOrNull()?.role=="ASSISTANT";if(hasAssistant)pending=null;_state.value=_state.value.copy(sending=false,sessionId=id,messages=detail.value.messages,aiError=if(hasAssistant)null else result.value.message?:"답변을 불러오지 못했습니다.")};EmotionServerResult.Unauthorized->_state.value=_state.value.copy(sending=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(sending=false,sessionId=id,aiError=detail.message)}};EmotionServerResult.Unauthorized->_state.value=_state.value.copy(sending=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(sending=false,aiError=result.message)}}}
    private fun greeting(text:String)=CounselingMessageDto(0,"ASSISTANT",text,"")
}
