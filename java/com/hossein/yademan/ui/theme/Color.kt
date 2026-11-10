// مسیر: app/src/main/java/com/hossein/yademan/ui/theme/Color.kt
package com.hossein.yademan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** توکن‌های رنگی دقیقاً از source-html/common.css و DESIGN-SPEC.md مخزن */
object YColors {
    // پس‌زمینه‌ها
    val Bg0 = Color(0xFF05070F)      // --bg  پس‌زمینه اصلی
    val Bg1 = Color(0xFF0B0F1E)      // لایه میانی (از پرامپ)
    val Card = Color(0xFF131A30)     // --card کارت‌ها، تایل‌ها، نوار پایین
    val Card2 = Color(0xFF1A2240)    // --card2 اینپوت، سوییچ خاموش، ترک پیشرفت
    val HeroTop = Color(0xFF161E38)  // شروع گرادینت کارت پیشرفت خانه

    // طلایی
    val Gold1 = Color(0xFFF7DE8B)
    val Gold2 = Color(0xFFDDAF4B)
    val Gold3 = Color(0xFFD9B45B)
    val OnGold = Color(0xFF181203)   // متن روی دکمه طلایی

    // متن
    val TextPrimary = Color(0xFFF7F4EC)
    val TextSecondary = Color(0xFFB9BED2)
    val TextMuted = Color(0xFF7E8399)

    // دسته‌ها
    val CatInstallment = Color(0xFFE8B84B)
    val CatCheck = Color(0xFF9D8CFF)
    val CatMeeting = Color(0xFF4CC3FF)
    val CatMedicine = Color(0xFFFF7A9E)
    val CatShopping = Color(0xFF3ED598)
    val CatNote = Color(0xFFFFB84D)
    val CatSubscription = Color(0xFFFF8F5C)

    // وضعیت
    val Danger = Color(0xFFFF6B81)
    val Success = Color(0xFF3ED598)
    val Warning = Color(0xFFFFB84D)
    val Info = Color(0xFF4CC3FF)

    // خط جداکننده/حاشیه: rgba(255,255,255,.06)
    val Line = Color(0x0FFFFFFF)
    // متن روی بج‌های رنگی
    val OnBadge = Color(0xFF0B0E18)
}

/** گرادینت‌های پرکاربرد */
object YBrush {
    // linear-gradient(135deg, gold1, gold2 55%, gold3) برای CTA و FAB
    val Gold: Brush = Brush.linearGradient(
        0f to YColors.Gold1,
        0.55f to YColors.Gold2,
        1f to YColors.Gold3
    )
    // linear-gradient(135deg, gold1, gold2) برای سگمنت/تب/سوییچ فعال
    val GoldSoft: Brush = Brush.linearGradient(listOf(YColors.Gold1, YColors.Gold2))
    // linear-gradient(90deg, gold3, gold1) پرشدگی نوار پیشرفت
    val ProgressFill: Brush = Brush.horizontalGradient(listOf(YColors.Gold3, YColors.Gold1))
    // linear-gradient(150deg, #161E38, #131A30 60%) کارت پیشرفت خانه
    val HeroCard: Brush = Brush.linearGradient(
        0f to YColors.HeroTop,
        0.6f to YColors.Card,
        1f to YColors.Card
    )
}
