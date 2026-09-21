package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

enum class WitStage{CHOOSE,PRACTICE,RESULT}
data class WitTrainingUiState(val loading:Boolean=true,val sending:Boolean=false,val config:WitConfigResponse?=null,val stage:WitStage=WitStage.CHOOSE,val category:String="",val categoryTitle:String="",val customSituation:String="",val voice:String="DEFAULT",val practiceId:String?=null,val categoryDescription:String="",val context:String="",val scenario:String="",val characterLine:String="",val inputText:String="",val result:WitActionResponse?=null,val speechBytes:ByteArray?=null,val speechToken:String?=null,val error:String?=null,val speechError:String?=null,val requiresLogin:Boolean=false)
class WitTrainingViewModel(app:Application):AndroidViewModel(app){
    private val repo=WitTrainingRepository(RetrofitClient.apiService,TokenManager(app));private val _state=MutableStateFlow(WitTrainingUiState());val state=_state.asStateFlow();private var pending:WitActionRequest?=null;private var speechJob:Job?=null
    init{loadConfig()}
    fun loadConfig()=viewModelScope.launch{_state.value=_state.value.copy(loading=true,error=null);when(val r=repo.config()){is EmotionServerResult.Success->_state.value=_state.value.copy(loading=false,config=r.value,voice=r.value.defaultVoice);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(loading=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(loading=false,error=r.message)}}
    fun custom(v:String){if(v.length<=300)_state.value=_state.value.copy(customSituation=v)};fun voice(v:String){if(v==_state.value.voice)return;_state.value=_state.value.copy(voice=v,speechBytes=null,speechError=null);_state.value.speechToken?.takeIf(String::isNotBlank)?.let(::speech)};fun text(v:String){if(v.length<=2000)_state.value=_state.value.copy(inputText=v,error=null)}
    fun selectCategory(id:String){val s=_state.value;if(s.sending||(id=="FREE"&&s.customSituation.isBlank()))return;val title=s.config?.categories?.firstOrNull{it.id==id}?.title.orEmpty();execute(WitActionRequest("scenario",UUID.randomUUID().toString(),category=id,customSituation=s.customSituation.trim()),title)}
    fun submit(){val s=_state.value;if(s.sending||s.practiceId==null||s.inputText.isBlank())return;execute(WitActionRequest("reply",UUID.randomUUID().toString(),practiceId=s.practiceId,text=s.inputText.trim()))}
    fun retry(){pending?.let{execute(it,_state.value.categoryTitle)}};fun next(){if(_state.value.category.isNotBlank())selectCategory(_state.value.category)};fun chooseOther(){pending=null;_state.value=_state.value.copy(stage=WitStage.CHOOSE,category="",categoryTitle="",practiceId=null,inputText="",result=null,error=null,speechBytes=null)};fun clearSpeech(){_state.value=_state.value.copy(speechBytes=null)}
    private fun execute(q:WitActionRequest,title:String=_state.value.categoryTitle){if(_state.value.sending)return;pending=q;viewModelScope.launch{_state.value=_state.value.copy(sending=true,error=null,speechError=null);when(val r=repo.action(q)){is EmotionServerResult.Success->apply(q.action,r.value,title);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(sending=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(sending=false,error=r.message)}}}
    private fun apply(action:String,r:WitActionResponse,title:String){if(!r.success){_state.value=_state.value.copy(sending=false,error=r.message?:"요청을 처리하지 못했습니다.");return};if(action=="scenario")_state.value=_state.value.copy(sending=false,stage=WitStage.PRACTICE,category=r.category.orEmpty(),categoryTitle=r.categoryTitle?:title,practiceId=r.practiceId,categoryDescription=r.categoryDescription.orEmpty(),context=r.context.orEmpty(),scenario=r.scenario.orEmpty(),characterLine=r.characterLine.orEmpty(),inputText="",result=null)else _state.value=_state.value.copy(sending=false,stage=WitStage.RESULT,result=r);r.ttsToken?.takeIf(String::isNotBlank)?.let(::speech)}
    private fun speech(token:String){speechJob?.cancel();val requestedVoice=_state.value.voice;speechJob=viewModelScope.launch{_state.value=_state.value.copy(speechError=null,speechBytes=null,speechToken=token);when(val r=repo.speech(token,requestedVoice)){is EmotionServerResult.Success->if(_state.value.voice==requestedVoice&&_state.value.speechToken==token)_state.value=_state.value.copy(speechBytes=r.value);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(requiresLogin=true);is EmotionServerResult.Error->if(_state.value.voice==requestedVoice&&_state.value.speechToken==token)_state.value=_state.value.copy(speechError=r.message)}}}
}
