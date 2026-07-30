package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FavoriteEmotionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteEmotionDao {
    @Query("SELECT * FROM favorite_emotions ORDER BY id ASC")
    fun getAllFavorites(): Flow<List<FavoriteEmotionEntity>>

    @Query("SELECT * FROM favorite_emotions WHERE word = :word LIMIT 1")
    suspend fun getFavoriteByWord(word: String): FavoriteEmotionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEmotionEntity)

    @Update
    suspend fun updateFavorite(favorite: FavoriteEmotionEntity)

    @Query("DELETE FROM favorite_emotions WHERE word = :word")
    suspend fun deleteFavoriteByWord(word: String)

    @Query("DELETE FROM favorite_emotions WHERE id = :id")
    suspend fun deleteFavoriteById(id: Int)

    @Query("UPDATE favorite_emotions SET countToday = 0, lastUpdatedDate = :todayStr WHERE lastUpdatedDate != :todayStr")
    suspend fun resetDailyCountForNewDay(todayStr: String)
}
