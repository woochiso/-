package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.RecoveryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class RecoveryStage{LIST,BEFORE,ACTIVITY,AFTER,COMPLETED}
data class RecoveryUiState(
    val isLoading:Boolean=false,val isSaving:Boolean=false,val overview:RecoveryOverviewResponse?=null,
    val selectedActivity:RecoveryActivityDto?=null,val stage:RecoveryStage=RecoveryStage.LIST,
    val scores:Map<String,Int> = emptyMap(),val memo:String="",val error:String?=null,val requiresLogin:Boolean=false
)

class RecoveryViewModel(application:Application):AndroidViewModel(application){
    private val repository=RecoveryRepository(RetrofitClient.apiService,TokenManager(application))
    private val _state=MutableStateFlow(RecoveryUiState())
    val state=_state.asStateFlow()
    private val _messages=MutableSharedFlow<String>()
    val messages=_messages.asSharedFlow()
    private var pendingStart:RecoverySessionRequest?=null

    fun load(){viewModelScope.launch{_state.value=_state.value.copy(isLoading=true,error=null);when(val result=repository.overview()){is EmotionServerResult.Success->_state.value=RecoveryUiState(overview=result.value);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(isLoading=false,requiresLogin=true,error="로그인이 만료되었습니다.");is EmotionServerResult.Error->_state.value=_state.value.copy(isLoading=false,error=result.message)}}}
    fun select(activity:RecoveryActivityDto){val categories=_state.value.overview?.categories.orEmpty();val existing=activity.todaySession;_state.value=_state.value.copy(selectedActivity=activity,stage=when(existing?.status){"STARTED"->RecoveryStage.ACTIVITY;"COMPLETED"->RecoveryStage.COMPLETED;else->RecoveryStage.BEFORE},scores=existing?.beforeEmotions?:categories.associate{it.categoryCode to 50},memo="",error=null)}
    fun setScore(code:String,value:Int){_state.value=_state.value.copy(scores=_state.value.scores+(code to value.coerceIn(0,100)))}
    fun setMemo(value:String){if(value.length<=1000)_state.value=_state.value.copy(memo=value)}
    fun showAfter(){val activity=_state.value.selectedActivity?:return;val categories=_state.value.overview?.categories.orEmpty();_state.value=_state.value.copy(stage=RecoveryStage.AFTER,scores=categories.associate{it.categoryCode to 50}.toMutableMap().apply{activity.todaySession?.afterEmotions?.let(::putAll)})}
    fun cancelEditor(){_state.value=_state.value.copy(stage=if(_state.value.selectedActivity?.todaySession?.status=="STARTED")RecoveryStage.ACTIVITY else RecoveryStage.LIST)}
    /** 팝업 자체만 닫습니다. 서버에 저장된 STARTED 세션은 overview에 그대로 남습니다. */
    fun dismissOverlay(){_state.value=_state.value.copy(selectedActivity=null,stage=RecoveryStage.LIST,scores=emptyMap(),memo="",error=null)}
    fun saveBefore(){val state=_state.value;val activity=state.selectedActivity?:return;if(state.isSaving)return;val current=pendingStart?.takeIf{it.activityId==activity.activityId&&it.emotions==state.scores}?:RecoverySessionRequest("start",activity.activityId,UUID.randomUUID().toString(),state.scores).also{pendingStart=it};save(current)}
    fun saveAfter(){val state=_state.value;val activity=state.selectedActivity?:return;val session=activity.todaySession?.recoverySessionId?:return;if(state.isSaving)return;save(RecoverySessionRequest("complete",activity.activityId,session,state.scores,state.memo))}
    private fun save(request:RecoverySessionRequest){viewModelScope.launch{
        _state.value=_state.value.copy(isSaving=true,error=null)
        when(val result=repository.save(request)){
            is EmotionServerResult.Success->{
                pendingStart=null
                _messages.emit(result.value.message?:"저장되었습니다.")
                when(val overviewResult=repository.overview()){
                    is EmotionServerResult.Success->{
                        val activity=overviewResult.value.activities.firstOrNull{it.activityId==request.activityId}
                        _state.value=RecoveryUiState(
                            overview=overviewResult.value,
                            selectedActivity=activity,
                            stage=if(request.action=="complete") RecoveryStage.COMPLETED else RecoveryStage.ACTIVITY,
                            scores=if(request.action=="complete") result.value.afterEmotions.orEmpty() else result.value.beforeEmotions,
                            memo=if(request.action=="complete") request.memo else ""
                        )
                    }
                    EmotionServerResult.Unauthorized->_state.value=_state.value.copy(isSaving=false,requiresLogin=true,error="로그인이 만료되었습니다.")
                    is EmotionServerResult.Error->_state.value=_state.value.copy(isSaving=false,error=overviewResult.message)
                }
            }
            EmotionServerResult.Unauthorized->_state.value=_state.value.copy(isSaving=false,requiresLogin=true,error="로그인이 만료되었습니다.")
            is EmotionServerResult.Error->{_state.value=_state.value.copy(isSaving=false,error=result.message);_messages.emit(result.message)}
        }
    }}
}
