package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AiTrainingAccent = Color(0xFF74AFDD)

private enum class AiTrainingStatus { COMING_SOON }

private data class AiTrainingItem(
    val id: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val icon: ImageVector,
    val status: AiTrainingStatus = AiTrainingStatus.COMING_SOON
)

private val aiTrainingItems = listOf(
    AiTrainingItem("make_ai_laugh", "AI를 웃겨라", "재미있는 이야기를 말로 표현하고 유머 전달력과 표현력을 연습해보세요.", listOf("유머", "말하기", "표현력"), Icons.Default.SentimentVerySatisfied),
    AiTrainingItem("blind_date", "AI 소개팅", "AI와 소개팅 상황을 연습하며 자연스럽게 대화를 이어가는 방법을 연습해보세요.", listOf("대화", "공감", "센스"), Icons.Default.Favorite),
    AiTrainingItem("refusal", "AI 거절연습", "직장, 친구, 일상생활의 다양한 부탁을 자연스럽게 거절하는 방법을 연습해보세요.", listOf("의사표현", "대처능력", "대화"), Icons.Default.Block),
    AiTrainingItem("quiz", "AI 상식퀴즈", "역사, 과학, 음악, 스포츠, 생활 등 다양한 분야의 상식을 재미있게 풀어보세요.", listOf("상식", "기억", "두뇌활동"), Icons.Default.Psychology),
    AiTrainingItem("pop_english", "AI 팝송영어", "팝송을 활용해 영어 단어와 문장, 뜻과 발음을 재미있게 공부해보세요.", listOf("영어", "발음", "어휘"), Icons.Default.MusicNote),
    AiTrainingItem("daily_english", "AI 생활영어", "여행, 식당, 호텔, 쇼핑 등 실제 생활에서 사용하는 영어회화를 연습해보세요.", listOf("영어회화", "듣기", "말하기"), Icons.Default.Translate),
    AiTrainingItem("debate", "AI 토론", "AI와 다양한 주제로 의견을 주고받으며 논리적으로 말하고 설득하는 방법을 연습해보세요.", listOf("논리", "토론", "설득력"), Icons.Default.Gavel),
    AiTrainingItem("vocal", "AI 보컬트레이닝", "노래를 부르고 음정, 리듬, 발음과 노래 표현력을 반복해서 연습해보세요.", listOf("보컬", "음정", "리듬"), Icons.Default.Mic)
)

@Composable
fun AiTrainingScreen(showPageTitle: Boolean = true) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        item {
            if (showPageTitle) Text("AI 트레이닝", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            if (showPageTitle) Spacer(modifier = Modifier.size(6.dp))
            Text("AI와 함께 재미있게 연습해 보세요.", style = MaterialTheme.typography.titleMedium, color = AiTrainingAccent, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.size(6.dp))
            Text("말하기, 유머, 영어, 상식, 토론, 노래까지 부담 없이 반복하면서 나의 다양한 능력을 키워보세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(aiTrainingItems, key = { it.id }) { item ->
            AiTrainingCard(item) {
                Toast.makeText(context, "현재 준비중인 프로그램입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun AiTrainingCard(item: AiTrainingItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(item.icon, contentDescription = null, tint = AiTrainingAccent, modifier = Modifier.size(28.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Surface(color = Color(0xFFEAF2FB), shape = RoundedCornerShape(8.dp)) {
                    Text("준비중", style = MaterialTheme.typography.labelSmall, color = AiTrainingAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.tags.forEach { tag ->
                    Surface(color = Color(0xFFEAF2FB), shape = RoundedCornerShape(8.dp)) {
                        Text("#$tag", style = MaterialTheme.typography.labelSmall, color = AiTrainingAccent, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
            }
        }
    }
}
