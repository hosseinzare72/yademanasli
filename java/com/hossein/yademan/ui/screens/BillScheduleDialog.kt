package com.hossein.yademan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.ui.components.Chip
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.JalaliConverter
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch

private data class ScheduleItem(val index: Int, val occ: Long)

/**
 * دیالوگ نمایش برنامهٔ کامل اقساط/چک‌ها.
 * کاملاً افزوده: فقط از توابع موجود استفاده می‌کند.
 */
@Composable
fun BillScheduleDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    onToggleDay: ((dayKey: String, done: Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val type = ReminderMeta.type(reminder)
    val unit = if (type == ReminderType.CHECK) "چک" else "قسط"
    val count = ReminderMeta.totalCount(reminder)

    val items = remember(reminder) {
        val list = mutableListOf<ScheduleItem>()
        if (count != null) {
            for (i in 1..count) {
                val occ = Recurrence.nthOccurrence(reminder, i) ?: break
                list.add(ScheduleItem(i, occ))
            }
        } else {
            var from = System.currentTimeMillis()
            for (i in 1..12) {
                val occ = Recurrence.nextOccurrence(reminder, from, skipDone = false) ?: break
                list.add(ScheduleItem(i, occ))
                from = occ + DateUtils.MINUTE
            }
        }
        list
    }

    val doneCount = items.count { ReminderMeta.isDoneOn(reminder, DateUtils.dayKey(it.occ)) }
    val nextIndex = items.firstOrNull { !ReminderMeta.isDoneOn(reminder, DateUtils.dayKey(it.occ)) }?.index
    val total = reminder.amountToman * items.size

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YColors.Card,
        title = {
            Column {
                Text(reminder.title, style = YText.CardTitle)
                Text(
                    buildString {
                        append("${items.size.fa()} $unit • ${doneCount.fa()} پرداخت‌شده")
                        if (reminder.amountToman > 0) append(" • جمع ${PersianDigits.toman(total)}")
                    },
                    style = YText.Meta,
                    color = YColors.TextSecondary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.occ }) { item ->
                    ScheduleRow(
                        item = item,
                        count = count,
                        amount = reminder.amountToman,
                        done = ReminderMeta.isDoneOn(reminder, DateUtils.dayKey(item.occ)),
                        isNext = item.index == nextIndex,
                        onClick = {
                            val day = DateUtils.dayKey(item.occ)
                            val cur = ReminderMeta.isDoneOn(reminder, day)
                            if (onToggleDay != null) {
                                onToggleDay(day, !cur)
                            } else {
                                scope.launch {
                                    ServiceLocator.reminders.setDoneOn(reminder, day, !cur)
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (onEdit != null) {
                TextButton(onClick = { onEdit(reminder.id); onDismiss() }) {
                    Text("ویرایش", style = YText.Name, color = YColors.Gold2)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", style = YText.Name, color = YColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    count: Int?,
    amount: Long,
    done: Boolean,
    isNext: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(YRadius.Md)
    val j = JalaliConverter.fromMillis(item.occ)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    done -> YColors.Success.copy(alpha = 0.08f)
                    isNext -> YColors.Gold2.copy(alpha = 0.10f)
                    else -> YColors.Card2
                }
            )
            .border(1.dp, if (isNext && !done) YColors.Gold2 else YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (done) YColors.Success.copy(alpha = 0.18f) else YColors.Gold2.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(item.index.fa(), style = YText.Name, color = if (done) YColors.Success else YColors.Gold2)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${JalaliConverter.weekdayNameFa(j.weekday)} ${j.formatShort()}",
                style = YText.Name,
                color = if (done) YColors.TextMuted else YColors.TextPrimary
            )
            Text(
                text = if (count != null) "مورد ${item.index.fa()} از ${count.fa()}" else DateUtils.timeFa(item.occ),
                style = YText.Meta
            )
        }
        if (amount > 0) {
            Text(PersianDigits.toman(amount), style = YText.Meta, color = YColors.TextSecondary)
        }
        Chip(
            label = when {
                done -> "پرداخت شده"
                isNext -> "سررسید بعدی"
                else -> "مانده"
            },
            color = when {
                done -> YColors.Success
                isNext -> YColors.Gold2
                else -> YColors.Warning
            }
        )
    }
}