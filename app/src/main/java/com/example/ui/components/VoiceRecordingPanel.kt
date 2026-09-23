package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.auth.TokenManager
import com.example.data.remote.RetrofitClient
import com.example.data.repository.EmotionServerResult
import com.example.data.repository.VoiceTranscriptionRepository
import com.example.ui.theme.AppActionButton
import com.example.ui.theme.FreshOutline
import com.example.ui.theme.FreshSurfaceVariant
import com.example.ui.theme.FreshTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VoiceRecordingPanel(
    enabled: Boolean,
    currentText: String,
    onTranscribed: (String) -> Unit,
    onAuthExpired: () -> Unit,
    autoTranscribeOnStop: Boolean = false,
    compact: Boolean = false,
    onRecordingStarted: () -> Unit = {},
    onBusyChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { VoiceTranscriptionRepository(RetrofitClient.apiService, TokenManager(context.applicationContext)) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val player = remember { MediaPlayer() }
    var playing by remember { mutableStateOf(false) }
    var transcribing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var appliedTranscript by remember { mutableStateOf<String?>(null) }

    fun stopPlayback() {
        runCatching { if (player.isPlaying) player.stop() }
        playing = false
    }
    fun releaseRecorder() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
    }
    fun discardRecording(clearTranscript: Boolean) {
        stopPlayback(); releaseRecorder(); file?.delete(); file = null
        recording = false; elapsed = 0L; error = null
        if (clearTranscript && appliedTranscript != null && currentText.trim() == appliedTranscript?.trim()) onTranscribed("")
        appliedTranscript = null
    }
    fun startRecording() {
        discardRecording(clearTranscript = true)
        onRecordingStarted()
        val target = File(context.cacheDir, "voice-input-${System.nanoTime()}.m4a")
        @Suppress("DEPRECATION")
        val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        runCatching {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(128_000)
            next.setAudioSamplingRate(44_100)
            next.setOutputFile(target.absolutePath)
            next.prepare(); next.start()
        }.onSuccess {
            recorder = next; file = target; startedAt = SystemClock.elapsedRealtime(); recording = true
        }.onFailure {
            runCatching { next.release() }; target.delete(); error = "녹음을 시작하지 못했습니다. 마이크를 확인해주세요."
        }
    }
    fun stopRecording() {
        if (!recording) return
        val duration = SystemClock.elapsedRealtime() - startedAt
        runCatching { recorder?.stop() }.onFailure { file?.delete(); file = null; error = "녹음 내용을 저장하지 못했습니다. 다시 시도해주세요." }
        releaseRecorder(); recording = false; elapsed = duration
        if (duration < 1_000L) { file?.delete(); file = null; error = "1초 이상 녹음해주세요." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else error = "마이크 권한이 필요합니다."
    }
    fun requestRecording() {
        if (!enabled || transcribing) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun togglePlayback() {
        val source = file ?: return
        if (playing) stopPlayback() else runCatching {
            player.reset(); player.setDataSource(source.absolutePath)
            player.setOnCompletionListener { playing = false }
            player.prepare(); player.start(); playing = true
        }.onFailure { playing = false; error = "녹음 내용을 재생하지 못했습니다." }
    }
    fun transcribe() {
        val source = file ?: return
        if (transcribing) return
        scope.launch {
            transcribing = true; error = null
            when (val result = withContext(Dispatchers.IO) { repository.transcribe(source) }) {
                is EmotionServerResult.Success -> {
                    onTranscribed(result.value.text)
                    if (autoTranscribeOnStop) {
                        stopPlayback(); source.delete(); file = null; elapsed = 0L; appliedTranscript = null
                    } else appliedTranscript = result.value.text
                }
                EmotionServerResult.Unauthorized -> onAuthExpired()
                is EmotionServerResult.Error -> error = result.message
            }
            transcribing = false
        }
    }

    LaunchedEffect(recording) { while (recording) { elapsed = SystemClock.elapsedRealtime() - startedAt; delay(250) } }
    LaunchedEffect(recording, transcribing) { onBusyChanged(recording || transcribing) }
    DisposableEffect(Unit) { onDispose { if (recording) runCatching { recorder?.stop() }; releaseRecorder(); stopPlayback(); player.release(); file?.delete() } }

    val container: @Composable (@Composable () -> Unit) -> Unit = { content ->
        if (compact) Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
        else Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FreshSurfaceVariant), border = BorderStroke(1.dp, FreshOutline), shape = RoundedCornerShape(14.dp)) { content() }
    }
    container {
        Column(Modifier.padding(if (compact) 6.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
            if (transcribing) Text("음성을 글로 바꾸고 있어요...", style = MaterialTheme.typography.bodySmall, color = FreshTextMuted)
            else if (recording) Text("● 듣고 있어요", style = MaterialTheme.typography.bodySmall, color = AppActionButton)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (recording) {
                        stopRecording()
                        if (autoTranscribeOnStop && file != null) transcribe()
                    } else requestRecording()
                }, enabled = enabled && !transcribing, colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) {
                    Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, null)
                    Text(if (recording) " 말하기 완료" else if (error != null && compact) " 다시 녹음" else " 녹음 시작")
                }
                Text("%02d:%02d".format((elapsed / 1000) / 60, (elapsed / 1000) % 60), color = FreshTextMuted)
            }
            if (!compact && file != null && !recording) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = ::togglePlayback, enabled = !transcribing) { Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, null); Text(if (playing) " 재생 멈춤" else " 내 음성 듣기") }
                    OutlinedButton(onClick = { discardRecording(clearTranscript = true) }, enabled = !transcribing) { Icon(Icons.Default.Refresh, null); Text(" 다시 녹음") }
                }
                Button(onClick = ::transcribe, enabled = enabled && !transcribing, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppActionButton)) {
                    if (transcribing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Text(if (transcribing) " 음성을 텍스트로 변환 중..." else " 음성을 텍스트로 변환")
                }
            }
            error?.let { Text(if(compact) "음성을 글로 바꾸지 못했어요. 다시 말씀해 주세요." else it, style = MaterialTheme.typography.bodySmall, color = FreshTextMuted) }
            if (!compact) Text("녹음은 텍스트 변환 후 서버에 보관되지 않습니다.", style = MaterialTheme.typography.bodySmall, color = FreshTextMuted)
        }
    }
}
