package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.content.ContentValues
import android.os.Environment
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AiTrainingHeader
import com.example.ui.viewmodel.HumorTrainingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HumorTrainingScreen(viewModel: HumorTrainingViewModel, onAuthExpired: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var audioDuration by remember { mutableDoubleStateOf(0.0) }
    var directlyRecorded by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    val player = remember { MediaPlayer() }
    val userPlayer = remember { MediaPlayer() }
    var playable by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var userPlaying by remember { mutableStateOf(false) }
    var speechFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    fun stopAiAudio() {
        if (playing) runCatching { player.pause(); player.seekTo(0) }
        playing = false
    }
    fun stopUserAudio() {
        if (userPlaying) runCatching { userPlayer.pause(); userPlayer.seekTo(0) }
        userPlaying = false
    }

    fun releaseRecorder() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
    }
    fun startRecording() {
        stopAiAudio()
        stopUserAudio()
        viewModel.reset()
        localMessage = null
        val target = File(context.cacheDir, "humor-" + System.currentTimeMillis() + ".m4a")
        @Suppress("DEPRECATION")
        val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        runCatching {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(128_000)
            next.setAudioSamplingRate(44_100)
            next.setOutputFile(target.absolutePath)
            next.prepare()
            next.start()
        }.onSuccess {
            recorder = next
            audioFile = target
            audioDuration = 0.0
            directlyRecorded = true
            startedAt = SystemClock.elapsedRealtime()
            elapsed = 0L
            recording = true
        }.onFailure {
            runCatching { next.release() }
            localMessage = "녹음을 시작하지 못했습니다. 마이크를 확인해주세요."
        }
    }
    fun stopRecording() {
        if (!recording) return
        val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
        runCatching { recorder?.stop() }.onFailure {
            audioFile?.delete()
            audioFile = null
            localMessage = "녹음 내용을 확인하지 못했습니다. 다시 녹음해주세요."
        }
        releaseRecorder()
        recording = false
        audioDuration = seconds
        when {
            seconds < 1.8 -> {
                audioFile?.delete(); audioFile = null
                localMessage = "2초 이상 말씀해주세요."
            }
            seconds > 120.5 -> {
                audioFile?.delete(); audioFile = null
                localMessage = "AI를 웃겨라는 최대 2분까지 들을 수 있어요."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else localMessage = "마이크 권한을 허용하면 직접 녹음할 수 있습니다."
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val target = File(context.cacheDir, "humor-upload-" + System.currentTimeMillis() + ".m4a")
            context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) } ?: error("empty")
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.div(1000.0) ?: 0.0
            retriever.release()
            require(duration in 1.8..120.5)
            require(target.length() in 4_000..(8L * 1024L * 1024L))
            viewModel.reset()
            audioFile = target
            audioDuration = duration
            directlyRecorded = false
            localMessage = null
        }.onFailure { localMessage = "2초~2분, 8MB 이하의 MP3·M4A·WebM 음성파일을 선택해주세요." }
    }

    LaunchedEffect(recording) {
        while (recording) {
            elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000
            if (elapsed >= 120) stopRecording()
            delay(250)
        }
    }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }
    LaunchedEffect(state.speechBytes) {
        val bytes = state.speechBytes ?: return@LaunchedEffect
        runCatching {
            stopUserAudio()
            player.reset()
            speechFile?.delete()
            speechFile = File(context.cacheDir, "humor-feedback-" + System.currentTimeMillis() + ".mp3").apply { writeBytes(bytes) }
            player.setDataSource(speechFile!!.absolutePath)
            player.setOnCompletionListener { playing = false }
            player.prepare()
            playable = true
            playing = true
            player.start()
            viewModel.clearSpeech()
        }.onFailure {
            playable = false
            playing = false
            localMessage = "AI 음성을 재생하지 못했습니다. 결과는 글로 확인해주세요."
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (recording) runCatching { recorder?.stop() }
            releaseRecorder()
            stopUserAudio()
            userPlayer.release()
            stopAiAudio()
            player.release()
            speechFile?.delete()
        }
    }

    HumorTrainingContent(
        state = state,
        recording = recording,
        elapsed = elapsed,
        hasAudio = audioFile != null,
        directlyRecorded = directlyRecorded,
        localMessage = localMessage,
        playable = playable,
        playing = playing,
        userPlaying = userPlaying,
        onRecord = {
            if (recording) stopRecording()
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onChooseFile = { fileLauncher.launch(arrayOf("audio/*", "video/webm")) },
        onAnalyze = {
            stopUserAudio()
            stopAiAudio()
            runCatching { player.reset() }
            playable = false
            audioFile?.let { viewModel.analyze(it, audioDuration, if (it.name.startsWith("humor-upload")) "upload" else "record") }
        },
        onRetry = viewModel::retry,
        onUserAudioToggle = {
            if (userPlaying) stopUserAudio()
            else audioFile?.let { file ->
                stopAiAudio()
                runCatching {
                    userPlayer.reset()
                    userPlayer.setDataSource(file.absolutePath)
                    userPlayer.setOnCompletionListener { userPlaying = false }
                    userPlayer.prepare()
                    userPlayer.start()
                    userPlaying = true
                }.onFailure { localMessage = "녹음파일을 재생하지 못했습니다." }
            }
        },
        onSaveRecording = {
            val source = audioFile
            if (source != null && directlyRecorded) scope.launch {
                val savedAt = withContext(Dispatchers.IO) { saveHumorRecording(context, source) }
                Toast.makeText(
                    context,
                    if (savedAt != null) "녹음파일을 저장했습니다.\n" + savedAt else "녹음파일을 저장하지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onAiAudioToggle = {
            if (playing) stopAiAudio()
            else {
                stopUserAudio()
                runCatching { player.seekTo(0); player.start(); playing = true }
                    .onFailure { localMessage = "AI 음성을 재생하지 못했습니다. 결과는 글로 확인해주세요." }
            }
        },
        onRestart = {
            stopAiAudio(); stopUserAudio()
            audioFile?.delete(); audioFile = null
            directlyRecorded = false
            elapsed = 0; viewModel.reset()
        }
    )
}

@Composable
private fun HumorTrainingContent(
    state: com.example.ui.viewmodel.HumorTrainingUiState,
    recording: Boolean,
    elapsed: Long,
    hasAudio: Boolean,
    directlyRecorded: Boolean,
    localMessage: String?,
    playable: Boolean,
    playing: Boolean,
    userPlaying: Boolean,
    onRecord: () -> Unit,
    onChooseFile: () -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
    onUserAudioToggle: () -> Unit,
    onSaveRecording: () -> Unit,
    onAiAudioToggle: () -> Unit,
    onRestart: () -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AiTrainingHeader(
                icon = "😂",
                title = "AI를 웃겨라",
                description = "재미있는 이야기를 들려주고\nAI의 웃음 점수와 반응을 확인해보세요."
            )
        }
        item { TrainingGuideCard() }
        item {
            TrainingCardSurface {
                Column(Modifier.padding(18.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        color = com.example.ui.theme.FreshLightIndigoContainer
                    ) {
                        Icon(
                            imageVector = when {
                                recording -> androidx.compose.material.icons.Icons.Default.Stop
                                hasAudio && directlyRecorded -> androidx.compose.material.icons.Icons.Default.CheckCircle
                                hasAudio -> androidx.compose.material.icons.Icons.Default.FolderOpen
                                else -> androidx.compose.material.icons.Icons.Default.Mic
                            },
                            contentDescription = null,
                            tint = com.example.ui.theme.AppActionButton,
                            modifier = Modifier.padding(14.dp).size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "MICROPHONE SETUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.example.ui.theme.FreshDeepIndigo,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (recording) "이야기를 듣고 있어요" else if (hasAudio) "이야기 준비 완료" else "재미있는 이야기를 들려주세요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "%02d:%02d".format(elapsed / 60, elapsed % 60),
                        style = MaterialTheme.typography.headlineMedium,
                        color = com.example.ui.theme.AppActionButton
                    )
                    Text("2초 이상 · 최대 2분 · 최대 8MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        enabled = !state.analyzing,
                        onClick = onRecord,
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AppActionButton)
                    ) {
                        Icon(if (recording) androidx.compose.material.icons.Icons.Default.Stop else androidx.compose.material.icons.Icons.Default.Mic, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (recording) "녹음 종료" else "직접 녹음")
                    }
                    TextButton(enabled = !recording && !state.analyzing, onClick = onChooseFile) {
                        Icon(androidx.compose.material.icons.Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(6.dp))
                        Text("녹음파일 불러오기")
                    }
                    if (hasAudio) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !state.analyzing, onClick = onUserAudioToggle) {
                                Icon(
                                    if (userPlaying) androidx.compose.material.icons.Icons.Default.Stop
                                    else androidx.compose.material.icons.Icons.Default.PlayArrow,
                                    null
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (userPlaying) "멈춤" else "들어보기")
                            }
                            if (directlyRecorded) {
                                OutlinedButton(enabled = !state.analyzing, onClick = onSaveRecording) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Download, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("녹음파일 저장")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (hasAudio) item {
            Button(
                onClick = onAnalyze,
                enabled = !recording && !state.analyzing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AppActionButton)
            ) {
                if (state.analyzing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else {
                    Icon(androidx.compose.material.icons.Icons.Default.SmartToy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("AI에게 들려주기")
                }
            }
        }
        if (state.analyzing) item { Text("AI 웃음 심사위원이 이야기를 듣고 있습니다...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        (state.error ?: localMessage)?.let { message ->
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.error != null) TextButton(onClick = onRetry) {
                            Icon(androidx.compose.material.icons.Icons.Default.Refresh, null)
                            Text("다시 시도")
                        }
                    }
                }
            }
        }
        state.result?.let { result ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.FreshLightIndigoContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.FreshOutline),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.SmartToy, null, tint = com.example.ui.theme.AppActionButton)
                            Spacer(Modifier.width(8.dp))
                            Text("AI 웃음 심사위원", color = com.example.ui.theme.FreshDeepIndigo, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Text("😂 웃음 점수", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text((result.score ?: 0).toString() + "점", style = MaterialTheme.typography.headlineMedium, color = com.example.ui.theme.FreshDeepIndigo, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        result.transcript?.takeIf { it.isNotBlank() }?.let { Text("“" + it + "”", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        result.highlight?.takeIf { it.isNotBlank() }?.let { FeedbackBlock("좋았던 점", it) }
                        result.deliveryFeedback?.takeIf { it.isNotBlank() }?.let { FeedbackBlock("🎙 말하기 포인트", it) }
                        result.suggestion?.takeIf { it.isNotBlank() }?.let { FeedbackBlock("다음 도전", it) }
                        if (state.speechLoading) {
                            Text("AI 음성을 준비하고 있습니다...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        if (playable) OutlinedButton(onClick = onAiAudioToggle) {
                            Icon(
                                if (playing) androidx.compose.material.icons.Icons.Default.Stop
                                else androidx.compose.material.icons.Icons.Default.VolumeUp,
                                null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (playing) "음성 멈춤" else "다시 듣기")
                        }
                        (state.speechError ?: result.audioAnalysisNotice)?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                            Icon(androidx.compose.material.icons.Icons.Default.Refresh, null)
                            Spacer(Modifier.width(6.dp))
                            Text("다시 도전")
                        }
                    }
                }
            }
        }
        item {
            Text("녹음 음성은 AI 분석을 위해 일시적으로 전송되며 분석 후 우치소 서버에 보관하지 않습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrainingGuideCard() {
    TrainingCardSurface {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("AI를 웃기는 방법", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            TrainingStepRow("🎙", "① 이야기 녹음")
            TrainingStepRow("🤖", "② AI에게 들려주기")
            TrainingStepRow("😂", "③ 웃음 점수 확인")
            HorizontalDivider(color = com.example.ui.theme.FreshOutline)
            Text(
                "정답은 없습니다. 편하게 즐겨보세요!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrainingStepRow(emoji: String, title: String) {
    Text(
        "$emoji  $title",
        color = com.example.ui.theme.FreshDeepIndigo,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    )
}

@Composable
private fun TrainingCardSurface(content: @Composable () -> Unit) {
    Surface(
        color = com.example.ui.theme.FreshSurfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.FreshOutline),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun FeedbackBlock(title: String, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun saveHumorRecording(context: Context, source: File): String? = runCatching {
    val name = "Woochiso_AI를웃겨라_" +
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.KOREA).format(Date()) + ".m4a"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Woochiso")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("insert failed")
        try {
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("stream failed")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
        "Downloads/Woochiso"
    } else {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Woochiso").apply { mkdirs() }
        source.copyTo(File(directory, name), overwrite = false)
        directory.absolutePath
    }
}.getOrNull()
