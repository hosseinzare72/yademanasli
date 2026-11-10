// مسیر: app/src/main/java/com/hossein/yademan/ui/components/Chip.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YText

/**
 * چیپ وضعیت (.chip): پیله کامل (border-radius:999px در مخزن)، متن ۱۰.۵ bold،
 * پس‌زمینه همان رنگ با آلفای ۰.۱۴
 */
@Composable
fun Chip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 999.dp,
    icon: ImageVector? = null,
    bgAlpha: Float = 0.14f
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.copy(alpha = bgAlpha))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        Text(text = label, style = YText.Chip, color = color, maxLines = 1)
    }
}
