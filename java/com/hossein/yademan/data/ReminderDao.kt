// مسیر: app/src/main/java/com/hossein/yademan/data/ReminderDao.kt
package com.hossein.yademan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    /** همه رکوردهای حذف‌نشده؛ UI با Flow خودکار به‌روز می‌شود */
    @Query("SELECT * FROM reminders WHERE is_deleted = 0 ORDER BY sort_order ASC, due_at ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE is_deleted = 0 ORDER BY sort_order ASC, due_at ASC")
    suspend fun getAll(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Reminder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<Reminder>)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE sync_status IN ('local_new', 'local_changed') AND is_deleted = 0")
    suspend fun getDirty(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE is_deleted = 1 AND sync_status = 'local_deleted'")
    suspend fun getPendingDeletes(): List<Reminder>

    /** فقط اگر در حین سینک تغییری نکرده باشد، synced می‌شود */
    @Query("UPDATE reminders SET sync_status = 'synced' WHERE id = :id AND updated_at = :updatedAt AND is_deleted = 0")
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM reminders WHERE id IN (:ids) AND is_deleted = 1")
    suspend fun purgeDeleted(ids: List<String>)

    @Query("SELECT COUNT(*) FROM reminders WHERE is_deleted = 0")
    suspend fun count(): Int
}
