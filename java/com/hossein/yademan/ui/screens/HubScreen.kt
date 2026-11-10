// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/HubScreen.kt
package com.hossein.yademan.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.services.HouseholdSyncManager
import com.hossein.yademan.ui.components.CheckCircle
import com.hossein.yademan.ui.components.Chip
import com.hossein.yademan.ui.components.EmptyState
import com.hossein.yademan.ui.components.GhostButton
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.HouseholdOwnerChip
import com.hossein.yademan.ui.components.IconTile
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "HubScreen"

class HubViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    var key by mutableStateOf("")
        private set

    val all: StateFlow<List<Reminder>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun init(key: String) {
        if (this.key != key) this.key = key
    }

    fun itemsOf(list: List<Reminder>): List<Reminder> =
        list.filter { !it.isDeleted && ReminderMeta.str(it, "hub") == key }
            .sortedBy { DateUtils.isoToMillis(it.dueAt) ?: Long.MAX_VALUE }

    fun candidatesOf(list: List<Reminder>): List<Reminder> =
        list.filter { !it.isDeleted && ReminderMeta.str(it, "hub").isNullOrBlank() }
            .sortedBy { DateUtils.isoToMillis(it.dueAt) ?: Long.MAX_VALUE }

    /**
     * ✅ نهایی: برای یادآورهای یک‌بار با ارسال dayKey
     */
    fun toggle(r: Reminder) {
        viewModelScope.launch {
            val day = DateUtils.todayKey()
            val newState = !ReminderMeta.isDoneOn(r, day)

            repo.setDoneOn(r, day, newState)
            Log.d(TAG, "toggle: local done=$newState, day=$day")

            if (HouseholdSyncManager.isForeign(r)) {
                runCatching { HouseholdSyncManager.toggleDoneRemote(r, newState, day) }
                    .onFailure { e -> Log.w(TAG, "toggle remote failed", e) }
            }
        }
    }

    /**
     * ✅ نهایی: برای یادآورهای دوره‌ای با ارسال dayKey
     */
    fun toggleOcc(r: Reminder, occ: Long, done: Boolean) {
        viewModelScope.launch {
            val dayKey = DateUtils.dayKey(occ)

            repo.setDoneOn(r, dayKey, done)
            Log.d(TAG, "toggleOcc: local done=$done, dayKey=$dayKey, occ=$occ")

            if (HouseholdSyncManager.isForeign(r)) {
                runCatching { HouseholdSyncManager.toggleDoneRemote(r, done, dayKey) }
                    .onFailure { e -> Log.w(TAG, "toggleOcc remote failed", e) }
            }
        }
    }

    fun move(ids: Set<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            ids.forEach { id ->
                val r = repo.getById(id) ?: return@forEach
                repo.update(ReminderMeta.with(r, "hub", key))
            }
            onDone()
        }
    }
}

@Composable
fun HubScreen(
    hubKey: String,
    label: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    vm: HubViewModel = viewModel()
) {
    LaunchedEffect(hubKey) { vm.init(hubKey) }
    val all by vm.all.collectAsStateWithLifecycle()
    val rows = remember(all, vm.key) { vm.itemsOf(all) }
    val todayKey = DateUtils.todayKey()
    val now = System.currentTimeMillis()
    var showMove by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            title = label,
            subtitle = "${rows.size.fa()} مورد",
            leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
            actions = { HeaderIconButton(icon = YIcons.Plus, contentDescription = "افزودن", onClick = onAdd) }
        )
        if (rows.isEmpty()) {
            EmptyState(message = "هنوز موردی در این بخش نیست", icon = if (hubKey == "rent") YIcons.Rent else YIcons.Calendar)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoldButton(text = "افزودن به این بخش", onClick = onAdd, modifier = Modifier.fillMaxWidth())
                GhostButton(text = "انتقال از یادآوری‌های کلی", onClick = { showMove = true }, modifier = Modifier.fillMaxWidth())
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp)) {
                items(rows, key = { it.id }) { r ->
                    val repeating = Recurrence.isRepeating(r)
                    if (repeating || r.amountToman > 0) {
                        HubBillRow(
                            r = r,
                            now = now,
                            onPay = { vm.toggleOcc(r, BillsLogic.info(r, now).occurrence, true) },
                            onUndo = { vm.toggleOcc(r, BillsLogic.info(r, now).occurrence, false) },
                            onClick = { onEdit(r.id) }
                        )
                    } else {
                        HubRow(
                            r = r,
                            todayKey = todayKey,
                            onToggle = { vm.toggle(r) },
                            onClick = { onEdit(r.id) }
                        )
                    }
                }
            }
        }
    }

    if (showMove) {
        val candidates = remember(all) { vm.candidatesOf(all) }
        MoveDialog(
            candidates = candidates,
            onConfirm = { ids -> vm.move(ids) { showMove = false } },
            onDismiss = { showMove = false }
        )
    }
}

@Composable
private fun HubBillRow(
    r: Reminder,
    now: Long,
    onPay: () -> Unit,
    onUndo: () -> Unit,
    onClick: () -> Unit
) {
    val b = BillsLogic.info(r, now)
    val type = ReminderMeta.type(r)
    val color = typeColor(type)
    val chipColor = when {
        b.paid -> YColors.Success
        b.daysLeft <= 0 -> YColors.Danger
        b.daysLeft <= 3 -> YColors.Warning
        else -> YColors.Info
    }
    val shape = RoundedCornerShape(YRadius.Md)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(color = color, icon = typeIcon(type), size = 44.dp, cornerRadius = 14.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = r.title, style = YText.Name, maxLines = 1)
                Text(
                    text = buildString {
                        append(Recurrence.labelFa(r))
                        append(" • سررسید: ")
                        append(DateUtils.dateShortFa(b.occurrence))
                        if (r.alarmEnabled) append(" • یادآوری خودکار")
                    },
                    style = YText.Meta,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
                HouseholdOwnerChip(reminder = r, modifier = Modifier.padding(top = 6.dp))
            }
            if (r.amountToman > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = r.amountToman.fa(), style = YText.Amount)
                    Text(text = "تومان", style = YText.Meta)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(label = BillsLogic.statusText(b), color = chipColor, icon = if (b.paid) YIcons.CheckMark else YIcons.Clock)
                b.progressText?.let { Chip(label = it, color = if (b.finished) YColors.Success else YColors.Gold2) }
            }
            if (b.paid) {
                GhostButton(text = "برگرداندن", onClick = onUndo)
            } else {
                GoldButton(text = "پرداخت", onClick = onPay, small = true)
            }
        }
    }
}

@Composable
private fun HubRow(r: Reminder, todayKey: String, onToggle: () -> Unit, onClick: () -> Unit) {
    val done = ReminderMeta.isDoneOn(r, todayKey)
    val millis = DateUtils.isoToMillis(r.dueAt)
    val type = ReminderMeta.type(r)
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = typeColor(type), icon = typeIcon(type), size = 42.dp, cornerRadius = 14.dp, iconSize = 21.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = r.title,
                style = YText.Name,
                color = if (done) YColors.TextMuted else YColors.TextPrimary,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
            val sub = buildString {
                if (millis != null) {
                    append(DateUtils.dateShortFa(millis))
                    append(" • ")
                    append(DateUtils.timeFa(millis))
                }
                if (r.amountToman > 0) {
                    if (isNotEmpty()) append(" • ")
                    append(PersianDigits.toman(r.amountToman))
                }
            }
            if (sub.isNotBlank()) Text(sub, style = YText.Meta, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        CheckCircle(checked = done, onToggle = onToggle)
    }
}

@Composable
private fun MoveDialog(candidates: List<Reminder>, onConfirm: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YColors.Card,
        title = { Text("انتقال به این بخش", style = YText.CardTitle) },
        text = {
            if (candidates.isEmpty()) {
                Text("موردی برای انتقال نیست", style = YText.Meta)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(candidates, key = { it.id }) { r ->
                        val on = r.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = if (on) selected - r.id else selected + r.id }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CheckCircle(checked = on, onToggle = { selected = if (on) selected - r.id else selected + r.id })
                            Text(r.title, style = YText.Name, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected.isNotEmpty(), onClick = { onConfirm(selected) }) {
                Text("انتقال", style = YText.Name, color = YColors.Gold2)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", style = YText.Name, color = YColors.TextSecondary) }
        }
    )
}