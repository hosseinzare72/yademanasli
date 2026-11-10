// مسیر: app/src/main/java/com/hossein/yademan/utils/JalaliUtils.kt
package com.hossein.yademan.utils

import java.util.GregorianCalendar

/**
 * توابع کمکی تقویم شمسی برای انتخابگر تاریخ.
 * به‌جای الگوریتم تبدیل مستقل، از جست‌وجوی دودویی روی JalaliConverterِ خودِ پروژه
 * استفاده می‌کنیم تا «نمایش» و «انتخاب» همیشه با هم دقیق باشند.
 */
object JalaliUtils {

    private const val DAY = 86_400_000L
    private val LO = GregorianCalendar(1990, 0, 1, 12, 0, 0).timeInMillis
    private val HI = GregorianCalendar(2090, 0, 1, 12, 0, 0).timeInMillis

    /** تعداد روزهای یک ماه شمسی */
    fun monthLength(jy: Int, jm: Int): Int {
        val start = toMillis(jy, jm, 1)
        val next = if (jm == 12) toMillis(jy + 1, 1, 1) else toMillis(jy, jm + 1, 1)
        return ((next - start) / DAY).toInt()
    }

    /** آیا سال شمسی کبیسه است */
    fun isLeap(jy: Int): Boolean = monthLength(jy, 12) == 30

    /** تبدیل تاریخ شمسی به millis (نیمروزِ همان روز به وقت محلی) */
    fun toMillis(jy: Int, jm: Int, jd: Int): Long {
        val target = jy * 10000 + jm * 100 + jd
        var lo = LO
        var hi = HI
        while (lo < hi) {
            val mid = lo + (hi - lo) / 2
            val j = JalaliConverter.fromMillis(mid)
            val v = j.year * 10000 + j.month * 100 + j.day
            if (v < target) lo = mid + 1 else hi = mid
        }
        return DateUtils.startOfDay(lo) + 12 * 3600_000L
    }
}