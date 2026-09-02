package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StoryUiState(val loading:Boolean=false,val stories:List<StoryDto> = emptyList(),val editing:StoryDto?=null,val message:String?=null,val error:String?=null,val requiresLogin:Boolean=false,val mutationVersion:Int=0)
class StoryViewModel(app:Application):AndroidViewModel(app){
    private val repo=StoryRepository(RetrofitClient.apiService,TokenManager(app)); private val _state=MutableStateFlow(StoryUiState()); val state=_state.asStateFlow()
    fun load(){viewModelScope.launch{_state.update{it.copy(loading=true,error=null)};when(val r=repo.list()){is StoryResult.Success->_state.update{it.copy(loading=false,stories=r.value)};is StoryResult.Error->_state.update{it.copy(loading=false,error=r.message)};StoryResult.Unauthorized->_state.update{it.copy(loading=false,requiresLogin=true,error="로그인이 만료되었습니다.")}}}}
    fun detail(id:Long){viewModelScope.launch{when(val r=repo.detail(id)){is StoryResult.Success->_state.update{it.copy(editing=r.value)};is StoryResult.Error->_state.update{it.copy(error=r.message)};StoryResult.Unauthorized->_state.update{it.copy(requiresLogin=true)}}}}
    fun create(r:StoryWriteRequest)=mutate{repo.create(r)}
    fun update(r:StoryUpdateRequest)=mutate{repo.update(r)}
    fun delete(id:Long){viewModelScope.launch{when(val r=repo.delete(id)){is StoryResult.Success->{_state.update{it.copy(message=r.message)};load()};is StoryResult.Error->_state.update{it.copy(error=r.message)};StoryResult.Unauthorized->_state.update{it.copy(requiresLogin=true)}}}}
    private fun mutate(call:suspend()->StoryResult<StoryDto>){viewModelScope.launch{when(val r=call()){is StoryResult.Success->{_state.update{it.copy(editing=null,message=r.message,mutationVersion=it.mutationVersion+1)};load()};is StoryResult.Error->_state.update{it.copy(error=r.message)};StoryResult.Unauthorized->_state.update{it.copy(requiresLogin=true)}}}}
    fun clearEditing()=_state.update{it.copy(editing=null)}; fun consume()=_state.update{it.copy(message=null,error=null)}
}
