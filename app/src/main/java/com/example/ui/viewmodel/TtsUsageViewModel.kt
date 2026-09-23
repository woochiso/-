package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.TtsUsageResponse
import com.example.data.repository.TtsUsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TtsUsageUiState(val loading:Boolean=false,val usage:TtsUsageResponse?=null,val error:String?=null)

class TtsUsageViewModel(app:Application):AndroidViewModel(app){
    private val repository=TtsUsageRepository(RetrofitClient.apiService,TokenManager(app))
    private val _state=MutableStateFlow(TtsUsageUiState())
    val state=_state.asStateFlow()
    fun load(){if(_state.value.loading)return;viewModelScope.launch{_state.value=TtsUsageUiState(loading=true);_state.value=runCatching{TtsUsageUiState(usage=repository.load())}.getOrElse{TtsUsageUiState(error="AI 음성 사용량을 불러오지 못했습니다.")}}}
}
