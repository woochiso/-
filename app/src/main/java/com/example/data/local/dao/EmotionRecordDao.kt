package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.EmotionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionRecordDao {
    @Query("SELECT * FROM emotion_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<EmotionRecordEntity>>

    @Query("SELECT * FROM emotion_records ORDER BY createdAt DESC")
    suspend fun getAllRecordsDirect(): List<EmotionRecordEntity>

    @Query("SELECT * FROM emotion_records WHERE recordDate = :date ORDER BY createdAt DESC")
    fun getRecordsByDate(date: String): Flow<List<EmotionRecordEntity>>

    @Query("SELECT * FROM emotion_records WHERE recordDate = :date ORDER BY createdAt DESC")
    suspend fun getRecordsByDateDirect(date: String): List<EmotionRecordEntity>

    @Query("SELECT * FROM emotion_records WHERE recordDate BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<EmotionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EmotionRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<EmotionRecordEntity>)

    @Update
    suspend fun updateRecord(record: EmotionRecordEntity)

    @Query("DELETE FROM emotion_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)
}
