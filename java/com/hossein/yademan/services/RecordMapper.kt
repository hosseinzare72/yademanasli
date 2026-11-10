// مسیر: app/src/main/java/com/hossein/yademan/services/RecordMapper.kt
package com.hossein.yademan.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hossein.yademan.data.Category
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.data.SyncStatus
import com.hossein.yademan.utils.DateUtils

object RecordMapper {

    val REMINDER_TYPES = setOf("reminder", "task", "note", "event")

    // ---------- دسترسی‌های JsonNull-ایمن (رفع کامل کرش سینک) ----------
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

    private fun JsonObject.sInt(key: String): Int? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return try {
            el.asInt
        } catch (e: Exception) {
            try { el.asString.trim().toIntOrNull() } catch (e2: Exception) { null }
        }
    }

    private fun JsonObject.sLong(key: String): Long? {
        val el = get(key) ?: return null
        if (el.isJsonNull || !el.isJsonPrimitive) return null
        return try {
            el.asLong
        } catch (e: Exception) {
            try { el.asString.trim().toLongOrNull() } catch (e2: Exception) { null }
        }
    }

    private fun JsonObject.sObj(key: String): JsonObject? {
        val el = get(key) ?: return null
        return if (el.isJsonObject) el.asJsonObject else null
    }

    fun recordTypeFor(r: Reminder): String = when (ReminderMeta.type(r)) {
        ReminderType.NOTE -> "note"
        ReminderType.MEETING -> "event"
        ReminderType.SHOPPING -> "task"
        else -> "reminder"
    }

    fun reminderData(r: Reminder): JsonObject = JsonObject().apply {
        addProperty("id", r.id)
        addProperty("title", r.title)
        addProperty("note", r.note)
        addProperty("due_at", r.dueAt)
        addProperty("all_day", r.allDay)
        addProperty("repeat_rule", r.repeatRule)
        addProperty("repeat_interval", r.repeatInterval)
        addProperty("weekdays_mask", r.weekdaysMask)
        addProperty("end_at", r.endAt)
        addProperty("priority", r.priority)
        addProperty("category_id", r.categoryId)
        addProperty("tags_json", r.tagsJson)
        addProperty("is_done", r.isDone)
        addProperty("done_at", r.doneAt)
        addProperty("alarm_enabled", r.alarmEnabled)
        addProperty("alarm_offset_minutes", r.alarmOffsetMinutes)
        addProperty("sms_reminder", r.smsReminder)
        addProperty("email_reminder", r.emailReminder)
        addProperty("color", r.color)
        addProperty("sort_order", r.sortOrder)
        addProperty("amount_toman", r.amountToman)
        addProperty("meta_json", r.metaJson)
        addProperty("created_at", r.createdAt)
        addProperty("updated_at", r.updatedAt)
        addProperty("device_id", r.deviceId)
        addProperty("version", r.version)
    }

    fun reminderRecord(r: Reminder): JsonObject = JsonObject().apply {
        addProperty("id", r.id)
        addProperty("record_type", recordTypeFor(r))
        add("record_data", reminderData(r))
        addProperty("version", r.version)
        addProperty("updated_at", r.updatedAt)
        addProperty("device_id", r.deviceId)
        addProperty("is_deleted", false)
    }

    fun categoryRecord(c: Category): JsonObject = JsonObject().apply {
        addProperty("id", c.id)
        addProperty("record_type", "category")
        add("record_data", JsonObject().apply {
            addProperty("id", c.id)
            addProperty("name", c.name)
            addProperty("color", c.color)
            addProperty("sort_order", c.sortOrder)
            addProperty("created_at", c.createdAt)
            addProperty("updated_at", c.updatedAt)
            addProperty("device_id", c.deviceId)
            addProperty("version", c.version)
        })
        addProperty("version", c.version)
        addProperty("updated_at", c.updatedAt)
        addProperty("device_id", c.deviceId)
        addProperty("is_deleted", false)
    }

    fun recordId(rec: JsonObject): String? =
        rec.sStr("record_id") ?: rec.sStr("id") ?: rec.sObj("record_data")?.sStr("id")

    fun recordVersion(rec: JsonObject): Int =
        rec.sInt("version") ?: rec.sObj("record_data")?.sInt("version") ?: 1

    fun isDeletedRecord(rec: JsonObject): Boolean =
        rec.sBool("is_deleted") ?: rec.sBool("deleted") ?: false

    /** رکورد سرور → Reminder (کاملاً JsonNull-ایمن) */
    fun reminderFrom(rec: JsonObject): Reminder? {
        val id = recordId(rec) ?: return null
        val d = rec.sObj("record_data") ?: JsonObject()
        val now = DateUtils.nowIso()
        val updated = rec.sStr("updated_at") ?: d.sStr("updated_at") ?: now

        val metaEl = d.get("meta")
        val metaFromObj = if (metaEl != null && metaEl.isJsonObject) metaEl.toString() else null
        var meta = d.sStr("meta_json") ?: metaFromObj ?: d.sStr("meta") ?: "{}"

        val metaObj = ReminderMeta.parse(meta)
        if (!metaObj.has("type")) {
            val guessed = when (rec.sStr("record_type")) {
                "note" -> ReminderType.NOTE
                "event" -> ReminderType.MEETING
                "task" -> ReminderType.SHOPPING
                else -> ReminderType.fromCategory(d.sStr("category_id"))
            }
            metaObj.addProperty("type", guessed)
            meta = metaObj.toString()
        }

        return Reminder(
            id = id,
            title = d.sStr("title") ?: "",
            note = d.sStr("note") ?: "",
            dueAt = d.sStr("due_at") ?: now,
            allDay = d.sBool("all_day") ?: false,
            repeatRule = d.sStr("repeat_rule") ?: "none",
            repeatInterval = d.sInt("repeat_interval") ?: 1,
            weekdaysMask = d.sInt("weekdays_mask") ?: 0,
            endAt = d.sStr("end_at"),
            priority = d.sInt("priority") ?: 1,
            categoryId = d.sStr("category_id"),
            tagsJson = d.sStr("tags_json"),
            isDone = d.sBool("is_done") ?: false,
            doneAt = d.sStr("done_at"),
            alarmEnabled = d.sBool("alarm_enabled") ?: true,
            alarmOffsetMinutes = d.sInt("alarm_offset_minutes") ?: 0,
            smsReminder = d.sBool("sms_reminder") ?: false,
            emailReminder = d.sBool("email_reminder") ?: false,
            color = d.sStr("color") ?: "#D9B45B",
            sortOrder = d.sInt("sort_order") ?: 0,
            amountToman = d.sLong("amount_toman") ?: 0L,
            metaJson = meta,
            createdAt = d.sStr("created_at") ?: updated,
            updatedAt = updated,
            deviceId = d.sStr("device_id") ?: rec.sStr("device_id") ?: "server",
            version = recordVersion(rec),
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    fun categoryFrom(rec: JsonObject): Category? {
        val id = recordId(rec) ?: return null
        val d = rec.sObj("record_data") ?: JsonObject()
        val now = DateUtils.nowIso()
        val updated = rec.sStr("updated_at") ?: d.sStr("updated_at") ?: now
        return Category(
            id = id,
            name = d.sStr("name") ?: "",
            color = d.sStr("color") ?: "#D9B45B",
            sortOrder = d.sInt("sort_order") ?: 0,
            createdAt = d.sStr("created_at") ?: updated,
            updatedAt = updated,
            deviceId = d.sStr("device_id") ?: "server",
            version = recordVersion(rec),
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }
}