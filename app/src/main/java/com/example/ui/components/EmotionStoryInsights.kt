package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmotionColors
import com.example.ui.viewmodel.EmotionStoryInsightsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionStoryInsightsPanel(
    state: EmotionStoryInsightsUiState,
    selectionColor: Color,
    parentLabel: String? = null,
    onRetry: () -> Unit,
    onOpenStory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.selectedCategoryCode == null && state.selectedEmotionId == null) return

    var showAllStories by remember(state.selectedCategoryCode, state.selectedEmotionId) {
        mutableStateOf(false)
    }
    val stories = state.response?.stories.orEmpty()
    val visibleStories = if (showAllStories) stories else stories.take(3)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, selectionColor.copy(alpha = 0.35f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("선택한 감정", style = MaterialTheme.typography.labelMedium, color = selectionColor)
            Text(state.selectedLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            parentLabel?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = selectionColor, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "총 ${state.selectedCount}회 · 관련 사연 ${state.response?.summary?.storyCount ?: 0}건",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("관련 사연을 불러오는 중...", Modifier.padding(start = 10.dp))
                }
                state.error != null -> Column {
                    Text("관련 사연을 불러오지 못했습니다.", color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry) { Text("다시 시도") }
                }
                stories.isEmpty() -> Text(
                    "선택한 기간에 이 감정과 연결된 사연이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    visibleStories.forEach { story ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = selectionColor.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, selectionColor.copy(alpha = 0.2f))
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(story.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(story.recordDate.replace('-', '.'), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(story.summary, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    story.emotions.forEach { emotion ->
                                        val color = EmotionColors.forCategoryCode(emotion.categoryCode, selectionColor)
                                        Row(
                                            modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(Modifier.size(7.dp).background(color, CircleShape))
                                            Text("${emotion.emotionName} ${emotion.count}회", Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Text("이 감정 기록 ${story.totalCount}회", style = MaterialTheme.typography.labelMedium, color = selectionColor)
                                Button(onClick = { onOpenStory(story.storyId) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("사연 자세히 보기")
                                }
                            }
                        }
                    }

                    if (stories.size > 3) {
                        OutlinedButton(
                            onClick = { showAllStories = !showAllStories },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showAllStories) "접기" else "더보기 (전체 ${stories.size}건 보기)")
                        }
                    }
                }
            }
        }
    }
}
