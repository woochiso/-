package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.EmotionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionItemDao {
    @Query("SELECT * FROM emotion_items ORDER BY displayOrder ASC")
    fun getAllItems(): Flow<List<EmotionItemEntity>>

    @Query("SELECT * FROM emotion_items ORDER BY displayOrder ASC")
    suspend fun getAllItemsDirect(): List<EmotionItemEntity>

    @Query("SELECT * FROM emotion_items WHERE categoryId = :categoryId ORDER BY displayOrder ASC")
    fun getItemsByCategoryId(categoryId: Int): Flow<List<EmotionItemEntity>>

    @Query("SELECT * FROM emotion_items WHERE emotionName = :name LIMIT 1")
    suspend fun getItemByName(name: String): EmotionItemEntity?

    @Query("SELECT * FROM emotion_items WHERE isFavorite = 1")
    fun getFavoriteItems(): Flow<List<EmotionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: EmotionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<EmotionItemEntity>)

    @Update
    suspend fun updateItem(item: EmotionItemEntity)

    @Query("UPDATE emotion_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM emotion_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}
