// مسیر: app/src/main/java/com/hossein/yademan/utils/JalaliConverter.kt
package com.hossein.yademan.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** تاریخ/زمان شمسی به وقت محلی دستگاه. weekday: ۰=شنبه ... ۶=جمعه */
data class JalaliDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val weekday: Int,
    val hour: Int = 0,
    val minute: Int = 0
) {
    /** «شنبه ۴ مهر ۱۴۰۵» */
    fun formatLong(): String =
        "${JalaliConverter.weekdayNameFa(weekday)} ${day.toString().fa()} ${JalaliConverter.monthName(month)} ${year.toString().fa()}"

    /** «۷ مهر» */
    fun formatShort(): String = "${day.toString().fa()} ${JalaliConverter.monthName(month)}"

    /** «مهر ۱۴۰۵» */
    fun formatMonthYear(): String = "${JalaliConverter.monthName(month)} ${year.toString().fa()}"

    /** «۰۹:۳۰» */
    fun formatTime(): String =
        "%02d:%02d".format(Locale.US, hour, minute).fa()
}

/**
 * مبدل دوطرفه میلادی↔شمسی بر پایه الگوریتم jalaali (jalaali-js).
 * بدون java.time تا روی minSdk 24 بدون desugaring کار کند.
 */
object JalaliConverter {

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val weekdayNames = arrayOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    fun monthName(month: Int): String = monthNames[(month - 1).coerceIn(0, 11)]

    fun weekdayNameFa(weekday: Int): String = weekdayNames[weekday.coerceIn(0, 6)]

    /** حرف اول روز هفته برای نوار هفته (ش ی د س چ پ ج) */
    fun weekdayShortFa(weekday: Int): String = weekdayNameFa(weekday).substring(0, 1)

    // ---------------- هسته الگوریتم ----------------

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += jump / 33 * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += n / 33 * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - (gy / 100 + 1) * 3 / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    /** میلادی → شماره روز ژولینی */
    fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = (gy + (gm - 8) / 6 + 100100) * 1461 / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (gy + 100100 + (gm - 8) / 6) / 100 * 3 / 4 + 752
        return d
    }

    private fun d2g(jdn: Int): IntArray {
        var j = 4 * jdn + 139361631
        j = j + (4 * jdn + 183187720) / 146097 * 3 / 4 * 4 - 3908
        val i = (j % 1461) / 4 * 5 + 308
        val gd = (i % 153) / 5 + 1
        val gm = (i / 153) % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return intArrayOf(gy, gm, gd)
    }

    /** شمسی → شماره روز ژولینی (JDN) */
    fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - jm / 7 * (jm - 7) + jd - 1
    }

    /** شماره روز ژولینی → [سال، ماه، روز] شمسی */
    fun d2j(jdn: Int): IntArray {
        val gy = d2g(jdn)[0]
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) return intArrayOf(jy, 1 + k / 31, k % 31 + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return intArrayOf(jy, 7 + k / 30, k % 30 + 1)
    }

    /** ۰=شنبه ... ۶=جمعه */
    private fun weekdayOf(jdn: Int): Int = ((jdn % 7) + 2) % 7

    // ---------------- API عمومی ----------------

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray = d2j(g2d(gy, gm, gd))

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray = d2g(j2d(jy, jm, jd))

    fun isLeapJalaliYear(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeapJalaliYear(jy) -> 30
        else -> 29
    }

    /** تبدیل Calendar محلی به شمسی */
    fun fromCalendar(cal: Calendar): JalaliDateTime {
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val jdn = g2d(gy, gm, gd)
        val j = d2j(jdn)
        return JalaliDateTime(
            year = j[0], month = j[1], day = j[2],
            weekday = weekdayOf(jdn),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE)
        )
    }

    /** امروز به وقت محلی */
    fun today(): JalaliDateTime = fromCalendar(Calendar.getInstance())

    /** تاریخ شمسی + ساعت محلی → میلی‌ثانیه (روزهای خارج از بازه ماه خودکار جابه‌جا می‌شوند) */
    fun toMillis(jy: Int, jm: Int, jd: Int, hour: Int = 0, minute: Int = 0): Long {
        // اول روز ۱ همان ماه را می‌سازیم و بعد اختلاف روز را اضافه می‌کنیم تا jd>طول ماه هم درست شود
        val normalized = normalizeMonth(jy, jm)
        val g = jalaliToGregorian(normalized[0], normalized[1], 1)
        val cal = Calendar.getInstance().apply {
            clear()
            set(g[0], g[1] - 1, g[2], hour, minute, 0)
            add(Calendar.DAY_OF_MONTH, jd - 1)
        }
        return cal.timeInMillis
    }

    /** میلی‌ثانیه → شمسی به وقت محلی */
    fun fromMillis(millis: Long): JalaliDateTime =
        fromCalendar(Calendar.getInstance().apply { timeInMillis = millis })

    /** ماه خارج از ۱..۱۲ را به سال/ماه معتبر تبدیل می‌کند */
    fun normalizeMonth(jy: Int, jm: Int): IntArray {
        val total = jy * 12 + (jm - 1)
        return intArrayOf(Math.floorDiv(total, 12), Math.floorMod(total, 12) + 1)
    }

    /** اولین لحظه ماه شمسی جاری (یا با جابه‌جایی) به میلی‌ثانیه */
    fun monthStartMillis(jy: Int, jm: Int): Long {
        val n = normalizeMonth(jy, jm)
        return toMillis(n[0], n[1], 1, 0, 0)
    }

    /** تاریخ شمسی + ساعت محلی → رشته ISO UTC مثل 2026-09-26T05:30:00Z */
    fun toUtcIso(jy: Int, jm: Int, jd: Int, hour: Int, minute: Int): String =
        isoFormatter().format(Date(toMillis(jy, jm, jd, hour, minute)))

    /** رشته ISO UTC → شمسی به وقت محلی؛ در صورت خطای پارس null */
    fun fromUtcIso(iso: String): JalaliDateTime? {
        val date = parseIso(iso) ?: return null
        val cal = Calendar.getInstance().apply { time = date }
        return fromCalendar(cal)
    }

    /** پارس ISO با/بدون میلی‌ثانیه */
    fun parseIso(iso: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (p in patterns) {
            try {
                val f = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                return f.parse(iso)
            } catch (ignored: ParseException) {
                // الگوی بعدی امتحان می‌شود
            }
        }
        return null
    }

    fun nowUtcIso(): String = isoFormatter().format(Date())

    /** میلی‌ثانیه → رشته ISO UTC */
    fun millisToIso(millis: Long): String = isoFormatter().format(Date(millis))

    private fun isoFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
}
