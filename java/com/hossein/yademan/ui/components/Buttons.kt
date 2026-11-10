// مسیر: app/src/main/java/com/hossein/yademan/ui/components/Buttons.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText

/** دکمه طلایی (.btn-gold): عادی ۴۸ تمام‌عرض با گوشه ۱۴؛ small ارتفاع ۳۴ با گوشه ۱۲ */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(if (small) 12.dp else YRadius.Sm)
    val sized = if (small) modifier.height(34.dp) else modifier.fillMaxWidth().height(48.dp)
    Row(
        modifier = sized
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = YColors.Gold2,
                spotColor = YColors.Gold2
            )
            .clip(shape)
            .background(YBrush.Gold)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 16.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = YColors.OnGold, modifier = Modifier.size(18.dp))
        }
        Text(text = text, style = if (small) YText.ButtonSmall else YText.Button, maxLines = 1)
    }
}

/** دکمه خاکستری (.btn-ghost): ارتفاع ۳۴، گوشه ۱۲، پس‌زمینه card2 */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(YColors.Card2)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = YText.ButtonSmall, color = YColors.TextSecondary, maxLines = 1)
    }
}
