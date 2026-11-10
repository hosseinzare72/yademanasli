// مسیر: app/src/main/java/com/hossein/yademan/utils/DateUtils.kt
package com.hossein.yademan.utils

import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.repository.MetaKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

object DateUtils {
    const val MINUTE = 60_000L
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR

    enum class DateFormat(val title: String, val id: String) {
        FULL("کامل (۲۳ مهر ۱۴۰۶ - ۱۴۰۶/۰۷/۲۳)", "full"),
        TEXT("متنی (۲۳ مهر ۱۴۰۶)", "text"),
        NUMERIC("عددی (۱۴۰۶/۰۷/۲۳)", "numeric"),
        SHORT("کوتاه (۰۷/۲۳)", "short");

        companion object {
            fun fromId(id: String?): DateFormat = entries.find { it.id == id } ?: FULL
        }
    }

    private val MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val _preferredFormat = MutableStateFlow(DateFormat.FULL)
    val preferredFormatFlow: StateFlow<DateFormat> = _preferredFormat.asStateFlow()

    suspend fun updatePreferredFormat(format: DateFormat) {
        _preferredFormat.value = format
        ServiceLocator.meta.set(MetaKeys.DATE_FORMAT, format.id)
    }

    fun getPreferredDateFormat(): DateFormat = _preferredFormat.value

    fun epochDay(millis: Long): Long {
        val offset = TimeZone.getDefault().getOffset(millis)
        return Math.floorDiv(millis + offset, DAY)
    }

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun todayStart(): Long = startOfDay(System.currentTimeMillis())

    fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

    fun atTime(dayMillis: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun hourOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
    fun minuteOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MINUTE)

    fun dayKey(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return buildString(10) {
            append(y)
            append('-')
            if (m < 10) append('0')
            append(m)
            append('-')
            if (d < 10) append('0')
            append(d)
        }
    }

    fun todayKey(): String = dayKey(System.currentTimeMillis())

    private val isoCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val NO_VALUE = Long.MIN_VALUE

    fun isoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val cached = isoCache[iso]
        if (cached != null) return if (cached == NO_VALUE) null else cached
        val v = JalaliConverter.parseIso(iso)?.time
        if (isoCache.size > 4000) isoCache.clear()
        isoCache[iso] = v ?: NO_VALUE
        return v
    }

    fun millisToIso(millis: Long): String = JalaliConverter.millisToIso(millis)
    fun nowIso(): String = JalaliConverter.nowUtcIso()

    fun timeFa(millis: Long): String =
        "%02d:%02d".format(java.util.Locale.US, hourOf(millis), minuteOf(millis)).fa()

    /**
     * فرمت‌دهی هوشمند تاریخ بر اساس تنظیمات کاربر.
     * ✅ اصلاح شد: سال بدون جداکنندهٔ هزارگان (1405 نه 1,405)
     */
    fun formatDateFa(millis: Long, format: DateFormat = getPreferredDateFormat()): String {
        val j = JalaliConverter.fromMillis(millis)
        val y = j.year.toString().fa()           // ✅ تبدیل به رشته قبل از fa()
        val m = j.month.toString().padStart(2, '0').fa()
        val d = j.day.toString().padStart(2, '0').fa()
        val mName = MONTH_NAMES.getOrNull(j.month - 1) ?: "ماه"

        return when (format) {
            DateFormat.FULL -> "$d $mName $y ($y/$m/$d)"
            DateFormat.TEXT -> "$d $mName $y"
            DateFormat.NUMERIC -> "$y/$m/$d"
            DateFormat.SHORT -> "$m/$d"
        }
    }

    fun dateShortFa(millis: Long): String = formatDateFa(millis, getPreferredDateFormat())

    fun relativeFa(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - millis
        if (diff < 0) {
            val ahead = -diff
            return when {
                ahead < HOUR -> "${(ahead / MINUTE).coerceAtLeast(1).toInt().fa()} دقیقه دیگر"
                ahead < DAY -> "${(ahead / HOUR).toInt().fa()} ساعت دیگر"
                else -> "${(ahead / DAY).toInt().fa()} روز دیگر"
            }
        }
        val dayDiff = epochDay(now) - epochDay(millis)
        return when {
            diff < MINUTE -> "همین الان"
            diff < HOUR -> "${(diff / MINUTE).toInt().fa()} دقیقه پیش"
            dayDiff == 0L -> "${(diff / HOUR).toInt().fa()} ساعت پیش"
            dayDiff == 1L -> "دیروز"
            dayDiff < 7 -> "${dayDiff.toInt().fa()} روز پیش"
            dayDiff < 30 -> "${(dayDiff / 7).toInt().fa()} هفته پیش"
            dayDiff < 365 -> "${(dayDiff / 30).toInt().fa()} ماه پیش"
            else -> "${(dayDiff / 365).toInt().fa()} سال پیش"
        }
    }
}