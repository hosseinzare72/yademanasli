// مسیر: app/src/main/java/com/hossein/yademan/ui/components/CheckCircle.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors

/** دایره چک: انجام‌شده = طلایی پر با تیک تیره؛ باز = حلقه کم‌رنگ */
@Composable
fun CheckCircle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(YBrush.GoldSoft)
                else Modifier.border(1.8.dp, YColors.TextMuted.copy(alpha = 0.55f), CircleShape)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = YIcons.CheckMark,
                contentDescription = "انجام شد",
                tint = YColors.OnGold,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}
