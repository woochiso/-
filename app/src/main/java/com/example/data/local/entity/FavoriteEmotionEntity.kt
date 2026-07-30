package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_emotions")
data class FavoriteEmotionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val categoryCode: String,
    val countToday: Int = 0,
    val countTotal: Int = 0,
    val lastUpdatedDate: String = "",
    val connectedStoryTitle: String? = null
)
