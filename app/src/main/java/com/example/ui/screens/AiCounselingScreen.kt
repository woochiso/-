package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.CounselingMessageDto
import com.example.ui.viewmodel.CounselingStage
import com.example.ui.viewmodel.CounselingViewModel
import kotlinx.coroutines.flow.first

private val CounselingAccent=Color(0xFF74AFDD)

@Composable
fun AiCounselingScreen(viewModel:CounselingViewModel,onAuthExpired:()->Unit,showPageTitle:Boolean=true){
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmNew by remember{mutableStateOf(false)}
    var deleteCandidate by remember{mutableStateOf<com.example.data.remote.dto.CounselingSessionDto?>(null)}
    val snackbarHostState=remember{SnackbarHostState()}
    LaunchedEffect(Unit){viewModel.load()}
    LaunchedEffect(state.requiresLogin){if(state.requiresLogin)onAuthExpired()}
    LaunchedEffect(state.notice){state.notice?.let{snackbarHostState.showSnackbar(it);viewModel.consumeNotice()}}
    BackHandler(enabled=state.stage!=CounselingStage.START){viewModel.navigateBack()}
    Box(Modifier.fillMaxSize()){
        when(state.stage){
            CounselingStage.START->CounselingStart(state.loading,state.error,state.sessions,state.deletingSessionId,viewModel::freeTalk,viewModel::chooseToday,viewModel::chooseStory,viewModel::openSession,{deleteCandidate=it},showPageTitle)
            CounselingStage.TODAY->CounselingToday(state.today,viewModel::startToday,viewModel::newCounseling)
            CounselingStage.STORIES->CounselingStories(state.stories,state.selectedStory?.storyId,viewModel::selectStory,viewModel::startStory,viewModel::newCounseling)
            CounselingStage.CHAT, CounselingStage.HISTORY->CounselingChat(state.messages,state.input,state.sending,state.aiError,viewModel::setInput,viewModel::send,viewModel::retry,viewModel::finish,{confirmNew=true},showPageTitle)
        }
        SnackbarHost(snackbarHostState,Modifier.align(Alignment.BottomCenter))
    }
    if(confirmNew)AlertDialog(onDismissRequest={confirmNew=false},title={Text("새 상담을 시작할까요?")},text={Text("현재 상담은 서버에 저장된 내용까지 유지됩니다.")},dismissButton={TextButton(onClick={confirmNew=false}){Text("취소")}},confirmButton={TextButton(onClick={confirmNew=false;viewModel.newCounseling()}){Text("새 상담 시작")}})
    deleteCandidate?.let{session->AlertDialog(onDismissRequest={if(state.deletingSessionId==null)deleteCandidate=null},title={Text("상담 기록을 삭제할까요?")},text={Text("이 상담의 대화 내용이 모두 삭제됩니다.\n삭제한 상담은 복구할 수 없습니다.")},dismissButton={TextButton(onClick={deleteCandidate=null},enabled=state.deletingSessionId==null){Text("취소")}},confirmButton={TextButton(onClick={deleteCandidate=null;viewModel.deleteSession(session.sessionId)},enabled=state.deletingSessionId==null,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("삭제")}})}
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

@Composable private fun CounselingChat(messages:List<CounselingMessageDto>,input:String,sending:Boolean,aiError:String?,setInput:(String)->Unit,send:()->Unit,retry:()->Unit,finish:()->Unit,newChat:()->Unit,showPageTitle:Boolean){
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
        LazyColumn(state=listState,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp),userScrollEnabled=true){items(messages,key={it.messageId}){message->MessageBubble(message)};if(sending)item{Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp));Text("AI가 답변을 작성하고 있습니다...",style=MaterialTheme.typography.bodySmall)}};aiError?.let{item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)){Column(Modifier.padding(12.dp)){Text("답변을 불러오지 못했습니다.",color=MaterialTheme.colorScheme.onErrorContainer);Text(it,style=MaterialTheme.typography.bodySmall);TextButton(onClick=retry,enabled=!sending){Text("다시 시도")}}}}}}
        TextButton(onClick=finish,enabled=!sending&&messages.any{it.role=="USER"},modifier=Modifier.align(Alignment.End).padding(horizontal=12.dp)){Text("상담 마무리")}
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(value=input,onValueChange=setInput,placeholder={Text("메시지를 입력하세요...")},maxLines=5,enabled=!sending,modifier=Modifier.weight(1f));FilledIconButton(onClick=send,enabled=input.isNotBlank()&&!sending,colors=IconButtonDefaults.filledIconButtonColors(containerColor=CounselingAccent)){Icon(Icons.Default.Send,"전송",tint=Color.White)}}
    }
}

@Composable private fun MessageBubble(message:CounselingMessageDto){val user=message.role=="USER";Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user)Arrangement.End else Arrangement.Start){Surface(color=if(user)CounselingAccent else Color(0xFFF0F4F8),shape=RoundedCornerShape(16.dp),modifier=Modifier.widthIn(max=300.dp)){Text(message.content,Modifier.padding(horizontal=13.dp,vertical=10.dp),color=if(user)Color.White else MaterialTheme.colorScheme.onSurface)}}}
