package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.RecoveryActivityDto
import com.example.ui.theme.parseServerColor
import com.example.ui.viewmodel.RecoveryStage
import com.example.ui.viewmodel.RecoveryViewModel

private val RecoveryAccent=Color(0xFF74AFDD)

@Composable
fun RecoveryScreen(viewModel:RecoveryViewModel,onAuthExpired:()->Unit,showPageTitle:Boolean=true){
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context=LocalContext.current
    LaunchedEffect(Unit){viewModel.load()}
    LaunchedEffect(Unit){viewModel.messages.collect{Toast.makeText(context,it,Toast.LENGTH_SHORT).show()}}
    LaunchedEffect(state.requiresLogin){if(state.requiresLogin)onAuthExpired()}
    Box(Modifier.fillMaxSize()){
        when{
            state.isLoading&&state.overview==null->CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error!=null&&state.overview==null->Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Text(state.error.orEmpty(),color=MaterialTheme.colorScheme.error);TextButton(onClick=viewModel::load){Text("다시 시도")}}
            else->LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                item{if(showPageTitle){Text("감정회복",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp))};Text("오늘 나에게 도움이 될 수 있는 회복 활동을 선택해보세요.",style=MaterialTheme.typography.titleMedium,color=RecoveryAccent);Spacer(Modifier.height(6.dp));Text("활동 전과 활동 후의 감정을 기록하면 나에게 어떤 활동이 도움이 되었는지 확인할 수 있습니다.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(14.dp));Surface(color=Color(0xFFEAF2FB),shape=RoundedCornerShape(14.dp)){Text("오늘 ${state.overview?.todayCount?:0}개 실천",Modifier.padding(12.dp),fontWeight=FontWeight.Bold,color=RecoveryAccent)};Spacer(Modifier.height(8.dp));Text("오늘의 회복 활동",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                items(state.overview?.activities.orEmpty(),key={it.activityId}){activity->RecoveryActivityCard(activity){viewModel.select(activity)}}
                if(state.overview?.recentRecords?.isNotEmpty()==true){item{Spacer(Modifier.height(8.dp));Text("최근 회복 기록",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};items(state.overview!!.recentRecords,key={it.recoverySessionId}){record->Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text("${record.iconText} ${record.activityName}",fontWeight=FontWeight.Bold);Text(record.completedAt.take(10).replace('-','.'),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("활동 전: ${dominantEmotion(record.beforeEmotions,state.overview!!.categories)} · 활동 후: ${dominantEmotion(record.afterEmotions,state.overview!!.categories)}",style=MaterialTheme.typography.bodySmall);if(record.memo.isNotBlank())Text(record.memo,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
            }
        }
    }
    val activity=state.selectedActivity
    if(activity!=null&&state.stage in listOf(RecoveryStage.BEFORE,RecoveryStage.AFTER))EmotionScoreDialog(title=if(state.stage==RecoveryStage.BEFORE)"활동 전 감정" else "활동 후 감정",question=if(state.stage==RecoveryStage.BEFORE)"이 활동을 하기 전 지금 어떤 감정을 느끼고 있나요?" else "활동 후 지금 어떤 감정을 느끼고 있나요?",state=state,viewModel=viewModel,onSave=if(state.stage==RecoveryStage.BEFORE)viewModel::saveBefore else viewModel::saveAfter)
    if(activity!=null&&state.stage==RecoveryStage.ACTIVITY)AlertDialog(onDismissRequest=viewModel::dismissOverlay,title={Text("✓ 활동 전 기록 완료")},text={Column{Text(activity.activityName,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("회복 활동을 실천해보세요.");Spacer(Modifier.height(12.dp));Button(onClick=viewModel::showAfter,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=RecoveryAccent)){Text("활동 후 감정 기록",color=Color.White)}}},confirmButton={TextButton(onClick=viewModel::dismissOverlay){Text("닫기")}})
    if(activity!=null&&state.stage==RecoveryStage.COMPLETED){val session=activity.todaySession;AlertDialog(onDismissRequest=viewModel::dismissOverlay,title={Text("✓ 회복 활동 기록 완료")},text={Column{Text(activity.activityName,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("활동 전");Text(dominantEmotion(session?.beforeEmotions.orEmpty(),state.overview?.categories.orEmpty()),fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));Text("활동 후");Text(dominantEmotion(session?.afterEmotions.orEmpty(),state.overview?.categories.orEmpty()),fontWeight=FontWeight.SemiBold);if(state.memo.isNotBlank()){Spacer(Modifier.height(10.dp));Text("메모");Text(state.memo,color=MaterialTheme.colorScheme.onSurfaceVariant)}}},confirmButton={TextButton(onClick=viewModel::dismissOverlay){Text("확인")}})}
}

@Composable private fun RecoveryActivityCard(activity:RecoveryActivityDto,onClick:()->Unit){val status=activity.todaySession?.status;Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(activity.iconText,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(activity.activityName,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(activity.shortAction,style=MaterialTheme.typography.labelMedium,color=RecoveryAccent)};Surface(color=Color(0xFFEAF2FB),shape=RoundedCornerShape(8.dp)){Text(when(status){"STARTED"->"활동 후 기록";"COMPLETED"->"오늘 완료";else->"오늘 체크"},Modifier.padding(horizontal=9.dp,vertical=5.dp),color=RecoveryAccent,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall)}};Spacer(Modifier.height(8.dp));Text(activity.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun EmotionScoreDialog(title:String,question:String,state:com.example.ui.viewmodel.RecoveryUiState,viewModel:RecoveryViewModel,onSave:()->Unit){AlertDialog(onDismissRequest={if(!state.isSaving)viewModel.cancelEditor()},title={Text(title)},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()).imePadding()){Text(question);Spacer(Modifier.height(8.dp));state.overview?.categories.orEmpty().forEach{category->val value=state.scores[category.categoryCode]?:50;Text("${category.categoryHanja} ${category.categoryName}  $value",fontWeight=FontWeight.SemiBold,color=parseServerColor(category.colorCode));Slider(value=value.toFloat(),onValueChange={viewModel.setScore(category.categoryCode,it.toInt())},valueRange=0f..100f,steps=19,enabled=!state.isSaving)};if(state.stage==RecoveryStage.AFTER){Text("오늘 활동 후 느낀 점",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));OutlinedTextField(value=state.memo,onValueChange=viewModel::setMemo,placeholder={Text("활동을 하면서 느낀 점을 자유롭게 적어보세요.")},supportingText={Text("${state.memo.length}/1000 · 선택 입력")},minLines=3,maxLines=6,enabled=!state.isSaving,modifier=Modifier.fillMaxWidth())}}},confirmButton={Button(onClick=onSave,enabled=!state.isSaving,colors=ButtonDefaults.buttonColors(containerColor=RecoveryAccent)){if(state.isSaving)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Color.White)else Text(if(state.stage==RecoveryStage.BEFORE)"활동 전 저장" else "완료 저장",color=Color.White)}},dismissButton={TextButton(onClick=viewModel::cancelEditor,enabled=!state.isSaving){Text("취소")}})}

private fun dominantEmotion(values:Map<String,Int>,categories:List<com.example.data.remote.dto.RecoveryCategoryDto>):String{val code=values.maxByOrNull{it.value}?.key?:return "-";return categories.firstOrNull{it.categoryCode==code}?.categoryName?:code}
