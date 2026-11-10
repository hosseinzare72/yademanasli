// مسیر: app/src/main/java/com/hossein/yademan/data/SyncMeta.kt
package com.hossein.yademan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** key-value برای متادیتای سینک و تنظیمات محلی */
@Entity(tableName = "sync_meta")
data class SyncMeta(
    @PrimaryKey val key: String,
    val value: String
)
