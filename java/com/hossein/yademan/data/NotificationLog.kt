// مسیر: app/src/main/java/com/hossein/yademan/data/NotificationLog.kt
package com.hossein.yademan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** جدول محلی اعلان‌ها (سینک نمی‌شود) */
@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    @ColumnInfo(name = "created_at") val createdAt: String,   // ISO UTC
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @ColumnInfo(name = "source_id") val sourceId: String? = null // شناسه یادآور منبع
)
