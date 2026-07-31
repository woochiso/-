package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.EmotionCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionCategoryDao {
    @Query("SELECT * FROM emotion_categories ORDER BY displayOrder ASC")
    fun getAllCategories(): Flow<List<EmotionCategoryEntity>>

    @Query("SELECT * FROM emotion_categories ORDER BY displayOrder ASC")
    suspend fun getAllCategoriesDirect(): List<EmotionCategoryEntity>

    @Query("SELECT * FROM emotion_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): EmotionCategoryEntity?

    @Query("SELECT * FROM emotion_categories WHERE code = :code LIMIT 1")
    suspend fun getCategoryByCode(code: String): EmotionCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: EmotionCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<EmotionCategoryEntity>)

    @Update
    suspend fun updateCategory(category: EmotionCategoryEntity)
}
