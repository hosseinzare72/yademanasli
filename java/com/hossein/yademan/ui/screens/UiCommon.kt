// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/UiCommon.kt
package com.hossein.yademan.ui.screens

import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.JalaliConverter
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** تیک زمان: هر ۳۰ ثانیه «الان» را منتشر می‌کند تا وضعیت‌های وابسته به ساعت به‌روز شوند */
object TimeTicker {
    val now: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }
}

/** وضعیت یک قسط/چک/اشتراک در ماه جاری */
data class BillInfo(
    val reminder: Reminder,
    val type: String,
    val occurrence: Long,
    val paid: Boolean,
    val daysLeft: Int,
    val inThisMonth: Boolean,
    /** شماره این قسط (۱ = اولین)؛ null اگر نامعلوم */
    val index: Int? = null,
    /** تعداد کل اقساط؛ null = نامحدود */
    val total: Int? = null,
    /** آخرین قسط هم پرداخت شده → تسویه کامل، دیگر هشداری نمی‌آید */
    val finished: Boolean = false
) {
    /** «قسط ۳ از ۱۰» */
    val progressText: String?
        get() = if (total != null && index != null) "${index.coerceAtMost(total).fa()} از ${total.fa()}" else null
}

/** منطق مشترک اقساط، چک‌ها و اشتراک‌ها (صفحه‌ها + بج‌های خانه) */
object BillsLogic {

    fun isBill(r: Reminder): Boolean {
        val t = ReminderMeta.type(r)
        return t == ReminderType.INSTALLMENT || t == ReminderType.CHECK
    }

    fun isSubscription(r: Reminder): Boolean = ReminderMeta.type(r) == ReminderType.SUBSCRIPTION

    /**
     * وقوع مربوط به ماه جاری: اولین وقوع از ابتدای ماه شمسی جاری به بعد
     * (برای یک‌بارهای گذشته، خود due_at؛ برای اقساطِ تعداددار که تمام شده‌اند، آخرین قسط).
     */
    fun info(r: Reminder, now: Long): BillInfo {
        val today = JalaliConverter.fromMillis(now)
        val monthStart = JalaliConverter.monthStartMillis(today.year, today.month)
        val nextMonthStart = JalaliConverter.monthStartMillis(today.year, today.month + 1)
        val base = Recurrence.baseMillis(r) ?: now
        val total = if (Recurrence.isRepeating(r)) ReminderMeta.totalCount(r) else null
        val last = if (total != null) Recurrence.lastOccurrence(r) else null
        val occ = Recurrence.nextOccurrence(r, monthStart, skipDone = false) ?: last ?: base
        val paid = ReminderMeta.isDoneOn(r, DateUtils.dayKey(occ))
        val daysLeft = (DateUtils.epochDay(occ) - DateUtils.epochDay(now)).toInt()
        val index = if (total != null) Recurrence.occurrenceIndex(r, occ) else null
        val finished = last != null && paid && DateUtils.epochDay(occ) >= DateUtils.epochDay(last)
        return BillInfo(
            reminder = r,
            type = ReminderMeta.type(r),
            occurrence = occ,
            paid = paid,
            daysLeft = daysLeft,
            inThisMonth = occ in monthStart until nextMonthStart,
            index = index,
            total = total,
            finished = finished
        )
    }

    fun statusText(b: BillInfo): String = when {
        b.finished -> "تسویه شد 🎉"
        b.paid && b.type == ReminderType.SUBSCRIPTION -> "تمدید شد"
        b.paid -> "پرداخت شد"
        b.daysLeft < 0 -> "گذشته"
        b.daysLeft == 0 -> if (b.type == ReminderType.SUBSCRIPTION) "تمدید امروز" else "سررسید امروز"
        b.daysLeft == 1 -> if (b.type == ReminderType.SUBSCRIPTION) "تمدید فردا" else "سررسید فردا"
        else -> "${b.daysLeft.fa()} روز مانده"
    }
}

/** زیرعنوان عمومی یک یادآور بر اساس نوع */
fun reminderSubtitle(r: Reminder): String {
    val type = ReminderMeta.type(r)
    return when (type) {
        ReminderType.MEDICINE -> ReminderMeta.str(r, "dose")?.takeIf { it.isNotBlank() } ?: "دارو"
        ReminderType.MEETING -> ReminderMeta.str(r, "location")?.takeIf { it.isNotBlank() } ?: "قرار"
        ReminderType.INSTALLMENT, ReminderType.CHECK, ReminderType.SUBSCRIPTION ->
            if (r.amountToman > 0) PersianDigits.toman(r.amountToman) else ReminderType.labelFa(type)
        else -> r.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
            ?: ReminderMeta.str(r, "location")?.takeIf { it.isNotBlank() }
            ?: ReminderType.labelFa(type)
    }
}
