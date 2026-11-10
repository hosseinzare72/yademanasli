// مسیر: app/src/main/java/com/hossein/yademan/data/CategoryDao.kt
package com.hossein.yademan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_deleted = 0 ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE is_deleted = 0 ORDER BY sort_order ASC")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<Category>)

    @Query("SELECT * FROM categories WHERE sync_status IN ('local_new', 'local_changed') AND is_deleted = 0")
    suspend fun getDirty(): List<Category>

    @Query("SELECT * FROM categories WHERE is_deleted = 1 AND sync_status = 'local_deleted'")
    suspend fun getPendingDeletes(): List<Category>

    @Query("UPDATE categories SET sync_status = 'synced' WHERE id = :id AND updated_at = :updatedAt AND is_deleted = 0")
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM categories WHERE id IN (:ids) AND is_deleted = 1")
    suspend fun purgeDeleted(ids: List<String>)
}
