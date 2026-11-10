// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/CalendarScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

data class WeekDayCell(
    val dayMillis: Long,
    val label: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val dots: List<Color>
)

data class TimelineItem(
    val reminder: Reminder,
    val type: String,
    val occurrence: Long,
    val time: String,
    val done: Boolean
)

data class CalendarState(
    val title: String = "",
    val subtitle: String = "",
    val selectedDay: Long = 0L,
    val isTodaySelected: Boolean = true,
    val week: List<WeekDayCell> = emptyList(),
    val events: List<TimelineItem> = emptyList()
)

class CalendarViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    private val selected = MutableStateFlow(DateUtils.todayStart())

    val state: StateFlow<CalendarState> = combine(repo.observeAll(), selected, TimeTicker.now) { list, sel, now ->
        build(list, sel, now)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    private fun eventsOn(list: List<Reminder>, day: Long): List<TimelineItem> {
        val key = DateUtils.dayKey(day)
        return list.mapNotNull { r ->
            val type = ReminderMeta.type(r)
            if (type == ReminderType.SHOPPING || type == ReminderType.NOTE) return@mapNotNull null
            val occ = Recurrence.occurrenceOnDay(r, day) ?: return@mapNotNull null
            TimelineItem(r, type, occ, DateUtils.timeFa(occ), ReminderMeta.isDoneOn(r, key))
        }.sortedBy { it.occurrence }
    }

    private fun build(list: List<Reminder>, sel: Long, now: Long): CalendarState {
        val j = JalaliConverter.fromMillis(sel)
        val weekStart = DateUtils.addDays(sel, -j.weekday) // شنبه
        val todayDay = DateUtils.epochDay(now)
        val week = (0 until 7).map { i ->
            val d = DateUtils.addDays(weekStart, i)
            val jd = JalaliConverter.fromMillis(d)
            val dots = eventsOn(list, d).map { typeColor(it.type) }.distinct().take(3)
            WeekDayCell(
                dayMillis = d,
                label = JalaliConverter.weekdayShortFa(i),
                dayNumber = jd.day.fa(),
                isToday = DateUtils.epochDay(d) == todayDay,
                isSelected = DateUtils.epochDay(d) == DateUtils.epochDay(sel),
                dots = dots
            )
        }
        val events = eventsOn(list, sel)
        return CalendarState(
            title = j.formatMonthYear(),
            subtitle = "${JalaliConverter.weekdayNameFa(j.weekday)} ${j.formatShort()} • ${events.size.fa()} رویداد",
            selectedDay = sel,
            isTodaySelected = DateUtils.epochDay(sel) == todayDay,
            week = week,
            events = events
        )
    }

    fun select(day: Long) {
        selected.value = DateUtils.startOfDay(day)
    }

    fun shiftWeek(weeks: Int) {
        selected.value = DateUtils.addDays(selected.value, 7 * weeks)
    }

    fun goToday() {
        selected.value = DateUtils.todayStart()
    }

    /** toggle هوشمند: اگر رکورد مال همسر باشد، از سرور به‌روز می‌شود */
    fun toggleSmart(item: TimelineItem) {
        viewModelScope.launch {
            if (HouseholdSyncManager.isForeign(item.reminder)) {
                HouseholdSyncManager.toggleDoneRemote(item.reminder, !item.done)
            } else {
                repo.setDoneOn(item.reminder, DateUtils.dayKey(item.occurrence), !item.done)
            }
        }
    }
}

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onAdd: (dayMillis: Long) -> Unit,
    vm: CalendarViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = state.title,
                subtitle = state.subtitle,
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
                actions = {
                    HeaderIconButton(icon = YIcons.Plus, contentDescription = "رویداد جدید", onClick = { onAdd(state.selectedDay) })
                }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "هفته قبل", onClick = { vm.shiftWeek(-1) })
                Text(
                    text = if (state.isTodaySelected) "این هفته" else "برو به امروز",
                    style = YText.MetaBold,
                    color = if (state.isTodaySelected) YColors.TextMuted else YColors.Gold2,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.goToday() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                HeaderIconButton(icon = YIcons.ChevronLeft, contentDescription = "هفته بعد", onClick = { vm.shiftWeek(1) })
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.week.forEach { cell ->
                    WeekCell(cell = cell, modifier = Modifier.weight(1f), onClick = { vm.select(cell.dayMillis) })
                }
            }
        }
        item {
            SectionTitle(
                title = if (state.isTodaySelected) "برنامه امروز" else "برنامه روز",
                trailingChip = { Chip(label = "${state.events.size.fa()} رویداد", color = YColors.Info) }
            )
        }
        if (state.events.isEmpty()) {
            item { EmptyState(message = "برای این روز رویدادی نیست", icon = YIcons.Calendar) }
        } else {
            itemsIndexed(state.events, key = { _, e -> e.reminder.id }) { index, ev ->
                TimelineRow(
                    item = ev,
                    isLast = index == state.events.lastIndex,
                    onToggle = { vm.toggleSmart(ev) }
                )
            }
        }
    }
}

@Composable
private fun WeekCell(cell: WeekDayCell, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bgMod = when {
        cell.isSelected -> Modifier.background(YBrush.GoldSoft)
        cell.isToday -> Modifier.background(YColors.Gold2.copy(alpha = 0.14f))
        else -> Modifier.background(YColors.Card)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .then(bgMod)
            .border(1.dp, if (cell.isToday && !cell.isSelected) YColors.Gold2.copy(alpha = 0.5f) else YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = cell.label,
            style = YText.Label,
            color = if (cell.isSelected) YColors.OnGold else YColors.TextMuted
        )
        Text(
            text = cell.dayNumber,
            style = YText.CardTitle,
            color = when {
                cell.isSelected -> YColors.OnGold
                cell.isToday -> YColors.Gold1
                else -> YColors.TextPrimary
            }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.height(6.dp)
        ) {
            cell.dots.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (cell.isSelected) YColors.OnGold else c)
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem, isLast: Boolean, onToggle: () -> Unit) {
    val color = typeColor(item.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = item.time,
            style = YText.MetaBold,
            color = YColors.TextSecondary,
            modifier = Modifier
                .width(44.dp)
                .padding(top = 18.dp)
        )
        TimelineDot(color = color, showLine = !isLast)
        val shape = RoundedCornerShape(YRadius.Md)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
                .clip(shape)
                .background(YColors.Card)
                .border(1.dp, YColors.Line, shape)
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconTile(color = color, icon = typeIcon(item.type), size = 38.dp, cornerRadius = 12.dp, iconSize = 19.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.reminder.title,
                    style = YText.Name,
                    color = if (item.done) YColors.TextMuted else YColors.TextPrimary,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1
                )
                Text(text = reminderSubtitle(item.reminder), style = YText.Meta, maxLines = 1)
                // چیپ مالکیت در گروه دونفره
                HouseholdOwnerChip(
                    reminder = item.reminder,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            CheckCircle(checked = item.done, onToggle = onToggle, size = 22.dp)
        }
    }
}