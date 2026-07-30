package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dateString: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val primaryCategoryCode: String,
    val emotionsListCsv: String, // Comma separated emotion words
    val memo: String,
    val intensity: Int = 3 // 1 to 5 scale
)
