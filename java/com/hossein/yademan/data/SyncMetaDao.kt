// مسیر: app/src/main/java/com/hossein/yademan/data/SyncMetaDao.kt
package com.hossein.yademan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: SyncMeta)

    @Query("SELECT value FROM sync_meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM sync_meta WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<String?>

    @Query("DELETE FROM sync_meta WHERE `key` = :key")
    suspend fun delete(key: String)
}
