// مسیر: app/src/main/java/com/hossein/yademan/services/AlarmScheduler.kt
package com.hossein.yademan.services

import android.app.AlarmManager
import android.app.PendingIntent
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
import com.hossein.yademan.utils.Recurrence
import java.util.Calendar
import kotlin.math.max

object AlarmScheduler {
    private const val TAG = "YademanAlarm"

    const val ACTION_FIRE = "com.hossein.yademan.action.REMINDER_FIRE"
    const val ACTION_DONE = "com.hossein.yademan.action.REMINDER_DONE"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_OCCURRENCE = "occurrence_at"
    const val EXTRA_FOLLOW_UP = "follow_up"
    const val EXTRA_STAGE = "stage"
    const val TEST_ID = "yademan_test"

    const val STAGE_MAIN = "main"
    const val STAGE_OFFSET = "offset"
    const val STAGE_TWO_DAYS = "two_days"
    const val STAGE_ONE_DAY = "one_day"
    const val STAGE_MORNING = "morning"

    private const val FOLLOW_UP_DELAY = 10 * DateUtils.MINUTE
    const val REQ_MAIN = 0
    const val REQ_TWO_DAYS = 1
    const val REQ_ONE_DAY = 2
    const val REQ_MORNING = 3
    const val REQ_FOLLOW_UP = 4

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    private fun fireIntent(context: Context, id: String, followUp: Boolean, stage: String, reqCode: Int): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("yademan://${if (followUp) "followup" else "reminder"}/${Uri.encode(id)}/$reqCode")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_FOLLOW_UP, followUp)
            putExtra(EXTRA_STAGE, stage)
        }

    private fun setAlarm(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = alarmManager(context)
        try {
            if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun shiftIfQuiet(triggerAt: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = triggerAt
        val h = cal.get(Calendar.HOUR_OF_DAY)
        if (h >= 23 || h < 7) {
            cal.set(Calendar.HOUR_OF_DAY, 7)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (h >= 23) cal.add(Calendar.DAY_OF_YEAR, 1)
            return max(cal.timeInMillis, System.currentTimeMillis() + 1_500)
        }
        return triggerAt
    }

    /** ✅ ویجت را با هر تغییر تازه می‌کند */
    private fun refreshWidget(context: Context) {
        try { RemindersWidgetProvider.refresh(context) } catch (_: Exception) { }
    }

    suspend fun schedule(context: Context, r: Reminder, keepFollowUp: Boolean = false) {
        cancelOccurrenceAlarms(context, r.id)
        if (!keepFollowUp) cancelFollowUp(context, r.id)
        if (r.isDeleted || !r.alarmEnabled) { refreshWidget(context); return }

        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val todayKey = DateUtils.dayKey(now)
        val fired = ServiceLocator.meta.get(MetaKeys.FIRED_PREFIX + r.id)?.toLongOrNull()

        // ✅ اعلان جبرانی: مورد امروزِ انجام‌نشده که ساعتش گذشته و اعلام نشده → فوراً اعلام کن
        val todayOcc = Recurrence.occurrenceOnDay(r, todayStart)
        if (todayOcc != null && todayOcc <= now && fired != todayOcc && !ReminderMeta.isDoneOn(r, todayKey)) {
            val intent = fireIntent(context, r.id, false, STAGE_MAIN, REQ_MAIN).putExtra(EXTRA_OCCURRENCE, todayOcc)
            val pi = PendingIntent.getBroadcast(context, REQ_MAIN, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            setAlarm(context, now + 1_500, pi)
            refreshWidget(context)
            return
        }

        var occ = Recurrence.nextOccurrence(r, now) ?: run { refreshWidget(context); return }
        if (fired == occ) {
            occ = Recurrence.nextOccurrence(r, occ + DateUtils.MINUTE) ?: run { refreshWidget(context); return }
        }

        val type = ReminderMeta.type(r)
        if (type == ReminderType.INSTALLMENT || type == ReminderType.CHECK) {
            scheduleInstallmentCheckAlarms(context, r, occ, now)
        } else {
            val offset = r.alarmOffsetMinutes.coerceAtLeast(0) * DateUtils.MINUTE
            val trigger = shiftIfQuiet(max(occ - offset, now + 1_500))
            val intent = fireIntent(context, r.id, false, STAGE_MAIN, REQ_MAIN).putExtra(EXTRA_OCCURRENCE, occ)
            val pi = PendingIntent.getBroadcast(context, REQ_MAIN, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            setAlarm(context, trigger, pi)
            Log.d(TAG, "scheduled ${r.id} ($type) trigger=$trigger")
        }
        refreshWidget(context)
    }

    private fun scheduleInstallmentCheckAlarms(context: Context, r: Reminder, occ: Long, now: Long) {
        val twoDaysBefore = occ - 2 * DateUtils.DAY
        if (twoDaysBefore > now + 1_500) {
            val intent = fireIntent(context, r.id, false, STAGE_TWO_DAYS, REQ_TWO_DAYS).putExtra(EXTRA_OCCURRENCE, occ)
            val pi = PendingIntent.getBroadcast(context, REQ_TWO_DAYS, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            setAlarm(context, shiftIfQuiet(twoDaysBefore), pi)
        }
        val oneDayBefore = occ - DateUtils.DAY
        if (oneDayBefore > now + 1_500) {
            val intent = fireIntent(context, r.id, false, STAGE_ONE_DAY, REQ_ONE_DAY).putExtra(EXTRA_OCCURRENCE, occ)
            val pi = PendingIntent.getBroadcast(context, REQ_ONE_DAY, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            setAlarm(context, shiftIfQuiet(oneDayBefore), pi)
        }
        val cal = Calendar.getInstance()
        cal.timeInMillis = occ
        cal.set(Calendar.HOUR_OF_DAY, 7); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val morning = cal.timeInMillis
        if (morning > now + 1_500) {
            val intent = fireIntent(context, r.id, false, STAGE_MORNING, REQ_MORNING).putExtra(EXTRA_OCCURRENCE, occ)
            val pi = PendingIntent.getBroadcast(context, REQ_MORNING, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            setAlarm(context, morning, pi)
        }
        val offset = r.alarmOffsetMinutes.coerceAtLeast(0) * DateUtils.MINUTE
        val intent = fireIntent(context, r.id, false, STAGE_MAIN, REQ_MAIN).putExtra(EXTRA_OCCURRENCE, occ)
        val pi = PendingIntent.getBroadcast(context, REQ_MAIN, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        setAlarm(context, max(occ - offset, now + 1_500), pi)
    }

    fun scheduleFollowUp(context: Context, id: String, occurrence: Long) {
        val now = System.currentTimeMillis()
        if (now - occurrence > DateUtils.DAY) return
        var at = max(occurrence, now) + FOLLOW_UP_DELAY
        at = shiftIfQuiet(at)
        val intent = fireIntent(context, id, true, "followup", REQ_FOLLOW_UP).putExtra(EXTRA_OCCURRENCE, occurrence)
        val pi = PendingIntent.getBroadcast(context, REQ_FOLLOW_UP, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        setAlarm(context, at, pi)
    }

    fun scheduleTest(context: Context, delaySeconds: Int = 10) {
        val intent = fireIntent(context, TEST_ID, false, "test", REQ_MAIN)
            .putExtra(EXTRA_OCCURRENCE, System.currentTimeMillis() + delaySeconds * 1000L)
        val pi = PendingIntent.getBroadcast(context, REQ_MAIN, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        setAlarm(context, System.currentTimeMillis() + delaySeconds * 1000L, pi)
    }

    private fun cancelByReqCode(context: Context, id: String, followUp: Boolean, reqCode: Int) {
        val pi = PendingIntent.getBroadcast(context, reqCode, fireIntent(context, id, followUp, "", reqCode), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) { alarmManager(context).cancel(pi); pi.cancel() }
    }

    fun cancelOccurrenceAlarms(context: Context, id: String) {
        cancelByReqCode(context, id, false, REQ_MAIN)
        cancelByReqCode(context, id, false, REQ_TWO_DAYS)
        cancelByReqCode(context, id, false, REQ_ONE_DAY)
        cancelByReqCode(context, id, false, REQ_MORNING)
    }

    fun cancelFollowUp(context: Context, id: String) = cancelByReqCode(context, id, true, REQ_FOLLOW_UP)

    fun cancelAll(context: Context, id: String) {
        cancelOccurrenceAlarms(context, id)
        cancelFollowUp(context, id)
        refreshWidget(context)
    }

    fun cancel(context: Context, id: String) = cancelAll(context, id)

    suspend fun rescheduleAll(context: Context) {
        val all = ServiceLocator.reminders.getAll()
        for (r in all) {
            try { schedule(context, r, keepFollowUp = true) } catch (_: Exception) { }
        }
        refreshWidget(context)
    }
}