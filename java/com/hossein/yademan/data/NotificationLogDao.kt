// مسیر: app/src/main/java/com/hossein/yademan/data/NotificationLogDao.kt
package com.hossein.yademan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(log: NotificationLog): Long

    @Query("SELECT * FROM notification_log ORDER BY created_at DESC, id DESC LIMIT 200")
    fun observeRecent(): Flow<List<NotificationLog>>

    @Query("SELECT COUNT(*) FROM notification_log WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE notification_log SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Int)

    @Query("UPDATE notification_log SET is_read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notification_log WHERE created_at < :beforeIso")
    suspend fun deleteOlderThan(beforeIso: String)
}
