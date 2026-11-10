// مسیر: app/src/main/java/com/hossein/yademan/ui/components/ProgressRing.kt
package com.hossein.yademan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YColors

/**
 * حلقه درصد طلایی مطابق SVG خانه: viewBox ۹۶، شعاع ۴۰، استروک ۹،
 * شروع از بالا و گرادینت gold1→gold2 با سر گرد
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 9.dp,
    trackColor: Color = YColors.Card2,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "ringProgress"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val dim = this.size.minDimension
            // در طرح مرکز خط استروک ۸ واحد از لبه ۹۶تایی فاصله دارد
            val inset = maxOf(stroke / 2f, dim * 8f / 96f)
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(dim - inset * 2f, dim - inset * 2f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (animated > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(YColors.Gold1, YColors.Gold2),
                        start = Offset.Zero,
                        end = Offset(dim, dim)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}
