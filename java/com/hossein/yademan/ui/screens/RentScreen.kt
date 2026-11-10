// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/RentScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.hossein.yademan.services.RentItem
import com.hossein.yademan.services.RentStore
import com.hossein.yademan.ui.components.Chip
import com.hossein.yademan.ui.components.ConfirmDialog
import com.hossein.yademan.ui.components.GhostButton
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.SegmentedRow
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.fa
import java.util.UUID
import kotlin.math.abs

@Composable
fun RentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(RentStore.all(context)) }
    var editing by remember { mutableStateOf<RentItem?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RentItem?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        items = RentStore.all(context)
        RentStore.scheduleAll(context)
    }

    fun refresh() { items = RentStore.all(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "اجاره",
                subtitle = "مدیریت سررسید اجاره‌های پرداختی و دریافتی",
                actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onBack) }
            )
        }
        item {
            GoldButton(
                text = "افزودن اجاره جدید",
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
        if (items.isEmpty()) {
            item {
                Text(
                    text = "هنوز موردی ثبت نکرده‌ای. با دکمه بالا اولین اجاره را اضافه کن.",
                    style = YText.Meta, color = YColors.TextMuted,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        items(items) { item ->
            RentCard(
                item = item,
                onPay = {
                    RentStore.setPaid(context, item, true)
                    refresh()
                },
                onUndo = {
                    RentStore.setPaid(context, item, false)
                    refresh()
                },
                onClick = { editing = item },
                onDelete = { deleting = item }
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showAdd) {
        RentEditDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { newItem ->
                RentStore.upsert(context, newItem)
                showAdd = false
                refresh()
            }
        )
    }

    editing?.let { currentItem ->
        RentEditDialog(
            initial = currentItem,
            onDismiss = { editing = null },
            onSave = { updatedItem ->
                RentStore.upsert(context, updatedItem)
                editing = null
                refresh()
            }
        )
    }

    deleting?.let { currentItem ->
        ConfirmDialog(
            title = "حذف این اجاره؟",
            message = "«${currentItem.title}» برای همیشه حذف می‌شود و یادآور آن لغو می‌گردد.",
            confirmText = "حذف",
            onConfirm = {
                RentStore.delete(context, currentItem.id)
                deleting = null
                refresh()
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun RentCard(
    item: RentItem,
    onPay: () -> Unit,
    onUndo: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(YRadius.Md)
    val paid = RentStore.isPaid(item)
    val days = RentStore.daysLeft(item)

    val chipLabel = when {
        paid -> "پرداخت شد ✓"
        days < 0 -> "${abs(days).fa()} روز گذشته"
        else -> "${days.fa()} روز مانده"
    }
    val chipColor = when {
        paid -> YColors.Success
        days <= 3 -> YColors.Danger
        else -> YColors.Success
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.title, style = YText.Name, modifier = Modifier.weight(1f))
            Chip(label = chipLabel, color = chipColor, icon = if (paid) YIcons.CheckMark else YIcons.Clock)
        }

        Text(
            text = "${if (item.kind == "pay") "پرداختی" else "دریافتی"} • روز ${item.dueDay.fa()} هر ماه • ${String.format("%,d", item.amountToman)} تومان",
            style = YText.Meta,
            modifier = Modifier.padding(top = 6.dp)
        )

        if (item.ownerName.isNotBlank()) {
            Text(text = item.ownerName, style = YText.Meta, color = YColors.TextMuted, modifier = Modifier.padding(top = 2.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDelete) {
                Text(text = "حذف", style = YText.Meta, color = YColors.Danger)
            }
            if (paid) {
                GhostButton(text = "برگرداندن", onClick = onUndo)
            } else {
                GoldButton(text = "پرداخت شد", onClick = onPay, small = true)
            }
        }
    }
}

@Composable
private fun RentEditDialog(
    initial: RentItem?,
    onDismiss: () -> Unit,
    onSave: (RentItem) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var kindIdx by remember { mutableStateOf(if (initial?.kind == "get") 1 else 0) }
    var amount by remember { mutableStateOf(initial?.amountToman?.toString() ?: "") }
    var dueDay by remember { mutableStateOf(initial?.dueDay?.toString() ?: "1") }
    var owner by remember { mutableStateOf(initial?.ownerName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YColors.Card,
        title = { Text(text = if (initial == null) "اجاره جدید" else "ویرایش اجاره", style = YText.CardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedRow(options = listOf("پرداختی", "دریافتی"), selectedIndex = kindIdx, onSelect = { kindIdx = it })
                YTextField(value = title, onValueChange = { title = it.take(40) }, placeholder = "عنوان (مثلاً اجاره خانه)")
                YTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() }.take(12) }, placeholder = "مبلغ ماهانه (تومان)", keyboardType = KeyboardType.Number)
                YTextField(value = dueDay, onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) }, placeholder = "روز سررسید هر ماه (۱ تا ۳۱)", keyboardType = KeyboardType.Number)
                YTextField(value = owner, onValueChange = { owner = it.take(30) }, placeholder = "نام موجر / مستأجر")
                YTextField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) }, placeholder = "شماره تماس", keyboardType = KeyboardType.Phone)
                YTextField(value = note, onValueChange = { note = it.take(60) }, placeholder = "یادداشت (اختیاری)")
                error?.let { Text(text = it, style = YText.Meta, color = YColors.Danger) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val day = dueDay.toIntOrNull() ?: 0
                if (title.isBlank()) { error = "عنوان را وارد کن"; return@TextButton }
                if (day !in 1..31) { error = "روز سررسید باید بین ۱ تا ۳۱ باشد"; return@TextButton }
                onSave(
                    RentItem(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        kind = if (kindIdx == 0) "pay" else "get",
                        amountToman = amount.toLongOrNull() ?: 0L,
                        dueDay = day,
                        ownerName = owner.trim(),
                        phone = phone.trim(),
                        note = note.trim(),
                        paidKeys = initial?.paidKeys ?: emptyList()
                    )
                )
            }) { Text(text = "ذخیره", style = YText.Name, color = YColors.Gold2) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "انصراف", style = YText.Name, color = YColors.TextSecondary) } }
    )
}