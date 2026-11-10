// مسیر: app/src/main/java/com/hossein/yademan/utils/PersianDigits.kt
package com.hossein.yademan.utils

import kotlin.math.abs

/** تبدیل ارقام لاتین به فارسی و جداکننده هزارگان «٬» */
object PersianDigits {
    private val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    const val THOUSANDS_SEPARATOR = '٬'
    const val PERCENT = '٪'

    /** هر رقم 0-9 (و ارقام عربی) را به معادل فارسی تبدیل می‌کند */
    fun toPersian(input: String): String = buildString(input.length) {
        for (c in input) {
            append(
                when (c) {
                    in '0'..'9' -> digits[c - '0']
                    in '٠'..'٩' -> digits[c - '٠']
                    else -> c
                }
            )
        }
    }

    /** ارقام فارسی/عربی را به لاتین برمی‌گرداند (برای پارس ورودی کاربر) */
    fun toLatin(input: String): String = buildString(input.length) {
        for (c in input) {
            append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
    }

    /** پارس عدد از متن کاربر (با ارقام فارسی و جداکننده) */
    fun parseLong(input: String): Long? =
        toLatin(input).filter { it.isDigit() }.takeIf { it.isNotEmpty() && it.length <= 15 }?.toLongOrNull()

    /** عدد با جداکننده هزارگان و ارقام فارسی: 12500000 → ۱۲٬۵۰۰٬۰۰۰ */
    fun formatNumber(value: Long): String {
        val raw = abs(value).toString()
        val grouped = raw.reversed().chunked(3).joinToString(THOUSANDS_SEPARATOR.toString()).reversed()
        return toPersian(if (value < 0) "-$grouped" else grouped)
    }

    /** درصد فارسی: 68 → ۶۸٪ */
    fun percent(value: Int): String = toPersian(value.toString()) + PERCENT

    /** مبلغ با واحد: ۱۲٬۵۰۰٬۰۰۰ تومان */
    fun toman(value: Long): String = formatNumber(value) + " تومان"
}

/** helper عمومی: هر رشته/عدد را فارسی می‌کند */
fun fa(value: Any?): String = when (value) {
    null -> ""
    is Long -> PersianDigits.formatNumber(value)
    is Int -> PersianDigits.formatNumber(value.toLong())
    else -> PersianDigits.toPersian(value.toString())
}

fun String.fa(): String = PersianDigits.toPersian(this)
fun Int.fa(): String = PersianDigits.formatNumber(this.toLong())
fun Long.fa(): String = PersianDigits.formatNumber(this)
