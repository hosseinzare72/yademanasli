// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/TrustScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrustItem(
    val reminder: Reminder,
    val kind: String,
    val holder: String,
    val due: Long,
    val done: Boolean
)

class TrustViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders

    val state: StateFlow<List<TrustItem>> = repo.observeAll().map { list ->
        list.filter { ReminderMeta.type(it) == ReminderType.TRUST }
            .sortedBy { DateUtils.isoToMillis(it.dueAt) ?: Long.MAX_VALUE }
            .map { r ->
                TrustItem(
                    reminder = r,
                    kind = ReminderMeta.str(r, "kind") ?: "امانت",
                    holder = ReminderMeta.str(r, "holder") ?: "",
                    due = DateUtils.isoToMillis(r.dueAt) ?: System.currentTimeMillis(),
                    done = r.isDone
                )
            }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** toggle هوشمند: اگر رکورد مال همسر باشد، از سرور به‌روز می‌شود */
    fun toggleRetrievedSmart(item: TrustItem) {
        viewModelScope.launch {
            if (HouseholdSyncManager.isForeign(item.reminder)) {
                HouseholdSyncManager.toggleDoneRemote(item.reminder, !item.done)
            } else {
                repo.setDoneOn(item.reminder, DateUtils.dayKey(System.currentTimeMillis()), !item.done)
            }
        }
    }
}

@Composable
fun TrustScreen(onBack: () -> Unit, onAdd: () -> Unit, vm: TrustViewModel = viewModel()) {
    val items by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "امانات و ضمانت‌ها",
                subtitle = "${items.count { !it.done }.fa()} مورد در امانت",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
                actions = { HeaderIconButton(icon = YIcons.Plus, contentDescription = "امانت جدید", onClick = onAdd) }
            )
        }
        if (items.isEmpty()) {
            item { EmptyState(message = "چک یا سفته‌ای در امانت نیست", icon = YIcons.Note) }
        } else {
            items(items, key = { it.reminder.id }) { t ->
                TrustRow(
                    item = t,
                    onToggle = { vm.toggleRetrievedSmart(t) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
        item {
            Text(
                text = "سر تاریخ بازیابی، یادمان بهت یادآوری می‌کند که چک/سفته را پس بگیری.",
                style = YText.Meta,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrustRow(item: TrustItem, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    val overdue = !item.done && item.due < System.currentTimeMillis()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = YColors.Info, icon = YIcons.Note, size = 44.dp, cornerRadius = 14.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.reminder.title,
                style = YText.Name,
                color = if (item.done) YColors.TextMuted else YColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = "${item.kind}${if (item.holder.isNotBlank()) " • نزد ${item.holder}" else ""}",
                style = YText.Meta,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    label = if (item.done) "پس گرفته شد ✓" else "بازیابی: ${DateUtils.dateShortFa(item.due)}",
                    color = when {
                        item.done -> YColors.Success
                        overdue -> YColors.Danger
                        else -> YColors.Info
                    },
                    icon = YIcons.Clock
                )
                // چیپ مالکیت در گروه دونفره
                HouseholdOwnerChip(reminder = item.reminder)
            }
        }
        if (item.done) {
            GhostButton(text = "برگردان", onClick = onToggle)
        } else {
            GoldButton(text = "پس گرفتم ✓", onClick = onToggle, small = true)
        }
    }
}