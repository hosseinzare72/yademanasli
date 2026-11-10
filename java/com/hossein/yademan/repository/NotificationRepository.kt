// مسیر: app/src/main/java/com/hossein/yademan/repository/NotificationRepository.kt
package com.hossein.yademan.repository

import com.hossein.yademan.data.NotificationLog
import com.hossein.yademan.data.NotificationLogDao
import com.hossein.yademan.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationLogDao) {
    fun observeRecent(): Flow<List<NotificationLog>> = dao.observeRecent()
    fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    suspend fun log(title: String, body: String, sourceId: String?) {
        dao.insert(NotificationLog(title = title, body = body, createdAt = DateUtils.nowIso(), sourceId = sourceId))
        // اعلان‌های قدیمی‌تر از ۶۰ روز پاک می‌شوند
        dao.deleteOlderThan(DateUtils.millisToIso(System.currentTimeMillis() - 60 * DateUtils.DAY))
    }

    suspend fun markRead(id: Int) = dao.markRead(id)
    suspend fun markAllRead() = dao.markAllRead()
}
