// مسیر: app/src/main/java/com/hossein/yademan/data/Reminder.kt
package com.hossein.yademan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** وضعیت‌های سینک هر رکورد */
object SyncStatus {
    const val LOCAL_NEW = "local_new"
    const val LOCAL_CHANGED = "local_changed"
    const val LOCAL_DELETED = "local_deleted"
    const val SYNCED = "synced"
}

/**
 * جدول reminders — همه انواع (قسط، چک، قرار، دارو، خرید، یادداشت، سایر) در این جدول‌اند
 * و نوع در meta_json.type ذخیره می‌شود. ساختار meta برای هر نوع در ReminderMeta.kt مستند شده.
 * نام ستون‌ها دقیقاً snake_case مطابق پرامپ است.
 */
@Entity(
    tableName = "reminders",
    indices = [Index("sync_status"), Index("category_id"), Index("is_deleted")]
)
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    @ColumnInfo(name = "due_at") val dueAt: String,                 // ISO UTC
    @ColumnInfo(name = "all_day") val allDay: Boolean = false,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String = "none", // none|daily|weekly|monthly|yearly
    @ColumnInfo(name = "repeat_interval") val repeatInterval: Int = 1,
    @ColumnInfo(name = "weekdays_mask") val weekdaysMask: Int = 0,     // بیت ۰=شنبه ... ۶=جمعه
    @ColumnInfo(name = "end_at") val endAt: String? = null,
    val priority: Int = 1,                                           // ۰=کم، ۱=متوسط، ۲=زیاد
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "tags_json") val tagsJson: String? = null,
    @ColumnInfo(name = "is_done") val isDone: Boolean = false,       // فقط برای یادآورهای یک‌بار
    @ColumnInfo(name = "done_at") val doneAt: String? = null,
    @ColumnInfo(name = "alarm_enabled") val alarmEnabled: Boolean = true,
    @ColumnInfo(name = "alarm_offset_minutes") val alarmOffsetMinutes: Int = 0,
    @ColumnInfo(name = "sms_reminder") val smsReminder: Boolean = false,
    @ColumnInfo(name = "email_reminder") val emailReminder: Boolean = false,
    val color: String = "#D9B45B",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "amount_toman") val amountToman: Long = 0L,
    @ColumnInfo(name = "meta_json") val metaJson: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    val version: Int = 1,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncStatus.LOCAL_NEW,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
