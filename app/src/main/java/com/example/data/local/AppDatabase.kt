package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DiaryEntryDao
import com.example.data.local.dao.EmotionCategoryDao
import com.example.data.local.dao.EmotionItemDao
import com.example.data.local.dao.EmotionRecordDao
import com.example.data.local.dao.FavoriteEmotionDao
import com.example.data.local.dao.InnerStoryDao
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.local.entity.EmotionCategoryEntity
import com.example.data.local.entity.EmotionItemEntity
import com.example.data.local.entity.EmotionRecordEntity
import com.example.data.local.entity.FavoriteEmotionEntity
import com.example.data.local.entity.InnerStoryEntity
import com.example.data.model.EmotionCategory
import com.example.data.model.PresetEmotions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        EmotionCategoryEntity::class,
        EmotionItemEntity::class,
        EmotionRecordEntity::class,
        FavoriteEmotionEntity::class,
        DiaryEntryEntity::class,
        InnerStoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emotionCategoryDao(): EmotionCategoryDao
    abstract fun emotionItemDao(): EmotionItemDao
    abstract fun emotionRecordDao(): EmotionRecordDao
    abstract fun favoriteEmotionDao(): FavoriteEmotionDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun innerStoryDao(): InnerStoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "emotion_diary_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        ensureCategoriesAndPresetItems(database)
                    }
                }
            }

            private suspend fun ensureCategoriesAndPresetItems(db: AppDatabase) {
                val categoryDao = db.emotionCategoryDao()
                val itemDao = db.emotionItemDao()

                val existingCategories = categoryDao.getAllCategoriesDirect()
                if (existingCategories.isEmpty()) {
                    val defaultCategories = listOf(
                        EmotionCategoryEntity(id = 1, code = "JOY", name = "희", displayOrder = 1),
                        EmotionCategoryEntity(id = 2, code = "ANGER", name = "노", displayOrder = 2),
                        EmotionCategoryEntity(id = 3, code = "SORROW", name = "애(哀)", displayOrder = 3),
                        EmotionCategoryEntity(id = 4, code = "PLEASURE", name = "락", displayOrder = 4),
                        EmotionCategoryEntity(id = 5, code = "LOVE", name = "애(愛)", displayOrder = 5),
                        EmotionCategoryEntity(id = 6, code = "HATRED", name = "오", displayOrder = 6),
                        EmotionCategoryEntity(id = 7, code = "DESIRE", name = "욕", displayOrder = 7)
                    )
                    categoryDao.insertCategories(defaultCategories)
                }

                val existingItems = itemDao.getAllItemsDirect()
                if (existingItems.isEmpty()) {
                    val categoryMap = mapOf(
                        EmotionCategory.JOY to 1,
                        EmotionCategory.ANGER to 2,
                        EmotionCategory.SORROW to 3,
                        EmotionCategory.PLEASURE to 4,
                        EmotionCategory.LOVE to 5,
                        EmotionCategory.HATRED to 6,
                        EmotionCategory.DESIRE to 7
                    )

                    val presetEntities = PresetEmotions.ALL_EMOTIONS.mapIndexed { index, item ->
                        EmotionItemEntity(
                            categoryId = categoryMap[item.category] ?: 1,
                            emotionName = item.word,
                            displayOrder = index + 1,
                            isFavorite = false
                        )
                    }
                    itemDao.insertItems(presetEntities)
                }
            }

            private suspend fun populateInitialData(db: AppDatabase) {
                ensureCategoriesAndPresetItems(db)
            }
        }
    }
}
