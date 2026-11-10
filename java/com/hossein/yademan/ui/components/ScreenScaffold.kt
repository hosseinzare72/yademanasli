// مسیر: app/src/main/java/com/hossein/yademan/ui/components/ScreenScaffold.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText

/**
 * پس‌زمینه صفحه (.phone): رنگ bg + درخشش طلایی بالا-راست و هاله سرمه‌ای پایین-چپ.
 * موقعیت‌ها مطلق هستند (مثل CSS) و با RTL قرینه نمی‌شوند.
 */
@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YColors.Bg0)
            .drawBehind {
                val w = size.width
                val h = size.height
                val goldCenter = Offset(w * 0.88f, -h * 0.12f)
                val goldRadius = w * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(YColors.Gold2.copy(alpha = 0.10f), Color.Transparent),
                        center = goldCenter,
                        radius = goldRadius
                    ),
                    radius = goldRadius,
                    center = goldCenter
                )
                val navyCenter = Offset(w * 0.08f, h * 1.12f)
                val navyRadius = w * 0.68f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(YColors.Card2.copy(alpha = 0.9f), Color.Transparent),
                        center = navyCenter,
                        radius = navyRadius
                    ),
                    radius = navyRadius,
                    center = navyCenter
                )
            },
        content = content
    )
}

/** دکمه آیکنی هدر (.iconbtn): ۴۰×۴۰ گوشه ۱۴، با نقطه طلایی اختیاری */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDot: Boolean = false
) {
    val shape = RoundedCornerShape(YRadius.Sm)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = YColors.TextSecondary, modifier = Modifier.size(20.dp))
        if (showDot) {
            // نقطه ۷ با هاله ۳dp (box-shadow 0 0 0 3px)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 7.dp, top = 6.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(YColors.Gold2.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(YColors.Gold2)
                )
            }
        }
    }
}

/** هدر صفحه (.header): عنوان ۲۱ اکسترابولد + زیرعنوان، آیتم‌های اختیاری در دو طرف */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = if (leading != null) YText.Greeting else YText.Title)
            if (subtitle != null) {
                Text(text = subtitle, style = YText.Sub, modifier = Modifier.padding(top = 1.dp))
            }
        }
        actions()
    }
}
