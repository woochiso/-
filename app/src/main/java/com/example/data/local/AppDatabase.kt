package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DiaryEntryDao
import com.example.data.local.dao.FavoriteEmotionDao
import com.example.data.local.dao.InnerStoryDao
import com.example.data.local.entity.DiaryEntryEntity
import com.example.data.local.entity.FavoriteEmotionEntity
import com.example.data.local.entity.InnerStoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        FavoriteEmotionEntity::class,
        DiaryEntryEntity::class,
        InnerStoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

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

            private suspend fun populateInitialData(db: AppDatabase) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Initial Seed Favorites
                val defaultFavorites = listOf(
                    FavoriteEmotionEntity(word = "감사한", categoryCode = "JOY", countToday = 3, countTotal = 12, lastUpdatedDate = todayStr),
                    FavoriteEmotionEntity(word = "행복한", categoryCode = "JOY", countToday = 2, countTotal = 8, lastUpdatedDate = todayStr),
                    FavoriteEmotionEntity(word = "속상한", categoryCode = "ANGER", countToday = 1, countTotal = 4, lastUpdatedDate = todayStr),
                    FavoriteEmotionEntity(word = "우울한", categoryCode = "SORROW", countToday = 0, countTotal = 3, lastUpdatedDate = todayStr),
                    FavoriteEmotionEntity(word = "즐거운", categoryCode = "PLEASURE", countToday = 4, countTotal = 15, lastUpdatedDate = todayStr),
                    FavoriteEmotionEntity(word = "다정한", categoryCode = "LOVE", countToday = 2, countTotal = 9, lastUpdatedDate = todayStr),
                    FavoriteEmotionEntity(word = "바라는", categoryCode = "DESIRE", countToday = 1, countTotal = 5, lastUpdatedDate = todayStr)
                )

                defaultFavorites.forEach {
                    db.favoriteEmotionDao().insertFavorite(it)
                }

                // Initial Seed Diary Entry
                val sampleEntry = DiaryEntryEntity(
                    dateString = todayStr,
                    primaryCategoryCode = "JOY",
                    emotionsListCsv = "감사한, 행복한, 흐뭇한",
                    memo = "오늘 하루도 따뜻한 차 한 잔과 함께 편안하게 감정을 마주해보았습니다.",
                    intensity = 4
                )
                db.diaryEntryDao().insertEntry(sampleEntry)
            }
        }
    }
}
