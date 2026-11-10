// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/RemindersScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.services.HouseholdSyncManager
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.*
import com.hossein.yademan.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

data class ReminderListRow(
    val reminder: Reminder,
    val type: String,
    val next: Long?,
    val dateText: String,
    val done: Boolean
)

data class RemindersState(
    val today: List<ReminderListRow> = emptyList(),
    val upcoming: List<ReminderListRow> = emptyList(),
    val done: List<ReminderListRow> = emptyList(),
    val billsCount: Int = 0,
    val eventsToday: Int = 0,
    val medsToday: Int = 0,
    val notesCount: Int = 0,
    val trustsCount: Int = 0,
    val subscriptionsCount: Int = 0
)

class RemindersViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders

    val state: StateFlow<RemindersState> = combine(repo.observeAll(), TimeTicker.now) { list, now ->
        val todayStart = DateUtils.startOfDay(now)
        val tomorrowStart = DateUtils.addDays(todayStart, 1)
        val todayKey = DateUtils.dayKey(now)
        val relevant = list.filter {
            val t = ReminderMeta.type(it)
            t != ReminderType.SHOPPING && t != ReminderType.NOTE
        }
        val today = relevant.mapNotNull { r ->
            val occ = Recurrence.occurrenceOnDay(r, todayStart) ?: return@mapNotNull null
            ReminderListRow(r, ReminderMeta.type(r), occ, "امروز • ${DateUtils.timeFa(occ)}", ReminderMeta.isDoneOn(r, todayKey))
        }.sortedBy { it.next }
        val upcoming = relevant.mapNotNull { r ->
            val occ = Recurrence.nextOccurrence(r, tomorrowStart) ?: return@mapNotNull null
            if (occ - now > 60 * DateUtils.DAY) return@mapNotNull null
            ReminderListRow(r, ReminderMeta.type(r), occ, "${DateUtils.dateShortFa(occ)} • ${DateUtils.timeFa(occ)}", false)
        }.sortedBy { it.next }
        val done = relevant.filter { !Recurrence.isRepeating(it) && it.isDone }.map { r ->
            val base = Recurrence.baseMillis(r) ?: now
            ReminderListRow(r, ReminderMeta.type(r), base, DateUtils.dateShortFa(base), true)
        }.sortedByDescending { it.next }
        RemindersState(
            today = today,
            upcoming = upcoming,
            done = done,
            billsCount = list.count { BillsLogic.isBill(it) },
            eventsToday = today.count { it.type == ReminderType.MEETING },
            medsToday = today.count { it.type == ReminderType.MEDICINE },
            notesCount = list.count { ReminderMeta.type(it) == ReminderType.NOTE },
            trustsCount = list.count { ReminderMeta.type(it) == ReminderType.TRUST && !it.isDone },
            subscriptionsCount = list.count { BillsLogic.isSubscription(it) }
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    /** toggle هوشمند: اگر رکورد مال همسر باشد، از سرور به‌روز می‌شود */
    fun toggleSmart(row: ReminderListRow) {
        val occ = row.next ?: return
        viewModelScope.launch {
            if (HouseholdSyncManager.isForeign(row.reminder)) {
                // رکورد مال همسر است → به سرور hh_update می‌زنیم
                HouseholdSyncManager.toggleDoneRemote(row.reminder, !row.done)
            } else {
                // رکورد مال خودمان است → روش محلی
                repo.setDoneOn(row.reminder, DateUtils.dayKey(occ), !row.done)
            }
        }
    }

    fun delete(row: ReminderListRow) {
        viewModelScope.launch { repo.delete(row.reminder.id) }
    }
}

@Composable
fun RemindersScreen(
    onOpenBills: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMedicine: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenTrust: () -> Unit,
    onEdit: (String) -> Unit,
    vm: RemindersViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<ReminderListRow?>(null) }
    val rows = when (tab) {
        0 -> state.today
        1 -> state.upcoming
        else -> state.done
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "یادآوری‌ها",
                subtitle = "${state.today.count { !it.done }.fa()} مورد امروز باقی مانده"
            )
        }
        item {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("اقساط و چک‌ها", "${state.billsCount.fa()} مورد", YIcons.Installment, YColors.CatInstallment, Modifier.weight(1f), onOpenBills)
                    QuickCard("قرارها و تقویم", "${state.eventsToday.fa()} رویداد امروز", YIcons.Calendar, YColors.CatMeeting, Modifier.weight(1f), onOpenCalendar)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("داروها", "${state.medsToday.fa()} دارو امروز", YIcons.Medicine, YColors.CatMedicine, Modifier.weight(1f), onOpenMedicine)
                    QuickCard("یادداشت‌ها", "${state.notesCount.fa()} یادداشت", YIcons.Note, YColors.CatNote, Modifier.weight(1f), onOpenNotes)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("اشتراک‌ها", "${state.subscriptionsCount.fa()} اشتراک", YIcons.Subscription, YColors.CatSubscription, Modifier.weight(1f), onOpenSubscriptions)
                    QuickCard("امانات", "${state.trustsCount.fa()} امانت", YIcons.Note, YColors.Info, Modifier.weight(1f), onOpenTrust)
                }
            }
        }
        item {
            SegmentedRow(
                options = listOf(
                    "امروز (${state.today.size.fa()})",
                    "آینده (${state.upcoming.size.fa()})",
                    "انجام‌شده (${state.done.size.fa()})"
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp)
            )
        }
        if (rows.isEmpty()) {
            item { EmptyState(message = "موردی نیست", icon = YIcons.Bell) }
        } else {
            items(rows, key = { "${tab}_${it.reminder.id}" }) { row ->
                ReminderRowView(
                    row = row,
                    showCheck = tab != 1,
                    onToggle = { vm.toggleSmart(row) },
                    onEdit = { onEdit(row.reminder.id) },
                    onLongPress = { toDelete = row },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
        item {
            Text(
                text = "ضربه برای ویرایش • نگه‌داشتن برای حذف",
                style = YText.Meta,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    toDelete?.let { row ->
        ConfirmDialog(
            title = "حذف «${row.reminder.title}»؟",
            message = "این یادآور و همه تکرارهایش حذف می‌شود.",
            onConfirm = {
                vm.delete(row)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = modifier
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconTile(color = color, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = YText.Name, maxLines = 1)
            Text(text = subtitle, style = YText.Meta, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRowView(
    row: ReminderListRow,
    showCheck: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(YRadius.Md)
    val color = typeColor(row.type)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .combinedClickable(onClick = onEdit, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = color, icon = typeIcon(row.type), size = 42.dp, cornerRadius = 14.dp, iconSize = 21.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.reminder.title,
                style = YText.Name,
                color = if (row.done) YColors.TextMuted else YColors.TextPrimary,
                textDecoration = if (row.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
            Text(
                text = "${ReminderType.labelFa(row.type)} • ${Recurrence.labelFa(row.reminder)} • ${reminderSubtitle(row.reminder)}",
                style = YText.Meta,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            // چیپ «مال همسر» فقط در گروه دونفره و برای رکوردهای همسر نمایش داده می‌شود
            HouseholdOwnerChip(
                reminder = row.reminder,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (showCheck) {
            CheckCircle(
                checked = row.done,
                onToggle = onToggle,
                size = 28.dp
            )
        }
        Icon(
            imageVector = YIcons.Edit,
            contentDescription = "ویرایش",
            tint = YColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}