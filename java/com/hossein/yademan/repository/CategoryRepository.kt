// مسیر: app/src/main/java/com/hossein/yademan/repository/CategoryRepository.kt
package com.hossein.yademan.repository

import com.hossein.yademan.data.Category
import com.hossein.yademan.data.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    fun observeAll(): Flow<List<Category>> = dao.observeAll()
    suspend fun getAll(): List<Category> = dao.getAll()
    suspend fun getById(id: String): Category? = dao.getById(id)

    // ---------- سینک و Seed ----------
    suspend fun applyRemote(category: Category) = dao.upsert(category)
    suspend fun insertSeed(list: List<Category>) = dao.upsertAll(list)
    suspend fun getDirty(): List<Category> = dao.getDirty()
    suspend fun getPendingDeletes(): List<Category> = dao.getPendingDeletes()
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: String) = dao.markSyncedIfUnchanged(id, updatedAt)
    suspend fun purgeDeleted(ids: List<String>) = dao.purgeDeleted(ids)
    suspend fun hardDelete(id: String) = dao.hardDelete(id)
}
