package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.model.EmotionCategory
import com.example.ui.viewmodel.PieChartSegment
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionPieChart(
    segments: List<PieChartSegment>,
    modifier: Modifier = Modifier
) {
    // Ensure all 7 categories are present in order
    val orderedCategories = listOf(
        EmotionCategory.JOY,      // 희 (喜)
        EmotionCategory.ANGER,    // 노 (怒)
        EmotionCategory.SORROW,   // 애 (哀)
        EmotionCategory.PLEASURE, // 락 (樂)
        EmotionCategory.LOVE,     // 애 (愛)
        EmotionCategory.HATRED,   // 오 (惡)
        EmotionCategory.DESIRE    // 욕 (慾)
    )

    val segmentMap = remember(segments) { segments.associateBy { it.category } }
    val totalCount = remember(segments) { segments.sumOf { it.count } }

    val fullSegments = remember(segments, totalCount) {
        orderedCategories.map { cat ->
            val existing = segmentMap[cat]
            if (existing != null) existing
            else {
                val pct = if (totalCount > 0) 0f else 0f
                PieChartSegment(cat, 0, pct)
            }
        }
    }

    var selectedSegmentIndex by remember { mutableStateOf<Int?>(null) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "내면의 감정 분포 (원형 그래프)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(210.dp)
                        .pointerInput(fullSegments) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                var currentStartAngle = 0f
                                fullSegments.forEachIndexed { index, segment ->
                                    val sweep = (segment.percentage / 100f) * 360f
                                    val start = currentStartAngle
                                    val end = start + sweep

                                    val adjustedAngle = (angle - 270f + 360f) % 360f
                                    if (sweep > 0f && adjustedAngle in start..end) {
                                        selectedSegmentIndex = if (selectedSegmentIndex == index) null else index
                                        return@detectTapGestures
                                    }
                                    currentStartAngle += sweep
                                }
                            }
                        }
                ) {
                    val strokeWidth = 34.dp.toPx()
                    val diameter = size.minDimension - strokeWidth - 20.dp.toPx()
                    val pieSize = Size(diameter, diameter)
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )

                    // Draw base ring when no entries exist
                    if (totalCount == 0) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = pieSize,
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        var startAngle = -90f

                        fullSegments.forEachIndexed { index, segment ->
                            val sweepAngle = (segment.percentage / 100f) * 360f * progressAnim.value
                            val isSelected = selectedSegmentIndex == index
                            val currentStrokeWidth = if (isSelected) strokeWidth + 10.dp.toPx() else strokeWidth

                            if (sweepAngle > 0f) {
                                drawArc(
                                    color = segment.category.color,
                                    startAngle = startAngle,
                                    sweepAngle = (sweepAngle - 1.5f).coerceAtLeast(0.5f),
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = pieSize,
                                    style = Stroke(width = currentStrokeWidth)
                                )
                            } else {
                                // Draw thin tick line for 0% category so all 7 colors are visually represented on ring
                                drawArc(
                                    color = segment.category.color.copy(alpha = 0.5f),
                                    startAngle = startAngle,
                                    sweepAngle = 1.5f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = pieSize,
                                    style = Stroke(width = strokeWidth * 0.5f)
                                )
                            }

                            startAngle += sweepAngle
                        }
                    }
                }

                // Center Info display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedSegmentIndex != null && selectedSegmentIndex!! < fullSegments.size) {
                        val seg = fullSegments[selectedSegmentIndex!!]
                        Text(
                            text = seg.category.hanja,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = seg.category.color
                        )
                        Text(
                            text = "${seg.count}회 (${String.format("%.1f", seg.percentage)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "총 감정",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${totalCount}회",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend FlowRow with 7 ordered categories
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fullSegments.forEachIndexed { index, segment ->
                    val isSelected = selectedSegmentIndex == index
                    val hasCount = segment.count > 0

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSegmentIndex = if (isSelected) null else index
                            },
                        color = if (isSelected) segment.category.color.copy(alpha = 0.25f)
                        else if (hasCount) segment.category.color.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(segment.category.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${segment.category.hanja} ${segment.category.koreanLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (hasCount || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${segment.count}회",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasCount) segment.category.color else MaterialTheme.colorScheme.outline
                            )
                            if (hasCount) {
                                Text(
                                    text = " (${String.format("%.1f", segment.percentage)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubEmotionPieChart(
    subEmotionStats: List<com.example.ui.viewmodel.SubEmotionStat>,
    modifier: Modifier = Modifier
) {
    if (subEmotionStats.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "선택한 기간에 기록된 세부 감정이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val totalCount = subEmotionStats.sumOf { it.count }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "세부 감정별 점유율 (총 ${totalCount}회 기록)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Canvas Pie Chart for Sub-Emotions
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    var startAngle = -90f
                    val strokeWidth = 28.dp.toPx()

                    subEmotionStats.forEach { stat ->
                        val sweepAngle = (stat.percentage / 100f) * 360f
                        if (sweepAngle > 0f) {
                            drawArc(
                                color = stat.category.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "세부 감정",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${subEmotionStats.size}종류",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ranking list for sub-emotions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subEmotionStats.take(10).forEachIndexed { index, stat ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp)
                            )

                            // Parent Category Tag
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = stat.category.color.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = stat.category.hanja,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = stat.category.color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = stat.emotionName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "${stat.count}회 (${String.format("%.1f", stat.percentage)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
