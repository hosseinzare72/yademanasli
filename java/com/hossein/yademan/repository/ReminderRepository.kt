// مسیر: app/src/main/java/com/hossein/yademan/repository/ReminderRepository.kt
package com.hossein.yademan.repository

import android.content.Context
import android.util.Log
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderDao
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.data.SyncStatus
import com.hossein.yademan.services.AlarmScheduler
import com.hossein.yademan.services.SyncWorker
import com.hossein.yademan.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReminderRepository(private val dao: ReminderDao, private val context: Context) {
    private val tag = "YademanRepo"

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()
    fun observeById(id: String): Flow<Reminder?> = dao.observeById(id)
    suspend fun getAll(): List<Reminder> = dao.getAll()
    suspend fun getById(id: String): Reminder? = dao.getById(id)

    fun newReminder(type: String, title: String, dueAtMillis: Long): Reminder {
        val now = DateUtils.nowIso()
        return Reminder(
            id = UUID.randomUUID().toString(),
            title = title,
            dueAt = DateUtils.millisToIso(dueAtMillis),
            categoryId = ReminderType.categoryId(type),
            color = ReminderType.colorHex(type),
            metaJson = ReminderMeta.build(type),
            createdAt = now,
            updatedAt = now,
            deviceId = ServiceLocator.auth.deviceId,
            version = 1,
            syncStatus = SyncStatus.LOCAL_NEW
        )
    }

    private suspend fun resetFiredMarker(id: String) {
        try {
            ServiceLocator.meta.set(MetaKeys.FIRED_PREFIX + id, "")
        } catch (e: Exception) {
            Log.w(tag, "resetFiredMarker failed", e)
        }
    }

    suspend fun create(reminder: Reminder) {
        val rec = reminder.copy(
            updatedAt = DateUtils.nowIso(),
            deviceId = ServiceLocator.auth.deviceId,
            syncStatus = SyncStatus.LOCAL_NEW,
            isDeleted = false
        )
        dao.upsert(rec)
        resetFiredMarker(rec.id)
        try {
            AlarmScheduler.schedule(context, rec)
        } catch (e: Exception) {
            Log.e(tag, "schedule failed (create)", e)
        }
        try {
            SyncWorker.requestSoon(context)
        } catch (e: Exception) {
            Log.e(tag, "sync request failed (create)", e)
        }
    }

    suspend fun update(reminder: Reminder) {
        val current = dao.getById(reminder.id)
        val status = if (current == null || current.syncStatus == SyncStatus.LOCAL_NEW) {
            SyncStatus.LOCAL_NEW
        } else {
            SyncStatus.LOCAL_CHANGED
        }
        val rec = reminder.copy(
            updatedAt = DateUtils.nowIso(),
            version = (current?.version ?: reminder.version) + 1,
            deviceId = ServiceLocator.auth.deviceId,
            syncStatus = status,
            isDeleted = false
        )
        dao.upsert(rec)
        resetFiredMarker(rec.id)
        try {
            AlarmScheduler.schedule(context, rec)
        } catch (e: Exception) {
            Log.e(tag, "schedule failed (update)", e)
        }
        try {
            SyncWorker.requestSoon(context)
        } catch (e: Exception) {
            Log.e(tag, "sync request failed (update)", e)
        }
    }

    suspend fun setDoneOn(reminder: Reminder, dayKey: String, done: Boolean) {
        val fresh = dao.getById(reminder.id) ?: return
        update(ReminderMeta.withDoneOn(fresh, dayKey, done))
    }

    suspend fun delete(id: String) {
        val current = dao.getById(id) ?: return
        if (current.syncStatus == SyncStatus.LOCAL_NEW) {
            dao.hardDelete(id)
        } else {
            dao.upsert(
                current.copy(
                    isDeleted = true,
                    syncStatus = SyncStatus.LOCAL_DELETED,
                    version = current.version + 1,
                    updatedAt = DateUtils.nowIso()
                )
            )
        }
        try {
            AlarmScheduler.cancelAll(context, id)
        } catch (e: Exception) {
            Log.e(tag, "cancel failed", e)
        }
        try {
            SyncWorker.requestSoon(context)
        } catch (e: Exception) {
            Log.e(tag, "sync request failed (delete)", e)
        }
    }

    suspend fun deleteMany(ids: List<String>) {
        ids.forEach { delete(it) }
    }

    suspend fun applyRemote(reminder: Reminder) = dao.upsert(reminder)
    suspend fun insertSeed(list: List<Reminder>) = dao.upsertAll(list)
    suspend fun getDirty(): List<Reminder> = dao.getDirty()
    suspend fun getPendingDeletes(): List<Reminder> = dao.getPendingDeletes()
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: String) = dao.markSyncedIfUnchanged(id, updatedAt)
    suspend fun purgeDeleted(ids: List<String>) = dao.purgeDeleted(ids)
    suspend fun hardDelete(id: String) = dao.hardDelete(id)
}