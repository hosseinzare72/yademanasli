// مسیر: app/src/main/java/com/hossein/yademan/repository/SyncRepository.kt
package com.hossein.yademan.repository

import com.hossein.yademan.data.SyncMeta
import com.hossein.yademan.data.SyncMetaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** دسترسی key-value به sync_meta (متادیتای سینک + تنظیمات) */
class SyncRepository(private val dao: SyncMetaDao) {
    suspend fun get(key: String): String? = dao.get(key)
    suspend fun set(key: String, value: String) = dao.set(SyncMeta(key, value))
    suspend fun delete(key: String) = dao.delete(key)
    fun observe(key: String): Flow<String?> = dao.observe(key).distinctUntilChanged()

    suspend fun getBool(key: String, default: Boolean): Boolean =
        dao.get(key)?.toBooleanStrictOrNull() ?: default

    suspend fun setBool(key: String, value: Boolean) = set(key, value.toString())

    fun observeBool(key: String, default: Boolean): Flow<Boolean> =
        dao.observe(key).map { it?.toBooleanStrictOrNull() ?: default }.distinctUntilChanged()
}