// مسیر: app/src/main/java/com/hossein/yademan/ui/components/JalaliDatePickerDialog.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.JalaliConverter
import com.hossein.yademan.utils.JalaliUtils
import com.hossein.yademan.utils.fa

/**
 * پنجره انتخاب تاریخ شمسی با تقویم ماهانه، میان‌برها و ناوبری ماه/سال.
 * خروجی: millis (نیمروز همان روز).
 */
@Composable
fun JalaliDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initial = JalaliConverter.fromMillis(initialMillis)
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month) }
    var selectedDay by remember { mutableIntStateOf(initial.day) }

    val todayJ = JalaliConverter.fromMillis(System.currentTimeMillis())

    fun goto(y: Int, m: Int, d: Int) {
        year = y
        month = m
        selectedDay = d.coerceIn(1, JalaliUtils.monthLength(y, m))
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(YColors.Card)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (month == 1) goto(year - 1, 12, selectedDay)
                        else goto(year, month - 1, selectedDay)
                    }) {
                        Icon(YIcons.ChevronLeft, null, tint = YColors.Gold2)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(JalaliConverter.monthName(month), style = YText.CardTitle, color = YColors.Gold1)
                        Text(year.toString().fa(), style = YText.Meta, color = YColors.TextSecondary)
                    }
                    IconButton(onClick = {
                        if (month == 12) goto(year + 1, 1, selectedDay)
                        else goto(year, month + 1, selectedDay)
                    }) {
                        Icon(YIcons.ChevronLeft, null, tint = YColors.Gold2, modifier = Modifier.scale(-1f, 1f))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { w ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(w, style = YText.Chip, color = YColors.TextMuted)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                val firstDayMillis = JalaliUtils.toMillis(year, month, 1)
                val firstWeekday = ((JalaliConverter.fromMillis(firstDayMillis).weekday % 7) + 7) % 7
                val daysInMonth = JalaliUtils.monthLength(year, month)

                (0 until 42).chunked(7).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { idx ->
                            val dayNum = idx - firstWeekday + 1
                            val valid = dayNum in 1..daysInMonth
                            val isSelected = valid && dayNum == selectedDay
                            val isToday = valid && dayNum == todayJ.day &&
                                    month == todayJ.month && year == todayJ.year

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> YColors.Gold2
                                            isToday -> YColors.Gold2.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (valid) Modifier.clickable { selectedDay = dayNum }
                                        else Modifier
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = YColors.Gold2,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (valid) {
                                    Text(
                                        dayNum.toString().fa(),
                                        style = YText.Name,
                                        color = if (isSelected) YColors.OnGold else YColors.TextPrimary,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShortcutChip("امروز", Modifier.weight(1f)) {
                        goto(todayJ.year, todayJ.month, todayJ.day)
                    }
                    ShortcutChip("فردا", Modifier.weight(1f)) {
                        val t = JalaliConverter.fromMillis(DateUtils.addDays(System.currentTimeMillis(), 1))
                        goto(t.year, t.month, t.day)
                    }
                    ShortcutChip("هفته بعد", Modifier.weight(1f)) {
                        val t = JalaliConverter.fromMillis(DateUtils.addDays(System.currentTimeMillis(), 7))
                        goto(t.year, t.month, t.day)
                    }
                    ShortcutChip("ماه بعد", Modifier.weight(1f)) {
                        if (month == 12) goto(year + 1, 1, selectedDay)
                        else goto(year, month + 1, selectedDay)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", style = YText.Name, color = YColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(JalaliUtils.toMillis(year, month, selectedDay)) }) {
                        Text("انتخاب", style = YText.Name, color = YColors.Gold2)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(YColors.Card2)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = YText.Chip, color = YColors.Gold2)
    }
}