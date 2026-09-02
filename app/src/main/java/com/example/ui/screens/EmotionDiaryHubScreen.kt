package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val DiaryAccent = Color(0xFF74AFDD)
private val DiaryLightBackground = Color(0xFFEAF2FB)

@Composable
fun EmotionDiaryHubScreen(
    onOpenHumanEmotions: () -> Unit,
    onOpenFavoriteEmotions: () -> Unit,
    onOpenEmotionDiary: () -> Unit,
    onOpenInnerStories: () -> Unit,
    onOpenRecovery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "감정다이어리",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "오늘의 감정을 기록하고 내 마음의 흐름을 확인해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(22.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DiaryMenuCard(
                title = "인간의 감정",
                description = "다양한 감정을 살펴보고 내가 느낀 감정을 찾아보세요.",
                icon = Icons.Default.SelfImprovement,
                onClick = onOpenHumanEmotions,
                modifier = Modifier.weight(1f)
            )
            DiaryMenuCard(
                title = "즐찾감정",
                description = "자주 느끼는 감정을 빠르게 선택하고 기록해보세요.",
                icon = Icons.Default.Star,
                onClick = onOpenFavoriteEmotions,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DiaryMenuCard(
                title = "감정그래프",
                description = "기록된 감정의 변화와 흐름을 확인해보세요.",
                icon = Icons.Default.PieChart,
                onClick = onOpenEmotionDiary,
                modifier = Modifier.weight(1f)
            )
            DiaryMenuCard(
                title = "나의 사연",
                description = "마음속에 남아 있는 이야기와 감정을 기록해보세요.",
                icon = Icons.Default.MenuBook,
                onClick = onOpenInnerStories,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        DiaryMenuCard(
            title = "감정회복",
            description = "현재 감정에 맞는 회복 활동을 확인해보세요.",
            icon = Icons.Default.Favorite,
            onClick = onOpenRecovery,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DiaryMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(170.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DiaryLightBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DiaryAccent,
                    modifier = Modifier.padding(10.dp).size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
