// مسیر: app/src/main/java/com/hossein/yademan/services/ReminderReceiver.kt
package com.hossein.yademan.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        /** اکشن اختصاصی برای دکمه «چرت زدن» */
        const val ACTION_SNOOZE = "com.hossein.yademan.ACTION_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "notif_id"
        /** مدت چرت پیش‌فرض: ۴ ساعت */
        const val SNOOZE_HOURS = 4
        private const val TAG = "YademanReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        ServiceLocator.appScope.launch {
            try {
                when (intent.action) {
                    AlarmScheduler.ACTION_FIRE -> onFire(appContext, intent)
                    AlarmScheduler.ACTION_DONE -> onDone(appContext, intent)
                    ACTION_SNOOZE -> onSnooze(appContext, intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "error", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onFire(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val followUp = intent.getBooleanExtra(AlarmScheduler.EXTRA_FOLLOW_UP, false)
        val stage = intent.getStringExtra(AlarmScheduler.EXTRA_STAGE) ?: AlarmScheduler.STAGE_MAIN
        val occurrence = intent.getLongExtra(AlarmScheduler.EXTRA_OCCURRENCE, System.currentTimeMillis())
        val meta = ServiceLocator.meta

        val silent = !meta.getBool(MetaKeys.NOTIF_SOUND, true) ||
                (meta.getBool(MetaKeys.NOTIF_DND, false) && isQuietHours())

        if (id == AlarmScheduler.TEST_ID) {
            NotificationHelper.show(
                context, id.hashCode(),
                "اعلان آزمایشی یادمان",
                "اگر این را می‌بینی، اعلان‌ها با اپ بسته هم کار می‌کنند ✓",
                silent, null, occurrence, null
            )
            return
        }

        val r = ServiceLocator.reminders.getById(id)
        if (r == null || r.isDeleted || !r.alarmEnabled) {
            AlarmScheduler.cancelAll(context, id)
            return
        }

        val dayKey = DateUtils.dayKey(occurrence)
        val done = ReminderMeta.isDoneOn(r, dayKey)

        if (followUp) {
            if (!done) {
                NotificationHelper.show(
                    context, id.hashCode() + 1000,
                    "هنوز انجام نشده: ${r.title}",
                    bodyFor(r, occurrence),
                    silent, id, occurrence, doneLabel(r)
                )
                if (meta.getBool(MetaKeys.NOTIF_SMART, true)) {
                    AlarmScheduler.scheduleFollowUp(context, id, occurrence)
                }
            }
            return
        }

        if (!done) {
            val title = titleForStage(r, stage)
            val body = bodyFor(r, occurrence)
            NotificationHelper.show(context, id.hashCode(), title, body, silent, id, occurrence, doneLabel(r))
            try {
                ServiceLocator.notifications.log(title, body, id)
            } catch (_: Exception) {
            }
            if (stage == AlarmScheduler.STAGE_MAIN && meta.getBool(MetaKeys.NOTIF_SMART, true)) {
                AlarmScheduler.scheduleFollowUp(context, id, occurrence)
            }
        }

        if (stage == AlarmScheduler.STAGE_MAIN) {
            meta.set(MetaKeys.FIRED_PREFIX + id, occurrence.toString())
            AlarmScheduler.schedule(context, r, keepFollowUp = true)
        }
    }

    private suspend fun onDone(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val occurrence = intent.getLongExtra(AlarmScheduler.EXTRA_OCCURRENCE, System.currentTimeMillis())
        val r = ServiceLocator.reminders.getById(id) ?: return
        ServiceLocator.reminders.setDoneOn(r, DateUtils.dayKey(occurrence), true)
        AlarmScheduler.cancelFollowUp(context, id)
        NotificationHelper.cancel(context, id.hashCode())
        NotificationHelper.cancel(context, id.hashCode() + 1000)
    }

    /**
     * ✅ مدیریت دکمه «چرت زدن»:
     * ۱) نوتیفیکیشن فعلی و follow-up بسته می‌شوند
     * ۲) یک آلارم دقیق برای ۴ ساعت بعد تنظیم می‌شود که همان اعلان را دوباره نمایش می‌دهد
     * ۳) با تمام نسخه‌های اندروید (API 23 تا 35) سازگار است
     */
    private fun onSnooze(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val occurrence = intent.getLongExtra(AlarmScheduler.EXTRA_OCCURRENCE, System.currentTimeMillis())
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, id.hashCode())

        // ۱) بستن نوتیفیکیشن‌های فعلی
        NotificationHelper.cancel(context, notifId)
        NotificationHelper.cancel(context, id.hashCode())
        NotificationHelper.cancel(context, id.hashCode() + 1000)

        // ۲) لغو follow-up فعلی (تا ۴ ساعت دیگر دوباره سر و کله‌اش پیدا نشود)
        AlarmScheduler.cancelFollowUp(context, id)

        // ۳) تنظیم آلارم جدید برای ۴ ساعت بعد
        val snoozeAt = System.currentTimeMillis() + (SNOOZE_HOURS * 60L * 60L * 1000L)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_FIRE
            data = Uri.parse("yademan://fire/${Uri.encode(id)}")
            putExtra(AlarmScheduler.EXTRA_ID, id)
            putExtra(AlarmScheduler.EXTRA_OCCURRENCE, occurrence)
            putExtra(AlarmScheduler.EXTRA_STAGE, AlarmScheduler.STAGE_MAIN)
            putExtra(AlarmScheduler.EXTRA_FOLLOW_UP, false)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pi = PendingIntent.getBroadcast(context, id.hashCode() + 7777, snoozeIntent, flags)

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
                else ->
                    am.set(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
            }
            Log.d(TAG, "Snoozed $id for ${SNOOZE_HOURS}h → fires at $snoozeAt")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not allowed, falling back", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pi)
        }
    }

    private fun titleForStage(r: Reminder, stage: String): String {
        val base = r.title
        return when (stage) {
            AlarmScheduler.STAGE_TWO_DAYS -> "۲ روز مانده تا: $base"
            AlarmScheduler.STAGE_ONE_DAY -> "۱ روز مانده تا: $base"
            AlarmScheduler.STAGE_MORNING -> "صبح روز سررسید: $base"
            else -> base
        }
    }

    private fun doneLabel(r: Reminder): String = when (ReminderMeta.type(r)) {
        ReminderType.MEDICINE -> "مصرف شد"
        ReminderType.INSTALLMENT, ReminderType.CHECK -> "پرداخت شد"
        ReminderType.SUBSCRIPTION -> "تمدید شد"
        else -> "انجام شد"
    }

    private fun bodyFor(r: Reminder, occurrence: Long): String {
        val time = DateUtils.timeFa(occurrence)
        val date = DateUtils.dateShortFa(occurrence)
        val whenText = if (DateUtils.epochDay(occurrence) == DateUtils.epochDay(System.currentTimeMillis())) {
            "امروز ساعت $time"
        } else {
            "$date ساعت $time"
        }
        val amount = if (r.amountToman > 0) "${PersianDigits.toman(r.amountToman)} • " else ""

        val hub = ReminderMeta.str(r, "hub")
        if (hub != null) {
            val hubLabel = when (hub) {
                "bills" -> "قبض"
                "building" -> "شارژ ساختمان"
                "travel" -> "مسافرت"
                "insurance" -> "بیمه"
                "car" -> "سرویس خودرو"
                "salary" -> "حقوق"
                "pocket" -> "پول توجیبی"
                "birthday" -> "تولد"
                "anniversary" -> "سالگرد"
                "rent" -> "اجاره"
                else -> null
            }
            return if (hubLabel != null) "$amount$hubLabel: ${r.title} • $whenText"
            else "$amount${r.title} • $whenText"
        }

        return when (ReminderMeta.type(r)) {
            ReminderType.MEDICINE -> {
                val dose = ReminderMeta.str(r, "dose")
                if (dose.isNullOrBlank()) "وقت مصرف دارو • $time" else "وقت مصرف: $dose • $time"
            }
            ReminderType.INSTALLMENT, ReminderType.CHECK -> {
                val count = ReminderMeta.totalCount(r)
                val idx = if (count != null) Recurrence.occurrenceIndex(r, occurrence) else null
                val progress = if (count != null && idx != null) {
                    if (idx >= count) " • آخرین قسط!" else " • قسط ${idx.fa()} از ${count.fa()}"
                } else ""
                "${amount}سررسید $whenText$progress"
            }
            ReminderType.SUBSCRIPTION -> {
                val plan = ReminderMeta.str(r, "plan")?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                "${amount}تمدید اشتراک $whenText$plan"
            }
            ReminderType.MEETING -> {
                val loc = ReminderMeta.str(r, "location")
                if (loc.isNullOrBlank()) "قرار $whenText" else "قرار $whenText • $loc"
            }
            ReminderType.TRUST -> {
                val holder = ReminderMeta.str(r, "holder")
                if (holder.isNullOrBlank()) "امانت: ${r.title} • $whenText"
                else "امانت: ${r.title} (نزد $holder) • $whenText"
            }
            else -> {
                val note = r.note.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                "${r.title}$note • $whenText"
            }
        }
    }

    private fun isQuietHours(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h >= 23 || h < 7
    }
}