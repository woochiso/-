package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.VocalWavPreparer
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.VocalAnalysisResponse
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.VocalTrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class VocalTrainingUiState(val analyzing:Boolean=false,val result:VocalAnalysisResponse?=null,val speechLoading:Boolean=false,val speechBytes:ByteArray?=null,val error:String?=null,val speechError:String?=null,val requiresLogin:Boolean=false)

class VocalTrainingViewModel(application: Application):AndroidViewModel(application){
    private val repository=VocalTrainingRepository(RetrofitClient.apiService,TokenManager(application))
    private val _state=MutableStateFlow(VocalTrainingUiState()); val state=_state.asStateFlow()
    private var pending: Pending?=null
    data class Pending(val file:File,val mime:String,val duration:Double,val mode:String,val mrFile:File?)
    fun analyze(file:File,mime:String,duration:Double,mode:String,mrFile:File?){
        if(_state.value.analyzing)return
        if (BuildConfig.DEBUG) {
            Log.d("VOCAL_FLOW", "analyzeVocal entered")
            Log.d("VOCAL_FLOW", "source=${if(file.name.startsWith("vocal-upload")) "picked" else "recorded"} filename=${file.name} extension=${file.extension} exists=${file.exists()} size=${file.length()} mime=$mime")
        }
        pending=Pending(file,mime,duration,mode,mrFile)
        viewModelScope.launch{
            _state.value=_state.value.copy(analyzing=true,error=null,speechBytes=null,speechError=null)
            val prepared = try {
                withContext(Dispatchers.IO) {
                    var vocal: VocalWavPreparer.Prepared? = null
                    var mr: VocalWavPreparer.Prepared? = null
                    var mix: File? = null
                    try {
                        val preparedVocal = VocalWavPreparer.prepare(
                            file,
                            getApplication<Application>().cacheDir,
                            maxDurationSeconds = duration + 0.5
                        )
                        vocal = preparedVocal
                        mr = if (mode == "MR" && mrFile != null) {
                            VocalWavPreparer.prepare(
                                mrFile,
                                getApplication<Application>().cacheDir,
                                "mr",
                                duration + 0.5
                            )
                        } else null
                        mix = mr?.let { VocalWavPreparer.createMix(preparedVocal, it, getApplication<Application>().cacheDir) }
                        PreparedRequest(preparedVocal.file, mr?.file, mix)
                    } catch (error: Throwable) {
                        vocal?.file?.delete()
                        mr?.file?.delete()
                        mix?.delete()
                        throw error
                    }
                }
            } catch (error: VocalWavPreparer.PreparationException) {
                val message = when (error.code) {
                    "AUDIO_DECODE_FAILED" -> "녹음파일을 분석용으로 준비하지 못했습니다."
                    "AUDIO_RESAMPLE_FAILED", "WAV_CREATE_FAILED" -> "분석용 음성파일을 만들지 못했습니다."
                    else -> "녹음파일을 분석용으로 준비하지 못했습니다."
                }
                _state.value = _state.value.copy(analyzing=false, error=message)
                return@launch
            }
            val result = try {
                repository.analyze(prepared.vocal, duration, mode, prepared.mix)
            } finally {
                prepared.vocal.delete()
                prepared.mr?.delete()
                prepared.mix?.delete()
            }
            when(val r=result){
                is EmotionServerResult.Success->{
                    if (BuildConfig.DEBUG) Log.d("VOCAL_FLOW", "analysis response received")
                    _state.value=_state.value.copy(analyzing=false,result=r.value,error=null)
                    r.value.ttsToken?.takeIf(String::isNotBlank)?.let(::speech)
                }
                EmotionServerResult.Unauthorized->_state.value=_state.value.copy(analyzing=false,requiresLogin=true)
                is EmotionServerResult.Error->_state.value=_state.value.copy(analyzing=false,error=r.message)
            }
        }
    }
    private data class PreparedRequest(val vocal:File,val mr:File?,val mix:File?)
    fun retry(){pending?.let{analyze(it.file,it.mime,it.duration,it.mode,it.mrFile)}}
    fun reset(){pending=null;_state.value=VocalTrainingUiState()}
    fun clearSpeech(){_state.value=_state.value.copy(speechBytes=null)}
    private fun speech(token:String)=viewModelScope.launch{_state.value=_state.value.copy(speechLoading=true);when(val r=repository.speech(token)){is EmotionServerResult.Success->_state.value=_state.value.copy(speechLoading=false,speechBytes=r.value);EmotionServerResult.Unauthorized->_state.value=_state.value.copy(speechLoading=false,requiresLogin=true);is EmotionServerResult.Error->_state.value=_state.value.copy(speechLoading=false,speechError=r.message)}}
}
