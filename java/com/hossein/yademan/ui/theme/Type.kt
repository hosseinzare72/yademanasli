// مسیر: app/src/main/java/com/hossein/yademan/ui/theme/Type.kt
package com.hossein.yademan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hossein.yademan.R

/** وزیرمتن با چهار وزن 400/500/700/800 (فایل‌های TTF در res/font) */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold)
)

/** سبک‌های متنی مطابق common.css (هر px طرح = ۱ dp/sp) */
object YText {
    private fun s(size: Float, weight: FontWeight, color: androidx.compose.ui.graphics.Color, lh: Float = 1.6f) =
        TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = (size * lh).sp,
            color = color
        )

    val Title = s(21f, FontWeight.ExtraBold, YColors.TextPrimary, 1.35f).copy(letterSpacing = (-0.2).sp) // h1.title
    val Greeting = s(18f, FontWeight.ExtraBold, YColors.TextPrimary, 1.35f)
    val Sub = s(11.5f, FontWeight.Medium, YColors.TextSecondary)          // .sub
    val Body = s(13f, FontWeight.Normal, YColors.TextPrimary)             // body
    val Section = s(14f, FontWeight.ExtraBold, YColors.TextPrimary)       // .sect
    val CardTitle = s(15f, FontWeight.ExtraBold, YColors.TextPrimary)
    val Name = s(13f, FontWeight.Bold, YColors.TextPrimary)               // .lrow .name
    val Meta = s(10.5f, FontWeight.Medium, YColors.TextMuted)             // .lrow .meta
    val MetaBold = s(10.5f, FontWeight.Bold, YColors.TextMuted)
    val Chip = s(10.5f, FontWeight.Bold, YColors.TextPrimary)             // .chip / .seg .opt
    val Label = s(10f, FontWeight.Bold, YColors.TextSecondary)            // .hub .lbl
    val Amount = s(13f, FontWeight.ExtraBold, YColors.TextPrimary)        // .amount
    val Button = s(14f, FontWeight.ExtraBold, YColors.OnGold, 1.3f)       // .btn-gold
    val ButtonSmall = s(11.5f, FontWeight.ExtraBold, YColors.OnGold, 1.3f)
    val Tab = s(11.5f, FontWeight.ExtraBold, YColors.TextMuted)           // .tab
    val RingValue = s(20f, FontWeight.ExtraBold, YColors.Gold1, 1.2f)     // .ringtxt b
    val RingCaption = s(8.5f, FontWeight.Bold, YColors.TextMuted, 1.3f)   // .ringtxt span
    val Badge = s(10f, FontWeight.ExtraBold, YColors.OnBadge, 1.2f)       // .hub .badge
}

/** Typography متریال با وزیرمتن تا Text پیش‌فرض هم فارسی و درست رندر شود */
val YademanTypography = Typography(
    displayLarge = YText.Title.copy(fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = YText.Title.copy(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = YText.Title,
    titleMedium = YText.CardTitle,
    titleSmall = YText.Section,
    bodyLarge = YText.Body,
    bodyMedium = YText.Body,
    bodySmall = YText.Sub,
    labelLarge = YText.Button,
    labelMedium = YText.Chip,
    labelSmall = YText.Label
)
