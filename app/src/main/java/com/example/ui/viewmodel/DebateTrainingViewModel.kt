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
import kotlinx.coroutines.launch
import java.util.UUID
enum class DebateStage{SETUP,ROOM,RESULT}
data class DebateMessage(val role:String,val text:String,val ttsToken:String?=null)
data class DebateUiState(val loading:Boolean=true,val busy:Boolean=false,val config:DebateConfigResponse?=null,val stage:DebateStage=DebateStage.SETUP,val category:String="",val topic:DebateTopicDto?=null,val customTopic:String="",val side:String="",val practiceId:String?=null,val plan:DebatePlanDto?=null,val turns:Int=0,val messages:List<DebateMessage> = emptyList(),val input:String="",val result:DebateActionResponse?=null,val speechBytes:ByteArray?=null,val speechToken:String?=null,val speechRole:String="debater",val error:String?=null,val speechError:String?=null,val requiresLogin:Boolean=false)
class DebateTrainingViewModel(app:Application):AndroidViewModel(app){
 private val repo=DebateTrainingRepository(RetrofitClient.apiService,TokenManager(app));private val _s=MutableStateFlow(DebateUiState());val state=_s.asStateFlow();private var pending:DebateActionRequest?=null
 init{load()};fun load()=viewModelScope.launch{_s.value=_s.value.copy(loading=true,error=null);when(val r=repo.config()){is EmotionServerResult.Success->_s.value=_s.value.copy(loading=false,config=r.value);EmotionServerResult.Unauthorized->_s.value=_s.value.copy(loading=false,requiresLogin=true);is EmotionServerResult.Error->_s.value=_s.value.copy(loading=false,error=r.message)}}
 fun category(id:String){_s.value=_s.value.copy(category=id,topic=null,side="",customTopic="",error=null)}
 fun topic(v:DebateTopicDto){_s.value=_s.value.copy(topic=v,side="",error=null)}
 fun custom(v:String){if(v.length<=120)_s.value=_s.value.copy(customTopic=v,topic=DebateTopicDto("free",v,"찬성","반대"),error=null)}
 fun side(v:String){_s.value=_s.value.copy(side=v)};fun input(v:String){if(v.length<=2000)_s.value=_s.value.copy(input=v,error=null)}
 fun start(){val s=_s.value;val t=s.topic?:return;if(s.busy||s.side.isBlank())return;run(DebateActionRequest("start",UUID.randomUUID().toString(),s.category,t.id,s.customTopic.takeIf{s.category=="FREE"},s.side))}
 fun send(){val s=_s.value;val text=s.input.trim();if(s.busy||text.isBlank()||s.practiceId==null)return;run(DebateActionRequest("respond",UUID.randomUUID().toString(),practiceId=s.practiceId,turn=s.turns,text=text))}
 fun finish(){val s=_s.value;if(s.busy||s.turns<1||s.practiceId==null)return;run(DebateActionRequest("finish",UUID.randomUUID().toString(),practiceId=s.practiceId))}
 fun retry(){pending?.let(::run)};fun clearSpeech(){_s.value=_s.value.copy(speechBytes=null)}
 fun speech(token:String,role:String){viewModelScope.launch{_s.value=_s.value.copy(speechToken=token,speechRole=role,speechError=null);when(val r=repo.speech(token,role)){is EmotionServerResult.Success->_s.value=_s.value.copy(speechBytes=r.value);EmotionServerResult.Unauthorized->_s.value=_s.value.copy(requiresLogin=true);is EmotionServerResult.Error->_s.value=_s.value.copy(speechError=r.message)}}}
 fun fresh(){pending=null;_s.value=DebateUiState(loading=false,config=_s.value.config)}
 private fun run(q:DebateActionRequest){if(_s.value.busy)return;pending=q;viewModelScope.launch{_s.value=_s.value.copy(busy=true,error=null);when(val r=repo.action(q)){is EmotionServerResult.Success->apply(q.action,r.value);EmotionServerResult.Unauthorized->_s.value=_s.value.copy(busy=false,requiresLogin=true);is EmotionServerResult.Error->_s.value=_s.value.copy(busy=false,error=r.message)}}}
 private fun apply(a:String,r:DebateActionResponse){if(!r.success){_s.value=_s.value.copy(busy=false,error=r.message);return};when(a){"start"->{val m=DebateMessage("moderator",r.moderator.orEmpty(),r.moderatorTts);_s.value=_s.value.copy(busy=false,stage=DebateStage.ROOM,practiceId=r.practiceId,plan=r.plan,turns=0,messages=listOf(m));r.moderatorTts?.let{speech(it,"moderator")}};"respond"->{val add=buildList{add(DebateMessage("user",r.transcript.orEmpty()));if(!r.moderator.isNullOrBlank())add(DebateMessage("moderator",r.moderator,r.moderatorTts));add(DebateMessage("debater",r.debater.orEmpty(),r.debaterTts))};_s.value=_s.value.copy(busy=false,turns=r.turns,messages=_s.value.messages+add,input="");r.debaterTts?.let{speech(it,"debater")}};"finish"->{_s.value=_s.value.copy(busy=false,stage=DebateStage.RESULT,result=r);r.moderatorTts?.let{speech(it,"moderator")}}}}
}
