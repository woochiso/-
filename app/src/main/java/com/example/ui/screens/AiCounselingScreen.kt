package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.ui.theme.AppActionButton
import com.example.ui.theme.FreshTextMuted
import com.example.ui.components.VoiceRecordingPanel
import com.example.data.remote.dto.CounselingMessageDto
import com.example.data.remote.dto.CounselingCrisisDto
import com.example.ui.viewmodel.CounselingStage
import com.example.ui.viewmodel.CounselingViewModel
import com.example.ui.viewmodel.TtsUsageViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CounselingAccent=Color(0xFF74AFDD)
private enum class VoiceRecognitionState{IDLE,LISTENING,PROCESSING}

@Composable
fun AiCounselingScreen(viewModel:CounselingViewModel,usageViewModel:TtsUsageViewModel,onOpenVoiceGuide:()->Unit,onAuthExpired:()->Unit,showPageTitle:Boolean=true){
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmNew by remember{mutableStateOf(false)}
    var deleteCandidate by remember{mutableStateOf<com.example.data.remote.dto.CounselingSessionDto?>(null)}
    val snackbarHostState=remember{SnackbarHostState()}
    LaunchedEffect(Unit){viewModel.load()}
    LaunchedEffect(state.requiresLogin){if(state.requiresLogin)onAuthExpired()}
    LaunchedEffect(state.notice){state.notice?.let{snackbarHostState.showSnackbar(it);viewModel.consumeNotice()}}
    BackHandler(enabled=state.stage!=CounselingStage.MODE){viewModel.navigateBack()}
    Box(Modifier.fillMaxSize()){
        when(state.stage){
            CounselingStage.MODE->CounselingModeSelection(state.sessions.size,viewModel::chooseTextMode,viewModel::chooseVoiceMode,viewModel::showHistory,usageViewModel,onOpenVoiceGuide,showPageTitle)
            CounselingStage.START->CounselingStart(state.loading,state.error,state.sessions,state.deletingSessionId,viewModel::freeTalk,viewModel::chooseToday,viewModel::chooseStory,viewModel::openSession,{deleteCandidate=it},showPageTitle)
            CounselingStage.TODAY->CounselingToday(state.today,viewModel::startToday,viewModel::newCounseling)
            CounselingStage.STORIES->CounselingStories(state.stories,state.selectedStory?.storyId,viewModel::selectStory,viewModel::startStory,viewModel::newCounseling)
            CounselingStage.HISTORY->CounselingHistory(state.loading,state.error,state.sessions,state.deletingSessionId,viewModel::openSession,{deleteCandidate=it})
            CounselingStage.CHAT->if(state.voiceMode) VoiceCounselingChat(state.messages,state.sending,state.aiError,state.crisis,state.speechLoading,state.speechBytes,state.speechMessageId,state.speechError,viewModel::sendRecognized,viewModel::retry,viewModel::requestGreetingSpeech,viewModel::requestSpeech,viewModel::clearSpeech,viewModel::newCounseling,onAuthExpired,showPageTitle) else CounselingChat(state.messages,state.input,state.sending,state.aiError,state.crisis,viewModel::setInput,viewModel::send,viewModel::retry,viewModel::finish,{confirmNew=true},showPageTitle)
        }
        SnackbarHost(snackbarHostState,Modifier.align(Alignment.BottomCenter))
    }
    if(confirmNew)AlertDialog(onDismissRequest={confirmNew=false},title={Text("새 상담을 시작할까요?")},text={Text("현재 상담은 서버에 저장된 내용까지 유지됩니다.")},dismissButton={TextButton(onClick={confirmNew=false}){Text("취소")}},confirmButton={TextButton(onClick={confirmNew=false;viewModel.newCounseling()}){Text("새 상담 시작")}})
    deleteCandidate?.let{session->AlertDialog(onDismissRequest={if(state.deletingSessionId==null)deleteCandidate=null},title={Text("상담 기록을 삭제할까요?")},text={Text("이 상담의 대화 내용이 모두 삭제됩니다.\n삭제한 상담은 복구할 수 없습니다.")},dismissButton={TextButton(onClick={deleteCandidate=null},enabled=state.deletingSessionId==null){Text("취소")}},confirmButton={TextButton(onClick={deleteCandidate=null;viewModel.deleteSession(session.sessionId)},enabled=state.deletingSessionId==null,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("삭제")}})}
}

@Composable private fun CounselingModeSelection(historyCount:Int,text:()->Unit,voice:()->Unit,history:()->Unit,usageViewModel:TtsUsageViewModel,onOpenVoiceGuide:()->Unit,showPageTitle:Boolean){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,18.dp,20.dp,32.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{if(showPageTitle)Text("AI 상담",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Text("글이나 목소리로 지금 마음에 대해 이야기해보세요.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Text("상담 방식을 선택해주세요",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
        item{AiVoiceUsageCard(usageViewModel,onOpenVoiceGuide)}
        item{StartOption("텍스트 상담","메시지로 천천히 마음을 나눠보세요.",text)}
        item{StartOption("음성 상담","말로 이야기하고 AI 답변을 음성으로 들어보세요.",voice)}
        item{StartOption("이전 상담","저장된 상담 기록 ${historyCount}개를 확인합니다.",history)}
        item{Surface(color=Color(0xFFF4F7FA),shape=RoundedCornerShape(12.dp)){Text("AI 상담은 의료 진단이나 전문적인 심리치료를 대신하지 않습니다.",Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
    }
}

@Composable private fun CounselingHistory(loading:Boolean,error:String?,sessions:List<com.example.data.remote.dto.CounselingSessionDto>,deletingSessionId:Long?,open:(Long)->Unit,requestDelete:(com.example.data.remote.dto.CounselingSessionDto)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("이전 상담",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("저장된 상담 기록을 이어서 확인할 수 있습니다.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(loading)item{CircularProgressIndicator()}
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
        if(!loading&&sessions.isEmpty())item{Text("아직 저장된 상담 기록이 없습니다.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        items(sessions,key={it.sessionId}){session->Card(onClick={open(session.sessionId)},modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){Row(Modifier.fillMaxWidth().padding(start=14.dp,top=8.dp,bottom=8.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(session.title,fontWeight=FontWeight.SemiBold);Text(session.lastMessageAt.take(16).replace('-','.'),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={requestDelete(session)},enabled=deletingSessionId==null){if(deletingSessionId==session.sessionId)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Icon(Icons.Default.Delete,"${session.title} 상담 삭제")}}}}
    }
}

@Composable private fun CounselingStart(loading:Boolean,error:String?,sessions:List<com.example.data.remote.dto.CounselingSessionDto>,deletingSessionId:Long?,free:()->Unit,today:()->Unit,story:()->Unit,open:(Long)->Unit,requestDelete:(com.example.data.remote.dto.CounselingSessionDto)->Unit,showPageTitle:Boolean){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,18.dp,20.dp,32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{if(showPageTitle){Text("AI 상담",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp))};Text("마음속 이야기를 편하게 이야기해보세요.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(5.dp));Text("AI와 대화하며 감정을 정리하고 다른 관점에서 바라보는 연습을 해볼 수 있습니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Text("어떤 방식으로 이야기해볼까요?",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))}
        item{StartOption("그냥 이야기하기","지금 마음에 있는 이야기를 자유롭게 나눠보세요.",free)}
        item{StartOption("오늘 감정으로 시작","오늘 기록한 감정을 확인하고 동의한 뒤 시작합니다.",today)}
        item{StartOption("나의 사연으로 시작","직접 선택한 사연 하나만 참고해서 대화합니다.",story)}
        item{Surface(color=Color(0xFFF4F7FA),shape=RoundedCornerShape(12.dp)){Text("AI 상담은 감정을 정리하고 대화를 돕기 위한 기능이며 의료 진단이나 전문적인 심리치료를 대신하지 않습니다.",Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        if(loading)item{CircularProgressIndicator()}
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
        if(sessions.isNotEmpty()){item{Text("이전 상담",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))};items(sessions,key={it.sessionId}){session->Card(onClick={open(session.sessionId)},modifier=Modifier.fillMaxWidth(),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){Row(Modifier.fillMaxWidth().padding(start=14.dp,top=8.dp,bottom=8.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(session.title,fontWeight=FontWeight.SemiBold);Text(session.lastMessageAt.take(16).replace('-','.'),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={requestDelete(session)},enabled=deletingSessionId==null){if(deletingSessionId==session.sessionId)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else Icon(Icons.Default.Delete,"${session.title} 상담 삭제",tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
    }
}

@Composable private fun StartOption(title:String,description:String,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.Bold,color=CounselingAccent);Spacer(Modifier.height(4.dp));Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun CounselingToday(today:List<Pair<String,Int>>,start:()->Unit,back:()->Unit){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("오늘 감정으로 시작",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("오늘 기록한 감정을 참고해서 이야기해볼까요?",color=MaterialTheme.colorScheme.onSurfaceVariant)};item{Card(Modifier.fillMaxWidth(),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){Column(Modifier.padding(16.dp)){Text("오늘 기록한 감정",fontWeight=FontWeight.Bold);if(today.isEmpty())Text("오늘 기록한 감정이 없습니다.",color=MaterialTheme.colorScheme.onSurfaceVariant)else today.forEach{(name,count)->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text("${count}회",fontWeight=FontWeight.SemiBold)}}}}};item{Text("아래 버튼을 누르면 위 감정 기록을 상담 context로 사용하는 데 동의하게 됩니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick=start,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=CounselingAccent)){Text("감정 기록 참고해서 시작",color=Color.White)};TextButton(onClick=back,modifier=Modifier.fillMaxWidth()){Text("취소")}}}}

@Composable private fun CounselingStories(stories:List<com.example.data.remote.dto.StoryDto>,selected:Long?,select:(com.example.data.remote.dto.StoryDto)->Unit,start:()->Unit,back:()->Unit){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("나의 사연으로 시작",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("AI와 이야기할 사연 하나를 직접 선택해주세요.",color=MaterialTheme.colorScheme.onSurfaceVariant)};if(stories.isEmpty())item{Text("작성한 사연이 없습니다.")};items(stories,key={it.storyId}){story->Card(onClick={select(story)},modifier=Modifier.fillMaxWidth(),border=BorderStroke(if(selected==story.storyId)2.dp else 1.dp,if(selected==story.storyId)CounselingAccent else MaterialTheme.colorScheme.outlineVariant)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected==story.storyId,{select(story)});Column{Text(story.title,fontWeight=FontWeight.SemiBold);Text(story.createdAt.take(10),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}};item{selected?.let{Text("상담 시작을 누르면 선택한 사연 하나만 상담 context로 사용하는 데 동의하게 됩니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Button(onClick=start,enabled=selected!=null,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=CounselingAccent)){Text("상담 시작",color=Color.White)};TextButton(onClick=back,modifier=Modifier.fillMaxWidth()){Text("취소")}}}}

@Composable private fun CounselingChat(messages:List<CounselingMessageDto>,input:String,sending:Boolean,aiError:String?,crisis:CounselingCrisisDto?,setInput:(String)->Unit,send:()->Unit,retry:()->Unit,finish:()->Unit,newChat:()->Unit,showPageTitle:Boolean){
    val listState=rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var userDragged by remember{mutableStateOf(false)}
    var keepLatestVisible by remember{mutableStateOf(true)}
    LaunchedEffect(isDragged){
        if(isDragged){
            userDragged=true
        }else if(userDragged){
            snapshotFlow{listState.isScrollInProgress}.first{!it}
            val info=listState.layoutInfo
            keepLatestVisible=info.totalItemsCount==0||
                (info.visibleItemsInfo.lastOrNull()?.index?:-1)>=info.totalItemsCount-2
            userDragged=false
        }
    }
    val lastItemIndex=messages.lastIndex+(if(sending)1 else 0)+(if(aiError!=null)1 else 0)
    LaunchedEffect(messages.size,sending,aiError,keepLatestVisible){
        if(keepLatestVisible&&lastItemIndex>=0){
            withFrameNanos{}
            listState.animateScrollToItem(lastItemIndex)
        }
    }
    Column(Modifier.fillMaxSize().imePadding()){
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){if(showPageTitle)Text("AI 상담",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("마음속 이야기를 편하게 이야기해보세요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};TextButton(onClick=newChat){Icon(Icons.Default.Add,null);Text("새 상담")}}
        LazyColumn(state=listState,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp),userScrollEnabled=true){items(messages,key={it.messageId}){message->MessageBubble(message)};if(crisis?.showCard==true)item{CrisisSupportCard(crisis)};if(sending)item{Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp));Text("AI가 답변을 작성하고 있습니다...",style=MaterialTheme.typography.bodySmall)}};aiError?.let{item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)){Column(Modifier.padding(12.dp)){Text("답변을 불러오지 못했습니다.",color=MaterialTheme.colorScheme.onErrorContainer);Text(it,style=MaterialTheme.typography.bodySmall);TextButton(onClick=retry,enabled=!sending){Text("다시 시도")}}}}}}
        TextButton(onClick=finish,enabled=!sending&&messages.any{it.role=="USER"},modifier=Modifier.align(Alignment.End).padding(horizontal=12.dp)){Text("상담 마무리")}
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(value=input,onValueChange=setInput,placeholder={Text("메시지를 입력하세요...")},maxLines=5,enabled=!sending,modifier=Modifier.weight(1f));FilledIconButton(onClick=send,enabled=input.isNotBlank()&&!sending,colors=IconButtonDefaults.filledIconButtonColors(containerColor=CounselingAccent)){Icon(Icons.Default.Send,"전송",tint=Color.White)}}
    }
}

private val CounselingMarkdownBold=Regex("""\*\*([^*\n]+)\*\*""")
private fun displayCounselingContent(message:CounselingMessageDto):String =
    if(message.role=="ASSISTANT")CounselingMarkdownBold.replace(message.content){it.groupValues[1]} else message.content

@Composable private fun MessageBubble(message:CounselingMessageDto){val user=message.role=="USER";Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user)Arrangement.End else Arrangement.Start){Surface(color=if(user)CounselingAccent else Color(0xFFF0F4F8),shape=RoundedCornerShape(16.dp),modifier=Modifier.widthIn(max=300.dp)){Text(displayCounselingContent(message),Modifier.padding(horizontal=13.dp,vertical=10.dp),color=if(user)Color.White else MaterialTheme.colorScheme.onSurface)}}}

@Composable
private fun VoiceCounselingChat(
    messages:List<CounselingMessageDto>,sending:Boolean,aiError:String?,crisis:CounselingCrisisDto?,speechLoading:Boolean,
    speechBytes:ByteArray?,speechMessageId:Long?,speechError:String?,sendRecognized:(String)->Unit,
    retry:()->Unit,requestGreetingSpeech:()->Unit,requestSpeech:(CounselingMessageDto)->Unit,clearSpeech:()->Unit,endCounseling:()->Unit,onAuthExpired:()->Unit,showPageTitle:Boolean
){
    val context=LocalContext.current
    val lifecycleOwner=LocalLifecycleOwner.current
    val coroutineScope=rememberCoroutineScope()
    val listState=rememberLazyListState()
    var recognitionState by remember{mutableStateOf(VoiceRecognitionState.IDLE)}
    val listening=recognitionState==VoiceRecognitionState.LISTENING
    var committedTranscript by remember{mutableStateOf("")}
    var partialTranscript by remember{mutableStateOf("")}
    var userSpeakingSessionActive by remember{mutableStateOf(false)}
    var stopAfterFinalResult by remember{mutableStateOf(false)}
    var segmentRestartJob by remember{mutableStateOf<Job?>(null)}
    val recognized=listOf(committedTranscript,partialTranscript).filter{it.isNotBlank()}.joinToString(" ")
    var recognitionError by remember{mutableStateOf<String?>(null)}
    var playing by remember{mutableStateOf(false)}
    var counselingActive by remember{mutableStateOf(true)}
    var lastAudioFile by remember{mutableStateOf<java.io.File?>(null)}
    val mediaPlayer=remember{MediaPlayer()}
    LaunchedEffect(Unit){requestGreetingSpeech()}
    val recognizer=remember{if(SpeechRecognizer.isRecognitionAvailable(context))SpeechRecognizer.createSpeechRecognizer(context)else null}
    val recognitionIntent=remember{Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
        putExtra(RecognizerIntent.EXTRA_PROMPT,"편하게 말씀해주세요")
    }}
    fun appendFinalSegment(segment:String){
        val clean=segment.trim().replace(Regex("\\s+")," ")
        if(clean.isBlank())return
        committedTranscript=when{
            committedTranscript.isBlank()->clean
            committedTranscript.endsWith(clean)->committedTranscript
            clean.startsWith(committedTranscript)->clean
            else->"$committedTranscript $clean"
        }
        partialTranscript=""
        if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","VOICE_SEGMENT_RESULT chars=${clean.length} totalChars=${committedTranscript.length}")
    }
    fun sendCompletedTranscript(){
        val text=committedTranscript.trim()
        stopAfterFinalResult=false
        partialTranscript=""
        if(text.isNotBlank()){
            if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","VOICE_MESSAGE_SEND chars=${text.length}")
            committedTranscript=""
            sendRecognized(text)
        }else recognitionError="인식된 내용이 없습니다. 다시 말씀해주세요."
    }
    fun scheduleNextSegment(){
        segmentRestartJob?.cancel()
        segmentRestartJob=coroutineScope.launch{
            delay(550)
            if(userSpeakingSessionActive&&recognitionState==VoiceRecognitionState.IDLE&&!sending&&!playing&&!speechLoading){
                if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","VOICE_SEGMENT_RESTART")
                recognitionState=VoiceRecognitionState.PROCESSING
                runCatching{recognizer?.startListening(recognitionIntent)}.onFailure{
                    recognitionState=VoiceRecognitionState.IDLE
                    if(BuildConfig.DEBUG)Log.e("WOOCHISO_SPEECH","segment restart failure",it)
                    recognitionError="음성 인식을 다시 시작하지 못했습니다. 말하기 종료 후 다시 시도해주세요."
                }
            }
        }
    }
    DisposableEffect(recognizer){
        val listener=object:RecognitionListener{
            override fun onReadyForSpeech(params:Bundle?){recognitionState=VoiceRecognitionState.LISTENING;recognitionError=null;if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","VOICE_SEGMENT_START")}
            override fun onBeginningOfSpeech()=Unit
            override fun onRmsChanged(rmsdB:Float)=Unit
            override fun onBufferReceived(buffer:ByteArray?)=Unit
            override fun onEndOfSpeech(){recognitionState=VoiceRecognitionState.PROCESSING}
            override fun onError(error:Int){
                recognitionState=VoiceRecognitionState.IDLE
                if(BuildConfig.DEBUG)Log.w("WOOCHISO_SPEECH","recognition error code=$error")
                if(stopAfterFinalResult){appendFinalSegment(partialTranscript);sendCompletedTranscript();return}
                if(!userSpeakingSessionActive)return
                if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT||error==SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
                    recognitionError=if(error==SpeechRecognizer.ERROR_RECOGNIZER_BUSY)"음성 인식을 다시 준비하고 있습니다..." else null
                    scheduleNextSegment()
                }else{
                    userSpeakingSessionActive=false
                    recognitionError="음성을 인식하지 못했습니다. 다시 말씀해주세요."
                }
            }
            override fun onResults(results:Bundle?){
                appendFinalSegment(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
                recognitionState=VoiceRecognitionState.IDLE
                if(stopAfterFinalResult)sendCompletedTranscript() else if(userSpeakingSessionActive)scheduleNextSegment()
            }
            override fun onPartialResults(partialResults:Bundle?){partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{partialTranscript=it.trim()}}
            override fun onEvent(eventType:Int,params:Bundle?)=Unit
        }
        recognizer?.setRecognitionListener(listener)
        onDispose{segmentRestartJob?.cancel();userSpeakingSessionActive=false;recognizer?.cancel();recognizer?.destroy();recognitionState=VoiceRecognitionState.IDLE;runCatching{mediaPlayer.stop()};mediaPlayer.release();lastAudioFile?.delete()}
    }
    DisposableEffect(lifecycleOwner,recognizer,mediaPlayer){
        val observer=LifecycleEventObserver{_,event->
            if(event==Lifecycle.Event.ON_PAUSE){
                segmentRestartJob?.cancel();userSpeakingSessionActive=false;recognizer?.cancel();recognitionState=VoiceRecognitionState.IDLE
                if(playing){runCatching{mediaPlayer.pause()};playing=false}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}
    }
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted){committedTranscript="";partialTranscript="";userSpeakingSessionActive=true;stopAfterFinalResult=false;if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","VOICE_SESSION_START");recognitionState=VoiceRecognitionState.PROCESSING;recognizer?.startListening(recognitionIntent)}else recognitionError="음성 상담을 사용하려면 마이크 권한이 필요합니다."
    }
    fun startListening(){
        if(playing||sending||speechLoading||recognitionState!=VoiceRecognitionState.IDLE)return
        recognitionError=null
        committedTranscript="";partialTranscript="";userSpeakingSessionActive=true;stopAfterFinalResult=false
        if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","VOICE_SESSION_START")
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){recognitionState=VoiceRecognitionState.PROCESSING;recognizer?.startListening(recognitionIntent)}else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun stopUserSpeaking(){
        if(!userSpeakingSessionActive)return
        if(BuildConfig.DEBUG)Log.d("WOOCHISO_SPEECH","USER_SPEAKING_STOP")
        userSpeakingSessionActive=false;stopAfterFinalResult=true;segmentRestartJob?.cancel()
        when(recognitionState){
            VoiceRecognitionState.LISTENING,VoiceRecognitionState.PROCESSING->{recognitionState=VoiceRecognitionState.PROCESSING;recognizer?.stopListening()}
            VoiceRecognitionState.IDLE->{appendFinalSegment(partialTranscript);sendCompletedTranscript()}
        }
    }
    val latestAssistant=messages.lastOrNull{it.role=="ASSISTANT"&&it.messageId>0}
    LaunchedEffect(latestAssistant?.messageId){latestAssistant?.let(requestSpeech)}
    LaunchedEffect(speechBytes,speechMessageId){
        if(speechBytes!=null){
            val file=java.io.File(context.cacheDir,"counseling-${speechMessageId?:"reply"}.mp3")
            file.writeBytes(speechBytes)
            lastAudioFile?.takeIf{it!=file}?.delete();lastAudioFile=file
            segmentRestartJob?.cancel();userSpeakingSessionActive=false;recognizer?.cancel();recognitionState=VoiceRecognitionState.IDLE
            runCatching{mediaPlayer.reset();mediaPlayer.setDataSource(file.absolutePath);mediaPlayer.setOnCompletionListener{playing=false;recognitionError=if(counselingActive)"계속 말씀해주세요." else null;if(BuildConfig.DEBUG)Log.d("WOOCHISO_TTS","MEDIAPLAYER_COMPLETED")};mediaPlayer.prepare();if(BuildConfig.DEBUG)Log.d("WOOCHISO_TTS","MEDIAPLAYER_PREPARED");mediaPlayer.start();playing=true;if(BuildConfig.DEBUG)Log.d("WOOCHISO_TTS","MEDIAPLAYER_STARTED");recognitionError=null}.onFailure{if(BuildConfig.DEBUG)Log.e("WOOCHISO_TTS","MEDIAPLAYER_ERROR",it);recognitionError="AI 답변 음성을 재생하지 못했습니다. 계속 상담할 수 있습니다."}
            clearSpeech()
        }
    }
    val userReadingHistory by remember { derivedStateOf {
        val lastVisible=listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        listState.isScrollInProgress && lastVisible < messages.lastIndex - 1
    } }
    var keepLatestVisible by remember{mutableStateOf(true)}
    LaunchedEffect(userReadingHistory){if(userReadingHistory)keepLatestVisible=false else if(!listState.canScrollForward)keepLatestVisible=true}
    LaunchedEffect(messages.size,sending){if(messages.isNotEmpty()&&keepLatestVisible)listState.animateScrollToItem(messages.lastIndex+(if(sending)1 else 0))}
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if(showPageTitle) Text("AI 음성 상담",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("말로 이야기하고 답변을 음성으로 들어보세요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(state=listState,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            items(messages,key={it.messageId}){MessageBubble(it)}
            if(crisis?.showCard==true)item{CrisisSupportCard(crisis)}
            if(sending)item{Text("AI가 답변을 작성하고 있습니다...",style=MaterialTheme.typography.bodySmall)}
            aiError?.let{error->item{TextButton(onClick=retry){Text("다시 시도: $error")}}}
        }
        Surface(color=Color(0xFFF4F7FA),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp)) {
            Column(Modifier.padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(if(playing)"AI가 답변하고 있습니다..." else if(sending||speechLoading)"AI가 답변을 준비하고 있어요..." else "편하게 말씀해 주세요.",color=FreshTextMuted)
                speechError?.let{Text(it,color=FreshTextMuted,style=MaterialTheme.typography.bodySmall)}
                VoiceRecordingPanel(
                    enabled=!sending&&!speechLoading,
                    currentText="",
                    onTranscribed={text->if(text.isNotBlank())sendRecognized(text)},
                    onAuthExpired=onAuthExpired,
                    autoTranscribeOnStop=true,
                    compact=true,
                    onRecordingStarted={if(playing){runCatching{mediaPlayer.stop()};playing=false}}
                )
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    lastAudioFile?.takeIf{it.exists()}?.let {
                        OutlinedButton(onClick={if(playing){mediaPlayer.pause();playing=false}else{segmentRestartJob?.cancel();userSpeakingSessionActive=false;recognizer?.cancel();recognitionState=VoiceRecognitionState.IDLE;if(mediaPlayer.currentPosition>=mediaPlayer.duration)mediaPlayer.seekTo(0);mediaPlayer.start();playing=true}}) {
                            Icon(if(playing)Icons.Default.Stop else Icons.Default.PlayArrow,null)
                            Text(if(playing)"음성 중지" else "다시 듣기")
                        }
                    }
                    if(speechLoading) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick={counselingActive=false;segmentRestartJob?.cancel();userSpeakingSessionActive=false;recognizer?.cancel();recognitionState=VoiceRecognitionState.IDLE;if(playing)runCatching{mediaPlayer.stop()};playing=false;endCounseling()},
                    modifier=Modifier.fillMaxWidth(),
                    colors=ButtonDefaults.outlinedButtonColors(contentColor=AppActionButton),
                    border=BorderStroke(1.dp,AppActionButton)
                ){Icon(Icons.Default.Stop,null);Spacer(Modifier.width(6.dp));Text("음성 상담 종료")}
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun CrisisSupportCard(crisis:CounselingCrisisDto){
    val context=LocalContext.current
    fun dial(number:String){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:$number")))}
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFF0F2FF)),border=BorderStroke(1.dp,Color(0xFFBBC3F5)),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Text(if(crisis.level=="LEVEL_3")"지금은 안전을 먼저 확보해 주세요" else "혼자 감당하기 어렵다면 도움을 받을 수 있어요",fontWeight=FontWeight.Bold,color=Color(0xFF34418C))
            Text("자살예방 상담전화 109는 24시간 상담할 수 있습니다. 즉각적인 위험이나 부상이 있다면 119 또는 112에 연락해 주세요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                OutlinedButton(onClick={dial("109")},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("109")}
                OutlinedButton(onClick={dial("119")},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("119")}
                OutlinedButton(onClick={dial("112")},modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)){Text("112")}
            }
        }
    }
}
