package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Picture
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.local.entity.InnerStoryEntity
import com.example.data.model.EmotionCategory
import com.example.data.remote.dto.EmotionGraphResponse
import com.example.data.remote.dto.EmotionStoryRatioDto
import com.example.data.remote.dto.EmotionTimeGraphDto
import com.example.data.remote.dto.StoryDto
import com.example.ui.components.EmotionCategoryStat
import com.example.ui.components.EmotionOlympicRingsChart
import com.example.ui.components.SubEmotionPieChart
import com.example.ui.theme.EmotionColors
import com.example.ui.viewmodel.ChartTimeRange
import com.example.ui.viewmodel.EmotionViewModel
import com.example.utils.EmotionGraphPdfExporter
import com.example.utils.ShareUtils
import kotlin.math.cos
import kotlin.math.sin

private enum class GraphDashboardTab(val number: String, val label: String) {
    BUBBLE("01", "7감정 버블 그래프"),
    STORY("02", "사연별 비율"),
    TIME("03", "시간별 그래프"),
    DETAIL("04", "세부감정 그래프")
}

private enum class TimeGraphMode(val key: String, val label: String) {
    HOUR("hour", "시간대별"), DAY("day", "날짜별"), WEEKDAY("weekday", "요일별"),
    WEEK("week", "주별"), MONTH("month", "월별")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionDiaryScreen(
    viewModel: EmotionViewModel,
    diaryEntries: List<DiaryEntryEntity>,
    emotionCategoryStats: List<EmotionCategoryStat>,
    selectedDateRangeText: String,
    selectedRange: ChartTimeRange,
    innerStories: List<InnerStoryEntity>,
    serverStories: List<StoryDto> = emptyList(),
    loginNickname: String? = null,
    onOpenStoryDetail: (Long) -> Unit = {},
    showPageTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.emotionGraphState.collectAsStateWithLifecycle()
    val categories by viewModel.serverEmotionCategoryStats.collectAsStateWithLifecycle()
    val details by viewModel.serverSubEmotionStats.collectAsStateWithLifecycle()
    val rangeText by viewModel.serverGraphDateRangeText.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(GraphDashboardTab.BUBBLE) }
    var timeMode by remember { mutableStateOf(TimeGraphMode.HOUR) }
    var showDateDialog by remember { mutableStateOf(false) }
    val capturePicture = remember(selectedTab) { Picture() }

    LaunchedEffect(Unit) { viewModel.setChartRange(ChartTimeRange.WEEK) }

    fun saveCurrentGraph() {
        val bitmap = capturePicture.toBitmapOrNull()
        if (bitmap == null) {
            Toast.makeText(context, "그래프가 준비된 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val saved = ShareUtils.saveBitmapToGallery(context, bitmap, "Woochiso_${selectedTab.name}")
        Toast.makeText(context, if (saved) "그래프 이미지를 저장했습니다." else "이미지를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7FF)),
                border = BorderStroke(1.dp, Color(0xFFE4E7F2))
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    if (showPageTitle) Text("감정그래프", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("기록한 감정을 그래프로 확인하고 나의 감정 흐름을 살펴볼 수 있습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = ::saveCurrentGraph, enabled = state.response != null && !state.isLoading, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("이미지 저장")
                }
                Button(
                    onClick = {
                        val data = state.response ?: return@Button
                        val saved = EmotionGraphPdfExporter.save(context, data, rangeText.ifBlank { selectedDateRangeText })
                        Toast.makeText(context, if (saved) "전체 감정그래프 PDF를 저장했습니다." else "PDF를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = state.response != null && !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("PDF 저장")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GraphDashboardTab.entries.forEach { tab ->
                    FilterChip(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        label = { Text("${tab.number}  ${tab.label}", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF665DF5), selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            GraphSectionCard("조회 기간", "확인할 감정 기록의 기간을 선택하세요.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartTimeRange.entries.filter { it != ChartTimeRange.FAVORITES }.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { if (range == ChartTimeRange.CUSTOM) showDateDialog = true else viewModel.setChartRange(range) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (range == ChartTimeRange.CUSTOM) { Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
                                    Text(range.label)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF665DF5), selectedLabelColor = Color.White)
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            item { Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (state.error != null) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    TextButton(onClick = { viewModel.loadEmotionGraph(selectedRange) }) { Text("다시 시도") }
                }
            }
        } else state.response?.let { response ->
            item { EmotionSummary(response, selectedRange) }
            item {
                Box(Modifier.captureInto(capturePicture)) {
                    when (selectedTab) {
                        GraphDashboardTab.BUBBLE -> EmotionOlympicRingsChart(
                            stats = categories,
                            selectedDateRangeText = rangeText,
                            userNickname = loginNickname,
                            showNickname = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        GraphDashboardTab.STORY -> StoryRatioCard(response.storyRatios, rangeText)
                        GraphDashboardTab.TIME -> TimeGraphCard(response.timeGraphs, timeMode, { timeMode = it }, rangeText)
                        GraphDashboardTab.DETAIL -> SubEmotionPieChart(details, rangeText, loginNickname, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    if (showDateDialog) {
        var start by remember { mutableStateOf(viewModel.getDateBeforeDays(6)) }
        var end by remember { mutableStateOf(viewModel.getTodayDateString()) }
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("기간 선택") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text("시작일 (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(end, { end = it }, label = { Text("종료일 (YYYY-MM-DD)") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.setCustomDateRange(start, end); showDateDialog = false }) { Text("조회하기") } },
            dismissButton = { TextButton(onClick = { showDateDialog = false }) { Text("취소") } }
        )
    }
}

@Composable
private fun EmotionSummary(response: EmotionGraphResponse, range: ChartTimeRange) {
    val summary = response.summary
    val recorded = summary?.recordedEmotions ?: response.emotions.count { it.totalCount > 0 }
    val total = summary?.totalOccurrences ?: response.totalCount
    val top = summary?.topEmotion ?: response.emotions.maxByOrNull { it.totalCount }?.let {
        com.example.data.remote.dto.EmotionGraphTopEmotionDto(it.emotionId, it.emotionName, it.totalCount)
    }
    val label = response.period?.type?.let(::periodLabel) ?: range.label
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryTile("$label 기록", "${recorded}개", Modifier.weight(1f))
        SummaryTile("$label 발생", "${total}회", Modifier.weight(1f))
        SummaryTile("가장 많이 발생한 감정", top?.let { "${it.emotionName}\n${it.totalCount}회" } ?: "-\n0회", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE4E7F2)), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, minLines = 2)
            Text(value, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, minLines = 2)
        }
    }
}

@Composable
private fun GraphSectionCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFE4E7F2)), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
private fun StoryRatioCard(stories: List<EmotionStoryRatioDto>, rangeText: String) {
    GraphSectionCard("STORY RATIO · 사연별 비율", rangeText) {
        val active = stories.filter { it.totalCount > 0 }
        if (active.isEmpty()) {
            EmptyGraph("선택한 기간에 집계할 사연 기록이 없습니다.")
        } else {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.45f).padding(16.dp)) {
                val diameter = size.minDimension * .72f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var start = -90f
                active.forEach { story ->
                    val sweep = story.percentage.toFloat() * 3.6f
                    drawArc(story.color.toComposeColor(), start, (sweep - 1f).coerceAtLeast(.5f), false, topLeft, Size(diameter, diameter), style = Stroke(diameter * .22f))
                    start += sweep
                }
            }
            active.forEach { story ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(story.color.toComposeColor(), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(story.storyTitle, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${story.percentage.formatOne()}%", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeGraphCard(graphs: List<EmotionTimeGraphDto>, mode: TimeGraphMode, onMode: (TimeGraphMode) -> Unit, rangeText: String) {
    val graph = graphs.firstOrNull { it.mode == mode.key }
    GraphSectionCard("EMOTION TIMELINE · 시간별 그래프", rangeText) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TimeGraphMode.entries.forEach { item -> FilterChip(selected = item == mode, onClick = { onMode(item) }, label = { Text(item.label) }) }
        }
        if (graph == null || !graph.hasData) {
            EmptyGraph(if (graphs.isEmpty()) "시간별 그래프 데이터를 불러올 수 없습니다." else "이 기간에는 감정 기록이 없습니다.")
        } else {
            TimeLineChart(graph)
            graph.summary?.let { summary ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryTile("기록이 가장 많은 구간", summary.peakLabel ?: "-", Modifier.weight(1f))
                    SummaryTile("해당 구간 기록", "${summary.peakTotal}회", Modifier.weight(1f))
                    SummaryTile("대표 감정", summary.peakEmotion?.let { "${it.label}\n${it.count}회" } ?: "-", Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeLineChart(graph: EmotionTimeGraphDto) {
    Canvas(Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp, end = 8.dp, bottom = 30.dp, start = 34.dp)) {
        val max = graph.series.flatMap { it.values }.maxOrNull()?.coerceAtLeast(1) ?: 1
        val count = graph.labels.size.coerceAtLeast(2)
        repeat(5) { index ->
            val y = size.height * index / 4f
            drawLine(Color(0xFFE9ECF4), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        graph.series.forEach { series ->
            val color = series.color.toComposeColor()
            val points = series.values.mapIndexed { index, value ->
                Offset(size.width * index / (count - 1).toFloat(), size.height * (1f - value.toFloat() / max))
            }
            points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, 2.5.dp.toPx(), StrokeCap.Round) }
            if (graph.labels.size <= 31) points.forEach { drawCircle(color, 3.dp.toPx(), it) }
        }
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
            val step = (graph.labels.size / 6).coerceAtLeast(1)
            graph.labels.forEachIndexed { index, label -> if (index % step == 0 || index == graph.labels.lastIndex) canvas.nativeCanvas.drawText(label, size.width * index / (count - 1).toFloat(), size.height + 20.dp.toPx(), paint) }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        graph.series.forEach { series -> Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(series.color.toComposeColor(), CircleShape)); Spacer(Modifier.width(4.dp)); Text(series.categoryLabel, fontSize = 11.sp) } }
    }
}

@Composable
private fun EmptyGraph(message: String) {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
}

private fun Modifier.captureInto(picture: Picture): Modifier = drawWithCache {
    onDrawWithContent {
        if (size.width > 0f && size.height > 0f) {
            val target = androidx.compose.ui.graphics.Canvas(picture.beginRecording(size.width.toInt(), size.height.toInt()))
            draw(this, layoutDirection, target, size) { this@onDrawWithContent.drawContent() }
            picture.endRecording()
        }
        drawContent()
    }
}

private fun Picture.toBitmapOrNull(): Bitmap? = if (width <= 0 || height <= 0) null else runCatching {
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        AndroidCanvas(bitmap).apply { drawColor(android.graphics.Color.WHITE); drawPicture(this@toBitmapOrNull) }
    }
}.getOrNull()

private fun String.toComposeColor(): Color = runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(Color(0xFF74AFDD))
private fun Double.formatOne(): String = String.format(java.util.Locale.US, "%.1f", this)
private fun periodLabel(type: String): String = when (type) { "today" -> "오늘"; "7d" -> "최근 7일"; "30d" -> "최근 30일"; "all" -> "전체"; else -> "선택 기간" }
