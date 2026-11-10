// مسیر: app/src/main/java/com/hossein/yademan/ui/components/AppCard.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius

/** کارت پایه: پس‌زمینه card، حاشیه ۱dp با رنگ line، گوشه ۲۶ (قابل تغییر) */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    bg: Color = YColors.Card,
    brush: Brush? = null,
    cornerRadius: Dp = YRadius.Lg,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush ?: SolidColor(bg), shape)
            .border(1.dp, YColors.Line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}
