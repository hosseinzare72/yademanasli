// مسیر: app/src/main/java/com/hossein/yademan/ui/components/LinearProgress.kt
package com.hossein.yademan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors

/** نوار پیشرفت (.progress): ارتفاع ۸، گوشه کامل، پرشدگی گرادینت gold3→gold1 از سمت راست */
@Composable
fun LinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "linearProgress"
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(YColors.Card2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(YBrush.ProgressFill)
        )
    }
}
