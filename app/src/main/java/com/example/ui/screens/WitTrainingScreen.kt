package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.components.AiTrainingHeader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.remote.dto.WitScoresDto
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import java.io.File

@Composable fun WitTrainingScreen(vm:WitTrainingViewModel,onAuthExpired:()->Unit){
    val s by vm.state.collectAsStateWithLifecycle();val context=LocalContext.current;val player=remember{MediaPlayer()};var playing by remember{mutableStateOf(false)};var audioFile by remember{mutableStateOf<File?>(null)}
    fun stop(){runCatching{if(player.isPlaying)player.stop()};playing=false}
    fun toggle(){val f=audioFile?:return;if(playing)stop()else runCatching{player.reset();player.setDataSource(f.absolutePath);player.setOnCompletionListener{playing=false};player.prepare();player.start();playing=true}}
    DisposableEffect(Unit){onDispose{stop();player.release();audioFile?.delete()}}
    LaunchedEffect(s.requiresLogin){if(s.requiresLogin)onAuthExpired()}
    LaunchedEffect(s.speechBytes){s.speechBytes?.let{stop();audioFile?.delete();audioFile=File(context.cacheDir,"wit-feedback-${System.nanoTime()}.mp3").apply{writeBytes(it)};toggle();vm.clearSpeech()}}
    var voiceInputActive by remember{mutableStateOf(false)}
    var recognitionRunning by remember{mutableStateOf(false)}
    var voiceError by remember{mutableStateOf<String?>(null)}
    val speechController=remember{ContinuousWitSpeech(context,vm::text,{active,running->voiceInputActive=active;recognitionRunning=running},{voiceError=it})}
    val microphonePermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted){stop();voiceError=null;speechController.start(s.inputText)}else voiceError="마이크 권한이 필요합니다."}
    fun speak(){
        if(voiceInputActive){vm.text(speechController.finish())}
        else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){stop();voiceError=null;speechController.start(s.inputText)}
        else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(speechController){onDispose{speechController.destroy()}}
    LaunchedEffect(s.stage,s.sending){if(s.stage!=WitStage.PRACTICE||s.sending){speechController.finish();speechController.resetTranscript()}}
    fun selectVoice(voice:String){stop();audioFile?.delete();audioFile=null;vm.voice(voice)}
    when(s.stage){WitStage.CHOOSE->WitChoose(s,vm);WitStage.PRACTICE->WitPractice(s,vm,::selectVoice,::speak,voiceInputActive,recognitionRunning,voiceError,playing,::toggle);WitStage.RESULT->WitResult(s,vm,playing,::toggle)}
}

@Composable private fun WitChoose(s:WitTrainingUiState,vm:WitTrainingViewModel)=LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    item{AiTrainingHeader("💡","AI 재치와 센스","다양한 상황에 답해보며\n재치와 센스 있는 표현을 연습해보세요.")}
    item{WitCard{Text("연습 방법",fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text("① 상황 분야를 선택해요\n② 상대방의 말에 답해요\n③ 재치·상황 센스·배려 피드백을 확인해요",color=FreshTextVariant)}}
    item{Text("상황 분야를 선택해주세요",fontWeight=FontWeight.Bold)}
    when{ s.loading->item{Box(Modifier.fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(color=AppActionButton)}};s.error!=null->item{WitError(s.error,vm::loadConfig)};else->items(s.config?.categories.orEmpty(),key={it.id}){o->Column{WitCard(Modifier.clickable(enabled=!s.sending&&o.id!="FREE",onClick={vm.selectCategory(o.id)})){Text(o.title,fontWeight=FontWeight.Bold);Text(o.description,style=MaterialTheme.typography.bodySmall,color=FreshTextVariant)};if(o.id=="FREE"){OutlinedTextField(s.customSituation,vm::custom,Modifier.fillMaxWidth().padding(top=6.dp),label={Text("직접 연습할 상황")},minLines=2,maxLines=4,supportingText={Text("${s.customSituation.length}/300")});Button({vm.selectCategory("FREE")},Modifier.fillMaxWidth(),enabled=s.customSituation.isNotBlank()&&!s.sending,colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){Text("자유상황 시작")}}}}
    }
}

@Composable private fun WitPractice(s:WitTrainingUiState,vm:WitTrainingViewModel,selectVoice:(String)->Unit,voice:()->Unit,voiceInputActive:Boolean,recognitionRunning:Boolean,voiceError:String?,playing:Boolean,audio:()->Unit)=LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
    item{AiTrainingHeader("💡","AI 재치와 센스","다양한 상황에 답해보며\n재치와 센스 있는 표현을 연습해보세요.")}
    item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){s.config?.voices.orEmpty().forEach{o->FilterChip(selected=s.voice==o.id,onClick={selectVoice(o.id)},label={Text(o.title)})}}}
    item{Card(colors=CardDefaults.cardColors(containerColor=FreshLightIndigoContainer),border=BorderStroke(1.dp,FreshOutline),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text("💬 오늘의 상황",fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text(s.context,style=MaterialTheme.typography.bodySmall,color=FreshTextVariant);Text(s.scenario);Surface(color=Color.White.copy(alpha=.8f),shape=RoundedCornerShape(12.dp)){Text(s.characterLine,Modifier.padding(13.dp),fontWeight=FontWeight.SemiBold)};TextButton(audio){Icon(if(playing)Icons.Default.Stop else Icons.Default.PlayArrow,null);Text(if(playing)" 멈춤" else " 다시 듣기")}}}}
    item{Text("어떻게 답하시겠어요?",fontWeight=FontWeight.Bold);OutlinedTextField(s.inputText,vm::text,Modifier.fillMaxWidth(),enabled=!voiceInputActive,minLines=4,maxLines=7,placeholder={Text(if(voiceInputActive)"인식된 답변이 여기에 누적됩니다." else "이 상황에서 하고 싶은 말을 적어주세요.")},supportingText={Text("${s.inputText.length}/2000")});OutlinedButton(voice,Modifier.fillMaxWidth(),enabled=!s.sending){Icon(if(voiceInputActive)Icons.Default.Stop else Icons.Default.Mic,null);Text(if(voiceInputActive)" ■ 말하기 완료" else " 🎙 말하기")};if(voiceInputActive){Text(if(recognitionRunning)"🎙 듣고 있습니다..." else "🎙 다음 말을 기다리고 있습니다...",color=FreshDeepIndigo,style=MaterialTheme.typography.bodySmall);Text("잠시 말을 멈춰도 계속 듣습니다.\n모두 말씀하신 후 '말하기 완료'를 눌러주세요.",color=FreshTextMuted,style=MaterialTheme.typography.bodySmall)};voiceError?.let{Text(it,color=FreshTextVariant,style=MaterialTheme.typography.bodySmall)}}
    s.error?.let{item{WitError(it,vm::retry)}};s.speechError?.let{item{Text(it,color=FreshTextMuted,style=MaterialTheme.typography.bodySmall)}}
    item{Button(vm::submit,Modifier.fillMaxWidth().height(52.dp),enabled=s.inputText.isNotBlank()&&!s.sending&&!voiceInputActive,colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){if(s.sending){CircularProgressIndicator(Modifier.size(20.dp),color=Color.White);Text(" AI가 답변을 살펴보고 있어요...")}else Text("✨ 답변하기")}}
}

@Composable private fun WitResult(s:WitTrainingUiState,vm:WitTrainingViewModel,playing:Boolean,audio:()->Unit){val r=s.result;LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
    item{AiTrainingHeader("💡","AI 재치와 센스","다양한 상황에 답해보며\n재치와 센스 있는 표현을 연습해보세요.")}
    r?.transcript?.let{item{WitCard{Text("내 답변",fontWeight=FontWeight.Bold);Text(it)}}}
    if(r?.risk==true)item{WitCard{Text("안전 안내",fontWeight=FontWeight.Bold);Text(r.safetyMessage.orEmpty())}}
    else if(r!=null){item{WitScore(r.overallScore?:0,r.scores)};item{WitCard{Feedback("💬 상대방의 반응",r.characterResponse);Feedback("👍 좋았던 점",r.strength);Feedback("💡 조금 더 센스 있게",r.betterPhrase);Feedback("💬 또 다른 표현",r.alternativePhrase);TextButton(audio){Icon(if(playing)Icons.Default.Stop else Icons.Default.PlayArrow,null);Text(if(playing)" 피드백 멈춤" else " 피드백 듣기")}}}}
    s.speechError?.let{item{Text(it,color=FreshTextMuted)}};item{Button(vm::next,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){Text("✨ 다음 상황")};OutlinedButton(vm::chooseOther,Modifier.fillMaxWidth()){Text("분야 바꾸기")}}
}}

@Composable private fun WitScore(total:Int,scores:WitScoresDto?)=WitCard{Text("💡 재치/센스 점수",fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text("$total / 100",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=AppActionButton);listOf("재치" to (scores?.wit?:0),"상황 센스" to (scores?.situation?:0),"자연스러움" to (scores?.naturalness?:0),"상대방 배려" to (scores?.consideration?:0),"순발력" to (scores?.quickness?:0)).forEach{(n,v)->Text("$n  $v",style=MaterialTheme.typography.bodyMedium)};Text("AI 대화 연습용 참고 점수이며 성격이나 사회성을 판단하지 않습니다.",style=MaterialTheme.typography.bodySmall,color=FreshTextMuted)}
@Composable private fun Feedback(title:String,value:String?){if(!value.isNullOrBlank()){Text(title,fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text(value,color=FreshTextVariant);Spacer(Modifier.height(7.dp))}}
@Composable private fun WitCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit)=Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,FreshOutline),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp),content=content)}
@Composable private fun WitError(message:String?,retry:()->Unit)=WitCard{Text(message.orEmpty(),color=FreshTextVariant);TextButton(retry){Text("다시 시도")}}

private class ContinuousWitSpeech(
    context:Context,
    private val onText:(String)->Unit,
    private val onState:(Boolean,Boolean)->Unit,
    private val onError:(String?)->Unit
):RecognitionListener{
    private val recognizer=SpeechRecognizer.createSpeechRecognizer(context)
    private val handler=Handler(Looper.getMainLooper())
    private var voiceInputActive=false
    private var recognitionRunning=false
    private var destroyed=false
    private var committedTranscript=""
    private var interimTranscript=""
    private var lastFinalSegment=""
    private var restartScheduled=false
    private var userStopping=false
    private var sessionSequence=0
    private var busyRetryCount=0
    private var clientRetryCount=0
    private var emptyRetryCount=0
    private val restart=Runnable{
        restartScheduled=false
        if(voiceInputActive&&!recognitionRunning&&!destroyed&&!userStopping)listen()
        else speechLog("restartCancelled")
    }

    init{recognizer.setRecognitionListener(this)}

    fun start(initialText:String){
        if(destroyed)return
        committedTranscript=initialText.trim()
        interimTranscript=""
        lastFinalSegment=""
        busyRetryCount=0
        clientRetryCount=0
        emptyRetryCount=0
        userStopping=false
        voiceInputActive=true
        onError(null)
        onState(true,false)
        listen()
    }

    fun finish():String{
        speechLog("finishByUser")
        voiceInputActive=false
        userStopping=true
        cancelRestart()
        val finalText=combined(interimTranscript).trim()
        if(recognitionRunning)runCatching{recognizer.stopListening()}
        recognitionRunning=false
        interimTranscript=""
        committedTranscript=finalText
        onText(finalText)
        onState(false,false)
        return finalText
    }

    fun resetTranscript(){committedTranscript="";interimTranscript="";lastFinalSegment=""}

    fun destroy(){
        destroyed=true
        voiceInputActive=false
        userStopping=true
        restartScheduled=false
        handler.removeCallbacksAndMessages(null)
        runCatching{recognizer.cancel()}
        recognizer.destroy()
    }

    private fun listen(){
        if(!voiceInputActive||recognitionRunning||destroyed||userStopping){speechLog("startBlocked");return}
        sessionSequence++
        recognitionRunning=true
        interimTranscript=""
        speechLog("startListening")
        onState(true,true)
        runCatching{
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                putExtra(RecognizerIntent.EXTRA_PROMPT,"이 상황에 답하고 싶은 말을 해주세요")
            })
        }.onFailure{recognitionRunning=false;onState(voiceInputActive,false);onError("음성 인식을 시작하지 못했습니다.");speechLog("startFailed")}
    }

    private fun combined(segment:String)=listOf(committedTranscript,segment.trim()).filter{it.isNotBlank()}.joinToString(" ")
    private fun scheduleRestart(delayMillis:Long=650L){
        recognitionRunning=false
        interimTranscript=""
        onState(voiceInputActive,false)
        if(!voiceInputActive||destroyed||userStopping){cancelRestart();return}
        if(restartScheduled){speechLog("restartAlreadyScheduled");return}
        restartScheduled=true
        speechLog("scheduleRestart delay=$delayMillis")
        handler.postDelayed(restart,delayMillis)
    }
    private fun cancelRestart(){
        if(restartScheduled)speechLog("restartCancelled")
        handler.removeCallbacks(restart)
        restartScheduled=false
    }
    private fun commit(segment:String){val value=segment.trim();if(value.isNotBlank()&&value!=lastFinalSegment){committedTranscript=combined(value);lastFinalSegment=value;onText(committedTranscript)};interimTranscript=""}
    private fun speechLog(event:String){if(BuildConfig.DEBUG)Log.d("WIT_SPEECH","$event session=$sessionSequence active=$voiceInputActive running=$recognitionRunning restart=$restartScheduled")}

    override fun onReadyForSpeech(params:Bundle?){speechLog("onReadyForSpeech");recognitionRunning=true;onState(voiceInputActive,true)}
    override fun onBeginningOfSpeech(){speechLog("onBeginningOfSpeech");emptyRetryCount=0}
    override fun onRmsChanged(rmsdB:Float)=Unit
    override fun onBufferReceived(buffer:ByteArray?)=Unit
    override fun onEndOfSpeech(){speechLog("onEndOfSpeech");onState(voiceInputActive,recognitionRunning)}
    override fun onResults(results:Bundle?){
        speechLog("onResults")
        if(!voiceInputActive||userStopping)return
        val result=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        commit(result)
        busyRetryCount=0
        clientRetryCount=0
        if(result.isBlank())emptyRetryCount++ else emptyRetryCount=0
        if(emptyRetryCount>1){voiceInputActive=false;recognitionRunning=false;onState(false,false);onError("음성이 들리지 않았습니다. 다시 말하기를 눌러주세요.");cancelRestart()}
        else scheduleRestart()
    }
    override fun onPartialResults(partialResults:Bundle?){if(!voiceInputActive)return;interimTranscript=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim();if(interimTranscript.isNotBlank())onText(combined(interimTranscript))}
    override fun onEvent(eventType:Int,params:Bundle?)=Unit
    override fun onError(error:Int){
        speechLog("onError code=$error")
        recognitionRunning=false
        if(!voiceInputActive||userStopping){cancelRestart();onState(false,false);return}
        when(error){
            SpeechRecognizer.ERROR_NO_MATCH,SpeechRecognizer.ERROR_SPEECH_TIMEOUT->{
                val pending=interimTranscript
                if(pending.isNotBlank()){commit(pending);emptyRetryCount=0}else emptyRetryCount++
                if(emptyRetryCount<=1)scheduleRestart(750L)
                else{voiceInputActive=false;onState(false,false);onError("음성이 들리지 않았습니다. 다시 말하기를 눌러주세요.");cancelRestart()}
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY->{
                runCatching{recognizer.cancel()}
                busyRetryCount++
                if(busyRetryCount<=2)scheduleRestart(1500L)
                else{voiceInputActive=false;onState(false,false);onError("음성 인식을 다시 시작하지 못했습니다. 잠시 후 다시 시도해주세요.");cancelRestart()}
            }
            SpeechRecognizer.ERROR_CLIENT->{
                clientRetryCount++
                if(clientRetryCount<=1)scheduleRestart(1000L)
                else{voiceInputActive=false;onState(false,false);onError("음성 인식을 계속하지 못했습니다.");cancelRestart()}
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->{voiceInputActive=false;onState(false,false);onError("마이크 권한이 필요합니다.")}
            SpeechRecognizer.ERROR_AUDIO->{voiceInputActive=false;onState(false,false);onError("마이크를 사용할 수 없습니다.")}
            else->{voiceInputActive=false;onState(false,false);onError("음성 인식을 계속하지 못했습니다.")}
        }
    }
}
