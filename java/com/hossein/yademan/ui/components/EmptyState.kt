// مسیر: app/src/main/java/com/hossein/yademan/ui/components/EmptyState.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText

/** حالت خالی: کاشی آیکن کم‌رنگ + پیام وسط‌چین */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = YIcons.Note
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconTile(color = YColors.TextMuted, icon = icon, size = 64.dp, cornerRadius = 22.dp, iconSize = 28.dp, bgAlpha = 0.14f)
        Text(text = message, style = YText.Sub, textAlign = TextAlign.Center)
    }
}
