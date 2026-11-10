// مسیر: app/src/main/java/com/hossein/yademan/data/AppDatabase.kt
package com.hossein.yademan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * دیتابیس اصلی — Offline-first. همه خواندن/نوشتن‌ها از اینجاست و سینک در پس‌زمینه انجام می‌شود.
 * نسخه ۲: ستون‌ها snake_case شدند (نسخه قبلی ناسازگار بود و پاک می‌شود).
 */
@Database(
    entities = [Reminder::class, Category::class, NotificationLog::class, SyncMeta::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun syncMetaDao(): SyncMetaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "yademan.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
