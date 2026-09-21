package com.example.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.QuizOptionDto
import com.example.ui.components.AiTrainingHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuizStage
import com.example.ui.viewmodel.QuizTrainingUiState
import com.example.ui.viewmodel.QuizTrainingViewModel
import java.io.File

@Composable
fun QuizTrainingScreen(viewModel: QuizTrainingViewModel, onAuthExpired: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember { MediaPlayer() }
    val cache = remember { mutableMapOf<String, File>() }
    var activeToken by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }

    fun stopAudio() { runCatching { if (player.isPlaying) player.stop() }; playing = false }
    fun play(file: File, token: String) {
        runCatching {
            player.reset(); player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { it.start(); playing = true; activeToken = token }
            player.setOnCompletionListener { playing = false }
            player.prepareAsync()
        }
    }
    fun audio() {
        if (playing) stopAudio()
        else state.speechToken?.let { token -> cache[token]?.let { play(it, token) } ?: viewModel.replaySpeech(token) }
    }

    DisposableEffect(Unit) { onDispose { stopAudio(); player.release(); cache.values.forEach(File::delete) } }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }
    LaunchedEffect(state.submitting) { if (state.submitting) stopAudio() }
    LaunchedEffect(state.speechBytes, state.speechToken) {
        val bytes = state.speechBytes; val token = state.speechToken
        if (bytes != null && token != null) {
            val file = File(context.cacheDir, "quiz-$token.mp3")
            file.writeBytes(bytes); cache[token] = file; play(file, token); viewModel.clearSpeech()
        }
    }

    when (state.stage) {
        QuizStage.SETUP -> QuizSetup(state, viewModel)
        QuizStage.QUESTION -> QuizQuestion(state, viewModel, playing, ::audio)
        QuizStage.COMPLETE -> QuizComplete(state, viewModel)
    }
}

@Composable
private fun QuizSetup(state: QuizTrainingUiState, vm: QuizTrainingViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AiTrainingHeader("🧠", "AI 상식퀴즈", "다양한 상식 문제를 풀며\n재미있게 지식을 확인해보세요.")
            Text("시험이나 지능평가가 아닌 가벼운 집중 활동입니다.", style = MaterialTheme.typography.bodySmall, color = FreshTextMuted)
        }
        item { QuizGuideCard() }
        when {
            state.loading -> item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppActionButton) } }
            state.error != null -> item { QuizErrorCard(state.error, vm::loadConfig) }
            else -> {
                item { Text("분야를 선택해주세요.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                state.config?.categories.orEmpty().chunked(2).forEach { row ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { option -> Box(Modifier.weight(1f)) { QuizChoiceCard(option, state.category == option.id, state.submitting) { vm.selectCategory(option.id) } } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Text("난이도", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        state.config?.difficulties.orEmpty().forEach { option ->
                            FilterChip(
                                selected = state.difficulty == option.id,
                                onClick = { vm.selectDifficulty(option.id) },
                                label = { Text(option.title) },
                                enabled = !state.submitting,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    Text("AI 캐스터", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        state.config?.voices.orEmpty().forEach { option ->
                            FilterChip(
                                selected = state.voice == option.id,
                                onClick = { vm.selectVoice(option.id) },
                                label = { Text(option.title) },
                                enabled = !state.submitting,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                state.error?.let { item { QuizErrorCard(it, vm::retry) } }
                item {
                    Button(
                        vm::start,
                        Modifier.fillMaxWidth().height(52.dp),
                        enabled = state.category.isNotBlank() && !state.submitting,
                        colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            Spacer(Modifier.width(8.dp)); Text("AI가 10개의 퀴즈를 준비하고 있어요...")
                        } else Text("🧠 퀴즈 시작")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizGuideCard() = Card(
    colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant),
    border = BorderStroke(1.dp, FreshOutline),
    shape = RoundedCornerShape(18.dp)
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("상식퀴즈 방법", color = FreshDeepIndigo, fontWeight = FontWeight.Bold)
        Text("🧠 ① 분야와 난이도 선택")
        Text("❓ ② 20초 안에 문제 풀기")
        Text("💡 ③ 정답과 해설 확인")
        HorizontalDivider(color = FreshOutline)
        Text("재미있게 도전해보세요! 다른 회원과 비교하거나 순위를 매기지 않습니다.", style = MaterialTheme.typography.bodySmall, color = FreshTextVariant)
    }
}

@Composable
private fun QuizChoiceCard(option: QuizOptionDto, selected: Boolean, disabled: Boolean, onClick: () -> Unit) = Card(
    Modifier.fillMaxWidth().clickable(enabled = !disabled, onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = if (selected) FreshLightIndigoContainer else Color.White),
    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AppActionButton else FreshOutline),
    shape = RoundedCornerShape(14.dp)
) {
    Text(option.title, Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp), textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
}

@Composable
private fun QuizQuestion(state: QuizTrainingUiState, vm: QuizTrainingViewModel, playing: Boolean, audio: () -> Unit) {
    val q = state.question ?: return
    val result = state.answerResult
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            AiTrainingHeader("🧠", "AI 상식퀴즈", "다양한 상식 문제를 풀며\n재미있게 지식을 확인해보세요.")
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("문제 ${q.number} / ${q.total}", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = FreshDeepIndigo)
                Text("현재 ${state.score}개 정답", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Surface(shape = CircleShape, color = if (state.secondsLeft <= 5 && result == null) Color(0xFFFFF1E8) else FreshLightIndigoContainer) {
                    Text("${state.secondsLeft}", Modifier.size(46.dp).wrapContentSize(), fontWeight = FontWeight.Bold, color = FreshDeepIndigo)
                }
            }
            LinearProgressIndicator(
                progress = { q.number.toFloat() / q.total.coerceAtLeast(1) },
                Modifier.fillMaxWidth().padding(top = 8.dp),
                color = AppActionButton,
                trackColor = FreshLightIndigoContainer
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Q${q.number}", color = FreshDeepIndigo, fontWeight = FontWeight.Bold)
                    Text(q.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        q.options.forEachIndexed { index, text ->
            item { QuizAnswerOption(index, text, state.selectedAnswer, result?.correctIndex, result != null, state.submitting) { vm.answer(index) } }
        }
        item {
            OutlinedButton(audio, Modifier.fillMaxWidth(), enabled = !state.speechLoading && state.speechToken != null) {
                Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(7.dp))
                Text(if (playing) "음성 멈춤" else if (result == null) "문제 다시 듣기" else "정답·해설 다시 듣기")
            }
            if (state.speechLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppActionButton)
            state.speechError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FreshTextMuted) }
        }
        result?.let { answer ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            if (answer.timedOut) "⏱ 시간이 끝났어요" else if (answer.correct == true) "✓ 정답입니다!" else "✦ 아쉬워요",
                            fontWeight = FontWeight.Bold,
                            color = if (answer.correct == true) FreshDeepIndigo else FreshTextPrimary
                        )
                        Text("정답: ${answer.correctAnswer.orEmpty()}", fontWeight = FontWeight.SemiBold)
                        Text("💡 알아두면 좋아요", color = FreshDeepIndigo, fontWeight = FontWeight.Bold)
                        Text(answer.explanation.orEmpty(), color = FreshTextVariant)
                    }
                }
            }
            item {
                Button(vm::next, Modifier.fillMaxWidth().height(50.dp), enabled = !state.submitting, colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) {
                    if (state.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Text(if (answer.finished) "결과 보기" else "다음 문제 →")
                }
            }
        }
        state.error?.let { item { QuizErrorCard(it, vm::retry) } }
    }
}

@Composable
private fun QuizAnswerOption(index: Int, text: String, selected: Int?, correct: Int?, locked: Boolean, submitting: Boolean, onClick: () -> Unit) {
    val isCorrect = locked && correct == index
    val isWrong = locked && selected == index && correct != index
    val background = when { isCorrect -> Color(0xFFEAF8F1); isWrong -> Color(0xFFFFF4F1); selected == index -> FreshLightIndigoContainer; else -> Color.White }
    val border = when { isCorrect -> Color(0xFF58A77B); isWrong -> Color(0xFFD99282); selected == index -> AppActionButton; else -> FreshOutline }
    Surface(
        Modifier.fillMaxWidth().clickable(enabled = !locked && !submitting, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected == index || isCorrect) 2.dp else 1.dp, border)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${('A'.code + index).toChar()}.", color = FreshDeepIndigo, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp)); Text(text, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (isCorrect) Text("✓", color = Color(0xFF31855A), fontWeight = FontWeight.Bold)
            else if (isWrong) Text("×", color = Color(0xFFA65444), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuizComplete(state: QuizTrainingUiState, vm: QuizTrainingViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { AiTrainingHeader("🧠", "AI 상식퀴즈", "다양한 상식 문제를 풀며\n재미있게 지식을 확인해보세요.") }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FreshLightIndigoContainer), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎉 퀴즈 완료!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${state.score} / ${state.finalTotal}", style = MaterialTheme.typography.displaySmall, color = FreshDeepIndigo, fontWeight = FontWeight.Bold)
                    Text("${state.finalTotal}문제 중 ${state.score}문제 정답", fontWeight = FontWeight.SemiBold)
                    Text("${state.categoryTitle} · ${state.difficultyTitle}", color = FreshTextVariant)
                    HorizontalDivider(color = FreshOutline)
                    Text("재미있게 문제를 풀어본 것으로 충분해요. 이 결과는 지능이나 상식 수준을 평가하지 않습니다.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = FreshTextVariant)
                }
            }
        }
        item {
            Button(vm::challengeAgain, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) { Text("🔄 같은 설정으로 다시 도전") }
            OutlinedButton(vm::chooseOther, Modifier.fillMaxWidth()) { Text("다른 분야 선택") }
            Text("문제와 해설은 AI가 생성하므로 드물게 부정확할 수 있습니다. 정답이 의심되면 신뢰할 수 있는 자료로 다시 확인해주세요.", style = MaterialTheme.typography.bodySmall, color = FreshTextMuted, textAlign = TextAlign.Center)
        }
        state.error?.let { item { QuizErrorCard(it, vm::retry) } }
    }
}

@Composable
private fun QuizErrorCard(message: String, retry: () -> Unit) = Card(
    colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant),
    border = BorderStroke(1.dp, FreshOutline),
    shape = RoundedCornerShape(14.dp)
) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), color = FreshTextVariant)
        TextButton(retry) { Text("다시 시도") }
    }
}
