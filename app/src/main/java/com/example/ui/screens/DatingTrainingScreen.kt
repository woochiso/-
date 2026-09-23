package com.example.ui.screens

import android.app.Activity
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.data.remote.dto.DatingOptionDto
import com.example.ui.components.AiTrainingHeader
import com.example.ui.components.VoiceRecordingPanel
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun DatingTrainingScreen(viewModel: DatingTrainingViewModel, onAuthExpired: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember { MediaPlayer() }
    var playing by remember { mutableStateOf(false) }
    var playableToken by remember { mutableStateOf<String?>(null) }
    val audioCache = remember { mutableMapOf<String, File>() }
    var input by remember { mutableStateOf("") }
    var voiceBusy by remember { mutableStateOf(false) }

    fun stopAudio() { runCatching { if (player.isPlaying) player.stop() }; playing = false }
    fun play(file: File, token: String) {
        runCatching {
            player.reset(); player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { it.start(); playing = true; playableToken = token }
            player.setOnCompletionListener { playing = false }
            player.prepareAsync()
        }
    }
    DisposableEffect(Unit) { onDispose { stopAudio(); player.release(); audioCache.values.forEach { it.delete() } } }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }
    LaunchedEffect(state.speechBytes, state.speechToken) {
        val bytes = state.speechBytes; val token = state.speechToken
        if (bytes != null && token != null) {
            val file = File(context.cacheDir, "dating-$token.mp3"); file.writeBytes(bytes); audioCache[token] = file
            play(file, token); viewModel.clearSpeech()
        }
    }
    when (state.stage) {
        DatingStage.SETUP -> DatingSetup(state, viewModel)
        DatingStage.CHAT -> DatingChat(state, input, { input = it }, {
            val messageToSend = input
            stopAudio()
            viewModel.reply(messageToSend)
            input = ""
        }, onAuthExpired, voiceBusy, {voiceBusy=it}, {text->if(text.isNotBlank())viewModel.reply(text)}, {stopAudio()}, viewModel::finish, viewModel::retry, playing, {
            if (playing) stopAudio() else playableToken?.let { token -> audioCache[token]?.let { play(it, token) } }
        })
        DatingStage.RESULT -> DatingResult(state, viewModel::newPractice, playing, {
            if (playing) stopAudio() else playableToken?.let { token -> audioCache[token]?.let { play(it, token) } }
        })
    }
}

@Composable private fun DatingSetup(state: DatingTrainingUiState, vm: DatingTrainingViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AiTrainingHeader("💕", "AI 소개팅", "AI와 자연스럽게 대화하며\n소개팅 대화를 연습해보세요.") }
        item { GuideCard() }
        when {
            state.loading -> item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppActionButton) } }
            state.error != null -> item { ErrorCard(state.error, vm::loadConfig) }
            else -> {
                item { ChoiceSection("소개팅 상황", state.config?.categories.orEmpty(), state.selectedCategory, vm::selectCategory) }
                item { ChoiceSection("상대 스타일", state.config?.styles.orEmpty(), state.selectedStyle, vm::selectStyle) }
                item { ChoiceSection("상대 목소리", state.config?.voices.orEmpty(), state.selectedVoice, vm::selectVoice); Text("회원 나이와 관계없이 원하는 가상 상대 목소리를 직접 선택합니다.", style = MaterialTheme.typography.bodySmall, color = FreshTextMuted) }
                item { Button(vm::start, Modifier.fillMaxWidth().height(52.dp), enabled = !state.sending, colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { if (state.sending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("💕 소개팅 시작") } }
            }
        }
    }
}

@Composable private fun GuideCard() = Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(18.dp)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("AI 소개팅 방법", fontWeight = FontWeight.Bold, color = FreshDeepIndigo)
        Text("💕 ① 상황과 상대를 선택하고")
        Text("💬 ② 자연스럽게 대화한 뒤")
        Text("✨ ③ 연습 결과를 확인해보세요")
        HorizontalDivider(color = FreshOutline)
        Text("실제 만남이나 매칭이 아닌 대화 연습입니다. 정답은 없으니 편하게 시작해보세요!", style = MaterialTheme.typography.bodySmall, color = FreshTextVariant)
    }
}

@Composable private fun ChoiceSection(title: String, options: List<DatingOptionDto>, selected: String, choose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        options.forEach { option ->
            Card(Modifier.fillMaxWidth().clickable { choose(option.id) }, colors = CardDefaults.cardColors(containerColor = if (selected == option.id) FreshLightIndigoContainer else Color.White), border = BorderStroke(1.dp, if (selected == option.id) AppActionButton else FreshOutline), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == option.id, { choose(option.id) }); Spacer(Modifier.width(6.dp)); Column { Text(option.title, fontWeight = FontWeight.SemiBold); if (option.description.isNotBlank()) Text(option.description, style = MaterialTheme.typography.bodySmall, color = FreshTextVariant) }
                }
            }
        }
    }
}

@Composable private fun DatingChat(state: DatingTrainingUiState, input: String, setInput: (String)->Unit, send: ()->Unit, onAuthExpired:()->Unit, voiceBusy:Boolean, setVoiceBusy:(Boolean)->Unit, sendVoice:(String)->Unit, stopAudio:()->Unit, finish: ()->Unit, retry: ()->Unit, playing: Boolean, audio: ()->Unit) {
    val listState = rememberLazyListState(); val scope = rememberCoroutineScope()
    val userReadingHistory by remember { derivedStateOf { val last=listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1;listState.isScrollInProgress&&last<state.messages.lastIndex-1 } }
    var keepLatestVisible by remember{mutableStateOf(true)}
    LaunchedEffect(userReadingHistory){if(userReadingHistory)keepLatestVisible=false else if(!listState.canScrollForward)keepLatestVisible=true}
    LaunchedEffect(state.messages.size, state.sending) { if (state.messages.isNotEmpty()&&keepLatestVisible) listState.animateScrollToItem(state.messages.lastIndex) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = FreshLightIndigoContainer), border = BorderStroke(1.dp, FreshOutline)) {
            Column(Modifier.padding(12.dp)) { Text("💕 소개팅 상황", fontWeight = FontWeight.Bold, color = FreshDeepIndigo); Text(state.scenario, style = MaterialTheme.typography.bodySmall); Text("${state.turns}/${state.maxTurns}턴", style = MaterialTheme.typography.labelSmall, color = FreshTextMuted) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(state.messages) { index, message -> MessageBubble(message.role, message.text, if (index == state.messages.lastIndex && message.role == "assistant") {
                { TextButton(onClick = audio) { Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null); Text(if (playing) "음성 멈춤" else "다시 듣기") } }
            } else null) }
            if (state.sending) item { Text("상대가 답변을 생각하고 있어요...", color = FreshTextMuted, style = MaterialTheme.typography.bodySmall) }
            state.error?.let { item { ErrorCard(it, retry) } }
        }
        TextButton(onClick = finish, enabled = !state.sending && !voiceBusy && state.turns > 0, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("소개팅 마무리", color = FreshDeepIndigo) }
        if(state.sending) Text("AI가 답변을 준비하고 있어요...",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.bodySmall,color=FreshTextMuted)
        VoiceRecordingPanel(enabled=!state.sending,currentText="",onTranscribed=sendVoice,onAuthExpired=onAuthExpired,autoTranscribeOnStop=true,compact=true,onRecordingStarted=stopAudio,onBusyChanged=setVoiceBusy,modifier=Modifier.padding(horizontal=10.dp))
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, setInput, Modifier.weight(1f), enabled=!voiceBusy&&!state.sending, placeholder = { Text("메시지를 입력하세요...") }, maxLines = 4)
            IconButton(onClick = { send(); scope.launch { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex) } }, enabled = input.isNotBlank() && !state.sending && !voiceBusy) { Icon(Icons.Default.Send, "전송", tint = AppActionButton) }
        }
    }
}
@Composable private fun MessageBubble(role: String, text: String, controls: (@Composable ()->Unit)? = null) {
    val user = role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Card(Modifier.widthIn(max = 300.dp), colors = CardDefaults.cardColors(containerColor = if (user) AppActionButton else FreshSurfaceVariant), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { Text(text, color = if (user) Color.White else FreshTextPrimary); controls?.invoke() }
        }
    }
}

@Composable private fun DatingResult(state: DatingTrainingUiState, restart: ()->Unit, playing: Boolean, audio: ()->Unit) {
    val r = state.result
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Text("✨ 소개팅 결과", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("이번 대화를 돌아보고 다음 대화를 연습해보세요.", color = FreshTextVariant) }
        if (r != null) item { Card(colors = CardDefaults.cardColors(containerColor = FreshLightIndigoContainer), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("💕 대화 점수 ${r.practiceScore ?: 0}점", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FreshDeepIndigo)
            ResultLine("자연스러웠던 점", r.naturalPoint); ResultLine("한번 연습해볼 점", r.practicePoint); ResultLine("이런 표현도 있어요", r.alternativePhrase); ResultLine("대화 이어가기 팁", r.conversationTip)
            TextButton(onClick = audio) { Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null); Text(if (playing) "음성 멈춤" else "결과 다시 듣기") }
        } } }
        state.error?.let { item { ErrorCard(it, {}) } }
        item { Button(restart, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { Text("💕 새로운 소개팅") } }
    }
}
@Composable private fun ResultLine(title: String, body: String?) { if (!body.isNullOrBlank()) Column { Text(title, fontWeight = FontWeight.Bold); Text(body, color = FreshTextVariant) } }
@Composable private fun ErrorCard(message: String, retry: ()->Unit) = Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = FreshTextVariant); TextButton(retry) { Text("다시 시도") } } }

private class ContinuousDatingSpeech(
    context: Context,
    private val onText: (String) -> Unit,
    private val onState: (Boolean, Boolean) -> Unit,
    private val onError: (String?) -> Unit
) : RecognitionListener {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val handler = Handler(Looper.getMainLooper())
    private var voiceInputActive = false
    private var recognitionRunning = false
    private var destroyed = false
    private var committedTranscript = ""
    private var interimTranscript = ""
    private var lastFinalSegment = ""
    private val restart = Runnable { if (voiceInputActive && !recognitionRunning && !destroyed) listen() }

    init { recognizer.setRecognitionListener(this) }

    fun start(initialText: String) {
        if (destroyed) return
        committedTranscript = initialText.trim()
        interimTranscript = ""
        lastFinalSegment = ""
        voiceInputActive = true
        onError(null)
        onState(true, false)
        listen()
    }

    fun finish(): String {
        voiceInputActive = false
        handler.removeCallbacks(restart)
        val finalText = combined(interimTranscript).trim()
        if (recognitionRunning) runCatching { recognizer.stopListening() }
        recognitionRunning = false
        interimTranscript = ""
        committedTranscript = finalText
        onText(finalText)
        onState(false, false)
        return finalText
    }

    fun resetTranscript() {
        committedTranscript = ""
        interimTranscript = ""
        lastFinalSegment = ""
        onText("")
    }

    fun destroy() {
        destroyed = true
        voiceInputActive = false
        handler.removeCallbacksAndMessages(null)
        runCatching { recognizer.cancel() }
        recognizer.destroy()
    }

    private fun listen() {
        if (!voiceInputActive || recognitionRunning || destroyed) return
        recognitionRunning = true
        interimTranscript = ""
        onState(true, true)
        runCatching {
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "소개팅에서 나눌 이야기를 말해주세요")
            })
        }.onFailure {
            recognitionRunning = false
            onState(voiceInputActive, false)
            onError("음성 인식을 시작하지 못했습니다.")
        }
    }

    private fun combined(segment: String): String = listOf(committedTranscript, segment.trim()).filter { it.isNotBlank() }.joinToString(" ")
    private fun scheduleRestart() {
        recognitionRunning = false
        interimTranscript = ""
        onState(voiceInputActive, false)
        if (voiceInputActive && !destroyed) { handler.removeCallbacks(restart); handler.postDelayed(restart, 450L) }
    }
    private fun commit(segment: String) {
        val value = segment.trim()
        if (value.isNotBlank() && value != lastFinalSegment) {
            committedTranscript = combined(value)
            lastFinalSegment = value
            onText(committedTranscript)
        }
        interimTranscript = ""
    }

    override fun onReadyForSpeech(params: Bundle?) { recognitionRunning = true; onState(voiceInputActive, true) }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { recognitionRunning = false; onState(voiceInputActive, false) }
    override fun onResults(results: Bundle?) {
        if (!voiceInputActive) return
        commit(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
        scheduleRestart()
    }
    override fun onPartialResults(partialResults: Bundle?) {
        if (!voiceInputActive) return
        interimTranscript = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
        if (interimTranscript.isNotBlank()) onText(combined(interimTranscript))
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onError(error: Int) {
        recognitionRunning = false
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleRestart()
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { voiceInputActive = false; onState(false, false); onError("마이크 권한이 필요합니다.") }
            SpeechRecognizer.ERROR_AUDIO -> { voiceInputActive = false; onState(false, false); onError("마이크를 사용할 수 없습니다.") }
            else -> { voiceInputActive = false; onState(false, false); onError("음성 인식을 계속하지 못했습니다.") }
        }
    }
}
