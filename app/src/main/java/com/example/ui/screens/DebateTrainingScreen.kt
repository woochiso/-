@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.components.AiTrainingHeader
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import java.io.File
@Composable fun DebateTrainingScreen(vm:DebateTrainingViewModel,onAuthExpired:()->Unit){val s by vm.state.collectAsStateWithLifecycle();val ctx=androidx.compose.ui.platform.LocalContext.current;val p=remember{MediaPlayer()};val cache=remember{mutableMapOf<String,File>()};var playing by remember{mutableStateOf(false)};var active by remember{mutableStateOf<String?>(null)};var voiceMode by remember{mutableStateOf(false)};var recognizerListening by remember{mutableStateOf(false)};var voiceError by remember{mutableStateOf<String?>(null)}
 fun stop(){runCatching{if(p.isPlaying)p.stop()};playing=false};fun play(f:File,t:String){p.reset();p.setDataSource(f.path);p.setOnPreparedListener{it.start();playing=true;active=t};p.setOnCompletionListener{playing=false};p.prepareAsync()};DisposableEffect(Unit){onDispose{stop();p.release();cache.values.forEach{it.delete()}}};LaunchedEffect(s.requiresLogin){if(s.requiresLogin)onAuthExpired()};LaunchedEffect(s.busy){if(s.busy)stop()};LaunchedEffect(s.speechBytes,s.speechToken){val b=s.speechBytes;val t=s.speechToken;if(b!=null&&t!=null){val f=File(ctx.cacheDir,"debate-$t.mp3");f.writeBytes(b);cache[t]=f;play(f,t);vm.clearSpeech()}}
 val controller=remember{ContinuousDebateSpeech(ctx,{vm.input(it)},{mode,listening->voiceMode=mode;recognizerListening=listening},{voiceError=it})};val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it){stop();controller.start(s.input)}else voiceError="마이크 권한이 필요합니다."};fun toggleVoice(){if(voiceMode)controller.finish()else if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){stop();voiceError=null;controller.start(s.input)}else permission.launch(Manifest.permission.RECORD_AUDIO)}
 DisposableEffect(Unit){onDispose{controller.destroy()}};LaunchedEffect(s.stage,s.busy){if(s.stage!=DebateStage.ROOM||s.busy)controller.finish()}
 when(s.stage){DebateStage.SETUP->DebateSetup(s,vm);DebateStage.ROOM->DebateRoom(s,vm,::toggleVoice,voiceMode,recognizerListening,voiceError,playing,{t,r->if(playing&&active==t)stop()else cache[t]?.let{play(it,t)}?:vm.speech(t,r)});DebateStage.RESULT->DebateResult(s,vm,playing,{t->if(playing)stop()else cache[t]?.let{play(it,t)}?:vm.speech(t,"moderator")})}}
@Composable private fun DebateSetup(s:DebateUiState,vm:DebateTrainingViewModel)=LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{AiTrainingHeader("⚖️","AI 토론연습","AI와 의견을 주고받으며\n논리적으로 말하는 연습을 해보세요.")};item{Card(colors=CardDefaults.cardColors(FreshSurfaceVariant),border=BorderStroke(1.dp,FreshOutline)){Column(Modifier.padding(15.dp)){Text("AI 토론연습 방법",fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text("💬 ① 토론 주제 선택");Text("🗣 ② 내 의견 이야기");Text("🔄 ③ AI 반론에 답하기");Text("💡 ④ 토론 피드백 확인")}}};if(s.loading)item{CircularProgressIndicator()}else if(s.error!=null)item{Err(s.error,vm::load)}else{item{Text("① 분야 선택",fontWeight=FontWeight.Bold);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){s.config?.categories.orEmpty().forEach{FilterChip(s.category==it.id,{vm.category(it.id)},{Text(it.title)})}}};val c=s.config?.categories?.firstOrNull{it.id==s.category};if(c!=null){if(c.id=="FREE")item{OutlinedTextField(s.customTopic,vm::custom,Modifier.fillMaxWidth(),label={Text("자유토론 주제")},supportingText={Text("4~120자")})}else items(c.topics){t->Card(Modifier.fillMaxWidth().clickable{vm.topic(t)},colors=CardDefaults.cardColors(if(s.topic?.id==t.id)FreshLightIndigoContainer else Color.White),border=BorderStroke(1.dp,if(s.topic?.id==t.id)AppActionButton else FreshOutline)){Text((if(t.recommended)"추천 · " else "")+t.topic,Modifier.padding(13.dp))}}};s.topic?.let{t->item{Text("③ 내 입장 선택",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(s.side=="a",{vm.side("a")},{Text(t.a)},Modifier.weight(1f));FilterChip(s.side=="b",{vm.side("b")},{Text(t.b)},Modifier.weight(1f))}}};s.error?.let{item{Err(it,vm::retry)}};item{Button(vm::start,Modifier.fillMaxWidth(),enabled=s.topic!=null&&s.side.isNotBlank()&&!s.busy,colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){Text(if(s.busy)"토론을 준비하고 있어요..." else "⚖️ 토론 시작")}}}}
@Composable private fun DebateRoom(s:DebateUiState,vm:DebateTrainingViewModel,toggleVoice:()->Unit,voiceMode:Boolean,listening:Boolean,voiceError:String?,playing:Boolean,audio:(String,String)->Unit){val list=rememberLazyListState();LaunchedEffect(s.messages.size){if(!list.isScrollInProgress&&s.messages.isNotEmpty())list.animateScrollToItem(s.messages.lastIndex)};Column(Modifier.fillMaxSize().imePadding().padding(horizontal=16.dp)){Text(s.plan?.topic.orEmpty(),fontWeight=FontWeight.Bold);Text("내 입장: ${s.plan?.userSide} · AI: ${s.plan?.debaterSide} · 내 발언 ${s.turns}/${s.config?.maxTurns?:12}",style=MaterialTheme.typography.bodySmall,color=FreshTextVariant);LazyColumn(Modifier.weight(1f),state=list,verticalArrangement=Arrangement.spacedBy(9.dp)){items(s.messages){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.role=="user")Arrangement.End else Arrangement.Start){Card(Modifier.fillMaxWidth(.84f),colors=CardDefaults.cardColors(if(m.role=="user")FreshLightIndigoContainer else Color.White),border=BorderStroke(1.dp,FreshOutline)){Column(Modifier.padding(12.dp)){Text(if(m.role=="user")"👤 나" else if(m.role=="moderator")"🎙️ AI 진행자" else "🤖 AI 토론자",fontWeight=FontWeight.Bold);Text(m.text);m.ttsToken?.let{TextButton({audio(it,m.role)}){Icon(if(playing)Icons.Default.Stop else Icons.Default.PlayArrow,null);Text(if(playing)"음성 멈춤" else "의견 듣기")}}}}}}};s.error?.let{Err(it,vm::retry)};if(s.busy)Text("AI가 반론을 생각하고 있어요...",color=FreshDeepIndigo);if(voiceMode)Text(if(listening)"🎙 듣고 있습니다... 잠시 쉬어도 계속 이어집니다." else "🎙 다음 말을 기다리고 있습니다...",color=FreshDeepIndigo,style=MaterialTheme.typography.bodySmall);voiceError?.let{Text(it,color=FreshTextVariant,style=MaterialTheme.typography.bodySmall)};OutlinedTextField(s.input,vm::input,Modifier.fillMaxWidth(),enabled=!voiceMode,placeholder={Text(if(voiceMode)"인식된 의견이 여기에 누적됩니다" else "내 주장과 근거를 입력하세요")});OutlinedButton(toggleVoice,Modifier.fillMaxWidth(),enabled=!s.busy){Icon(if(voiceMode)Icons.Default.Stop else Icons.Default.Mic,null);Text(if(voiceMode)" ■ 말하기 완료" else if(s.input.isBlank())" 🎙 말하기 시작" else " 🎙 다시 말하기")};Row{Button(vm::send,Modifier.weight(1f),enabled=s.input.isNotBlank()&&!s.busy&&!voiceMode&&s.turns<12){Icon(Icons.Default.Send,null);Text(" 보내기")};TextButton(vm::finish,enabled=s.turns>0&&!s.busy&&!voiceMode){Text("■ 토론 마무리")}}}}
@Composable private fun DebateResult(s:DebateUiState,vm:DebateTrainingViewModel,playing:Boolean,audio:(String)->Unit){val r=s.result;LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("✨ 토론 피드백",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};item{Card(colors=CardDefaults.cardColors(FreshLightIndigoContainer)){Column(Modifier.padding(16.dp)){Text(r?.moderatorSummary.orEmpty());Text("종합점수 ${r?.overallScore?:0} / 100",fontWeight=FontWeight.Bold);r?.moderatorTts?.let{TextButton({audio(it)}){Text(if(playing)"■ 음성 멈춤" else "▶ 피드백 듣기")}}}}};listOf("논리성" to r?.scores?.logic,"근거 제시" to r?.scores?.evidence,"상대 의견 대응" to r?.scores?.response,"주장 일관성" to r?.scores?.consistency,"표현의 명확성" to r?.scores?.clarity).forEach{(n,v)->item{Text("$n: ${v?:0}")}};item{Feedback("👍 잘한 점",r?.strength);Feedback("🌱 연습할 점",r?.practicePoint);Feedback("💡 다른 표현 예시",r?.alternativePhrase);Feedback("🗣 다음 토론 팁",r?.conversationTip);Button(vm::fresh,Modifier.fillMaxWidth()){Text("⚖️ 새로운 토론")}}}}
@Composable private fun Feedback(t:String,v:String?)=Column{Text(t,fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text(v.orEmpty(),color=FreshTextVariant)}
@Composable private fun Err(m:String,retry:()->Unit)=Card(colors=CardDefaults.cardColors(FreshSurfaceVariant)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(m,Modifier.weight(1f));TextButton(retry){Text("다시 시도")}}}

private class ContinuousDebateSpeech(
    context: android.content.Context,
    private val onText: (String) -> Unit,
    private val onState: (Boolean, Boolean) -> Unit,
    private val onErrorMessage: (String?) -> Unit
) : RecognitionListener {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val handler = Handler(Looper.getMainLooper())
    private var voiceMode = false
    private var listening = false
    private var committed = ""
    private var restartFailures = 0
    private val restart = Runnable { if (voiceMode && !listening) startSession() }
    init { recognizer.setRecognitionListener(this) }
    fun start(seed: String) { committed = seed.trim(); voiceMode = true; restartFailures = 0; onErrorMessage(null); onState(true, false); startSession() }
    fun finish() { voiceMode = false; handler.removeCallbacks(restart); if (listening) runCatching { recognizer.stopListening() }; listening = false; onState(false, false) }
    fun destroy() { voiceMode = false; handler.removeCallbacksAndMessages(null); runCatching { recognizer.cancel() }; recognizer.destroy() }
    private fun startSession() {
        if (!voiceMode || listening) return
        listening = true; onState(true, true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { recognizer.startListening(intent) }.onFailure { scheduleRestart(600) }
    }
    private fun combined(partial: String = "") = listOf(committed, partial.trim()).filter { it.isNotBlank() }.joinToString(" ")
    private fun scheduleRestart(delay: Long = 350) { listening = false; onState(voiceMode, false); handler.removeCallbacks(restart); if (voiceMode) handler.postDelayed(restart, delay) }
    override fun onReadyForSpeech(params: Bundle?) { restartFailures = 0; listening = true; onState(voiceMode, true) }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { listening = false; onState(voiceMode, false) }
    override fun onPartialResults(partialResults: Bundle?) { val p=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();if(p.isNotBlank())onText(combined(p)) }
    override fun onResults(results: Bundle?) { val final=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim();if(final.isNotBlank()){committed=combined(final);onText(committed)};scheduleRestart() }
    override fun onError(error: Int) {
        listening = false
        if (!voiceMode) { onState(false, false); return }
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> scheduleRestart()
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> { if (++restartFailures <= 3) scheduleRestart(700) else stopWithError("음성 인식을 다시 시작하지 못했습니다. 잠시 후 다시 시도해주세요.") }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> stopWithError("마이크 권한이 필요합니다.")
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> stopWithError("음성 인식을 위한 네트워크 연결을 확인해주세요.")
            SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> stopWithError("음성 인식 서버에 연결하지 못했습니다.")
            else -> stopWithError("음성 인식을 계속하지 못했습니다. 다시 말하기를 눌러주세요.")
        }
    }
    private fun stopWithError(message:String){voiceMode=false;handler.removeCallbacks(restart);onState(false,false);onErrorMessage(message)}
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
