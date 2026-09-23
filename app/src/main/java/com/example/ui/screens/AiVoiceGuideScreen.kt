package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.TtsUsageViewModel

private val VoiceBlue=Color(0xFF064AD6)

@Composable fun AiVoiceUsageCard(viewModel:TtsUsageViewModel,onOpenGuide:()->Unit){
    val state by viewModel.state.collectAsStateWithLifecycle();LaunchedEffect(Unit){viewModel.load()}
    Card(onClick=onOpenGuide,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFF5F9FC)),border=BorderStroke(1.dp,Color(0xFFCBD5E1))){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text("AI 음성 이용안내",fontWeight=FontWeight.Bold,color=VoiceBlue)
        state.usage?.let{u->if(u.limited){val shown=minOf(u.used,u.limit?:u.used);Text("오늘 AI 음성 $shown / ${u.limit}회",fontWeight=FontWeight.SemiBold);if(u.remaining==0)Text("오늘 사용할 수 있는 AI 음성을 모두 사용했습니다. 텍스트 기능은 계속 이용하실 수 있어요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}else Text("AI 음성 이용 가능",style=MaterialTheme.typography.bodyMedium)}
        if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(state.error!=null)Text(state.error!!,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("자세히 보기  ›",style=MaterialTheme.typography.labelMedium,color=Color(0xFF74AFDD))
    }}
}

@Composable fun AiVoiceGuideScreen(viewModel:TtsUsageViewModel){
    LaunchedEffect(Unit){viewModel.load()}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,16.dp,20.dp,36.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{AiVoiceUsageCard(viewModel,{})}
        item{Text("우치소의 AI 음성은 외부 음성 생성 서비스를 이용하여 제공됩니다.",style=MaterialTheme.typography.bodyLarge);Spacer(Modifier.height(8.dp));Text("FREE 회원은 하루 최대 30회의 AI 음성을 이용할 수 있습니다.",fontWeight=FontWeight.Bold,color=VoiceBlue)}
        item{GuideCard("이용 기준",listOf("AI 상담과 AI 트레이닝의 AI 음성 사용량은 모두 합산됩니다.","새로운 AI 음성이 생성될 때 1회 사용됩니다.","이미 생성된 동일한 음성을 다시 듣는 경우 새 음성을 생성하지 않으면 추가되지 않습니다.","음성 생성에 실패한 경우 사용 횟수에 포함되지 않습니다.","하루 사용 횟수를 모두 사용해도 텍스트 AI 기능은 계속 이용할 수 있습니다.","AI 음성 사용 횟수는 매일 자정(한국시간)에 초기화됩니다."))}
        item{GuideCard("사용 예시",listOf("AI 상담 10회 + AI 소개팅 8회 + AI 토론연습 7회 + AI 재치와 센스 5회 = 총 30회"))}
        item{Text("오늘 사용할 수 있는 AI 음성을 모두 사용한 경우에도 텍스트 기능은 계속 이용하실 수 있습니다. AI 음성은 다음 날 다시 이용할 수 있습니다.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable private fun GuideCard(title:String,items:List<String>){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Color(0xFFCBD5E1)),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title,fontWeight=FontWeight.Bold,color=VoiceBlue);items.forEach{Text("• $it",style=MaterialTheme.typography.bodyMedium,lineHeight=MaterialTheme.typography.bodyMedium.lineHeight)}}}}
