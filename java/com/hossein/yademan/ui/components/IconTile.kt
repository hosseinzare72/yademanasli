// مسیر: app/src/main/java/com/hossein/yademan/ui/components/IconTile.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YRadius

/** کاشی آیکن رنگی (.tile): ۴۴×۴۴، گوشه ۱۵، پس‌زمینه همان رنگ با آلفای کم */
@Composable
fun IconTile(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = YRadius.Tile,
    iconSize: Dp = 22.dp,
    bgAlpha: Float = 0.25f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.copy(alpha = bgAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}
