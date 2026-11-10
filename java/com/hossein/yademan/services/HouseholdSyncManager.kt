// مسیر: app/src/main/java/com/hossein/yademan/services/HouseholdSyncManager.kt
package com.hossein.yademan.services

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.SyncStatus
import com.hossein.yademan.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object HouseholdSyncManager {

    private const val TAG = "YademanSync"
    private const val BASE = "https://kafezare.ir/wp-json/lifeplanner/v1/"
    private const val HH_LAST_SYNC = "hh_last_sync"
    private val mutex = Mutex()

    var myUserId: String? = null
        private set

    // ---------- JsonNull-ایمن ----------
    private fun JsonObject.sStr(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull) return null
        return if (el.isJsonPrimitive) el.asString else el.toString()
    }

    private fun JsonObject.sBool(key: String): Boolean? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return try { el.asBoolean } catch (e: Exception) { null }
    }

    private fun JsonObject.sArr(key: String): JsonArray? {
        val el = get(key) ?: return null
        return if (el.isJsonArray) el.asJsonArray else null
    }

    // ---------- HTTP خام ----------
    private suspend fun post(path: String, body: JsonObject): JsonObject? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(BASE + path).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.readText() ?: return@withContext null
            val el = JsonParser.parseString(text)
            if (el.isJsonObject) el.asJsonObject else null
        } catch (e: Exception) {
            Log.w(TAG, "household POST $path failed", e)
            null
        }
    }

    private fun authBody(): JsonObject = JsonObject().apply {
        ServiceLocator.auth.token?.takeIf { it.isNotBlank() }?.let { addProperty("token", it) }
    }

    // ---------- سینک کامل گروه ----------
    suspend fun sync(): Unit = mutex.withLock {
        try {
            val token = ServiceLocator.auth.token
            if (token.isNullOrBlank()) return@withLock

            val st = post("household/status", authBody()) ?: return@withLock
            if (st.sBool("active") != true) return@withLock

            val myRole = st.sStr("my_role")
            val members = st.sArr("members")
            myUserId = members?.firstOrNull { el ->
                el.isJsonObject && el.asJsonObject.sStr("role") == myRole
            }?.asJsonObject?.sStr("id")

            pushMyDirty()
            pullShared()
            AlarmScheduler.rescheduleAll(ServiceLocator.app)
            Log.d(TAG, "household sync ok")
        } catch (e: Exception) {
            Log.e(TAG, "household sync failed (نادیده گرفته شد)", e)
        }
    }

    private suspend fun pushMyDirty() {
        val repo = ServiceLocator.reminders
        val dirty = repo.getDirty().filter { r ->
            val owner = ReminderMeta.ownerUserId(r)
            owner == null || owner == myUserId
        }
        if (dirty.isEmpty()) return

        val records = JsonArray()
        dirty.forEach { records.add(RecordMapper.reminderRecord(it)) }

        val body = authBody()
        body.addProperty("device_id", ServiceLocator.auth.deviceId)
        body.add("records", records)
        post("household/push", body)
    }

    private suspend fun pullShared() {
        val meta = ServiceLocator.meta
        val last = meta.get(HH_LAST_SYNC) ?: "1970-01-01 00:00:00"
        val body = authBody()
        body.addProperty("last_sync", last)
        val resp = post("household/pull", body) ?: return
        val arr = resp.sArr("records") ?: return
        val repo = ServiceLocator.reminders

        for (el in arr) {
            if (!el.isJsonObject) continue
            val rec = el.asJsonObject
            val id = RecordMapper.recordId(rec) ?: continue
            val remoteVersion = RecordMapper.recordVersion(rec)
            val deleted = RecordMapper.isDeletedRecord(rec)
            val local = repo.getById(id)

            // ✅ اصلاح: رکوردی که محلی تغییر کرده (LOCAL_CHANGED/LOCAL_NEW) فقط وقتی
            // overwrite می‌شود که نسخه سرور واقعاً بزرگ‌تر باشد، نه مساوی.
            val remoteWins = when {
                local == null -> true
                local.syncStatus == SyncStatus.SYNCED -> remoteVersion >= local.version
                else -> remoteVersion > local.version
            }
            if (!remoteWins) continue

            if (deleted) {
                repo.hardDelete(id)
                AlarmScheduler.cancel(ServiceLocator.app, id)
            } else {
                RecordMapper.reminderFrom(rec)?.let { repo.applyRemote(it) }
            }
        }

        val newLast = resp.sStr("last_sync") ?: resp.sStr("server_time") ?: last
        meta.set(HH_LAST_SYNC, newLast)
    }

    // ---------- عملیات روی رکورد همسر ----------
    fun isForeign(r: Reminder): Boolean {
        val owner = ReminderMeta.ownerUserId(r) ?: return false
        val me = myUserId ?: return false
        return owner != me
    }

    /**
     * ✅ اصلاح نهایی: وضعیت جدیدِ پرداخت را با همان endpoint استاندارد household/push
     * به سرور می‌فرستیم (کل رکوردِ به‌روزشده)، تا سرور و گوشی همسر دقیقاً همان چیزی
     * را بگیرند که محلی ثبت شده. دیگر مشکل «برگشتن پرداخت» وجود ندارد.
     *
     * نکته: فراخوانی‌کننده (BillsViewModel/HubViewModel) قبل از این تابع،
     * تغییر را محلی اعمال کرده است؛ پس اینجا فقط کافی است وضعیت فعلی push شود.
     */
    suspend fun toggleDoneRemote(reminder: Reminder, done: Boolean, dayKey: String? = null): Boolean {
        return try {
            val repo = ServiceLocator.reminders
            val fresh = repo.getById(reminder.id) ?: reminder
            val body = authBody()
            body.addProperty("device_id", ServiceLocator.auth.deviceId)
            val records = JsonArray()
            records.add(RecordMapper.reminderRecord(fresh))
            body.add("records", records)
            val resp = post("household/push", body)
            val ok = resp != null
            Log.d(TAG, "toggleDoneRemote push: ok=$ok, id=${reminder.id}, done=$done")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "toggleDoneRemote failed (will sync later)", e)
            false
        }
    }
}