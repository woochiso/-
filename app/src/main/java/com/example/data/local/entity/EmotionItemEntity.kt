package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emotion_items")
data class EmotionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoryId: Int,
    val emotionName: String,
    val displayOrder: Int,
    val isFavorite: Boolean = false
)
