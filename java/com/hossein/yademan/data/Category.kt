// مسیر: app/src/main/java/com/hossein/yademan/data/Category.kt
package com.hossein.yademan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** جدول categories */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    val version: Int = 1,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncStatus.LOCAL_NEW,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
