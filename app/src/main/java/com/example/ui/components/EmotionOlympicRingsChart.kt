package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Picture
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.unit.sp
import com.example.data.model.EmotionCategory
import kotlin.math.cos
import kotlin.math.sin

data class EmotionCategoryStat(
    val category: EmotionCategory,
    val count: Int,
    val percentage: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionOlympicRingsChart(
    stats: List<EmotionCategoryStat>,
    selectedDateRangeText: String,
    userNickname: String? = null,
    picture: Picture? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategoryCode by remember { mutableStateOf<String?>(null) }

    val totalCount = remember(stats) { stats.sumOf { it.count } }
    val maxCount = remember(stats) { stats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1 }

    // Map stats by category code
    val statMap = remember(stats) {
        stats.associateBy { it.category.code }
    }

    // 7 categories arranged in circle order
    val orderedCategories = listOf(
        EmotionCategory.JOY,      // 喜 (Red / Orange)
        EmotionCategory.ANGER,    // 怒 (Orange / Red)
        EmotionCategory.PLEASURE, // 樂 (Green)
        EmotionCategory.LOVE,     // 愛 (Teal / Blue)
        EmotionCategory.SORROW,   // 哀 (Cyan / Sky)
        EmotionCategory.HATRED,   // 惡 (Indigo / Violet)
        EmotionCategory.DESIRE    // 慾 (Purple / Magenta)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(
                if (picture != null) {
                    Modifier.drawWithCache {
                        val width = size.width.toInt()
                        val height = size.height.toInt()
                        onDrawWithContent {
                            if (width > 0 && height > 0) {
                                val pictureCanvas = Canvas(
                                    picture.beginRecording(width, height)
                                )
                                draw(this, layoutDirection, pictureCanvas, size) {
                                    this@onDrawWithContent.drawContent()
                                }
                                picture.endRecording()
                            }
                            drawContent()
                        }
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title: 별명(조회날짜)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.BubbleChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val displayName = if (!userNickname.isNullOrBlank()) userNickname else "사용자"
                    Text(
                        text = "$displayName($selectedDateRangeText)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "총 ${totalCount}회",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "감정 다이어리(우치소) • 원형 감정 그래프",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Ring Container (7 Ring Flower pattern matching user image)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight

                val centerXPx = containerWidth / 2
                val centerYPx = containerHeight / 2

                // Ring radius around center
                val ringRadius = 64.dp

                // Guide inner circle canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cX = size.width / 2
                    val cY = size.height / 2
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.15f),
                        radius = ringRadius.toPx(),
                        center = Offset(cX, cY)
                    )
                }

                // Render 7 Circles in Ring arrangement
                orderedCategories.forEachIndexed { index, cat ->
                    val stat = statMap[cat.code] ?: EmotionCategoryStat(cat, 0, 0f)
                    val count = stat.count

                    // Calculate angle for circle index (7 equal divisions = ~51.4 degrees each)
                    val angleDeg = -90.0 + (index * (360.0 / 7.0))
                    val angleRad = Math.toRadians(angleDeg)

                    // Position X & Y from center
                    val offsetX = ringRadius * cos(angleRad).toFloat()
                    val offsetY = ringRadius * sin(angleRad).toFloat()

                    val circleX = centerXPx + offsetX
                    val circleY = centerYPx + offsetY

                    // Area-based diameter calculation (Area proportional to count => Radius proportional to sqrt(count))
                    val minDiameter = 38.dp
                    val maxDiameter = 96.dp
                    val targetDiameter = if (maxCount > 0 && count > 0) {
                        val sqrtRatio = kotlin.math.sqrt(count.toFloat() / maxCount.toFloat())
                        minDiameter + (maxDiameter - minDiameter) * sqrtRatio
                    } else {
                        minDiameter
                    }
                    val animatedSize by animateDpAsState(
                        targetValue = targetDiameter,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "ringSize"
                    )

                    val isSelected = selectedCategoryCode == cat.code

                    OlympicRingCircleItem(
                        stat = stat,
                        size = animatedSize,
                        isSelected = isSelected,
                        centerX = circleX,
                        centerY = circleY,
                        onClick = {
                            selectedCategoryCode = if (isSelected) null else cat.code
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend / Selected Category Detail Card
            if (selectedCategoryCode != null) {
                val activeStat = statMap[selectedCategoryCode]
                if (activeStat != null) {
                    val cat = activeStat.category
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = cat.color.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, cat.color.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(cat.color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.hanja.substring(0, 1),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${cat.hanja} ${cat.koreanLabel}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = cat.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${activeStat.count}회",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = cat.color
                                )
                                Text(
                                    text = "${String.format("%.1f", activeStat.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // All 7 Category Chips Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stats.forEach { stat ->
                    val isSelected = selectedCategoryCode == stat.category.code
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCategoryCode = if (isSelected) null else stat.category.code
                            },
                        color = if (isSelected) stat.category.color.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(stat.category.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${stat.category.hanja} ${stat.category.koreanLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stat.count}회",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (stat.count > 0) stat.category.color else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OlympicRingCircleItem(
    stat: EmotionCategoryStat,
    size: Dp,
    isSelected: Boolean,
    centerX: Dp,
    centerY: Dp,
    onClick: () -> Unit
) {
    val category = stat.category
    val count = stat.count

    val hasCount = count > 0

    // Position circle centered at (centerX, centerY)
    Box(
        modifier = Modifier
            .offset(x = centerX - (size / 2), y = centerY - (size / 2))
            .size(size)
            .shadow(
                elevation = if (isSelected) 8.dp else if (hasCount) 4.dp else 1.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(
                color = if (isSelected) category.color
                else if (hasCount) category.color.copy(alpha = 0.85f)
                else category.color.copy(alpha = 0.35f)
            )
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.White.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            val hanjaFontSize = when {
                size > 72.dp -> 18.sp
                size > 52.dp -> 14.sp
                else -> 11.sp
            }
            val labelFontSize = when {
                size > 72.dp -> 12.sp
                size > 52.dp -> 10.sp
                else -> 8.sp
            }
            val countFontSize = when {
                size > 72.dp -> 14.sp
                size > 52.dp -> 11.sp
                else -> 9.sp
            }

            Text(
                text = category.hanja.substring(0, 1),
                fontSize = hanjaFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (size > 42.dp) {
                Text(
                    text = category.koreanLabel,
                    fontSize = labelFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1
                )
            }

            Text(
                text = "${count}회",
                fontSize = countFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

