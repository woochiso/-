package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionTrendChart(
    dailySeries: List<Map<String, Any?>>,
    categories: List<EmotionCategoryStat>,
    included: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text("감정 변화", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            if (!included) {
                Text(
                    "전체 기간에서는 감정 변화 그래프를 제공하지 않습니다.\n최근 7일, 최근 30일 또는 기간 선택을 이용해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            if (dailySeries.isEmpty()) {
                Text(
                    "선택한 기간에 표시할 감정 변화 데이터가 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val maxValue = max(
                1,
                dailySeries.maxOfOrNull { day ->
                    categories.maxOfOrNull { stat -> day.number(stat.category.code) } ?: 0
                } ?: 1
            )

            Canvas(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 10.dp.toPx()
                val bottom = size.height - 10.dp.toPx()
                repeat(5) { index ->
                    val y = top + (bottom - top) * index / 4f
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                categories.forEach { stat ->
                    val points = dailySeries.mapIndexed { index, day ->
                        val x = if (dailySeries.size == 1) (left + right) / 2f
                        else left + (right - left) * index / (dailySeries.lastIndex.toFloat())
                        val value = day.number(stat.category.code)
                        val y = bottom - (bottom - top) * value / maxValue.toFloat()
                        Offset(x, y)
                    }
                    points.zipWithNext().forEach { (start, end) ->
                        drawLine(stat.color, start, end, 2.5.dp.toPx(), cap = StrokeCap.Round)
                    }
                    points.forEach { drawCircle(stat.color, 3.dp.toPx(), it) }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dailySeries.first().dateLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dailySeries.size > 1) {
                    Text(dailySeries.last().dateLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { stat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(stat.color, CircleShape))
                        Text(
                            stat.categoryLabel.substringAfter(' ', stat.categoryLabel),
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun Map<String, Any?>.number(code: String): Int =
    (this[code] as? Number)?.toInt() ?: this[code]?.toString()?.toIntOrNull() ?: 0

private fun Map<String, Any?>.dateLabel(): String =
    this["date"]?.toString()?.takeLast(5)?.replace('-', '.') ?: ""
