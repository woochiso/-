package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.*
import com.example.ui.viewmodel.AiEmotionAnalysisViewModel
import com.example.utils.AiEmotionAnalysisPdfExporter
import com.example.utils.AiAnalysisTextFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private val AnalysisAccent = Color(0xFF74AFDD)
private val BeforeColor = Color(0xFFC9DFF0)
private val AfterColor = Color(0xFF4D9CD8)

@Composable
fun AiEmotionAnalysisScreen(
    viewModel: AiEmotionAnalysisViewModel,
    onAuthExpired: () -> Unit,
    nickname: String = "회원",
    showPageTitle: Boolean = true
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.requiresLogin) { if (state.requiresLogin) onAuthExpired() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 14.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (showPageTitle) Text("AI 감정분석", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("회복활동 전후의 감정 변화를 비교하고,\n반복되는 변화 패턴을 AI가 함께 살펴봅니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::load, colors = ButtonDefaults.buttonColors(containerColor = AnalysisAccent)) { Text("새로고침") }
                OutlinedButton(
                    enabled = state.data != null && !state.loading,
                    onClick = {
                        val data = state.data ?: return@OutlinedButton
                        val ok = AiEmotionAnalysisPdfExporter.save(context, data, state.aiText, nickname)
                        Toast.makeText(context, if (ok) "PDF를 Downloads/Woochiso에 저장했습니다." else "PDF 저장에 실패했습니다.", Toast.LENGTH_LONG).show()
                    }
                ) { Text("PDF 저장") }
            }
        }
        item { PeriodSelector(state.period, viewModel::selectPeriod) }
        if (state.loading && state.data == null) item { LoadingCard() }
        state.error?.let { message -> item { AnalysisCard("불러오기 오류") { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = viewModel::load) { Text("다시 시도") } } } }
        state.data?.let { data ->
            if (data.comparableRecords == 0) {
                item { AnalysisCard("분석할 기록이 없습니다") { EmptyText("선택한 기간에 비교 가능한 회복활동 전후 기록이 없습니다.") } }
            } else {
                item { NumberedTitle("01", "최근 회복활동", data.period.label) }
                item { RecentSessions(data.recentSessions, state.selectedSessionId, viewModel::selectSession) }
                val selected = data.recentSessions.firstOrNull { it.sessionId == state.selectedSessionId } ?: data.recentSessions.firstOrNull()
                item { NumberedTitle("02", "활동 전·후 감정 변화", selected?.activityName.orEmpty()) }
                item { AnalysisCard("7감정 비교") { if (selected == null) EmptyText("비교할 기록이 없습니다.") else BeforeAfterChart(selected.changes) } }
                item { NumberedTitle("03", "나에게 관찰된 회복 패턴", "최소 ${data.minimumPatternRecords}회부터 반복 패턴을 살펴봅니다.") }
                items(data.activityPatterns, key = { "${it.activityId}:${it.activityName}" }) { PatternCard(it, data.minimumPatternRecords) }
                item { NumberedTitle("04", "AI가 분석한 나의 감정 변화", if (state.aiCached) "기존 분석 결과를 재사용했습니다." else "서버에서 구조화된 집계값만 분석합니다.") }
                item { AiResultCard(data, state.aiLoading, state.aiText, state.aiError) { viewModel.requestAiAnalysis(false) } }
                item { NumberedTitle("05", "분석 근거", "AI 설명과 동일한 서버 집계값입니다.") }
                item { EvidenceCard(data) }
                item { NumberedTitle("06", "장기 변화", "연결된 사연의 회상 빈도 변화를 비교합니다.") }
                item { LongTermCard(data.longTerm) }
            }
        }
    }
}

@Composable private fun PeriodSelector(selected: String, select: (String, String?, String?) -> Unit) {
    val context = LocalContext.current
    val values = listOf("7d" to "최근 7일", "30d" to "최근 30일", "90d" to "최근 3개월", "all" to "전체")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(values) { (value, label) -> FilterChip(selected == value, { select(value, null, null) }, { Text(label) }) }
        item {
            FilterChip(selected == "custom", {
                pickDateRange(context) { start, end -> select("custom", start, end) }
            }, { Text("기간 선택") })
        }
    }
}

private fun pickDateRange(context: android.content.Context, done: (String, String) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(context, { _, sy, sm, sd ->
        val start = String.format(Locale.US, "%04d-%02d-%02d", sy, sm + 1, sd)
        DatePickerDialog(context, { _, ey, em, ed ->
            val end = String.format(Locale.US, "%04d-%02d-%02d", ey, em + 1, ed)
            if (start <= end) done(start, end) else Toast.makeText(context, "조회 기간을 확인해주세요.", Toast.LENGTH_SHORT).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable private fun RecentSessions(sessions: List<RecoveryInsightSessionDto>, selected: String?, select: (String) -> Unit) {
    if (sessions.isEmpty()) { AnalysisCard("최근 회복활동") { EmptyText("표시할 회복활동이 없습니다.") }; return }
    Column {
        Text("옆으로 넘겨 다른 활동도 확인할 수 있습니다.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 7.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth * 0.86f
            LazyRow(contentPadding = PaddingValues(end = maxWidth * 0.14f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.sessionId }) { session ->
                    Card(
                        Modifier.width(cardWidth).clickable { select(session.sessionId) },
                        border = BorderStroke(if (session.sessionId == selected) 2.dp else 1.dp, if (session.sessionId == selected) AnalysisAccent else MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(session.activityName, fontWeight = FontWeight.Bold)
                            Text(session.recordedAt.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            session.changes.filter { it.delta != 0 }.sortedByDescending { abs(it.delta) }.take(4).forEach { Text("${it.categoryLabel} ${it.before} → ${it.after}  ${signed(it.delta.toDouble())}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun BeforeAfterChart(changes: List<RecoveryEmotionChangeDto>) {
    Row(verticalAlignment = Alignment.CenterVertically) { LegendDot(BeforeColor, "활동 전"); Spacer(Modifier.width(14.dp)); LegendDot(AfterColor, "활동 후") }
    Spacer(Modifier.height(10.dp))
    changes.forEach { item ->
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.categoryLabel, fontWeight = FontWeight.SemiBold); Text("${item.before} → ${item.after}   ${signed(item.delta.toDouble())}") }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth(item.before.coerceIn(0,100) / 100f).fillMaxHeight().background(BeforeColor, RoundedCornerShape(8.dp))) }
            Spacer(Modifier.height(3.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth(item.after.coerceIn(0,100) / 100f).fillMaxHeight().background(AfterColor, RoundedCornerShape(8.dp))) }
        }
    }
}

@Composable private fun PatternCard(item: RecoveryActivityPatternDto, minimum: Int) = AnalysisCard(item.activityName) {
    Text("${item.count}회 기록", color = MaterialTheme.colorScheme.onSurfaceVariant)
    item.emotionChanges.filter { it.averageDelta != 0.0 }.take(3).forEach { change ->
        Text("${change.categoryLabel} 평균 ${signed(change.averageDelta)} · 같은 방향 ${change.sameDirectionCount}/${item.count}회 (${change.sameDirectionRatio}%)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp))
    }
    val note = when { !item.enoughData -> "패턴 확인까지 ${(minimum - item.count).coerceAtLeast(0)}회 더 필요합니다."; item.hasRepeatedChange -> "반복 변화 관찰됨"; else -> "변화 방향이 일정하지 않아 기록을 더 살펴봐야 합니다." }
    Text(note, color = if (item.hasRepeatedChange) AnalysisAccent else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp))
}

@Composable private fun AiResultCard(data: RecoveryInsightDataDto, loading: Boolean, text: String?, error: String?, retry: () -> Unit) = AnalysisCard("AI 분석") {
    when {
        data.comparableRecords < data.minimumPatternRecords -> EmptyText("아직 분석하기 위한 기록이 충분하지 않습니다. 회복활동 전후 기록이 ${data.minimumPatternRecords}회 이상 쌓이면 반복 패턴을 AI가 분석합니다.")
        loading -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Text("AI가 감정 변화 기록을 분석하고 있습니다.") }
        !text.isNullOrBlank() -> AiSections(text)
        error != null -> { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = retry) { Text("다시 시도") } }
        else -> Button(onClick = retry, colors = ButtonDefaults.buttonColors(containerColor = AnalysisAccent)) { Text("AI 감정 분석 받기") }
    }
}

@Composable private fun AiSections(raw: String) {
    AiAnalysisTextFormatter.sections(raw).forEachIndexed { index, (title, body) ->
        Surface(
            Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 10.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF5F9FC)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = AnalysisAccent)
                AiAnalysisTextFormatter.paragraphs(body).forEach { paragraph ->
                    Text(paragraph, modifier = Modifier.padding(top = 7.dp), lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                }
            }
        }
    }
}

@Composable private fun EvidenceCard(data: RecoveryInsightDataDto) = AnalysisCard("실제 분석 데이터") {
    StatRow("분석기간", data.period.label)
    StatRow("비교 가능한 전후 기록", "${data.comparableRecords}회")
    StatRow("분석된 회복활동", "${data.activityPatterns.size}종")
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text("전체 기록 평균 변화", fontWeight = FontWeight.Bold)
    data.overallChanges.forEach { StatRow(it.categoryLabel, signed(it.averageDelta)) }
}

@Composable private fun LongTermCard(data: RecoveryLongTermDto) = AnalysisCard("사연 회상 빈도") {
    if (!data.available || data.stories.isEmpty()) EmptyText("선택한 기간에 연결된 사연 회상 기록이 없어 장기 변화를 표시하지 않습니다.")
    else data.stories.take(5).forEach { story ->
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(story.storyTitle, fontWeight = FontWeight.Bold)
            val previousAvailable = story.previousDataAvailable ?: (story.previousDailyAverage != null)
            Text(if (!previousAvailable) "이전 기록 없음" else "이전 하루 평균 ${"%.2f".format(story.previousDailyAverage ?: 0.0)}회 → 최근 하루 평균 ${"%.2f".format(story.dailyAverage)}회", style = MaterialTheme.typography.bodySmall)
            Text(when { !previousAvailable -> "이전 기록 없음"; story.changePercent == null && (story.previousDailyAverage ?: 0.0) == 0.0 && story.dailyAverage > 0.0 -> "회상 빈도 증가"; story.changePercent == null -> "회상 빈도 변화 비교 불가"; story.changePercent > 0 -> "회상 빈도 ↑ ${formatPercent(abs(story.changePercent))}%"; story.changePercent < 0 -> "회상 빈도 ↓ ${formatPercent(abs(story.changePercent))}%"; else -> "회상 빈도 변화 없음" }, color = AnalysisAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun NumberedTitle(number: String, title: String, subtitle: String) { Column { Text("$number  $title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun AnalysisCard(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); content() } } }
@Composable private fun LoadingCard() = AnalysisCard("데이터를 불러오는 중") { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("AI 감정분석 데이터를 불러오는 중입니다.") } }
@Composable private fun EmptyText(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
@Composable private fun StatRow(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.SemiBold) }
@Composable private fun LegendDot(color: Color, text: String) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50))); Spacer(Modifier.width(5.dp)); Text(text, style = MaterialTheme.typography.bodySmall) } }
private fun signed(value: Double): String = if (value > 0) "+${formatNumber(value)}" else formatNumber(value)
private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
private fun formatPercent(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
