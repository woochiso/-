package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.local.entity.FavoriteEmotionEntity
import com.example.data.local.entity.InnerStoryEntity
import com.example.data.model.EmotionCategory
import com.example.data.model.EmotionWordItem
import com.example.data.model.PresetEmotions
import com.example.data.repository.EmotionRepository
import com.example.data.repository.EmotionServerRepository
import com.example.data.repository.EmotionServerResult
import com.example.data.remote.RetrofitClient
import com.example.data.remote.dto.*
import com.example.auth.TokenManager
import com.example.BuildConfig
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.ui.components.EmotionCategoryStat

data class FavoriteServerUiState(
    val favorites: List<FavoriteEmotionServerDto> = emptyList(),
    val recordDate: String = "",
    val editedTodayCounts: Map<Long, Int> = emptyMap(),
    val addCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val addingEmotionIds: Set<Long> = emptySet(),
    val correctingEmotionIds: Set<Long> = emptySet(),
    val removingEmotionIds: Set<Long> = emptySet(),
    val error: String? = null,
    val requiresLogin: Boolean = false
)

data class HumanEmotionCategoryItem(
    val categoryId: Long,
    val category: EmotionCategory,
    val label: String,
    val description: String
)

data class HumanEmotionsUiState(
    val categories: List<HumanEmotionCategoryItem> = emptyList(),
    val emotions: List<EmotionWordItem> = emptyList(),
    val isLoading: Boolean = false,
    val savingEmotionIds: Set<Long> = emptySet(),
    val error: String? = null,
    val requiresLogin: Boolean = false
)

data class EmotionStoryUiState(
    val emotionId: Long? = null,
    val isLoading: Boolean = false,
    val options: EmotionStoryOptionsResponse? = null,
    val isSaving: Boolean = false,
    val error: String? = null
)

data class EmotionStoryConnection(
    val emotionId: Long,
    val selectedStoryId: Long,
    val selectedStoryTitle: String,
    val selectedStoryPeriod: String,
    val linkStrength: Int
)

data class PieChartSegment(
    val category: EmotionCategory,
    val count: Int,
    val percentage: Float,
    val color: androidx.compose.ui.graphics.Color = category.color,
    val categoryLabel: String = "${category.hanja} ${category.koreanLabel}",
    val categoryHanja: String = category.hanja
)

data class SubEmotionStat(
    val emotionId: Long? = null,
    val emotionName: String,
    val category: EmotionCategory,
    val count: Int,
    val percentage: Float,
    val color: androidx.compose.ui.graphics.Color = category.color,
    val categoryLabel: String = "${category.hanja} ${category.koreanLabel}",
    val averageLinkStrength: Double? = null
)

enum class GraphScope { ALL, STORY }

data class EmotionGraphUiState(
    val isLoading: Boolean = false,
    val response: EmotionGraphResponse? = null,
    val error: String? = null,
    val requiresLogin: Boolean = false,
    val scope: GraphScope = GraphScope.ALL,
    val story: StoryGraphStoryDto? = null
)

data class EmotionStoryInsightsUiState(
    val isLoading: Boolean = false,
    val response: EmotionStoryInsightsResponse? = null,
    val selectedCategoryCode: String? = null,
    val selectedEmotionId: Long? = null,
    val selectedLabel: String = "",
    val selectedCount: Int = 0,
    val error: String? = null,
    val requiresLogin: Boolean = false
)

enum class StatDisplayType(val label: String) {
    CATEGORY("대분류별 통계"),
    SUB_EMOTION("세부 감정별 통계")
}

enum class ChartTimeRange(val label: String) {
    TODAY("오늘"),
    WEEK("최근 7일"),
    MONTH("최근 30일"),
    ALL_TIME("전체"),
    CUSTOM("기간 선택"),
    FAVORITES("즐겨찾기")
}

class EmotionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EmotionRepository
    private val serverRepository = EmotionServerRepository(RetrofitClient.apiService, TokenManager(application))
    private val _favoriteServerState = MutableStateFlow(FavoriteServerUiState())
    val favoriteServerState: StateFlow<FavoriteServerUiState> = _favoriteServerState.asStateFlow()
    private val pendingAddRequests = mutableMapOf<Long, TodayEmotionAddRequest>()
    private val pendingCorrectionRequests = mutableMapOf<Long, TodayEmotionCorrectionRequest>()
    private val _humanEmotionsState = MutableStateFlow(HumanEmotionsUiState())
    val humanEmotionsState: StateFlow<HumanEmotionsUiState> = _humanEmotionsState.asStateFlow()
    private val _emotionStoryState = MutableStateFlow(EmotionStoryUiState())
    val emotionStoryState: StateFlow<EmotionStoryUiState> = _emotionStoryState.asStateFlow()
    private val _emotionStoryConnections = MutableStateFlow<Map<Long, EmotionStoryConnection>>(emptyMap())
    val emotionStoryConnections: StateFlow<Map<Long, EmotionStoryConnection>> = _emotionStoryConnections.asStateFlow()
    private val _emotionGraphState = MutableStateFlow(EmotionGraphUiState())
    val emotionGraphState: StateFlow<EmotionGraphUiState> = _emotionGraphState.asStateFlow()
    private val _emotionStoryInsightsState = MutableStateFlow(EmotionStoryInsightsUiState())
    val emotionStoryInsightsState: StateFlow<EmotionStoryInsightsUiState> = _emotionStoryInsightsState.asStateFlow()
    val serverEmotionCategoryStats: StateFlow<List<EmotionCategoryStat>> = _emotionGraphState.map { state ->
        state.response?.categories?.map { dto ->
            val category = serverCategory(dto.categoryCode)
            EmotionCategoryStat(
                category = category,
                count = dto.totalCount,
                percentage = dto.percentage.toFloat(),
                categoryLabel = dto.categoryLabel,
                categoryHanja = dto.categoryHanja,
                color = com.example.ui.theme.EmotionColors.forCategoryCode(dto.categoryCode, category.color)
            )
        }.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val serverSubEmotionStats: StateFlow<List<SubEmotionStat>> = _emotionGraphState.map { state ->
        val total = state.response?.totalCount ?: 0
        state.response?.emotions?.map { dto ->
            val category = serverCategory(dto.categoryCode)
            SubEmotionStat(
                emotionId = dto.emotionId,
                emotionName = dto.emotionName,
                category = category,
                count = dto.totalCount,
                percentage = if (total > 0) dto.totalCount * 100f / total else 0f,
                color = com.example.ui.theme.EmotionColors.forCategoryCode(dto.categoryCode, category.color),
                categoryLabel = dto.categoryLabel
                ,averageLinkStrength = dto.averageLinkStrength
            )
        }.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val serverPieChartSegments: StateFlow<List<PieChartSegment>> = serverEmotionCategoryStats.map { stats ->
        stats.map { PieChartSegment(it.category, it.count, it.percentage, it.color, it.categoryLabel, it.categoryHanja) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val serverGraphDateRangeText: StateFlow<String> = _emotionGraphState.map { state ->
        state.response?.period?.let { "${periodLabel(it.type)} (${it.startDate} ~ ${it.endDate})" } ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        val db = AppDatabase.getDatabase(application)
        repository = EmotionRepository(
            favoriteEmotionDao = db.favoriteEmotionDao(),
            diaryEntryDao = db.diaryEntryDao(),
            innerStoryDao = db.innerStoryDao(),
            emotionCategoryDao = db.emotionCategoryDao(),
            emotionItemDao = db.emotionItemDao(),
            emotionRecordDao = db.emotionRecordDao()
        )
        checkDailyReset()
        viewModelScope.launch {
            repository.purgeQuietTranquilityStories()
            repository.cleanupDuplicateRecords()
        }
    }

    fun checkDailyReset() {
        viewModelScope.launch {
            repository.checkAndResetDailyFavorites()
        }
    }

    val favorites: StateFlow<List<FavoriteEmotionEntity>> = repository.allFavorites
        .map { list ->
            val todayStr = getTodayDateString()
            list.map { fav ->
                if (fav.lastUpdatedDate != todayStr && fav.countToday != 0) {
                    fav.copy(countToday = 0)
                } else {
                    fav
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = repository.allDiaryEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val innerStories: StateFlow<List<InnerStoryEntity>> = repository.allInnerStories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val prefs = application.getSharedPreferences("emotion_diary_prefs", Context.MODE_PRIVATE)

    private val _userNickname = MutableStateFlow<String?>(prefs.getString("user_nickname", null))
    val userNickname: StateFlow<String?> = _userNickname.asStateFlow()

    fun saveUserNickname(nickname: String) {
        val trimmed = nickname.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("user_nickname", trimmed).apply()
            _userNickname.value = trimmed
            viewModelScope.launch {
                _toastEvent.emit("반갑습니다, ${trimmed}님!")
            }
        }
    }

    fun updateNickname(newNickname: String) {
        val trimmed = newNickname.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("user_nickname", trimmed).apply()
            _userNickname.value = trimmed
            viewModelScope.launch {
                _toastEvent.emit("별명이 '${trimmed}'(으)로 변경되었습니다.")
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<EmotionCategory?>(null)
    val selectedCategoryFilter: StateFlow<EmotionCategory?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedChartRange = MutableStateFlow(ChartTimeRange.WEEK)
    val selectedChartRange: StateFlow<ChartTimeRange> = _selectedChartRange.asStateFlow()

    private val _selectedDiaryDate = MutableStateFlow(getTodayDateString())
    val selectedDiaryDate: StateFlow<String> = _selectedDiaryDate.asStateFlow()

    private val _customStartDate = MutableStateFlow(getTodayDateString())
    val customStartDate: StateFlow<String> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(getTodayDateString())
    val customEndDate: StateFlow<String> = _customEndDate.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Filtered diary entries by selected date range
    val filteredDiaryEntries: StateFlow<List<DiaryEntryEntity>> = combine(
        selectedChartRange,
        customStartDate,
        customEndDate,
        diaryEntries
    ) { range, start, end, entries ->
        val today = getTodayDateString()
        when (range) {
            ChartTimeRange.TODAY -> entries.filter { it.dateString == today }
            ChartTimeRange.WEEK -> {
                val startWeek = getDateBeforeDays(6)
                entries.filter { it.dateString in startWeek..today }
            }
            ChartTimeRange.MONTH -> {
                val startMonth = getDateBeforeDays(29)
                entries.filter { it.dateString in startMonth..today }
            }
            ChartTimeRange.ALL_TIME -> entries
            ChartTimeRange.CUSTOM -> {
                val minDate = if (start <= end) start else end
                val maxDate = if (start <= end) end else start
                entries.filter { it.dateString in minDate..maxDate }
            }
            ChartTimeRange.FAVORITES -> entries
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Formatted range text description
    val selectedDateRangeText: StateFlow<String> = combine(
        selectedChartRange,
        customStartDate,
        customEndDate
    ) { range, start, end ->
        val today = getTodayDateString()
        when (range) {
            ChartTimeRange.TODAY -> "오늘 (${today})"
            ChartTimeRange.WEEK -> "최근 7일 (${getDateBeforeDays(6)} ~ ${today})"
            ChartTimeRange.MONTH -> "최근 30일 (${getDateBeforeDays(29)} ~ ${today})"
            ChartTimeRange.ALL_TIME -> "전체 기록 기간"
            ChartTimeRange.CUSTOM -> {
                val minDate = if (start <= end) start else end
                val maxDate = if (start <= end) end else start
                "선택 기간 ($minDate ~ $maxDate)"
            }
            ChartTimeRange.FAVORITES -> "자주 느끼는 감정 기준"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "최근 7일"
    )

    // 7 Olympic Category Stats calculation
    val emotionCategoryStats: StateFlow<List<EmotionCategoryStat>> = combine(
        selectedChartRange,
        filteredDiaryEntries,
        favorites
    ) { range, entries, favList ->
        val categoryCountMap = EmotionCategory.entries.associateWith { 0 }.toMutableMap()

        if (range == ChartTimeRange.FAVORITES) {
            favList.forEach { fav ->
                val cat = EmotionCategory.fromCode(fav.categoryCode)
                val count = if (fav.countTotal > 0) fav.countTotal else fav.countToday
                categoryCountMap[cat] = (categoryCountMap[cat] ?: 0) + count
            }
        } else {
            // Group entries by dateString so each date's single saved record is used for counts
            val entriesByDate = entries.groupBy { it.dateString }
            entriesByDate.forEach { (_, dateEntries) ->
                val mainEntry = dateEntries.find { it.memo.startsWith("오늘의 즐찾 감정 저장") }
                    ?: dateEntries.maxByOrNull { it.timestamp }

                mainEntry?.let { entry ->
                    val entryFallbackCat = EmotionCategory.fromCode(entry.primaryCategoryCode)
                    val words = entry.emotionsListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (words.isEmpty()) {
                        categoryCountMap[entryFallbackCat] = (categoryCountMap[entryFallbackCat] ?: 0) + 1
                    } else {
                        words.forEach { rawWord ->
                            val count = Regex("(\\d+)회").find(rawWord)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                            val cleanWord = rawWord.replace(Regex("\\s*\\d+회"), "")
                                .replace(Regex("\\(사연:.*?\\)"), "")
                                .trim()
                            val resolvedCat = PresetEmotions.ALL_EMOTIONS.find { it.word == cleanWord }?.category ?: entryFallbackCat
                            categoryCountMap[resolvedCat] = (categoryCountMap[resolvedCat] ?: 0) + count
                        }
                    }
                }
            }
        }

        val totalCount = categoryCountMap.values.sum()

        EmotionCategory.entries.map { cat ->
            val count = categoryCountMap[cat] ?: 0
            val pct = if (totalCount > 0) (count.toFloat() / totalCount.toFloat()) * 100f else 0f
            EmotionCategoryStat(category = cat, count = count, percentage = pct)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EmotionCategory.entries.map { EmotionCategoryStat(it, 0, 0f) }
    )

    // Combined filtered human emotions list with favorite status
    val humanEmotionsList: StateFlow<List<EmotionWordItem>> = combine(
        _selectedCategoryFilter,
        favorites
    ) { categoryFilter, favList ->
        val favoriteSet = favList.map { it.word }.toSet()

        // Combine preset emotions with any custom added favorites
        val customFavItems = favList.filter { fav ->
            PresetEmotions.ALL_EMOTIONS.none { it.word == fav.word }
        }.map { fav ->
            val cat = EmotionCategory.fromCode(fav.categoryCode)
            EmotionWordItem(word = fav.word, category = cat, isFavorite = true)
        }

        val allEmotions = PresetEmotions.ALL_EMOTIONS + customFavItems

        allEmotions.filter { item ->
            categoryFilter == null || item.category == categoryFilter
        }.map { item ->
            item.copy(isFavorite = favoriteSet.contains(item.word))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PresetEmotions.ALL_EMOTIONS
    )

    private val _selectedStatDisplayType = MutableStateFlow(StatDisplayType.CATEGORY)
    val selectedStatDisplayType: StateFlow<StatDisplayType> = _selectedStatDisplayType.asStateFlow()

    fun setStatDisplayType(type: StatDisplayType) {
        if (_selectedStatDisplayType.value != type) clearEmotionStoryInsights()
        _selectedStatDisplayType.value = type
    }

    // Sub-emotion detailed stats calculation
    val subEmotionStats: StateFlow<List<SubEmotionStat>> = combine(
        selectedChartRange,
        filteredDiaryEntries,
        favorites,
        repository.allEmotionRecords,
        repository.allEmotionItems
    ) { range, entries, favList, records, items ->
        val wordCountMap = mutableMapOf<String, Pair<EmotionCategory, Int>>()
        val itemCategoryMap = items.associate { item ->
            val cat = when (item.categoryId) {
                1 -> EmotionCategory.JOY
                2 -> EmotionCategory.ANGER
                3 -> EmotionCategory.SORROW
                4 -> EmotionCategory.PLEASURE
                5 -> EmotionCategory.LOVE
                6 -> EmotionCategory.HATRED
                7 -> EmotionCategory.DESIRE
                else -> EmotionCategory.JOY
            }
            item.emotionName to cat
        }

        if (range == ChartTimeRange.FAVORITES) {
            favList.forEach { fav ->
                val cat = EmotionCategory.fromCode(fav.categoryCode)
                val count = if (fav.countTotal > 0) fav.countTotal else fav.countToday
                if (count > 0) {
                    val current = wordCountMap[fav.word]?.second ?: 0
                    wordCountMap[fav.word] = Pair(cat, current + count)
                }
            }
        } else {
            entries.forEach { entry ->
                val cat = EmotionCategory.fromCode(entry.primaryCategoryCode)
                val words = entry.emotionsListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                words.forEach { rawWord ->
                    val count = Regex("(\\d+)회").find(rawWord)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    val cleanWord = rawWord.replace(Regex("\\s*\\d+회"), "").trim()
                    val resolvedCat = itemCategoryMap[cleanWord] ?: cat
                    val current = wordCountMap[cleanWord]?.second ?: 0
                    wordCountMap[cleanWord] = Pair(resolvedCat, current + count)
                }
            }

            val today = getTodayDateString()
            val filteredRecords = when (range) {
                ChartTimeRange.TODAY -> records.filter { it.recordDate == today }
                ChartTimeRange.WEEK -> {
                    val start = getDateBeforeDays(6)
                    records.filter { it.recordDate in start..today }
                }
                ChartTimeRange.MONTH -> {
                    val start = getDateBeforeDays(29)
                    records.filter { it.recordDate in start..today }
                }
                ChartTimeRange.ALL_TIME -> records
                ChartTimeRange.CUSTOM -> records
                ChartTimeRange.FAVORITES -> emptyList()
            }

            val itemIdToNameMap = items.associate { it.id to it.emotionName }
            val categoryIdToCatMap = mapOf(
                1 to EmotionCategory.JOY,
                2 to EmotionCategory.ANGER,
                3 to EmotionCategory.SORROW,
                4 to EmotionCategory.PLEASURE,
                5 to EmotionCategory.LOVE,
                6 to EmotionCategory.HATRED,
                7 to EmotionCategory.DESIRE
            )

            filteredRecords.forEach { rec ->
                val name = itemIdToNameMap[rec.emotionItemId]
                val cat = categoryIdToCatMap[rec.categoryId] ?: EmotionCategory.JOY
                if (name != null && !wordCountMap.containsKey(name)) {
                    val current = wordCountMap[name]?.second ?: 0
                    wordCountMap[name] = Pair(cat, current + rec.count)
                }
            }
        }

        val totalCount = wordCountMap.values.sumOf { it.second }
        if (totalCount == 0) {
            emptyList()
        } else {
            wordCountMap.map { (word, pair) ->
                val pct = (pair.second.toFloat() / totalCount.toFloat()) * 100f
                SubEmotionStat(
                    emotionName = word,
                    category = pair.first,
                    count = pair.second,
                    percentage = pct
                )
            }.sortedByDescending { it.count }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Pie chart statistics calculation - Unified with emotionCategoryStats
    val pieChartSegments: StateFlow<List<PieChartSegment>> = emotionCategoryStats.map { stats ->
        stats.map { stat ->
            PieChartSegment(
                category = stat.category,
                count = stat.count,
                percentage = stat.percentage
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EmotionCategory.entries.map { PieChartSegment(it, 0, 0f) }
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: EmotionCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun setChartRange(range: ChartTimeRange) {
        _selectedChartRange.value = range
        if (range != ChartTimeRange.CUSTOM) loadEmotionGraph(range)
    }

    fun setCustomDateRange(start: String, end: String) {
        _customStartDate.value = start
        _customEndDate.value = end
        _selectedChartRange.value = ChartTimeRange.CUSTOM
        loadEmotionGraph(ChartTimeRange.CUSTOM, minOf(start, end), maxOf(start, end))
    }

    fun loadEmotionGraph(range: ChartTimeRange = _selectedChartRange.value, startDate: String? = null, endDate: String? = null) {
        clearEmotionStoryInsights()
        viewModelScope.launch {
            _emotionGraphState.value = EmotionGraphUiState(isLoading = true)
            val period = when (range) {
                ChartTimeRange.TODAY -> "today"
                ChartTimeRange.WEEK -> "7d"
                ChartTimeRange.MONTH -> "30d"
                ChartTimeRange.ALL_TIME, ChartTimeRange.FAVORITES -> "all"
                ChartTimeRange.CUSTOM -> null
            }
            when (val result = serverRepository.graph(period, startDate, endDate)) {
                is EmotionServerResult.Success -> {
                    _emotionGraphState.value = EmotionGraphUiState(response = result.value)
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "EMOTION_GRAPH_SUCCESS period=${result.value.period?.type} start=${result.value.period?.startDate} end=${result.value.period?.endDate} total=${result.value.totalCount}")
                }
                EmotionServerResult.Unauthorized -> _emotionGraphState.value = EmotionGraphUiState(error = "로그인이 만료되었습니다. 다시 로그인해주세요.", requiresLogin = true)
                is EmotionServerResult.Error -> _emotionGraphState.value = EmotionGraphUiState(error = result.message)
            }
        }
    }

    fun loadStoryEmotionGraph(storyId: Long, range: ChartTimeRange = ChartTimeRange.WEEK, startDate: String? = null, endDate: String? = null) {
        clearEmotionStoryInsights()
        viewModelScope.launch {
            _selectedChartRange.value = range
            _emotionGraphState.value = EmotionGraphUiState(isLoading = true, scope = GraphScope.STORY)
            val period = when (range) {
                ChartTimeRange.TODAY -> "today"
                ChartTimeRange.WEEK -> "7d"
                ChartTimeRange.MONTH -> "30d"
                ChartTimeRange.ALL_TIME, ChartTimeRange.FAVORITES -> "all"
                ChartTimeRange.CUSTOM -> "custom"
            }
            if (BuildConfig.DEBUG) {
                Log.d("WOOCHISO_EMOTION", "STORY_GRAPH_REQUEST storyId=$storyId period=$period")
            }
            when (val result = serverRepository.storyGraph(storyId, period, startDate, endDate)) {
                is EmotionServerResult.Success -> {
                    val body = result.value
                    val normalized = EmotionGraphResponse(
                        success = body.success, period = body.period, totalCount = body.totalCount,
                        categories = body.categories.orEmpty(), emotions = body.emotions.orEmpty(),
                        dailySeriesIncluded = body.dailySeriesIncluded, dailySeries = body.dailySeries.orEmpty(),
                        message = body.message
                    )
                    _emotionGraphState.value = EmotionGraphUiState(response = normalized, scope = GraphScope.STORY, story = body.story)
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "STORY_GRAPH_RESPONSE success=${body.success} totalCount=${body.totalCount} categoriesCount=${body.categories.orEmpty().size} emotionsCount=${body.emotions.orEmpty().size} dailySeriesIncluded=${body.dailySeriesIncluded}")
                }
                EmotionServerResult.Unauthorized -> _emotionGraphState.value = EmotionGraphUiState(error = "로그인이 만료되었습니다. 다시 로그인해주세요.", requiresLogin = true, scope = GraphScope.STORY)
                is EmotionServerResult.Error -> _emotionGraphState.value = EmotionGraphUiState(error = result.message, scope = GraphScope.STORY)
            }
        }
    }

    fun enterStoryGraphScope() {
        clearEmotionStoryInsights()
        _selectedChartRange.value = ChartTimeRange.WEEK
        _emotionGraphState.value = EmotionGraphUiState(scope = GraphScope.STORY)
    }

    fun loadEmotionStoryInsights(
        categoryCode: String? = null,
        emotionId: Long? = null,
        label: String,
        count: Int,
        period: EmotionGraphPeriodDto?,
        storyId: Long? = null
    ) {
        if ((categoryCode == null) == (emotionId == null) || period == null) return
        viewModelScope.launch {
            _emotionStoryInsightsState.value = EmotionStoryInsightsUiState(
                isLoading = true,
                selectedCategoryCode = categoryCode,
                selectedEmotionId = emotionId,
                selectedLabel = label,
                selectedCount = count
            )
            val startDate = period.startDate.takeIf { period.type == "custom" }
            val endDate = period.endDate.takeIf { period.type == "custom" }
            when (val result = serverRepository.storyInsights(
                categoryCode, emotionId, period.type, startDate, endDate, storyId
            )) {
                is EmotionServerResult.Success -> _emotionStoryInsightsState.value =
                    EmotionStoryInsightsUiState(
                        response = result.value,
                        selectedCategoryCode = categoryCode,
                        selectedEmotionId = emotionId,
                        selectedLabel = label,
                        selectedCount = count
                    )
                EmotionServerResult.Unauthorized -> _emotionStoryInsightsState.value =
                    EmotionStoryInsightsUiState(
                        selectedCategoryCode = categoryCode,
                        selectedEmotionId = emotionId,
                        selectedLabel = label,
                        selectedCount = count,
                        error = "로그인이 만료되었습니다. 다시 로그인해주세요.",
                        requiresLogin = true
                    )
                is EmotionServerResult.Error -> _emotionStoryInsightsState.value =
                    EmotionStoryInsightsUiState(
                        selectedCategoryCode = categoryCode,
                        selectedEmotionId = emotionId,
                        selectedLabel = label,
                        selectedCount = count,
                        error = "관련 사연을 불러오지 못했습니다."
                    )
            }
        }
    }

    fun clearEmotionStoryInsights() {
        _emotionStoryInsightsState.value = EmotionStoryInsightsUiState()
    }

    private fun serverCategory(code: String): EmotionCategory = when (code.uppercase()) {
        "JOY" -> EmotionCategory.JOY
        "ANGER" -> EmotionCategory.ANGER
        "SADNESS", "SORROW" -> EmotionCategory.SORROW
        "PLEASURE" -> EmotionCategory.PLEASURE
        "LOVE" -> EmotionCategory.LOVE
        "DISLIKE", "HATRED" -> EmotionCategory.HATRED
        "DESIRE" -> EmotionCategory.DESIRE
        else -> EmotionCategory.JOY
    }

    private fun periodLabel(type: String): String = when (type) {
        "today" -> "오늘"
        "7d" -> "최근 7일"
        "30d" -> "최근 30일"
        "90d" -> "최근 90일"
        "1y" -> "최근 1년"
        "all" -> "전체 기록"
        else -> "선택 기간"
    }

    fun setSelectedDiaryDate(dateStr: String) {
        _selectedDiaryDate.value = dateStr
    }

    fun toggleFavorite(word: String, categoryCode: String) {
        viewModelScope.launch {
            val isAdded = repository.toggleFavorite(word, categoryCode)
            val msg = if (isAdded) "'$word' 감정을 즐겨찾기에 추가했습니다." else "'$word' 감정을 즐겨찾기에서 해제했습니다."
            _toastEvent.emit(msg)
        }
    }

    fun saveTodayEmotions() {
        if (_favoriteServerState.value.recordDate.isNotBlank()) {
            saveServerTodayEmotions()
            return
        }
        viewModelScope.launch {
            repository.saveTodayEmotionsToDiary()
            _toastEvent.emit("오늘 감정이 저장되었습니다.")
        }
    }

    fun loadHumanEmotions() {
        viewModelScope.launch {
            _humanEmotionsState.value = _humanEmotionsState.value.copy(isLoading = true, error = null)
            when (val result = serverRepository.master()) {
                is EmotionServerResult.Success -> {
                    val body = result.value
                    val categories = body.categories.map { dto ->
                        val category = serverCategory(dto.categoryCode)
                        HumanEmotionCategoryItem(
                            categoryId = dto.categoryId,
                            category = category,
                            label = dto.categoryLabel,
                            description = dto.description?.takeIf { it.isNotBlank() } ?: category.description
                        )
                    }
                    val emotions = body.emotions.map { dto ->
                        EmotionWordItem(
                            word = dto.emotionName,
                            category = serverCategory(dto.categoryCode),
                            isFavorite = dto.favorite,
                            emotionId = dto.emotionId,
                            description = dto.description
                        )
                    }
                    _humanEmotionsState.value = HumanEmotionsUiState(categories = categories, emotions = emotions)
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "HUMAN_EMOTIONS_MASTER_SUCCESS categories=${categories.size} emotions=${emotions.size}")
                }
                EmotionServerResult.Unauthorized -> _humanEmotionsState.value = HumanEmotionsUiState(
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요.", requiresLogin = true
                )
                is EmotionServerResult.Error -> _humanEmotionsState.value = HumanEmotionsUiState(error = result.message)
            }
        }
    }

    fun toggleServerFavorite(emotionId: Long) {
        val state = _humanEmotionsState.value
        if (emotionId in state.savingEmotionIds) return
        viewModelScope.launch {
            _humanEmotionsState.value = state.copy(savingEmotionIds = state.savingEmotionIds + emotionId, error = null)
            when (val result = serverRepository.toggleFavorite(emotionId)) {
                is EmotionServerResult.Success -> {
                    val body = result.value
                    val current = _humanEmotionsState.value
                    _humanEmotionsState.value = current.copy(
                        emotions = current.emotions.map { emotion ->
                            if (emotion.emotionId == body.emotionId) emotion.copy(isFavorite = body.favorite) else emotion
                        },
                        savingEmotionIds = current.savingEmotionIds - emotionId
                    )
                    _toastEvent.emit(body.message ?: if (body.favorite) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 제거되었습니다.")
                    loadServerFavorites()
                }
                EmotionServerResult.Unauthorized -> _humanEmotionsState.value = _humanEmotionsState.value.copy(
                    savingEmotionIds = _humanEmotionsState.value.savingEmotionIds - emotionId,
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요.",
                    requiresLogin = true
                )
                is EmotionServerResult.Error -> {
                    _humanEmotionsState.value = _humanEmotionsState.value.copy(
                        savingEmotionIds = _humanEmotionsState.value.savingEmotionIds - emotionId,
                        error = result.message
                    )
                    _toastEvent.emit("즐겨찾기를 저장하지 못했습니다. 다시 시도해주세요.")
                }
            }
        }
    }

    fun loadServerFavorites() {
        viewModelScope.launch {
            _favoriteServerState.value = _favoriteServerState.value.copy(isLoading = true, error = null)
            // Master is loaded first so every displayed ID is known to come from wc_emotions.
            when (val master = serverRepository.master()) {
                is EmotionServerResult.Success -> if (BuildConfig.DEBUG) {
                    Log.d("WOOCHISO_EMOTION", "EMOTIONS_MASTER_SUCCESS count=${master.value.emotions.size}")
                }
                EmotionServerResult.Unauthorized -> {
                    _favoriteServerState.value = _favoriteServerState.value.copy(isLoading = false, requiresLogin = true, error = "로그인이 만료되었습니다. 다시 로그인해주세요.")
                    return@launch
                }
                is EmotionServerResult.Error -> {
                    _favoriteServerState.value = _favoriteServerState.value.copy(isLoading = false, error = master.message)
                    return@launch
                }
            }
            when (val result = serverRepository.favorites()) {
                is EmotionServerResult.Success -> {
                    val body = result.value
                    repository.syncFavoriteCache(body.favorites, body.recordDate)
                    _favoriteServerState.value = FavoriteServerUiState(
                        favorites = body.favorites,
                        recordDate = body.recordDate,
                        editedTodayCounts = body.favorites.associate { it.emotionId to it.todayCount },
                        addCounts = body.favorites.associate { it.emotionId to 1 }
                    )
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "FAVORITES_SUCCESS count=${body.favorites.size} FAVORITES_RECORD_DATE=${body.recordDate}")
                    refreshEmotionStoryConnections(body.favorites, body.recordDate)
                }
                EmotionServerResult.Unauthorized -> _favoriteServerState.value = _favoriteServerState.value.copy(isLoading = false, requiresLogin = true, error = "로그인이 만료되었습니다. 다시 로그인해주세요.")
                is EmotionServerResult.Error -> _favoriteServerState.value = _favoriteServerState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun incrementServerFavorite(emotionId: Long) = editServerAddCount(emotionId, 1)
    fun decrementServerFavorite(emotionId: Long) = editServerAddCount(emotionId, -1)

    fun removeServerFavorite(emotionId: Long, onSuccess: () -> Unit = {}) {
        val state = _favoriteServerState.value
        if (emotionId in state.removingEmotionIds || state.favorites.none { it.emotionId == emotionId }) return
        viewModelScope.launch {
            _favoriteServerState.value = state.copy(
                removingEmotionIds = state.removingEmotionIds + emotionId,
                error = null
            )
            when (val result = serverRepository.toggleFavorite(emotionId, favorite = false)) {
                is EmotionServerResult.Success -> {
                    val current = _favoriteServerState.value
                    val remaining = current.favorites.filterNot { it.emotionId == emotionId }
                    repository.syncFavoriteCache(remaining, current.recordDate)
                    _favoriteServerState.value = current.copy(
                        favorites = remaining,
                        editedTodayCounts = current.editedTodayCounts - emotionId,
                        addCounts = current.addCounts - emotionId,
                        removingEmotionIds = current.removingEmotionIds - emotionId,
                        error = null
                    )
                    _emotionStoryConnections.value = _emotionStoryConnections.value - emotionId
                    val humanState = _humanEmotionsState.value
                    _humanEmotionsState.value = humanState.copy(
                        emotions = humanState.emotions.map { emotion ->
                            if (emotion.emotionId == emotionId) emotion.copy(isFavorite = false) else emotion
                        }
                    )
                    _toastEvent.emit(result.value.message ?: "즐겨찾기에서 제거되었습니다.")
                    onSuccess()
                }
                EmotionServerResult.Unauthorized -> _favoriteServerState.value = _favoriteServerState.value.copy(
                    removingEmotionIds = _favoriteServerState.value.removingEmotionIds - emotionId,
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요.",
                    requiresLogin = true
                )
                is EmotionServerResult.Error -> {
                    _favoriteServerState.value = _favoriteServerState.value.copy(
                        removingEmotionIds = _favoriteServerState.value.removingEmotionIds - emotionId,
                        error = result.message
                    )
                    _toastEvent.emit("즐겨찾기 해제에 실패했습니다. 다시 시도해주세요.")
                }
            }
        }
    }

    private fun editServerAddCount(emotionId: Long, delta: Int) {
        val state = _favoriteServerState.value
        if (emotionId in state.addingEmotionIds) return
        val current = state.addCounts[emotionId] ?: 1
        pendingAddRequests.remove(emotionId)
        _favoriteServerState.value = state.copy(
            addCounts = state.addCounts + (emotionId to (current + delta).coerceIn(1, 999))
        )
    }

    fun addServerFavoriteEmotion(emotionId: Long) {
        val state = _favoriteServerState.value
        if (emotionId in state.addingEmotionIds) return
        val favorite = state.favorites.firstOrNull { it.emotionId == emotionId } ?: return
        val addCount = state.addCounts[emotionId] ?: 1
        val existing = pendingAddRequests[emotionId]
        val request = existing?.takeIf { it.addCount == addCount }
            ?: TodayEmotionAddRequest(emotionId, addCount, UUID.randomUUID().toString()).also {
                pendingAddRequests[emotionId] = it
            }
        viewModelScope.launch {
            _favoriteServerState.value = _favoriteServerState.value.copy(
                addingEmotionIds = _favoriteServerState.value.addingEmotionIds + emotionId,
                error = null
            )
            when (val result = serverRepository.addToday(request)) {
                is EmotionServerResult.Success -> {
                    pendingAddRequests.remove(emotionId)
                    val body = result.value
                    val current = _favoriteServerState.value
                    val updated = current.favorites.map {
                        if (it.emotionId == emotionId) it.copy(
                            todayCount = body.newTotal,
                            totalCount = (it.totalCount + body.delta).coerceAtLeast(0)
                        ) else it
                    }
                    repository.syncFavoriteCache(updated, current.recordDate)
                    _favoriteServerState.value = current.copy(
                        favorites = updated,
                        editedTodayCounts = current.editedTodayCounts + (emotionId to body.newTotal),
                        addCounts = current.addCounts + (emotionId to 1),
                        addingEmotionIds = current.addingEmotionIds - emotionId,
                        error = null
                    )
                    _toastEvent.emit("${favorite.emotionName} ${body.addedCount}회를 추가 기록했습니다.")
                }
                EmotionServerResult.Unauthorized -> _favoriteServerState.value = _favoriteServerState.value.copy(
                    addingEmotionIds = _favoriteServerState.value.addingEmotionIds - emotionId,
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요.", requiresLogin = true
                ).also { _toastEvent.emit("로그인이 만료되었습니다. 다시 로그인해주세요.") }
                is EmotionServerResult.Error -> {
                    _favoriteServerState.value = _favoriteServerState.value.copy(
                        addingEmotionIds = _favoriteServerState.value.addingEmotionIds - emotionId,
                        error = result.message
                    )
                    _toastEvent.emit(result.message)
                }
            }
        }
    }

    fun correctServerFavoriteEmotion(emotionId: Long, newTotal: Int, onSuccess: () -> Unit = {}) {
        val state = _favoriteServerState.value
        if (newTotal !in 0..999 || emotionId in state.correctingEmotionIds) return
        val existing = pendingCorrectionRequests[emotionId]
        val request = existing?.takeIf { it.newTotal == newTotal }
            ?: TodayEmotionCorrectionRequest(emotionId, newTotal, UUID.randomUUID().toString()).also {
                pendingCorrectionRequests[emotionId] = it
            }
        viewModelScope.launch {
            _favoriteServerState.value = _favoriteServerState.value.copy(
                correctingEmotionIds = _favoriteServerState.value.correctingEmotionIds + emotionId,
                error = null
            )
            when (val result = serverRepository.correctToday(request)) {
                is EmotionServerResult.Success -> {
                    pendingCorrectionRequests.remove(emotionId)
                    val body = result.value
                    val current = _favoriteServerState.value
                    val updated = current.favorites.map {
                        if (it.emotionId == emotionId) it.copy(
                            todayCount = body.newTotal,
                            totalCount = (it.totalCount + body.delta).coerceAtLeast(0)
                        ) else it
                    }
                    repository.syncFavoriteCache(updated, current.recordDate)
                    _favoriteServerState.value = current.copy(
                        favorites = updated,
                        editedTodayCounts = current.editedTodayCounts + (emotionId to body.newTotal),
                        correctingEmotionIds = current.correctingEmotionIds - emotionId,
                        error = null
                    )
                    _toastEvent.emit(body.message ?: "오늘 누적 횟수를 수정했습니다.")
                    onSuccess()
                }
                EmotionServerResult.Unauthorized -> _favoriteServerState.value = _favoriteServerState.value.copy(
                    correctingEmotionIds = _favoriteServerState.value.correctingEmotionIds - emotionId,
                    error = "로그인이 만료되었습니다. 다시 로그인해주세요.", requiresLogin = true
                ).also { _toastEvent.emit("로그인이 만료되었습니다. 다시 로그인해주세요.") }
                is EmotionServerResult.Error -> {
                    _favoriteServerState.value = _favoriteServerState.value.copy(
                        correctingEmotionIds = _favoriteServerState.value.correctingEmotionIds - emotionId,
                        error = result.message
                    )
                    _toastEvent.emit(result.message)
                }
            }
        }
    }

    private fun saveServerTodayEmotions() {
        viewModelScope.launch {
            val state = _favoriteServerState.value
            if (state.isSaving || state.recordDate.isBlank()) return@launch
            val request = TodayEmotionSaveRequest(
                recordDate = state.recordDate,
                records = state.favorites.map { TodayEmotionRecordDto(it.emotionId, state.editedTodayCounts[it.emotionId] ?: it.todayCount) }
            )
            _favoriteServerState.value = state.copy(isSaving = true, error = null)
            when (val result = serverRepository.saveToday(request)) {
                is EmotionServerResult.Success -> {
                    val body = result.value
                    repository.syncFavoriteCache(body.favorites, body.recordDate)
                    repository.saveTodayEmotionsToDiary()
                    _favoriteServerState.value = FavoriteServerUiState(
                        favorites = body.favorites,
                        recordDate = body.recordDate,
                        editedTodayCounts = body.favorites.associate { it.emotionId to it.todayCount },
                        addCounts = body.favorites.associate { it.emotionId to 1 }
                    )
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "TODAY_SAVE_SUCCESS recordDate=${body.recordDate} count=${body.favorites.size}")
                    _toastEvent.emit(body.message ?: "오늘 감정이 저장되었습니다.")
                }
                EmotionServerResult.Unauthorized -> _favoriteServerState.value = state.copy(isSaving = false, requiresLogin = true, error = "로그인이 만료되었습니다. 다시 로그인해주세요.")
                is EmotionServerResult.Error -> _favoriteServerState.value = state.copy(isSaving = false, error = result.message)
            }
        }
    }

    fun loadEmotionStoryOptions(emotionId: Long) {
        val date = _favoriteServerState.value.recordDate
        if (date.isBlank()) return
        viewModelScope.launch {
            _emotionStoryState.value = EmotionStoryUiState(emotionId = emotionId, isLoading = true)
            if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "EMOTION_STORY_REQUEST emotionId=$emotionId recordDate=$date")
            when (val result = serverRepository.storyOptions(emotionId, date)) {
                is EmotionServerResult.Success -> {
                    _emotionStoryState.value = EmotionStoryUiState(emotionId = emotionId, options = result.value)
                    updateConnectionFromOptions(emotionId, result.value)
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "EMOTION_STORY_OPTIONS_SUCCESS emotionId=$emotionId count=${result.value.record?.count} storiesCount=${result.value.stories.size} selected=${result.value.selected != null}")
                }
                EmotionServerResult.Unauthorized -> _emotionStoryState.value = EmotionStoryUiState(emotionId = emotionId, error = "로그인이 만료되었습니다. 다시 로그인해주세요.")
                is EmotionServerResult.Error -> {
                    val message = if (result.status == 404 && result.message.contains("감정 기록")) "먼저 오늘 감정을 저장해주세요." else result.message
                    _emotionStoryState.value = EmotionStoryUiState(emotionId = emotionId, error = message)
                }
            }
        }
    }

    fun linkEmotionStory(emotionId: Long, storyId: Long, strength: Int, onSuccess: () -> Unit) {
        val date = _favoriteServerState.value.recordDate
        if (date.isBlank() || strength !in 1..5) return
        viewModelScope.launch {
            _emotionStoryState.value = _emotionStoryState.value.copy(isSaving = true, error = null)
            when (val result = serverRepository.linkStory(EmotionStoryLinkRequest(emotionId, storyId, date, strength))) {
                is EmotionServerResult.Success -> {
                    val confirmed = result.value.selected
                    val option = _emotionStoryState.value.options?.stories?.firstOrNull {
                        it.storyId == confirmed?.storyId
                    }
                    if (confirmed != null && option != null) {
                        _emotionStoryConnections.value = _emotionStoryConnections.value + (
                            emotionId to EmotionStoryConnection(
                                emotionId = emotionId,
                                selectedStoryId = confirmed.storyId,
                                selectedStoryTitle = option.title,
                                selectedStoryPeriod = option.period,
                                linkStrength = confirmed.linkStrength
                            )
                        )
                    }
                    _emotionStoryState.value = _emotionStoryState.value.copy(isSaving = false, options = _emotionStoryState.value.options?.copy(selected = result.value.selected))
                    if (BuildConfig.DEBUG) Log.d("WOOCHISO_EMOTION", "EMOTION_STORY_LINK_SUCCESS emotionId=$emotionId")
                    _toastEvent.emit(result.value.message ?: "사연 연결이 저장되었습니다.")
                    onSuccess()
                }
                EmotionServerResult.Unauthorized -> _emotionStoryState.value = _emotionStoryState.value.copy(isSaving = false, error = "로그인이 만료되었습니다. 다시 로그인해주세요.")
                is EmotionServerResult.Error -> _emotionStoryState.value = _emotionStoryState.value.copy(isSaving = false, error = if (result.status == 404 && result.message.contains("감정 기록")) "먼저 오늘 감정을 저장해주세요." else result.message)
            }
        }
    }

    fun clearEmotionStory() { _emotionStoryState.value = EmotionStoryUiState() }

    private suspend fun refreshEmotionStoryConnections(
        favorites: List<FavoriteEmotionServerDto>,
        recordDate: String
    ) = coroutineScope {
        val loaded = favorites.map { favorite ->
            async {
                when (val result = serverRepository.storyOptions(favorite.emotionId, recordDate)) {
                    is EmotionServerResult.Success -> connectionFromOptions(favorite.emotionId, result.value)
                    else -> null
                }
            }
        }.awaitAll().filterNotNull().associateBy { it.emotionId }
        _emotionStoryConnections.value = loaded
    }

    private fun updateConnectionFromOptions(emotionId: Long, options: EmotionStoryOptionsResponse) {
        val connection = connectionFromOptions(emotionId, options)
        _emotionStoryConnections.value = if (connection == null) {
            _emotionStoryConnections.value - emotionId
        } else {
            _emotionStoryConnections.value + (emotionId to connection)
        }
    }

    private fun connectionFromOptions(
        emotionId: Long,
        options: EmotionStoryOptionsResponse
    ): EmotionStoryConnection? {
        val selected = options.selected ?: return null
        val story = options.stories.firstOrNull { it.storyId == selected.storyId } ?: return null
        return EmotionStoryConnection(
            emotionId = emotionId,
            selectedStoryId = selected.storyId,
            selectedStoryTitle = story.title,
            selectedStoryPeriod = story.period,
            linkStrength = selected.linkStrength
        )
    }

    fun incrementFavoriteCount(favorite: FavoriteEmotionEntity) {
        viewModelScope.launch {
            repository.incrementFavoriteCount(favorite)
        }
    }

    fun decrementFavoriteCount(favorite: FavoriteEmotionEntity) {
        viewModelScope.launch {
            repository.decrementFavoriteCount(favorite)
        }
    }

    fun removeFavorite(word: String) {
        viewModelScope.launch {
            repository.removeFavorite(word)
            _toastEvent.emit("'$word' 감정을 삭제했습니다.")
        }
    }

    fun updateFavorite(favorite: FavoriteEmotionEntity) {
        viewModelScope.launch {
            repository.updateFavorite(favorite)
        }
    }

    fun addCustomFavorite(word: String, category: EmotionCategory) {
        viewModelScope.launch {
            repository.addFavorite(word, category.code)
            _toastEvent.emit("'$word' 감정을 즐겨찾기에 추가했습니다.")
        }
    }

    fun addDiaryEntry(
        dateString: String,
        primaryCategory: EmotionCategory,
        selectedEmotions: List<String>,
        memo: String,
        intensity: Int
    ) {
        viewModelScope.launch {
            repository.addDiaryEntry(
                dateString = dateString,
                primaryCategoryCode = primaryCategory.code,
                emotionsList = selectedEmotions,
                memo = memo,
                intensity = intensity
            )
            _toastEvent.emit("감정 다이어리가 저장되었습니다.")
        }
    }

    fun deleteDiaryEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteDiaryEntry(id)
            _toastEvent.emit("다이어리 기록을 삭제했습니다.")
        }
    }

    fun addInnerStory(
        title: String,
        content: String,
        reflection: String,
        primaryCategory: EmotionCategory,
        associatedEmotions: List<String>,
        dateString: String,
        eventPeriod: String = "",
        eventAge: Int? = null,
        eventYear: Int? = null,
        eventCategory: String = "",
        relatedPerson: String = "",
        importance: Int = 1,
        currentImpact: Int = 1,
        currentStatus: String = ""
    ) {
        viewModelScope.launch {
            repository.addInnerStory(
                title = title,
                content = content,
                reflection = reflection,
                primaryCategoryCode = primaryCategory.code,
                associatedEmotionsList = associatedEmotions,
                dateString = dateString,
                eventPeriod = eventPeriod,
                eventAge = eventAge,
                eventYear = eventYear,
                eventCategory = eventCategory,
                relatedPerson = relatedPerson,
                importance = importance,
                currentImpact = currentImpact,
                currentStatus = currentStatus
            )
            _toastEvent.emit("나의 사연이 저장되었습니다.")
        }
    }

    fun deleteInnerStory(id: Int) {
        viewModelScope.launch {
            repository.deleteInnerStory(id)
            _toastEvent.emit("사연을 삭제했습니다.")
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getDateBeforeDays(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}
