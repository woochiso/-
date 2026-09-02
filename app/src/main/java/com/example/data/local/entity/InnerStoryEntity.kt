package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inner_stories")
data class InnerStoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val reflection: String,
    val primaryCategoryCode: String,
    val associatedEmotionsCsv: String,
    val dateString: String,
    val eventPeriod: String = "",
    val eventAge: Int? = null,
    val eventYear: Int? = null,
    val eventCategory: String = "",
    val relatedPerson: String = "",
    val importance: Int = 1,
    val currentImpact: Int = 1,
    val currentStatus: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
