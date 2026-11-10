// مسیر: app/src/main/java/com/hossein/yademan/ui/components/SegmentedRow.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText

/** سگمنت (.seg): گزینه فعال = پیله طلایی با متن تیره */
@Composable
fun SegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(YRadius.Sm)
    val optionShape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(optionShape)
                    .then(if (selected) Modifier.background(YBrush.GoldSoft) else Modifier)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = YText.Chip,
                    color = if (selected) YColors.OnGold else YColors.TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}
