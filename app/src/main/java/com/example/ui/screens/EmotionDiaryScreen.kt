package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import android.util.Log
import com.example.BuildConfig
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.model.EmotionCategory
import com.example.data.model.PresetEmotions
import com.example.ui.components.EmotionPieChart
import com.example.ui.viewmodel.ChartTimeRange
import com.example.ui.viewmodel.EmotionViewModel
import com.example.ui.viewmodel.PieChartSegment
import com.example.utils.ShareUtils
import kotlinx.coroutines.launch

import com.example.ui.components.EmotionCategoryStat
import com.example.ui.components.EmotionOlympicRingsChart
import com.example.ui.components.EmotionTrendChart
import com.example.ui.components.EmotionStoryInsightsPanel

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.data.local.entity.InnerStoryEntity

import com.example.ui.components.ShareStoryDialog

import com.example.ui.components.SubEmotionPieChart
import com.example.ui.viewmodel.StatDisplayType
import com.example.ui.viewmodel.GraphScope
import com.example.data.remote.dto.StoryDto
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.example.ui.components.EmotionPieChart
import androidx.compose.material3.CircularProgressIndicator

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
    val view = LocalView.current
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var includeNicknameInShare by remember { mutableStateOf(true) }
    var includeStoryTitleInShare by remember { mutableStateOf(false) }
    var showStoryPicker by remember { mutableStateOf(false) }
    var selectedStory by remember { mutableStateOf<StoryDto?>(null) }

    val userNickname by viewModel.userNickname.collectAsStateWithLifecycle()
    val graphNickname = loginNickname?.takeIf { it.isNotBlank() } ?: userNickname?.takeIf { it.isNotBlank() }
    val selectedStatDisplayType by viewModel.selectedStatDisplayType.collectAsStateWithLifecycle()
    val subEmotionStats by viewModel.subEmotionStats.collectAsStateWithLifecycle()
    val pieChartSegments by viewModel.pieChartSegments.collectAsStateWithLifecycle()
    val graphState by viewModel.emotionGraphState.collectAsStateWithLifecycle()
    val storyInsightsState by viewModel.emotionStoryInsightsState.collectAsStateWithLifecycle()
    val serverCategoryStats by viewModel.serverEmotionCategoryStats.collectAsStateWithLifecycle()
    val serverSubEmotionStats by viewModel.serverSubEmotionStats.collectAsStateWithLifecycle()
    val serverPieSegments by viewModel.serverPieChartSegments.collectAsStateWithLifecycle()
    val serverRangeText by viewModel.serverGraphDateRangeText.collectAsStateWithLifecycle()
    val displayedCategoryStats = serverCategoryStats
    val displayedSubEmotionStats = serverSubEmotionStats
    val displayedPieSegments = serverPieSegments
    val displayedRangeText = serverRangeText.ifBlank { selectedDateRangeText }
    val canExportGraph = !graphState.isLoading && graphState.error == null && graphState.response != null
    val chartPicture = remember { Picture() }

    LaunchedEffect(Unit) { viewModel.setChartRange(ChartTimeRange.WEEK) }

    val filteredStories = remember(innerStories, selectedRange) {
        val today = viewModel.getTodayDateString()
        when (selectedRange) {
            ChartTimeRange.TODAY -> innerStories.filter { it.dateString == today }
            ChartTimeRange.WEEK -> {
                val start = viewModel.getDateBeforeDays(6)
                innerStories.filter { it.dateString in start..today }
            }
            ChartTimeRange.MONTH -> {
                val start = viewModel.getDateBeforeDays(29)
                innerStories.filter { it.dateString in start..today }
            }
            ChartTimeRange.ALL_TIME -> innerStories
            ChartTimeRange.CUSTOM -> innerStories
            ChartTimeRange.FAVORITES -> innerStories
        }
    }

    fun captureChartBitmap(): Bitmap? {
        return try {
            if (chartPicture.width > 0 && chartPicture.height > 0) {
                val bitmap = Bitmap.createBitmap(
                    chartPicture.width,
                    chartPicture.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawPicture(chartPicture)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Header Card - Bento Grid Style
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        if (showPageTitle) Text(
                            text = "감정그래프",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (showPageTitle) Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "기간별 감정 기록을 7감정 버블과 세부 통계로 확인해보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(selectedTabIndex = if (graphState.scope == GraphScope.ALL) 0 else 1) {
                    Tab(selected = graphState.scope == GraphScope.ALL, onClick = {
                        selectedStory = null
                        viewModel.setChartRange(ChartTimeRange.WEEK)
                    }, text = { Text("전체 감정") })
                    Tab(selected = graphState.scope == GraphScope.STORY, onClick = {
                        viewModel.enterStoryGraphScope()
                    }, text = { Text("나의 사연별 감정") })
                }
                if (graphState.scope == GraphScope.STORY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showStoryPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedStory?.let { "${it.title} · ${it.period}" } ?: "사연 선택 ▼")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Chart Range Selector FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChartTimeRange.entries.filter { it != ChartTimeRange.FAVORITES }.forEach { range ->
                        val isSelected = selectedRange == range
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (range == ChartTimeRange.CUSTOM) {
                                    showCustomDateDialog = true
                                } else if (graphState.scope == GraphScope.STORY) {
                                    selectedStory?.let { viewModel.loadStoryEmotionGraph(it.storyId, range) }
                                } else {
                                    viewModel.setChartRange(range)
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (range == ChartTimeRange.CUSTOM) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(range.label)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stat Display Type Switcher (대분류별 vs 세부 감정별)
                TabRow(
                    selectedTabIndex = selectedStatDisplayType.ordinal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    StatDisplayType.entries.forEach { statType ->
                        Tab(
                            selected = selectedStatDisplayType == statType,
                            onClick = { viewModel.setStatDisplayType(statType) },
                            text = {
                                Text(
                                    text = statType.label,
                                    fontWeight = if (selectedStatDisplayType == statType) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (graphState.scope == GraphScope.STORY && selectedStory == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        Text("감정그래프를 확인할 사연을 선택해주세요.")
                    }
                } else if (graphState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (graphState.error != null) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(graphState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = {
                            if (graphState.scope == GraphScope.STORY) {
                                selectedStory?.let { viewModel.loadStoryEmotionGraph(it.storyId, selectedRange) }
                            } else {
                                viewModel.loadEmotionGraph(selectedRange)
                            }
                        }) { Text("다시 시도") }
                    }
                } else if (selectedStatDisplayType == StatDisplayType.CATEGORY) {
                    EmotionOlympicRingsChart(
                        stats = displayedCategoryStats,
                        selectedDateRangeText = displayedRangeText,
                        userNickname = graphNickname,
                        showNickname = includeNicknameInShare,
                        picture = chartPicture,
                        selectedCategoryCode = storyInsightsState.selectedCategoryCode,
                        onCategorySelected = { stat ->
                            if (stat == null) viewModel.clearEmotionStoryInsights()
                            else viewModel.loadEmotionStoryInsights(
                                categoryCode = stat.category.code,
                                label = stat.categoryLabel,
                                count = stat.count,
                                period = graphState.response?.period,
                                storyId = graphState.story?.storyId
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SubEmotionPieChart(
                        subEmotionStats = displayedSubEmotionStats,
                        selectedDateRangeText = displayedRangeText,
                        userNickname = graphNickname,
                        showNickname = includeNicknameInShare,
                        picture = chartPicture,
                        selectedEmotionId = storyInsightsState.selectedEmotionId,
                        onEmotionSelected = { stat ->
                            if (stat == null) viewModel.clearEmotionStoryInsights()
                            else stat.emotionId?.let { emotionId ->
                                viewModel.loadEmotionStoryInsights(
                                    emotionId = emotionId,
                                    label = stat.emotionName,
                                    count = stat.count,
                                    period = graphState.response?.period,
                                    storyId = graphState.story?.storyId
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedStatDisplayType == StatDisplayType.CATEGORY && storyInsightsState.selectedCategoryCode != null) {
                    val selectedCategory = displayedCategoryStats.firstOrNull {
                        it.category.code == storyInsightsState.selectedCategoryCode
                    }
                    EmotionStoryInsightsPanel(
                        state = storyInsightsState,
                        selectionColor = selectedCategory?.color ?: MaterialTheme.colorScheme.primary,
                        onRetry = {
                            viewModel.loadEmotionStoryInsights(
                                categoryCode = storyInsightsState.selectedCategoryCode,
                                label = storyInsightsState.selectedLabel,
                                count = storyInsightsState.selectedCount,
                                period = graphState.response?.period,
                                storyId = graphState.story?.storyId
                            )
                        },
                        onOpenStory = onOpenStoryDetail
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (selectedStatDisplayType == StatDisplayType.SUB_EMOTION && storyInsightsState.selectedEmotionId != null) {
                    val selectedSubEmotion = displayedSubEmotionStats.firstOrNull {
                        it.emotionId == storyInsightsState.selectedEmotionId
                    }
                    EmotionStoryInsightsPanel(
                        state = storyInsightsState,
                        selectionColor = selectedSubEmotion?.color ?: MaterialTheme.colorScheme.primary,
                        parentLabel = selectedSubEmotion?.categoryLabel,
                        onRetry = {
                            viewModel.loadEmotionStoryInsights(
                                emotionId = storyInsightsState.selectedEmotionId,
                                label = storyInsightsState.selectedLabel,
                                count = storyInsightsState.selectedCount,
                                period = graphState.response?.period,
                                storyId = graphState.story?.storyId
                            )
                        },
                        onOpenStory = onOpenStoryDetail
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeNicknameInShare,
                        onCheckedChange = { includeNicknameInShare = it }
                    )
                    Text(
                        text = "공유 이미지에 닉네임 표시",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (graphState.scope == GraphScope.STORY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeStoryTitleInShare, onCheckedChange = { includeStoryTitleInShare = it })
                        Text("공유 이미지에 사연 제목 표시", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Action Bar: Image Export, Share and Link Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Image Export Button
                    Button(
                        enabled = canExportGraph,
                        onClick = {
                            if (BuildConfig.DEBUG) Log.d("WOOCHISO_GRAPH", "GRAPH_IMAGE_SAVE_CLICK")
                            val bitmap = captureChartBitmap()
                            if (bitmap != null) {
                                if (BuildConfig.DEBUG) Log.d("WOOCHISO_GRAPH", "GRAPH_IMAGE_CAPTURE_SUCCESS")
                                val saved = ShareUtils.saveBitmapToGallery(context, bitmap, "WoochisoEmotionGraph")
                                if (saved) {
                                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_GRAPH", "GRAPH_IMAGE_SAVE_SUCCESS")
                                    Toast.makeText(context, "감정그래프 이미지가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "이미지를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (BuildConfig.DEBUG) Log.d("WOOCHISO_GRAPH", "GRAPH_IMAGE_SAVE_FAILED reason=capture_unavailable")
                                Toast.makeText(context, "이미지를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.AppActionButton,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save Chart Image",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("이미지 저장", style = MaterialTheme.typography.labelSmall)
                    }

                    // Social Share Button
                    OutlinedButton(
                        enabled = canExportGraph,
                        onClick = {
                            val bitmap = captureChartBitmap()
                            val summaryText = buildString {
                                val namePrefix = if (includeNicknameInShare && !graphNickname.isNullOrBlank()) "${graphNickname}님의 " else ""
                                val graphTitle = if (graphState.scope == GraphScope.STORY) {
                                    if (includeStoryTitleInShare) selectedStory?.title ?: "선택한 사연" else "선택한 사연의 감정그래프"
                                } else "감정그래프"
                                append("📖 [${namePrefix}${graphTitle}]\n")
                                append("기간: $displayedRangeText\n\n")
                                append("감정 발생 분포:\n")
                                displayedCategoryStats.forEach { stat ->
                                    append("- ${stat.categoryLabel}: ${stat.count}회 (${String.format("%.1f", stat.percentage)}%)\n")
                                }
                                append("\n#감정그래프 #우치소 #희노애락애오욕")
                            }
                            ShareUtils.shareEmotionDiary(context, summaryText, bitmap)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share on Social",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("공유하기", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        enabled = canExportGraph,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("우치소 링크", "https://woochiso.com/"))
                            Toast.makeText(context, "우치소 링크를 복사했습니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Copy Woochiso Link",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("링크 복사", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (canExportGraph) {
                    Spacer(modifier = Modifier.height(12.dp))

                    EmotionPieChart(
                        segments = displayedPieSegments,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    EmotionTrendChart(
                        dailySeries = graphState.response?.dailySeries.orEmpty(),
                        categories = displayedCategoryStats,
                        included = graphState.response?.dailySeriesIncluded == true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (graphState.scope == GraphScope.STORY && selectedStory != null && graphState.response?.totalCount == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("아직 이 사연과 연결된 감정 기록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 감정그래프 화면은 서버 그래프와 공유 영역까지만 표시한다.
            // Room 데이터는 유지하되 기존 일지/사연 목록을 이 화면에는 붙이지 않는다.
            return@LazyColumn

            if (diaryEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "작성된 감정 다이어리가 없습니다.\n즐찾감정에서 오늘 느낀 감정을 기록해보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(diaryEntries, key = { it.id }) { entry ->
                    val primaryCat = EmotionCategory.fromCode(entry.primaryCategoryCode)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = primaryCat.color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${primaryCat.hanja} ${primaryCat.koreanLabel}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryCat.color,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.dateString,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteDiaryEntry(entry.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete entry",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Emotion Chips
                            val emotionList = entry.emotionsListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                emotionList.forEach { word ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "#$word",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Memo Text
                            if (entry.memo.isNotBlank()) {
                                Text(
                                    text = entry.memo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Intensity Indicator Stars
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "감정 강도: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (index < entry.intensity) primaryCat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Section: Related Inner Stories List (사연 리스트) ---
            item {
                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📖 나의 사연 리스트 (${filteredStories.size}개)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = selectedDateRangeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredStories.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "해당 기간에 기록된 나의 사연이 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredStories, key = { "story_${it.id}" }) { story ->
                    val cat = EmotionCategory.fromCode(story.primaryCategoryCode)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = cat.color.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "${cat.hanja} ${cat.koreanLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = cat.color,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = story.dateString,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteInnerStory(story.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete story",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = story.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (story.content.isNotBlank() && story.content != story.title) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = story.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (story.reflection.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = cat.color.copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = cat.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "성찰: ${story.reflection}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (story.associatedEmotionsCsv.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val tagList = story.associatedEmotionsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    tagList.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    // Modal Dialog to write new Diary Entry
    if (showAddEntryDialog) {
        var primaryCat by remember { mutableStateOf(EmotionCategory.JOY) }
        var selectedEmotions by remember { mutableStateOf(setOf<String>()) }
        var memoText by remember { mutableStateOf("") }
        var intensityValue by remember { mutableStateOf(3f) }

        val categoryWords = remember(primaryCat) {
            PresetEmotions.ALL_EMOTIONS.filter { it.category == primaryCat }.map { it.word }
        }

        AlertDialog(
            onDismissRequest = { showAddEntryDialog = false },
            title = { Text("새 감정 다이어리 작성") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "1. 대표 감정 영역 선택:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EmotionCategory.entries.forEach { cat ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = if (primaryCat == cat) cat.color else MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { primaryCat = cat }
                            ) {
                                Text(
                                    text = cat.hanja.substring(0, 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (primaryCat == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "2. 세부 감정 단어 선택:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryWords.take(12).forEach { word ->
                            val isSelected = selectedEmotions.contains(word)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedEmotions = if (isSelected) selectedEmotions - word else selectedEmotions + word
                                },
                                label = { Text(word, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "3. 감정 강도: ${intensityValue.toInt()}단계",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = intensityValue,
                        onValueChange = { intensityValue = it },
                        valueRange = 1f..5f,
                        steps = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it },
                        label = { Text("내면의 솔직한 느낌 적기 (선택)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.AppActionButton,
                        contentColor = Color.White
                    ),
                    onClick = {
                        val emotionsToSave = if (selectedEmotions.isEmpty()) listOf(primaryCat.koreanLabel) else selectedEmotions.toList()
                        viewModel.addDiaryEntry(
                            dateString = viewModel.getTodayDateString(),
                            primaryCategory = primaryCat,
                            selectedEmotions = emotionsToSave,
                            memo = memoText.trim(),
                            intensity = intensityValue.toInt()
                        )
                        showAddEntryDialog = false
                    }
                ) {
                    Text("저장하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEntryDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showStoryPicker) {
        AlertDialog(
            onDismissRequest = { showStoryPicker = false },
            title = { Text("사연 선택") },
            text = {
                if (serverStories.isEmpty()) {
                    Text("등록된 나의 사연이 없습니다.")
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(serverStories, key = { it.storyId }) { story ->
                            TextButton(
                                onClick = {
                                    selectedStory = story
                                    showStoryPicker = false
                                    viewModel.loadStoryEmotionGraph(story.storyId, ChartTimeRange.WEEK)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("${story.title} · ${story.period}") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStoryPicker = false }) { Text("닫기") } }
        )
    }

    if (showCustomDateDialog) {
        var startDateInput by remember { mutableStateOf(viewModel.getDateBeforeDays(6)) }
        var endDateInput by remember { mutableStateOf(viewModel.getTodayDateString()) }

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("기간 직접 선택")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "원하는 시작일과 종료일을 YYYY-MM-DD 형식으로 입력하거나 빠른 선택 버튼을 누르세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = startDateInput,
                        onValueChange = { startDateInput = it },
                        label = { Text("시작일 (예: 2026-07-01)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = endDateInput,
                        onValueChange = { endDateInput = it },
                        label = { Text("종료일 (예: 2026-07-28)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "빠른 선택:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                startDateInput = viewModel.getDateBeforeDays(6)
                                endDateInput = viewModel.getTodayDateString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("7일간", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                startDateInput = viewModel.getDateBeforeDays(13)
                                endDateInput = viewModel.getTodayDateString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("14일간", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                startDateInput = viewModel.getDateBeforeDays(29)
                                endDateInput = viewModel.getTodayDateString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("30일간", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.AppActionButton,
                        contentColor = Color.White
                    ),
                    onClick = {
                        val s = startDateInput.trim()
                        val e = endDateInput.trim()
                        if (s.isNotBlank() && e.isNotBlank()) {
                            if (graphState.scope == GraphScope.STORY) {
                                selectedStory?.let { viewModel.loadStoryEmotionGraph(it.storyId, ChartTimeRange.CUSTOM, s, e) }
                            } else {
                                viewModel.setCustomDateRange(s, e)
                            }
                        }
                        showCustomDateDialog = false
                    }
                ) {
                    Text("조회하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}
