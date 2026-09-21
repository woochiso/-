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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AiTrainingHeader
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.data.remote.dto.ExpressionOptionDto
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExpressionStage
import com.example.ui.viewmodel.ExpressionTrainingUiState
import com.example.ui.viewmodel.ExpressionTrainingViewModel
import java.io.File

@Composable
fun ExpressionTrainingScreen(viewModel: ExpressionTrainingViewModel, onAuthExpired: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember { MediaPlayer() }
    val cache = remember { mutableMapOf<String, File>() }
    var activeToken by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    fun stop() { runCatching { if (player.isPlaying) player.stop() }; playing = false }
    fun play(file: File, token: String) { runCatching { player.reset(); player.setDataSource(file.absolutePath); player.setOnPreparedListener { it.start(); playing = true; activeToken = token }; player.setOnCompletionListener { playing = false }; player.prepareAsync() } }
    DisposableEffect(Unit) { onDispose { stop(); player.release(); cache.values.forEach(File::delete) } }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }
    LaunchedEffect(state.speechBytes, state.speechToken) {
        val bytes = state.speechBytes; val token = state.speechToken
        if (bytes != null && token != null) { val file = File(context.cacheDir, "expression-$token.mp3"); file.writeBytes(bytes); cache[token] = file; play(file, token); viewModel.clearSpeech() }
    }
    var voiceInputActive by remember { mutableStateOf(false) }
    var recognitionRunning by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val speechController = remember {
        ContinuousExpressionSpeech(
            context = context,
            onText = viewModel::updateText,
            onState = { active, running -> voiceInputActive = active; recognitionRunning = running },
            onError = { voiceError = it }
        )
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { stop(); voiceError = null; speechController.start(state.inputText) }
        else voiceError = "마이크 권한이 필요합니다."
    }
    fun voiceInput() {
        if (voiceInputActive) {
            viewModel.updateText(speechController.finish())
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            stop(); voiceError = null; speechController.start(state.inputText)
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(speechController) { onDispose { speechController.destroy() } }
    LaunchedEffect(state.stage, state.sending) { if (state.stage != ExpressionStage.PRACTICE || state.sending) speechController.finish() }
    fun audio() { if (playing) stop() else activeToken?.let { token -> cache[token]?.let { play(it, token) } } }

    when (state.stage) {
        ExpressionStage.CHOOSE -> ExpressionChoose(state, viewModel)
        ExpressionStage.PRACTICE -> ExpressionPractice(state, viewModel, ::voiceInput, voiceInputActive, recognitionRunning, voiceError, playing, ::audio)
        ExpressionStage.RESULT -> ExpressionResult(state, viewModel, playing, ::audio)
    }
}

@Composable private fun ExpressionChoose(state: ExpressionTrainingUiState, vm: ExpressionTrainingViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AiTrainingHeader("💬", "AI와 감정표현 연습", "내 감정을 말로 표현하고\nAI와 함께 자연스러운 표현을 연습해보세요.") }
        item { ExpressionGuide() }
        item { Text("어떤 상황을 연습해볼까요?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        when {
            state.loading -> item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppActionButton) } }
            state.error != null -> item { ExpressionError(state.error, vm::loadConfig) }
            else -> items(state.config?.categories.orEmpty(), key = { it.id }) { option -> ExpressionCategoryCard(option, state.sending) { vm.selectCategory(option.id) } }
        }
    }
}
@Composable private fun ExpressionGuide() = Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("감정표현 연습 방법", color = FreshDeepIndigo, fontWeight = FontWeight.Bold); Text("🎭 ① 상황 확인"); Text("💬 ② 내 마음 표현"); Text("✨ ③ AI 피드백 확인"); HorizontalDivider(color = FreshOutline); Text("잘해야 하는 시험이 아닙니다. 편하게 표현해보세요!", style = MaterialTheme.typography.bodySmall, color = FreshTextVariant) } }
@Composable private fun ExpressionCategoryCard(option: ExpressionOptionDto, disabled: Boolean, choose: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(enabled = !disabled, onClick = choose), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text(option.title, fontWeight = FontWeight.Bold); Text(option.description, style = MaterialTheme.typography.bodySmall, color = FreshTextVariant) } }

@Composable private fun ExpressionPractice(state: ExpressionTrainingUiState, vm: ExpressionTrainingViewModel, voiceInput: () -> Unit, voiceInputActive: Boolean, recognitionRunning: Boolean, voiceError: String?, playing: Boolean, audio: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AiTrainingHeader("💬", "AI와 감정표현 연습", "내 감정을 말로 표현하고\nAI와 함께 자연스러운 표현을 연습해보세요.") }
        item { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("AI 목소리", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { state.config?.voices.orEmpty().forEach { option -> FilterChip(selected = state.voice == option.id, onClick = { vm.selectVoice(option.id) }, label = { Text(option.title) }, enabled = !state.sending) } } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = FreshLightIndigoContainer), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("🎭 오늘의 상황", color = FreshDeepIndigo, fontWeight = FontWeight.Bold); Text(state.scenario); Surface(color = Color.White.copy(alpha = .75f), shape = RoundedCornerShape(12.dp)) { Text(state.partnerLine, Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold) }; TextButton(onClick = audio) { androidx.compose.material3.Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null); Text(if (playing) "음성 멈춤" else "상황 다시 듣기") } } } }
        item { Text("내가 어떻게 말할까요?", fontWeight = FontWeight.Bold); OutlinedTextField(state.inputText, vm::updateText, Modifier.fillMaxWidth(), enabled = !voiceInputActive, minLines = 4, maxLines = 7, placeholder = { Text(if (voiceInputActive) "인식된 표현이 여기에 누적됩니다." else "편하게 이야기해 주세요.") }, supportingText = { Text("${state.inputText.length}/2000") }); OutlinedButton(voiceInput, Modifier.fillMaxWidth(), enabled = !state.sending) { androidx.compose.material3.Icon(if (voiceInputActive) Icons.Default.Stop else Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (voiceInputActive) "■ 말하기 완료" else "🎙 말로 표현하기") }; if (voiceInputActive) { Text(if (recognitionRunning) "🎙 듣고 있습니다..." else "🎙 다음 말을 기다리고 있습니다...", style = MaterialTheme.typography.bodySmall, color = FreshDeepIndigo); Text("잠시 말을 멈춰도 계속 듣습니다.\n모두 말씀하신 후 '말하기 완료'를 눌러주세요.", style = MaterialTheme.typography.bodySmall, color = FreshTextMuted) }; voiceError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FreshTextVariant) } }
        state.error?.let { item { ExpressionError(it, vm::retry) } }
        state.speechError?.let { item { Text(it, style = MaterialTheme.typography.bodySmall, color = FreshTextMuted) } }
        item { Button(vm::submit, Modifier.fillMaxWidth().height(52.dp), enabled = state.inputText.isNotBlank() && !state.sending && !voiceInputActive, colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { if (state.sending) { CircularProgressIndicator(Modifier.size(20.dp), color = Color.White); Spacer(Modifier.width(8.dp)); Text("AI가 표현을 살펴보고 있어요...") } else Text("✨ AI에게 전달") } }
    }
}

@Composable private fun ExpressionResult(state: ExpressionTrainingUiState, vm: ExpressionTrainingViewModel, playing: Boolean, audio: () -> Unit) {
    val r = state.result
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AiTrainingHeader("💬", "AI와 감정표현 연습", "내 감정을 말로 표현하고\nAI와 함께 자연스러운 표현을 연습해보세요.") }
        r?.transcript?.let { item { Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline)) { Column(Modifier.padding(14.dp)) { Text("내 표현", fontWeight = FontWeight.Bold); Text(it) } } } }
        if (r?.risk == true) item { ExpressionFeedbackItem("안전 안내", r.safetyMessage) }
        else if (r != null) item { Card(colors = CardDefaults.cardColors(containerColor = FreshLightIndigoContainer), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) { ExpressionFeedbackLine("💬 AI의 반응", r.reaction); ExpressionFeedbackLine("👍 잘 표현한 점", r.strength); ExpressionFeedbackLine("🌱 조금 다르게 표현해보기", r.suggestion); ExpressionFeedbackLine("💡 이렇게 말해봐도 좋아요", r.example); TextButton(audio) { androidx.compose.material3.Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null); Text(if (playing) "음성 멈춤" else "결과 다시 듣기") } } } }
        state.speechError?.let { item { Text(it, style = MaterialTheme.typography.bodySmall, color = FreshTextMuted) } }
        item { Button(vm::sameCategory, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { Text("🔄 같은 종류 다른 상황") }; OutlinedButton(vm::chooseOther, Modifier.fillMaxWidth()) { Text("🎯 다른 연습 선택") } }
    }
}
@Composable private fun ExpressionFeedbackLine(title: String, value: String?) { if (!value.isNullOrBlank()) Column { Text(title, fontWeight = FontWeight.Bold, color = FreshDeepIndigo); Text(value, color = FreshTextVariant) } }
@Composable private fun ExpressionFeedbackItem(title: String, value: String?) = Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline)) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(value.orEmpty(), color = FreshTextVariant) } }
@Composable private fun ExpressionError(message: String, retry: () -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, Modifier.weight(1f), color = FreshTextVariant); TextButton(retry) { Text("다시 시도") } } }

private class ContinuousExpressionSpeech(
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
                putExtra(RecognizerIntent.EXTRA_PROMPT, "이 상황에서 전하고 싶은 마음을 말해주세요")
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
