package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emotion_records")
data class EmotionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val recordDate: String,
    val categoryId: Int,
    val emotionItemId: Int,
    val count: Int = 1,
    val memo: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
