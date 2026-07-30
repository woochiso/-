package com.example.data.repository

import com.example.data.local.dao.DiaryEntryDao
import com.example.data.local.dao.FavoriteEmotionDao
import com.example.data.local.dao.InnerStoryDao
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.local.entity.FavoriteEmotionEntity
import com.example.data.local.entity.InnerStoryEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmotionRepository(
    private val favoriteEmotionDao: FavoriteEmotionDao,
    private val diaryEntryDao: DiaryEntryDao,
    private val innerStoryDao: InnerStoryDao
) {
    val allFavorites: Flow<List<FavoriteEmotionEntity>> = favoriteEmotionDao.getAllFavorites()
    val allDiaryEntries: Flow<List<DiaryEntryEntity>> = diaryEntryDao.getAllEntries()
    val allInnerStories: Flow<List<InnerStoryEntity>> = innerStoryDao.getAllStories()

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
        favoriteEmotionDao.deleteFavoriteByWord(word)
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
        syncFavoriteToDiary(updated, newToday, todayStr)
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
        syncFavoriteToDiary(updated, newToday, todayStr)
    }

    private suspend fun syncFavoriteToDiary(favorite: FavoriteEmotionEntity, newTodayCount: Int, todayStr: String) {
        val todayEntries = diaryEntryDao.getEntriesByDateDirect(todayStr)
        val existingEntry = todayEntries.find {
            it.emotionsListCsv.contains(favorite.word) || it.memo.contains(favorite.word)
        }

        if (existingEntry != null) {
            val updatedEntry = existingEntry.copy(
                memo = "즐찾 감정 '${favorite.word}' 발생 ${newTodayCount}회",
                intensity = newTodayCount.coerceIn(1, 5),
                timestamp = System.currentTimeMillis()
            )
            diaryEntryDao.updateEntry(updatedEntry)
        } else if (newTodayCount > 0) {
            val newEntry = DiaryEntryEntity(
                dateString = todayStr,
                primaryCategoryCode = favorite.categoryCode,
                emotionsListCsv = favorite.word,
                memo = "즐찾 감정 '${favorite.word}' 발생 ${newTodayCount}회",
                intensity = newTodayCount.coerceIn(1, 5),
                timestamp = System.currentTimeMillis()
            )
            diaryEntryDao.insertEntry(newEntry)
        }
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
    }

    suspend fun deleteInnerStory(id: Int) {
        innerStoryDao.deleteStoryById(id)
    }

    suspend fun purgeQuietTranquilityStories() {
        innerStoryDao.deleteQuietTranquilityStories()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
