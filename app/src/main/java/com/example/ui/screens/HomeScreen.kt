package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppActionButton

private val HomePrimary = Color(0xFF74AFDD)
private val HomeAccent = Color(0xFF74AFDD)
private val HomeLightBackground = Color(0xFFEAF2FB)

@Composable
fun HomeScreen(
    nickname: String?,
    onRecordEmotion: () -> Unit,
    onOpenIntroduction: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenHumanEmotions: () -> Unit,
    onOpenFavoriteEmotions: () -> Unit,
    onOpenEmotionDiary: () -> Unit,
    onOpenInnerStories: () -> Unit,
    onOpenAiCare: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Column {
                Text(
                    text = nickname?.takeIf(String::isNotBlank)?.let {
                        "${it}님,\n오늘의 마음은 어떠신가요?"
                    } ?: "오늘의 마음은\n어떠신가요?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "오늘 느낀 감정을 기록하고 내 마음의 흐름을 확인해보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = HomeLightBackground),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, HomePrimary.copy(alpha = 0.28f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = HomeAccent,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("오늘의 감정 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        "하루 동안 느낀 감정을 간단하게 기록해보세요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRecordEmotion,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppActionButton,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("감정 기록하기")
                    }
                }
            }
        }
        item { SectionTitle("우치소 알아보기") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeLinkCard(
                    title = "우치소 소개",
                    description = "우치소가 어떤 서비스인지 알아보세요.",
                    icon = Icons.Default.Info,
                    onClick = onOpenIntroduction,
                    modifier = Modifier.weight(1f)
                )
                HomeLinkCard(
                    title = "사용방법",
                    description = "감정다이어리를 사용하는 방법을 확인하세요.",
                    icon = Icons.Default.HelpOutline,
                    onClick = onOpenGuide,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { SectionTitle("감정다이어리") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeLinkCard("인간의 감정", "다양한 감정을 살펴보세요.", Icons.Default.SelfImprovement, onOpenHumanEmotions, Modifier.weight(1f))
                HomeLinkCard("즐찾감정", "자주 느끼는 감정을 빠르게 기록하세요.", Icons.Default.Star, onOpenFavoriteEmotions, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeLinkCard("감정그래프", "기록된 감정의 흐름을 확인하세요.", Icons.Default.PieChart, onOpenEmotionDiary, Modifier.weight(1f))
                HomeLinkCard("나의 사연", "내 마음속 이야기를 기록하세요.", Icons.Default.MenuBook, onOpenInnerStories, Modifier.weight(1f))
            }
        }
        item { SectionTitle("AI 마음케어") }
        item {
            Text(
                "나의 감정 기록을 바탕으로 마음을 더 깊이 이해해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeLinkCard("AI 감정분석", "기록된 감정의 흐름을 AI가 분석합니다.", Icons.Default.AutoAwesome, onOpenAiCare, Modifier.weight(1f), showPro = true)
                HomeLinkCard("AI 상담", "내 감정에 대해 AI와 대화해보세요.", Icons.Default.Chat, onOpenAiCare, Modifier.weight(1f), showPro = true)
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun HomeLinkCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPro: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = HomeAccent, modifier = Modifier.size(24.dp))
                if (showPro) {
                    Surface(color = HomeLightBackground, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HomeAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
