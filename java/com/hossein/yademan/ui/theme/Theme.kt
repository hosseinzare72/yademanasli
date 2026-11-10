// مسیر: app/src/main/java/com/hossein/yademan/ui/theme/Theme.kt
package com.hossein.yademan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** شعاع‌های گوشه از common.css: --r-lg / --r-md / --r-sm */
object YRadius {
    val Lg = 26.dp    // کارت بزرگ، نوار ناوبری
    val Md = 20.dp    // کارت‌های لیست
    val Sm = 14.dp    // دکمه، اینپوت، سگمنت، دکمه آیکنی هدر
    val Tile = 15.dp  // .tile
}

private val YademanColorScheme = darkColorScheme(
    primary = YColors.Gold2,
    onPrimary = YColors.OnGold,
    secondary = YColors.Gold1,
    onSecondary = YColors.OnGold,
    tertiary = YColors.Info,
    background = YColors.Bg0,
    onBackground = YColors.TextPrimary,
    surface = YColors.Card,
    onSurface = YColors.TextPrimary,
    surfaceVariant = YColors.Card2,
    onSurfaceVariant = YColors.TextSecondary,
    outline = YColors.Line,
    error = YColors.Danger,
    onError = YColors.OnGold
)

private val YademanShapes = Shapes(
    small = RoundedCornerShape(YRadius.Sm),
    medium = RoundedCornerShape(YRadius.Md),
    large = RoundedCornerShape(YRadius.Lg)
)

/**
 * تم سفارشی یادمان (تیره + طلایی).
 * جهت RTL هم اینجا اعمال می‌شود تا Previewها نیز راست‌چین باشند.
 */
@Composable
fun YademanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YademanColorScheme,
        typography = YademanTypography,
        shapes = YademanShapes
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalContentColor provides YColors.TextPrimary,
            content = content
        )
    }
}
