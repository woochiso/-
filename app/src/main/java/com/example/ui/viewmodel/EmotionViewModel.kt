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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.ui.components.EmotionCategoryStat

data class PieChartSegment(
    val category: EmotionCategory,
    val count: Int,
    val percentage: Float
)

data class SubEmotionStat(
    val emotionName: String,
    val category: EmotionCategory,
    val count: Int,
    val percentage: Float
)

enum class StatDisplayType(val label: String) {
    CATEGORY("대분류별 통계 (희노애락애오욕)"),
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

    private val _selectedChartRange = MutableStateFlow(ChartTimeRange.TODAY)
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
        initialValue = "오늘"
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
    }

    fun setCustomDateRange(start: String, end: String) {
        _customStartDate.value = start
        _customEndDate.value = end
        _selectedChartRange.value = ChartTimeRange.CUSTOM
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
        viewModelScope.launch {
            repository.saveTodayEmotionsToDiary()
            _toastEvent.emit("오늘 감정이 저장되었습니다.")
        }
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
        dateString: String
    ) {
        viewModelScope.launch {
            repository.addInnerStory(
                title = title,
                content = content,
                reflection = reflection,
                primaryCategoryCode = primaryCategory.code,
                associatedEmotionsList = associatedEmotions,
                dateString = dateString
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
