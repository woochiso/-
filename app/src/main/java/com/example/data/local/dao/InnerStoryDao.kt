package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InnerStoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InnerStoryDao {
    @Query("SELECT * FROM inner_stories ORDER BY id ASC")
    fun getAllStories(): Flow<List<InnerStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: InnerStoryEntity)

    @Update
    suspend fun updateStory(story: InnerStoryEntity)

    @Query("DELETE FROM inner_stories WHERE id = :id")
    suspend fun deleteStoryById(id: Int)

    @Query("DELETE FROM inner_stories WHERE title LIKE '%조용한%' OR title LIKE '%평온함%'")
    suspend fun deleteQuietTranquilityStories()
}
