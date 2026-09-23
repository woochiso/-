package com.example.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.remote.dto.VocalScoresDto
import com.example.ui.theme.*
import com.example.ui.components.AiTrainingHeader
import com.example.ui.viewmodel.VocalTrainingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

@Composable
fun VocalTrainingScreen(viewModel: VocalTrainingViewModel, onAuthExpired: () -> Unit) {
    val context=LocalContext.current; val state by viewModel.state.collectAsStateWithLifecycle(); val scope=rememberCoroutineScope()
    var mode by remember{mutableStateOf("VOCAL_ONLY")}; var recorder by remember{mutableStateOf<MediaRecorder?>(null)}; var recording by remember{mutableStateOf(false)}
    var startedAt by remember{mutableLongStateOf(0)}; var elapsed by remember{mutableLongStateOf(0)}; var file by remember{mutableStateOf<File?>(null)}
    var mime by remember{mutableStateOf("audio/mp4")}; var duration by remember{mutableDoubleStateOf(0.0)}; var direct by remember{mutableStateOf(false)}
    var localError by remember{mutableStateOf<String?>(null)}; var playing by remember{mutableStateOf(false)}; var feedbackPlaying by remember{mutableStateOf(false)}
    var feedbackFile by remember{mutableStateOf<File?>(null)}; var confirmExit by remember{mutableStateOf(false)}
    var mrFile by remember{mutableStateOf<File?>(null)}; var mrName by remember{mutableStateOf<String?>(null)}; var mrPlaying by remember{mutableStateOf(false)}
    var selectedMrUri by remember{mutableStateOf<Uri?>(null)}; var selectedMrMime by remember{mutableStateOf<String?>(null)}
    var mrPickerLaunched by remember{mutableStateOf(false)}; var mrPickerCallback by remember{mutableStateOf("NONE")}
    var mrPitchSemitone by rememberSaveable{mutableIntStateOf(0)}; var mrTempoPercent by rememberSaveable{mutableIntStateOf(100)}
    val audioPlayer=remember{MediaPlayer()}; val feedbackPlayer=remember{MediaPlayer()}; val mrPlayer=remember{MediaPlayer()}
    fun stopAudio(){runCatching{audioPlayer.stop()};audioPlayer.reset();playing=false}
    fun stopFeedback(){runCatching{feedbackPlayer.stop()};runCatching{feedbackPlayer.reset()};feedbackPlaying=false}
    fun playFeedback(){
        val source=feedbackFile?.takeIf{it.isFile&&it.length()>0}
        if(source==null){localError="피드백 음성을 재생하지 못했습니다.";return}
        stopAudio();stopFeedback();localError=null
        runCatching{
            feedbackPlayer.setOnCompletionListener{
                feedbackPlaying=false
                runCatching{feedbackPlayer.reset()}
            }
            feedbackPlayer.setOnErrorListener{_,_,_->
                feedbackPlaying=false
                runCatching{feedbackPlayer.reset()}
                localError="피드백 음성을 재생하지 못했습니다."
                true
            }
            feedbackPlayer.setDataSource(source.absolutePath)
            feedbackPlayer.prepare()
            feedbackPlayer.start()
            feedbackPlaying=true
        }.onFailure{
            feedbackPlaying=false
            runCatching{feedbackPlayer.reset()}
            localError="피드백 음성을 재생하지 못했습니다."
            if(BuildConfig.DEBUG) Log.e("VOCAL_TTS_PLAYBACK","feedback playback failed",it)
        }
    }
    fun stopMr(){
        if(BuildConfig.DEBUG&&mrPlaying) Log.d("VOCAL_MR_REAL", "stop clicked")
        runCatching{mrPlayer.stop()}
        mrPlayer.reset()
        mrPlaying=false
        if(BuildConfig.DEBUG) Log.d("VOCAL_MR_REAL", "player stopped")
    }
    fun startMrPlayback(uri:Uri, source:String){
        stopMr()
        mrPlayer.setDataSource(context,uri)
        mrPlayer.playbackParams=PlaybackParams()
            .setPitch(2f.pow(mrPitchSemitone/12f))
            .setSpeed(mrTempoPercent/100f)
        mrPlayer.setOnCompletionListener{mrPlaying=false;mrPlayer.reset();if(BuildConfig.DEBUG)Log.d("VOCAL_MR_REAL","$source MR completed")}
        mrPlayer.prepare()
        if(BuildConfig.DEBUG) Log.d("VOCAL_MR_REAL","$source MR prepared pitch=$mrPitchSemitone tempo=$mrTempoPercent")
        mrPlayer.start()
        mrPlaying=true
        if(BuildConfig.DEBUG) Log.d("VOCAL_MR_REAL","$source MR started pitch=$mrPitchSemitone tempo=$mrTempoPercent")
    }
    fun releaseRecorder(){runCatching{recorder?.reset()};runCatching{recorder?.release()};recorder=null}
    fun resetAttempt(){stopAudio();stopFeedback();stopMr();if(recording)runCatching{recorder?.stop()};releaseRecorder();recording=false;file?.delete();file=null;feedbackFile?.delete();feedbackFile=null;duration=0.0;elapsed=0;direct=false;localError=null;viewModel.reset()}
    fun start(){stopAudio();stopFeedback();viewModel.reset();localError=null;if(mode=="MR"&&selectedMrUri==null){localError="먼저 MR 반주 파일을 선택해주세요.";return};val target=File(context.cacheDir,"vocal-${System.currentTimeMillis()}.m4a");@Suppress("DEPRECATION") val r=if(Build.VERSION.SDK_INT>=31)MediaRecorder(context)else MediaRecorder();runCatching{r.setAudioSource(MediaRecorder.AudioSource.MIC);r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);r.setAudioEncodingBitRate(192_000);r.setAudioSamplingRate(44_100);r.setAudioChannels(1);r.setOutputFile(target.absolutePath);r.prepare();r.start();if(BuildConfig.DEBUG)Log.d("VOCAL_MR_REAL","microphone recording started mode=$mode");if(mode=="MR")selectedMrUri?.let{startMrPlayback(it,"recording")}}.onSuccess{recorder=r;file=target;mime="audio/mp4";direct=true;startedAt=SystemClock.elapsedRealtime();elapsed=0;recording=true}.onFailure{runCatching{r.release()};target.delete();stopMr();localError="녹음을 시작하지 못했습니다. 마이크를 확인해주세요."}}
    fun stop(){if(!recording)return;stopMr();val seconds=(SystemClock.elapsedRealtime()-startedAt)/1000.0;val ok=runCatching{recorder?.stop()}.isSuccess;releaseRecorder();recording=false;duration=seconds;file?.let{if(BuildConfig.DEBUG)Log.d("VOCAL_SAVE_DEBUG","source=${it.absolutePath} exists=${it.exists()} size=${it.length()} extension=${it.extension} mime=$mime")};if(!ok||seconds<1.8||file?.length()?:0<4000){file?.delete();file=null;localError="2초 이상 노래를 녹음해주세요."}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)start()else localError="보컬 녹음을 위해 마이크 권한이 필요합니다."}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)runCatching{val detected=context.contentResolver.getType(uri).orEmpty();val ext=when{detected.contains("mpeg")->"mp3";detected.contains("wav")->"wav";detected.contains("webm")->"webm";else->"m4a"};val target=File(context.cacheDir,"vocal-upload-${System.currentTimeMillis()}.$ext");context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{input.copyTo(it)}}?:error("empty");val mr=MediaMetadataRetriever();mr.setDataSource(context,uri);val sec=(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0)/1000.0;mr.release();require(sec in 1.8..120.5&&target.length() in 4000..8L*1024*1024);resetAttempt();file=target;duration=sec;elapsed=sec.toLong();mime=detected.ifBlank{if(ext=="mp3")"audio/mpeg" else "audio/mp4"};direct=false}.onFailure{localError="2초~2분, 8MB 이하의 MP3·WAV·M4A·WebM 파일을 선택해주세요."}}
    val mrPickerLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(BuildConfig.DEBUG) Log.d("VOCAL_MR_REAL", "picker callback uri=$uri")
        mrPickerCallback=if(uri==null)"NULL" else "URI"
        if(uri!=null){
            stopMr();mrFile?.delete();mrFile=null
            mrPitchSemitone=0;mrTempoPercent=100
            selectedMrUri=uri
            mrName="선택한 MR 파일"
            localError=null
            runCatching{context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}
                .onFailure{if(BuildConfig.DEBUG)Log.w("VOCAL_MR_REAL","persistable permission unavailable; keeping selected URI",it)}
            val detected=runCatching{context.contentResolver.getType(uri).orEmpty()}.getOrDefault("")
            selectedMrMime=detected.takeIf{it.isNotBlank()}
            mrName=runCatching{
                context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cursor->
                    if(cursor.moveToFirst())cursor.getString(0) else null
                }
            }.onFailure{if(BuildConfig.DEBUG)Log.w("VOCAL_MR_REAL","display name query failed; keeping selected URI",it)}
                .getOrNull()?.takeIf{it.isNotBlank()}?:"선택한 MR 파일"
            if(BuildConfig.DEBUG) Log.d("VOCAL_MR_DEBUG","selectedUri=$uri displayName=$mrName mime=${selectedMrMime.orEmpty()}")
            runCatching{
                val ext=when{detected.contains("mpeg",true)->"mp3";detected.contains("wav",true)->"wav";else->"m4a"}
                val target=File(context.cacheDir,"vocal-mr-${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{input.copyTo(it)}}?:error("empty")
                require(target.length() in 1..64L*1024*1024)
                mrFile=target
                if(BuildConfig.DEBUG) Log.d("VOCAL_MR_DEBUG","optional cache size=${target.length()}")
            }.onFailure{if(BuildConfig.DEBUG)Log.w("VOCAL_MR_DEBUG","optional MR cache failed; keeping selected URI",it)}
        }
    }
    LaunchedEffect(recording){while(recording){elapsed=(SystemClock.elapsedRealtime()-startedAt)/1000;if(elapsed>=120){stop();localError="최대 녹음시간 2분에 도달해 녹음을 완료했습니다."};delay(250)}}
    LaunchedEffect(state.requiresLogin){if(state.requiresLogin)onAuthExpired()}
    LaunchedEffect(state.speechBytes){val bytes=state.speechBytes?:return@LaunchedEffect;runCatching{feedbackFile?.delete();feedbackFile=File(context.cacheDir,"vocal-feedback-${System.currentTimeMillis()}.mp3").apply{writeBytes(bytes)};playFeedback()}.onFailure{localError="피드백 음성을 재생하지 못했습니다."}.also{viewModel.clearSpeech()}}
    BackHandler(recording){confirmExit=true}
    if(confirmExit)AlertDialog(onDismissRequest={confirmExit=false},title={Text("현재 녹음을 중단할까요?")},text={Text("녹음을 계속하거나 중단할 수 있습니다.")},confirmButton={TextButton(onClick={stop();confirmExit=false}){Text("녹음 중단")}},dismissButton={TextButton(onClick={confirmExit=false}){Text("계속 녹음")}})
    DisposableEffect(Unit){onDispose{if(recording)runCatching{recorder?.stop()};releaseRecorder();runCatching{audioPlayer.release()};runCatching{feedbackPlayer.release()};runCatching{mrPlayer.release()};feedbackFile?.delete();file?.delete();mrFile?.delete()}}

    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,18.dp,20.dp,32.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{AiTrainingHeader("🎤","AI 보컬트레이닝","노래를 불러보고 AI에게\n나의 보컬 표현을 들어보세요.")}
        item{VocalCard{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("🎵 연습 방식",fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=mode=="VOCAL_ONLY",onClick={if(mode=="MR")stopMr();mode="VOCAL_ONLY"},label={Text("🎤 목소리만")},modifier=Modifier.weight(1f));FilterChip(selected=mode=="MR",onClick={mode="MR"},label={Text("🎧 MR 반주와 함께")},modifier=Modifier.weight(1f))}}}}
        if(mode=="MR")item{VocalCard{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text("🎧 MR 반주 준비",fontWeight=FontWeight.Bold);OutlinedButton(enabled=!recording&&!state.analyzing,onClick={mrPickerLaunched=true;mrPickerCallback="NONE";mrPickerLauncher.launch(arrayOf("audio/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.FolderOpen,null);Text(if(selectedMrUri==null)" MR 파일 선택" else " 다른 MR 선택")};if(selectedMrUri!=null){Text("선택된 MR",fontWeight=FontWeight.Bold);Text(mrName.orEmpty(),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);HorizontalDivider();Text("🎵 MR 연습 설정",fontWeight=FontWeight.Bold);MrSettingRow(label="키",value=if(mrPitchSemitone==0)"원키" else "%+d키".format(mrPitchSemitone),decreaseEnabled=!recording&&!state.analyzing&&mrPitchSemitone>-6,increaseEnabled=!recording&&!state.analyzing&&mrPitchSemitone<6,onDecrease={stopMr();mrPitchSemitone--},onIncrease={stopMr();mrPitchSemitone++});MrSettingRow(label="템포",value=if(mrTempoPercent==100)"원속도" else "$mrTempoPercent%",decreaseEnabled=!recording&&!state.analyzing&&mrTempoPercent>70,increaseEnabled=!recording&&!state.analyzing&&mrTempoPercent<120,onDecrease={stopMr();mrTempoPercent-=5},onIncrease={stopMr();mrTempoPercent+=5});Text("현재 설정: ${if(mrPitchSemitone==0) "원키" else "%+d키".format(mrPitchSemitone)} · $mrTempoPercent%",style=MaterialTheme.typography.bodySmall,color=AppActionButton,fontWeight=FontWeight.SemiBold);TextButton(enabled=!recording&&!state.analyzing&&(mrPitchSemitone!=0||mrTempoPercent!=100),onClick={stopMr();mrPitchSemitone=0;mrTempoPercent=100},modifier=Modifier.align(Alignment.End)){Icon(Icons.Default.Refresh,null);Text(" 원래 설정으로 초기화")};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(enabled=!recording&&!mrPlaying,onClick={selectedMrUri?.let{Log.d("VOCAL_MR_REAL","play clicked");stopAudio();stopFeedback();runCatching{startMrPlayback(it,"preview")}.onFailure{stopMr();localError="MR 파일을 재생하지 못했습니다.";Log.e("VOCAL_MR_REAL","playback failed",it)}}}){Icon(Icons.Default.PlayArrow,null);Text(" MR 재생")};OutlinedButton(enabled=!recording&&mrPlaying,onClick={stopMr()}){Icon(Icons.Default.Stop,null);Text(" MR 정지")}}};Text("🎧 이어폰 사용을 권장합니다. MR 소리가 마이크에 다시 녹음되는 것을 줄일 수 있습니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("MR 재생과 보컬 녹음은 동시에 시작되며, 분석은 녹음된 보컬을 중심으로 진행됩니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        item{VocalCard{Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(if(recording)Icons.Default.Stop else if(file!=null)Icons.Default.CheckCircle else Icons.Default.Mic,null,tint=AppActionButton,modifier=Modifier.size(38.dp));Spacer(Modifier.height(8.dp));Text(if(recording)"🎤 녹음 중입니다" else if(file!=null)"🎵 녹음 준비 완료" else "🎤 노래를 불러보세요",fontWeight=FontWeight.Bold);Text("%02d:%02d".format(elapsed/60,elapsed%60),style=MaterialTheme.typography.headlineMedium,color=AppActionButton);Text("2초 이상 · 최대 2분 · 최대 8MB",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Button(enabled=!state.analyzing,onClick={if(recording)stop()else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)start()else permission.launch(Manifest.permission.RECORD_AUDIO)},colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){Icon(if(recording)Icons.Default.Stop else Icons.Default.Mic,null);Spacer(Modifier.width(6.dp));Text(if(recording)"녹음 완료" else "녹음 시작")};TextButton(enabled=!recording&&!state.analyzing,onClick={picker.launch(arrayOf("audio/mpeg","audio/wav","audio/mp4","audio/webm","video/webm"))}){Icon(Icons.Default.FolderOpen,null);Text(" 녹음파일 불러오기")};if(file!=null){Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onClick={if(playing)stopAudio()else file?.let{stopFeedback();runCatching{audioPlayer.setDataSource(it.absolutePath);audioPlayer.setOnCompletionListener{playing=false;audioPlayer.reset()};audioPlayer.prepare();audioPlayer.start();playing=true}.onFailure{localError="녹음파일을 재생하지 못했습니다."}}}){Icon(if(playing)Icons.Default.Stop else Icons.Default.PlayArrow,null);Text(if(playing)" 멈춤" else " 듣기")};if(direct)OutlinedButton(onClick={file?.let{source->scope.launch{val where=withContext(Dispatchers.IO){saveVocal(context,source)};Toast.makeText(context,if(where!=null)"녹음파일을 저장했습니다.\n$where" else "녹음파일을 저장하지 못했습니다.",Toast.LENGTH_SHORT).show()}}}){Icon(Icons.Default.Download,null);Text(" 저장")}};TextButton(onClick={resetAttempt()}){Icon(Icons.Default.Refresh,null);Text(" 다시 녹음")}}}}}
        item {
            VocalCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("🎤 보컬트레이닝 방법", fontWeight = FontWeight.Bold)
                    Text("① 노래를 녹음해보세요.  ② 녹음을 들어보세요.")
                    Text(
                        "③ AI 분석을 요청하고  ④ 보컬 피드백을 확인하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if(file!=null)item{Button(enabled=!recording&&!state.analyzing&&(mode!="MR"||selectedMrUri!=null),onClick={if(BuildConfig.DEBUG)Log.d("VOCAL_FLOW","AI ANALYZE BUTTON CLICKED");stopAudio();stopFeedback();stopMr();file?.let{if(BuildConfig.DEBUG)Log.d("VOCAL_UPLOAD_DEBUG","source=${if(it.name.startsWith("vocal-upload")) "picked" else "recorded"} path=${it.absolutePath} uri=none filename=${it.name} extension=${it.extension} exists=${it.exists()} size=${it.length()} mime=$mime requestBodyMime=$mime multipartField=audio multipartFilename=${it.name}");viewModel.analyze(it,mime,duration,mode,if(mode=="MR")mrFile else null)}},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){if(state.analyzing)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary)else Icon(Icons.Default.AutoAwesome,null);Text(if(state.analyzing)"  노래를 분석하고 있어요..." else "  AI 보컬 분석")}}
        (state.error?:localError)?.let{message->item{VocalCard{Column(Modifier.padding(16.dp)){Text(message,color=MaterialTheme.colorScheme.onSurfaceVariant);if(state.error!=null)TextButton(onClick=viewModel::retry){Text("다시 시도")}}}}}
        state.result?.let{result->item{VocalCard{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("AI VOCAL ANALYSIS",style=MaterialTheme.typography.labelSmall,color=FreshDeepIndigo,fontWeight=FontWeight.Bold);Text("🎤 보컬 점수 ${result.overallScore?:0}점",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=FreshDeepIndigo);Text("AI 기반 연습용 참고 점수입니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);result.scores?.let{Scores(it)};result.strength?.let{VocalFeedback("👍 잘된 점",it)};result.vocalPoint?.let{VocalFeedback("🎵 보컬 포인트",it)};result.nextChallenge?.let{VocalFeedback("🎯 다음 도전",it)};Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onClick={if(playing)stopAudio()else file?.let{stopFeedback();audioPlayer.setDataSource(it.absolutePath);audioPlayer.setOnCompletionListener{playing=false;audioPlayer.reset()};audioPlayer.prepare();audioPlayer.start();playing=true}}){Text(if(playing)"■ 녹음 멈춤" else "▶ 내 녹음")};if(state.speechLoading)CircularProgressIndicator(Modifier.size(24.dp))else if(feedbackFile!=null)OutlinedButton(onClick={if(feedbackPlaying)stopFeedback()else playFeedback()}){Text(if(feedbackPlaying)"■ 피드백 멈춤" else "🔊 피드백 듣기")}};state.speechError?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Button(onClick={resetAttempt()},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=AppActionButton)){Text("🎤 다시 연습하기")}}}}}
        item{Text("녹음 음성은 AI 분석을 위해 일시적으로 전송되며, 우치소 서버에 장기 보관하지 않습니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable private fun VocalCard(content: @Composable () -> Unit)=Surface(color=FreshSurfaceVariant,shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),border=BorderStroke(1.dp,FreshOutline),modifier=Modifier.fillMaxWidth(),content=content)
@Composable private fun MrSettingRow(label:String,value:String,decreaseEnabled:Boolean,increaseEnabled:Boolean,onDecrease:()->Unit,onIncrease:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.width(54.dp),fontWeight=FontWeight.SemiBold);IconButton(onClick=onDecrease,enabled=decreaseEnabled){Icon(Icons.Default.Remove,"$label 낮추기")};Text(value,Modifier.weight(1f),fontWeight=FontWeight.Bold,color=FreshDeepIndigo);IconButton(onClick=onIncrease,enabled=increaseEnabled){Icon(Icons.Default.Add,"$label 높이기")}}}
@Composable private fun Scores(s:VocalScoresDto){listOf("음정 안정감" to s.pitchStability,"리듬 안정감" to s.rhythmStability,"호흡" to s.breath,"발음 전달력" to s.pronunciation,"강약 표현" to s.dynamics,"감정 표현" to s.emotion,"전체 자연스러움" to s.naturalness).forEach{(label,value)->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.width(96.dp),style=MaterialTheme.typography.bodySmall);LinearProgressIndicator({value/100f},Modifier.weight(1f));Text(" $value")}}}
@Composable private fun VocalFeedback(title:String,text:String)=Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(title,fontWeight=FontWeight.Bold);Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)}}

private fun saveVocal(context:Context,source:File):String? {
    if(BuildConfig.DEBUG) Log.d("VOCAL_SAVE_DEBUG","save clicked source=${source.absolutePath} exists=${source.exists()} size=${source.length()} extension=${source.extension} mime=audio/mp4")
    return runCatching {
        val name="Woochiso_Vocal_"+SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(Date())+".m4a"
        if(Build.VERSION.SDK_INT>=29){
            val values=ContentValues().apply{
                put(MediaStore.MediaColumns.DISPLAY_NAME,name)
                put(MediaStore.MediaColumns.MIME_TYPE,"audio/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_MUSIC+"/Woochiso")
                put(MediaStore.MediaColumns.IS_PENDING,1)
            }
            val resolver=context.contentResolver
            val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)?:error("insert")
            resolver.openOutputStream(uri)?.use{out->source.inputStream().use{it.copyTo(out)}}?:error("stream")
            values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);resolver.update(uri,values,null,null)
            val projection=arrayOf(MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.SIZE,MediaStore.MediaColumns.MIME_TYPE)
            val saved=resolver.query(uri,projection,null,null,null)?.use{cursor->
                check(cursor.moveToFirst())
                Triple(cursor.getString(0),cursor.getLong(1),cursor.getString(2))
            }?:error("query")
            val reopen=resolver.openInputStream(uri)?.use{it.read()!=-1}==true
            check(saved.second>0&&reopen)
            if(BuildConfig.DEBUG) Log.d("VOCAL_SAVE_DEBUG","savedUri=$uri displayName=${saved.first} size=${saved.second} mime=${saved.third} reopen=$reopen")
            "Music/Woochiso"
        }else{
            val dir=File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Woochiso").apply{mkdirs()}
            val saved=source.copyTo(File(dir,name),false)
            check(saved.length()>0&&saved.inputStream().use{it.read()!=-1})
            if(BuildConfig.DEBUG) Log.d("VOCAL_SAVE_DEBUG","savedFile=${saved.absolutePath} size=${saved.length()} mime=audio/mp4 reopen=true")
            dir.absolutePath
        }
    }.onFailure{if(BuildConfig.DEBUG)Log.e("VOCAL_SAVE_DEBUG","save failed",it)}.getOrNull()
}
