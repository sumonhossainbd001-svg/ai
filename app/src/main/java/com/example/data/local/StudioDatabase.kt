package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        GenerationJobEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun generationJobDao(): GenerationJobDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var instance: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "ai_video_studio.db"
                ).build().also { instance = it }
            }
        }
    }
}
