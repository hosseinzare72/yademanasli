// مسیر: app/src/main/java/com/hossein/yademan/ui/components/ToggleRow.kt
package com.hossein.yademan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText

/** سوییچ طلایی (.toggle): ۴۶×۲۷؛ روشن = پیله طلایی + گنجک سفید در انتها */
@Composable
fun GoldSwitch(isOn: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val knobOffset by animateDpAsState(if (isOn) 19.dp else 0.dp, label = "knobOffset")
    val knobColor by animateColorAsState(if (isOn) Color.White else YColors.TextMuted, label = "knobColor")
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 27.dp)
            .clip(shape)
            .background(if (isOn) YBrush.GoldSoft else SolidColor(YColors.Card2), shape)
            .then(if (isOn) Modifier else Modifier.border(1.dp, YColors.Line, shape))
            .clickable { onToggle(!isOn) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .offset(x = knobOffset)
                .size(19.dp)
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}

/** ردیف تنظیم با کاشی آیکن، برچسب، زیرعنوان اختیاری و سوییچ طلایی */
@Composable
fun ToggleRow(
    icon: ImageVector,
    label: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconColor: Color = YColors.Gold2
) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable { onToggle(!isOn) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = iconColor, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp, bgAlpha = 0.16f)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = YText.Name)
            if (subtitle != null) Text(text = subtitle, style = YText.Meta)
        }
        GoldSwitch(isOn = isOn, onToggle = onToggle)
    }
}
