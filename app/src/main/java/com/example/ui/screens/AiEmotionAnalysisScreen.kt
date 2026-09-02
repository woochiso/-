package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.*
import com.example.ui.viewmodel.AiAnalysisTab
import com.example.ui.viewmodel.AiEmotionAnalysisViewModel
import kotlin.math.max
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val AnalysisAccent = Color(0xFF74AFDD)

@Composable
fun AiEmotionAnalysisScreen(viewModel: AiEmotionAnalysisViewModel, onAuthExpired: () -> Unit, showPageTitle: Boolean = true) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (showPageTitle) Text("AI 감정분석", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (showPageTitle) Spacer(Modifier.height(5.dp))
            Text("기록한 감정을 바탕으로 내 감정의 변화와 패턴을 확인해보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { AnalysisTabs(state.tab, viewModel::selectTab) }
        item { PeriodSelector(state.period, viewModel::selectPeriod) }
        if (state.loading && state.data == null) item { Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        state.error?.let { error -> item { ErrorCard(error, viewModel::load) } }
        state.data?.let { data ->
            when (state.tab) {
                AiAnalysisTab.OVERALL -> overallItems(data, state.aiLoading, state.aiText, state.aiError, state.aiCached, viewModel::requestAiAnalysis)
                AiAnalysisTab.HOURLY -> hourlyItems(data, state.timelineUnit, state.visibleCategories, viewModel::selectTimelineUnit, viewModel::toggleCategory)
                AiAnalysisTab.PATTERN -> patternItems(data, state.visibleCategories, viewModel::toggleCategory)
                AiAnalysisTab.STORY -> storyItems(data)
            }
        }
    }
}

@Composable private fun AnalysisTabs(selected: AiAnalysisTab, onSelect: (AiAnalysisTab) -> Unit) {
    val tabs = listOf(AiAnalysisTab.OVERALL to "전체감정", AiAnalysisTab.HOURLY to "시간별감정", AiAnalysisTab.PATTERN to "감정패턴", AiAnalysisTab.STORY to "사연별")
    ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == selected }, edgePadding = 0.dp, containerColor = Color.Transparent, contentColor = AnalysisAccent) {
        tabs.forEach { (tab, label) -> Tab(selected = tab == selected, onClick = { onSelect(tab) }, text = { Text(label, maxLines = 1) }) }
    }
}

@Composable private fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    val periods = listOf("today" to "오늘", "7d" to "7일", "30d" to "30일", "90d" to "90일", "1y" to "1년")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(periods) { (value, label) -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEAF2FB), selectedLabelColor = AnalysisAccent)) } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.overallItems(data: AiEmotionAnalysisResponse, aiLoading: Boolean, aiText: String?, aiError: String?, cached: Boolean, requestAi: (Boolean) -> Unit) {
    item { SectionHeader("${data.periodLabel} 요약", "${data.startDate.replace('-', '.')} ~ ${data.endDate.replace('-', '.')}") }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryBox("총 감정 기록", "${data.analysis.totalCount}회", Modifier.weight(1f))
            SummaryBox("감정 종류", "${data.patterns.uniqueEmotions}종", Modifier.weight(1f))
            SummaryBox("가장 많은 감정", data.categories.maxByOrNull { it.totalCount }?.name ?: "-", Modifier.weight(1f))
        }
    }
    item { AnalysisCard("7감정 분포") { EmotionBars(data.categories) } }
    item { AnalysisCard("감정 순위") { data.categories.sortedByDescending { it.totalCount }.forEachIndexed { index, item -> StatRow("${index + 1}. ${item.hanja} ${item.name}", "${item.totalCount}회", colorOf(item.color)) } } }
    item { AiInterpretationCard(aiLoading, aiText, aiError, cached, requestAi) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.hourlyItems(data: AiEmotionAnalysisResponse, unit: String, visible: Set<String>, selectUnit: (String) -> Unit, toggle: (String) -> Unit) {
    val unitLabels = listOf("hour" to "시간별", "day" to "날짜별", "week" to "주별", "month" to "월별")
    val title = mapOf("hour" to "00시~23시 감정 기록", "day" to "날짜별 감정 기록", "week" to "주별 감정 기록", "month" to "월별 감정 기록")[unit] ?: "감정 기록"
    item { SectionHeader("시간별감정", if (unit == "hour") data.hourly.collectionStart?.let { "시간별 데이터는 ${it.take(10).replace('-', '.')}부터 수집되었습니다." } ?: "수집된 시간 이벤트만 사용합니다." else "서버의 ${unitLabels.first { it.first == unit }.second} 집계 결과를 표시합니다.") }
    item { LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(unitLabels) { (value, label) -> FilterChip(selected = unit == value, onClick = { selectUnit(value) }, label = { Text(label) }) } } }
    item { CategoryToggles(data.hourly.series, visible, toggle) }
    item { AnalysisCard(title) { if (data.hourly.hasData) TimelineChart(data.hourly.labels, data.hourly.series, visible, unit) else EmptyText("선택한 기간에 기록된 감정이 없습니다.") } }
    item { AnalysisCard("${unitLabels.first { it.first == unit }.second} 요약") { val summary = data.hourly.summary; StatRow("기록이 가장 많은 구간", summary.peakLabel ?: "-"); StatRow("해당 구간 기록", "${summary.peakTotal}회"); StatRow("대표 감정", summary.peakCategory?.let { "${it.name} ${it.count}회" } ?: "-") } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.patternItems(data: AiEmotionAnalysisResponse, visible: Set<String>, toggle: (String) -> Unit) {
    item { SectionHeader("감정 변화 패턴", "기록 횟수의 증가·감소 흐름을 확인합니다.") }
    item { CategoryToggles(data.categories.map { AiAnalysisSeriesDto(it.code, it.label, it.name, it.color, it.values, it.totalCount) }, visible, toggle) }
    item { AnalysisCard("이전 기간 vs 최근 기간") { if (data.patterns.categoryChanges.all { it.current == 0 && it.previous == 0 }) EmptyText("아직 감정 변화 패턴을 비교하기 위한 기록 기간이 충분하지 않습니다.") else data.patterns.categoryChanges.forEach { change -> val marker = when { change.current > change.previous -> "▲"; change.current < change.previous -> "▼"; else -> "→" }; StatRow(change.categoryLabel, "이전 ${change.previous}회 → 최근 ${change.current}회  $marker${change.percent?.let { " ${kotlin.math.abs(it)}%" } ?: ""}", colorOf(change.color)) } } }
    item { AnalysisCard("주요 감정 변화 그래프") { TimelineChart(data.dates, data.categories.map { AiAnalysisSeriesDto(it.code, it.label, it.name, it.color, it.values, it.totalCount) }, visible, "day") } }
    item { AnalysisCard("자주 기록한 세부 감정") { if (data.patterns.topEmotions.isEmpty()) EmptyText("선택한 기간에 세부 감정 기록이 없습니다.") else data.patterns.topEmotions.forEachIndexed { i, e -> StatRow("${i + 1}. ${e.emotionName}", "${e.totalCount}회") } } }
    item { AnalysisCard("기록 기반 해석") { data.patterns.summaryMessages.forEach { Text("• $it", modifier = Modifier.padding(vertical = 3.dp)) }; Text("감정 기록 횟수는 감정의 강도나 의학적 상태를 의미하지 않습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.storyItems(data: AiEmotionAnalysisResponse) {
    item { SectionHeader("사연별 감정분석", "실제 사연-감정 연결 기록만 분석합니다.") }
    if (data.patterns.storyImpacts.isEmpty()) item { AnalysisCard("연결된 사연") { EmptyText("이 사연과 연결된 감정 기록이 아직 없습니다.") } }
    else items(data.patterns.storyImpacts, key = { it.storyId }) { story ->
        AnalysisCard(story.storyTitle) {
            StatRow("선택 기간", "${story.recentCount}회")
            StatRow("이전 동일 기간", "${story.pastCount}회")
            story.emotions.take(5).forEach { StatRow(it.emotionName, "${it.totalCount}회") }
            if (story.recoveryTrend.isNotBlank()) Text(story.recoveryTrend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable private fun SectionHeader(title: String, description: String) { Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
@Composable private fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier) { Surface(modifier, color = Color(0xFFEAF2FB), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(11.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Bold, color = AnalysisAccent, maxLines = 1) } } }
@Composable private fun AnalysisCard(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); content() } } }
@Composable private fun StatRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = color, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.SemiBold) } }
@Composable private fun EmptyText(text: String) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }

@Composable private fun EmotionBars(categories: List<AiAnalysisCategoryDto>) { val maximum = max(1, categories.maxOfOrNull { it.totalCount } ?: 1); categories.forEach { item -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) { Text(item.name, modifier = Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall); LinearProgressIndicator(progress = { item.totalCount.toFloat() / maximum }, modifier = Modifier.weight(1f).height(10.dp), color = colorOf(item.color), trackColor = MaterialTheme.colorScheme.surfaceVariant); Text("${item.totalCount}회", modifier = Modifier.width(52.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall) } } }

@Composable private fun CategoryToggles(series: List<AiAnalysisSeriesDto>, visible: Set<String>, toggle: (String) -> Unit) { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(series, key = { it.categoryCode }) { item -> FilterChip(selected = item.categoryCode in visible, onClick = { toggle(item.categoryCode) }, label = { Text(item.categoryName) }, leadingIcon = { Surface(Modifier.size(9.dp), color = colorOf(item.color), shape = RoundedCornerShape(50)) {} }) } } }

@Composable private fun TimelineChart(labels: List<String>, series: List<AiAnalysisSeriesDto>, visible: Set<String>, unit: String) {
    val shown = series.filter { it.categoryCode in visible }
    val dataMaximum = max(1, shown.flatMap { it.values }.maxOrNull() ?: 1)
    val tickStep = niceTickStep(dataMaximum)
    val axisMaximum = max(tickStep, ceil(dataMaximum.toDouble() / tickStep).toInt() * tickStep)
    val yTicks = (0..axisMaximum step tickStep).toList()
    val xTicks = xTickIndices(labels.size, unit)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val labelSize = with(density) { 12.sp.toPx() }
    val titleSize = with(density) { 11.sp.toPx() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(270.dp)) {
            val left = 48.dp.toPx(); val right = size.width - 8.dp.toPx(); val top = 24.dp.toPx(); val bottom = size.height - 36.dp.toPx()
            val plotWidth = right - left; val plotHeight = bottom - top
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = axisColor.toArgb(); textSize = labelSize }
            drawIntoCanvas { canvas ->
                paint.textAlign = android.graphics.Paint.Align.LEFT; paint.textSize = titleSize
                canvas.nativeCanvas.drawText("기록 횟수", 0f, titleSize, paint)
            }
            yTicks.forEach { tick ->
                val y = bottom - plotHeight * tick / axisMaximum
                drawLine(gridColor, Offset(left, y), Offset(right, y), if (tick == 0) 1.8f else 1f)
                drawIntoCanvas { canvas -> paint.textAlign = android.graphics.Paint.Align.RIGHT; paint.textSize = labelSize; canvas.nativeCanvas.drawText(tick.toString(), left - 8.dp.toPx(), y + labelSize * .35f, paint) }
            }
            shown.forEach { item ->
                val count = minOf(labels.size, item.values.size)
                val points = (0 until count).map { index -> Offset(if (count <= 1) left else left + plotWidth * index / (count - 1), bottom - plotHeight * item.values[index] / axisMaximum) }
                points.zipWithNext().forEach { (a, b) -> drawLine(colorOf(item.color), a, b, 3.5f) }
                points.forEach { drawCircle(colorOf(item.color), 3.8f, it) }
            }
            xTicks.forEach { index ->
                val x = if (labels.size <= 1) left else left + plotWidth * index / (labels.size - 1)
                drawLine(gridColor, Offset(x, bottom), Offset(x, bottom + 4.dp.toPx()), 1f)
                drawIntoCanvas { canvas -> paint.textAlign = when (index) { 0 -> android.graphics.Paint.Align.LEFT; labels.lastIndex -> android.graphics.Paint.Align.RIGHT; else -> android.graphics.Paint.Align.CENTER }; paint.textSize = labelSize; canvas.nativeCanvas.drawText(formatAxisLabel(labels[index], labels, unit), x, bottom + 19.dp.toPx(), paint) }
            }
        }
    }
}

private fun niceTickStep(maximum: Int): Int {
    if (maximum <= 5) return 1
    val raw = maximum / 4.0
    val magnitude = 10.0.pow(floor(log10(raw)))
    val normalized = raw / magnitude
    val nice = when { normalized <= 1 -> 1.0; normalized <= 2 -> 2.0; normalized <= 5 -> 5.0; else -> 10.0 }
    return max(1, (nice * magnitude).toInt())
}

private fun xTickIndices(count: Int, unit: String): List<Int> {
    if (count <= 0) return emptyList()
    if (unit == "hour" && count >= 24) return listOf(0, 3, 6, 9, 12, 15, 18, 21, 23)
    val maximumLabels = when (unit) { "day" -> if (count <= 8) count else 7; "week" -> 6; "month" -> 6; else -> 7 }
    if (count <= maximumLabels) return (0 until count).toList()
    return (0 until maximumLabels).map { index -> ((count - 1) * index.toFloat() / (maximumLabels - 1)).toInt() }.distinct()
}

private fun formatAxisLabel(label: String, all: List<String>, unit: String): String = when (unit) {
    "hour" -> label.removeSuffix("시")
    "week" -> label.replace("월 ", "월")
    "month" -> {
        val years = all.mapNotNull { it.takeIf { value -> value.length >= 7 }?.take(4) }.distinct()
        if (years.size <= 1) label.substringAfter('-').substringAfter('.').trimStart('0') + "월"
        else label.replace('-', '.').let { if (it.length >= 7) it.drop(2) else it }
    }
    else -> label.substringAfter('-').replace('-', '/')
}

@Composable private fun AiInterpretationCard(loading: Boolean, text: String?, error: String?, cached: Boolean, request: (Boolean) -> Unit) { AnalysisCard("AI 분석") { when { loading -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Text("통계 해석을 생성하고 있습니다.") }; text != null -> { if (cached) Text("저장된 분석", style = MaterialTheme.typography.labelSmall, color = AnalysisAccent); Text(text); Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = { request(true) }) { Text("AI 다시 분석") } }; error != null -> { Text("AI 분석을 불러오지 못했습니다.", color = MaterialTheme.colorScheme.error); Text(error, style = MaterialTheme.typography.bodySmall); TextButton(onClick = { request(false) }) { Text("다시 시도") } }; else -> { Text("통계 수치는 서버가 계산하며 AI는 계산된 결과만 해석합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(10.dp)); Button(onClick = { request(false) }, colors = ButtonDefaults.buttonColors(containerColor = AnalysisAccent)) { Text("AI 분석 시작", color = Color.White) } } } } }
@Composable private fun ErrorCard(message: String, retry: () -> Unit) { AnalysisCard("불러오기 오류") { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = retry) { Text("다시 시도") } } }
private fun colorOf(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(AnalysisAccent)
