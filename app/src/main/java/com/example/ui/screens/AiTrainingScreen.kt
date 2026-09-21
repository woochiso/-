package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppActionButton

private data class AiTrainingItem(val number:String,val icon:String,val title:String,val description:String,val tags:List<String>,val onClick:()->Unit)

@Composable
fun AiTrainingScreen(
    showPageTitle:Boolean=true,
    onOpenHumor:()->Unit,
    onOpenDating:()->Unit,
    onOpenEmotionExpression:()->Unit,
    onOpenQuiz:()->Unit,
    onOpenDebate:()->Unit,
    onOpenVocal:()->Unit,
    onOpenWit:()->Unit
) {
    val programs=listOf(
        AiTrainingItem("01","😂","AI를 웃겨라","재미있는 이야기를 말로 표현하고 유머 전달력과 표현력을 연습해보세요.",listOf("유머","말하기","표현력"),onOpenHumor),
        AiTrainingItem("02","💕","AI 소개팅","AI와 실제 소개팅처럼 대화하며 처음 만난 사람과 자연스럽게 이야기하는 연습을 해보세요.",listOf("대화","공감","센스"),onOpenDating),
        AiTrainingItem("03","💬","AI와 감정표현 연습","일상 속 다양한 상황에서 내 마음과 생각을 자연스럽게 표현하는 연습을 해보세요.",listOf("마음표현","배려","대화"),onOpenEmotionExpression),
        AiTrainingItem("04","🧠","AI 상식퀴즈","역사, 과학, 음악, 스포츠, 생활 등 다양한 분야의 상식을 재미있게 풀어보세요.",listOf("상식","기억","두뇌활동"),onOpenQuiz),
        AiTrainingItem("05","⚖️","AI 토론연습","AI 진행자와 토론자를 상대로 내 생각과 근거를 말하며 토론하는 연습을 해보세요.",listOf("논리","토론","표현"),onOpenDebate),
        AiTrainingItem("06","🎤","AI 보컬트레이닝","노래를 부르고 음정, 리듬, 발음과 노래 표현력을 반복해서 연습해보세요.",listOf("보컬","음정","리듬"),onOpenVocal),
        AiTrainingItem("07","💡","AI와 재치/센스 연습","다양한 상황에서 자연스럽고 재치 있게 말하는 연습을 해보세요.",listOf("재치","상황이해","배려"),onOpenWit)
    )
    LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(20.dp)){
        item{if(showPageTitle){Text("AI 트레이닝",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.size(6.dp))};Text("AI와 함께 재미있게 연습해 보세요.",style=MaterialTheme.typography.titleMedium,color=AppActionButton,fontWeight=FontWeight.SemiBold);Spacer(Modifier.size(6.dp));Text("말하기, 유머, 상식, 토론, 노래까지 부담 없이 반복하면서 나의 다양한 능력을 키워보세요.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        items(programs,key={it.number}){AiTrainingCard(it)}
    }
}

@Composable private fun AiTrainingCard(item:AiTrainingItem){
    Card(onClick=item.onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(14.dp)){Text(item.icon,Modifier.padding(12.dp),style=MaterialTheme.typography.headlineSmall)}
            Spacer(Modifier.size(12.dp));Column(Modifier.weight(1f)){Text(item.number,style=MaterialTheme.typography.labelMedium,color=AppActionButton,fontWeight=FontWeight.Bold);Text(item.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.size(4.dp));Text(item.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.size(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){item.tags.forEach{tag->Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(8.dp)){Text("#$tag",Modifier.padding(horizontal=7.dp,vertical=3.dp),style=MaterialTheme.typography.labelSmall,color=AppActionButton)}}}}
            Icon(Icons.Default.ChevronRight,"${item.title} 열기",tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun AiTrainingEntryScreen(description:String){
    Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Surface(Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Text("홈페이지의 현재 기능과 서버 구조를 확인하여 Android에 순차적으로 연결할 예정입니다.",Modifier.padding(18.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
}
