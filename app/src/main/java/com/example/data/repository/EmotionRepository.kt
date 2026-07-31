package com.example.data.repository

import com.example.data.local.dao.DiaryEntryDao
import com.example.data.local.dao.EmotionCategoryDao
import com.example.data.local.dao.EmotionItemDao
import com.example.data.local.dao.EmotionRecordDao
import com.example.data.local.dao.FavoriteEmotionDao
import com.example.data.local.dao.InnerStoryDao
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.local.entity.EmotionCategoryEntity
import com.example.data.local.entity.EmotionItemEntity
import com.example.data.local.entity.EmotionRecordEntity
import com.example.data.local.entity.FavoriteEmotionEntity
import com.example.data.local.entity.InnerStoryEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmotionRepository(
    private val favoriteEmotionDao: FavoriteEmotionDao,
    private val diaryEntryDao: DiaryEntryDao,
    private val innerStoryDao: InnerStoryDao,
    private val emotionCategoryDao: EmotionCategoryDao,
    private val emotionItemDao: EmotionItemDao,
    private val emotionRecordDao: EmotionRecordDao
) {
    val allFavorites: Flow<List<FavoriteEmotionEntity>> = favoriteEmotionDao.getAllFavorites()
    val allDiaryEntries: Flow<List<DiaryEntryEntity>> = diaryEntryDao.getAllEntries()
    val allInnerStories: Flow<List<InnerStoryEntity>> = innerStoryDao.getAllStories()

    val allCategories: Flow<List<EmotionCategoryEntity>> = emotionCategoryDao.getAllCategories()
    val allEmotionItems: Flow<List<EmotionItemEntity>> = emotionItemDao.getAllItems()
    val allEmotionRecords: Flow<List<EmotionRecordEntity>> = emotionRecordDao.getAllRecords()

    suspend fun checkAndResetDailyFavorites() {
        val todayStr = getTodayDateString()
        favoriteEmotionDao.resetDailyCountForNewDay(todayStr)
    }

    fun getDiaryEntriesForDate(dateString: String): Flow<List<DiaryEntryEntity>> {
        return diaryEntryDao.getEntriesByDate(dateString)
    }

    suspend fun toggleFavorite(word: String, categoryCode: String): Boolean {
        val cleanWord = word.trim()
        val existing = favoriteEmotionDao.getFavoriteByWord(cleanWord)
        val catId = getCategoryIdForCode(categoryCode)

        // Also update EmotionItemEntity.isFavorite
        val item = emotionItemDao.getItemByName(cleanWord)
        if (item != null) {
            emotionItemDao.updateFavoriteStatus(item.id, existing == null)
        } else if (cleanWord.isNotBlank()) {
            val newItem = EmotionItemEntity(
                categoryId = catId,
                emotionName = cleanWord,
                displayOrder = 999,
                isFavorite = existing == null
            )
            emotionItemDao.insertItem(newItem)
        }

        return if (existing != null) {
            favoriteEmotionDao.deleteFavoriteByWord(cleanWord)
            false // removed
        } else {
            val todayStr = getTodayDateString()
            val newFav = FavoriteEmotionEntity(
                word = cleanWord,
                categoryCode = categoryCode,
                countToday = 0,
                countTotal = 0,
                lastUpdatedDate = todayStr
            )
            favoriteEmotionDao.insertFavorite(newFav)
            true // added
        }
    }

    suspend fun addFavorite(word: String, categoryCode: String) {
        val cleanWord = word.trim()
        if (cleanWord.isBlank()) return
        val catId = getCategoryIdForCode(categoryCode)

        val item = emotionItemDao.getItemByName(cleanWord)
        if (item != null) {
            emotionItemDao.updateFavoriteStatus(item.id, true)
        } else {
            emotionItemDao.insertItem(
                EmotionItemEntity(
                    categoryId = catId,
                    emotionName = cleanWord,
                    displayOrder = 999,
                    isFavorite = true
                )
            )
        }

        val existing = favoriteEmotionDao.getFavoriteByWord(cleanWord)
        val todayStr = getTodayDateString()
        if (existing == null) {
            favoriteEmotionDao.insertFavorite(
                FavoriteEmotionEntity(
                    word = cleanWord,
                    categoryCode = categoryCode,
                    countToday = 0,
                    countTotal = 0,
                    lastUpdatedDate = todayStr
                )
            )
        } else {
            favoriteEmotionDao.updateFavorite(
                existing.copy(categoryCode = categoryCode, lastUpdatedDate = todayStr)
            )
        }
    }

    suspend fun removeFavorite(word: String) {
        val cleanWord = word.trim()
        favoriteEmotionDao.deleteFavoriteByWord(cleanWord)
        val item = emotionItemDao.getItemByName(cleanWord)
        if (item != null) {
            emotionItemDao.updateFavoriteStatus(item.id, false)
        }
    }

    suspend fun updateFavorite(favorite: FavoriteEmotionEntity) {
        favoriteEmotionDao.updateFavorite(favorite)
    }

    suspend fun incrementFavoriteCount(favorite: FavoriteEmotionEntity) {
        val todayStr = getTodayDateString()
        val isNewDay = favorite.lastUpdatedDate != todayStr

        val newToday = if (isNewDay) 1 else favorite.countToday + 1
        val newTotal = favorite.countTotal + 1

        val updated = favorite.copy(
            countToday = newToday,
            countTotal = newTotal,
            lastUpdatedDate = todayStr
        )
        favoriteEmotionDao.updateFavorite(updated)
    }

    suspend fun decrementFavoriteCount(favorite: FavoriteEmotionEntity) {
        val todayStr = getTodayDateString()
        val isNewDay = favorite.lastUpdatedDate != todayStr

        val currentToday = if (isNewDay) 0 else favorite.countToday
        val newToday = (currentToday - 1).coerceAtLeast(0)
        val newTotal = (favorite.countTotal - 1).coerceAtLeast(0)

        val updated = favorite.copy(
            countToday = newToday,
            countTotal = newTotal,
            lastUpdatedDate = todayStr
        )
        favoriteEmotionDao.updateFavorite(updated)
    }

    suspend fun saveTodayEmotionsToDiary() {
        val todayStr = getTodayDateString()
        val allFavs = favoriteEmotionDao.getAllFavoritesList()
        val activeFavs = allFavs.filter { it.countToday > 0 }

        if (activeFavs.isEmpty()) {
            val todayEntries = diaryEntryDao.getEntriesByDateDirect(todayStr)
            for (entry in todayEntries) {
                diaryEntryDao.deleteEntryById(entry.id)
            }
            val todayRecords = emotionRecordDao.getRecordsByDateDirect(todayStr)
            for (rec in todayRecords) {
                emotionRecordDao.deleteRecordById(rec.id)
            }
            return
        }

        val emotionsListCsv = activeFavs.joinToString(", ") { "${it.word} ${it.countToday}회" }
        val primaryCategory = activeFavs.maxByOrNull { it.countToday }?.categoryCode ?: activeFavs.first().categoryCode
        val totalCount = activeFavs.sumOf { it.countToday }
        val memoText = "오늘의 즐찾 감정 저장: " + activeFavs.joinToString(", ") { fav ->
            val storyText = if (!fav.connectedStoryTitle.isNullOrBlank()) "(사연: ${fav.connectedStoryTitle})" else ""
            "${fav.word} ${fav.countToday}회$storyText"
        }

        val todayEntries = diaryEntryDao.getEntriesByDateDirect(todayStr)
        val existingEntry = todayEntries.firstOrNull()

        if (existingEntry != null) {
            val updatedEntry = existingEntry.copy(
                primaryCategoryCode = primaryCategory,
                emotionsListCsv = emotionsListCsv,
                memo = memoText,
                intensity = totalCount.coerceIn(1, 5),
                timestamp = System.currentTimeMillis()
            )
            diaryEntryDao.updateEntry(updatedEntry)
            if (todayEntries.size > 1) {
                for (duplicate in todayEntries.drop(1)) {
                    diaryEntryDao.deleteEntryById(duplicate.id)
                }
            }
        } else {
            val newEntry = DiaryEntryEntity(
                dateString = todayStr,
                primaryCategoryCode = primaryCategory,
                emotionsListCsv = emotionsListCsv,
                memo = memoText,
                intensity = totalCount.coerceIn(1, 5),
                timestamp = System.currentTimeMillis()
            )
            diaryEntryDao.insertEntry(newEntry)
        }

        val existingRecords = emotionRecordDao.getRecordsByDateDirect(todayStr)
        val activeItemIds = mutableSetOf<Int>()

        for (fav in activeFavs) {
            val catId = getCategoryIdForCode(fav.categoryCode)
            var item = emotionItemDao.getItemByName(fav.word)
            if (item == null) {
                val newItemId = emotionItemDao.insertItem(
                    EmotionItemEntity(
                        categoryId = catId,
                        emotionName = fav.word,
                        displayOrder = 999,
                        isFavorite = true
                    )
                ).toInt()
                item = EmotionItemEntity(
                    id = newItemId,
                    categoryId = catId,
                    emotionName = fav.word,
                    displayOrder = 999,
                    isFavorite = true
                )
            }
            activeItemIds.add(item.id)

            val existingRec = existingRecords.find { it.emotionItemId == item.id }
            val storyMemo = if (!fav.connectedStoryTitle.isNullOrBlank()) "사연: ${fav.connectedStoryTitle}" else "즐찾 감정 저장"
            if (existingRec != null) {
                val updatedRecord = existingRec.copy(
                    count = fav.countToday,
                    memo = storyMemo,
                    updatedAt = System.currentTimeMillis()
                )
                emotionRecordDao.updateRecord(updatedRecord)
            } else {
                val record = EmotionRecordEntity(
                    recordDate = todayStr,
                    categoryId = item.categoryId,
                    emotionItemId = item.id,
                    count = fav.countToday,
                    memo = storyMemo,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                emotionRecordDao.insertRecord(record)
            }
        }

        for (staleRec in existingRecords.filter { it.emotionItemId !in activeItemIds }) {
            emotionRecordDao.deleteRecordById(staleRec.id)
        }
    }

    suspend fun cleanupDuplicateRecords() {
        val allEntries = diaryEntryDao.getAllEntriesDirect()
        val entriesByDate = allEntries.groupBy { it.dateString }
        for ((_, entries) in entriesByDate) {
            if (entries.size > 1) {
                val primaryEntry = entries.find { it.memo.startsWith("오늘의 즐찾 감정 저장") }
                    ?: entries.maxByOrNull { it.timestamp }
                if (primaryEntry != null) {
                    for (duplicate in entries.filter { it.id != primaryEntry.id }) {
                        diaryEntryDao.deleteEntryById(duplicate.id)
                    }
                }
            }
        }

        val allRecords = emotionRecordDao.getAllRecordsDirect()
        val recordsGrouped = allRecords.groupBy { "${it.recordDate}_${it.emotionItemId}" }
        for ((_, records) in recordsGrouped) {
            if (records.size > 1) {
                val primaryRecord = records.maxByOrNull { it.updatedAt }
                if (primaryRecord != null) {
                    for (duplicate in records.filter { it.id != primaryRecord.id }) {
                        emotionRecordDao.deleteRecordById(duplicate.id)
                    }
                }
            }
        }
    }

    suspend fun recordEmotionOccurrence(
        emotionWord: String,
        categoryCode: String,
        dateString: String = getTodayDateString(),
        memo: String = ""
    ) {
        val cleanWord = emotionWord.trim()
        if (cleanWord.isBlank()) return

        val catId = getCategoryIdForCode(categoryCode)
        var item = emotionItemDao.getItemByName(cleanWord)
        if (item == null) {
            val newItemId = emotionItemDao.insertItem(
                EmotionItemEntity(
                    categoryId = catId,
                    emotionName = cleanWord,
                    displayOrder = 999,
                    isFavorite = false
                )
            ).toInt()
            item = EmotionItemEntity(
                id = newItemId,
                categoryId = catId,
                emotionName = cleanWord,
                displayOrder = 999,
                isFavorite = false
            )
        }

        val record = EmotionRecordEntity(
            recordDate = dateString,
            categoryId = item.categoryId,
            emotionItemId = item.id,
            count = 1,
            memo = memo,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        emotionRecordDao.insertRecord(record)
    }

    suspend fun addDiaryEntry(
        dateString: String,
        primaryCategoryCode: String,
        emotionsList: List<String>,
        memo: String,
        intensity: Int
    ) {
        val entry = DiaryEntryEntity(
            dateString = dateString,
            timestamp = System.currentTimeMillis(),
            primaryCategoryCode = primaryCategoryCode,
            emotionsListCsv = emotionsList.joinToString(", "),
            memo = memo,
            intensity = intensity
        )
        diaryEntryDao.insertEntry(entry)

        // Also record each emotion in 3-table structure (emotion_records)
        emotionsList.forEach { word ->
            recordEmotionOccurrence(
                emotionWord = word,
                categoryCode = primaryCategoryCode,
                dateString = dateString,
                memo = memo
            )
        }
    }

    suspend fun deleteDiaryEntry(id: Int) {
        diaryEntryDao.deleteEntryById(id)
    }

    suspend fun addInnerStory(
        title: String,
        content: String,
        reflection: String,
        primaryCategoryCode: String,
        associatedEmotionsList: List<String>,
        dateString: String
    ) {
        val story = InnerStoryEntity(
            title = title,
            content = content,
            reflection = reflection,
            primaryCategoryCode = primaryCategoryCode,
            associatedEmotionsCsv = associatedEmotionsList.joinToString(", "),
            dateString = dateString,
            createdAt = System.currentTimeMillis()
        )
        innerStoryDao.insertStory(story)

        associatedEmotionsList.forEach { word ->
            recordEmotionOccurrence(
                emotionWord = word,
                categoryCode = primaryCategoryCode,
                dateString = dateString,
                memo = "나의 사연: $title"
            )
        }
    }

    suspend fun deleteInnerStory(id: Int) {
        innerStoryDao.deleteStoryById(id)
    }

    suspend fun purgeQuietTranquilityStories() {
        innerStoryDao.deleteQuietTranquilityStories()
    }

    fun getCategoryIdForCode(code: String): Int {
        return when (code.uppercase()) {
            "JOY" -> 1
            "ANGER" -> 2
            "SORROW" -> 3
            "PLEASURE" -> 4
            "LOVE" -> 5
            "HATRED", "OH" -> 6
            "DESIRE", "YOK" -> 7
            else -> 1
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
