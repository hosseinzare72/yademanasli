// مسیر: app/src/main/java/com/hossein/yademan/ui/components/TimelineDot.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YColors

/**
 * نقطه تایم‌لاین + خط اتصال عمودی.
 * والد باید Row با Modifier.height(IntrinsicSize.Min) باشد تا خط تا انتهای ردیف کشیده شود.
 */
@Composable
fun TimelineDot(
    color: Color,
    modifier: Modifier = Modifier,
    showLine: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
                .border(3.dp, color.copy(alpha = 0.25f), CircleShape)
        )
        if (showLine) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(2.dp)
                    .weight(1f)
                    .background(YColors.Line)
            )
        }
    }
}
