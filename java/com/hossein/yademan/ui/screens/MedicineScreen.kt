// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/MedicineScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

enum class MedState { TAKEN, MISSED, UPCOMING }

data class MedItem(
    val reminder: Reminder,
    val occurrence: Long,
    val time: String,
    val dose: String,
    val state: MedState
)

data class AdherenceDay(val label: String, val complete: Boolean, val isToday: Boolean, val isFuture: Boolean)

data class MedicineState(
    val today: List<MedItem> = emptyList(),
    val weekPercent: Int = 0,
    val streak: Int = 0,
    val week: List<AdherenceDay> = emptyList()
)

class MedicineViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders

    val state: StateFlow<MedicineState> = combine(repo.observeAll(), TimeTicker.now) { list, now ->
        build(list.filter { ReminderMeta.type(it) == ReminderType.MEDICINE }, now)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicineState())

    private fun dayStats(meds: List<Reminder>, day: Long): Pair<Int, Int> {
        val key = DateUtils.dayKey(day)
        var scheduled = 0
        var taken = 0
        meds.forEach { m ->
            if (Recurrence.occurrenceOnDay(m, day) != null) {
                scheduled++
                if (ReminderMeta.isDoneOn(m, key)) taken++
            }
        }
        return scheduled to taken
    }

    private fun build(meds: List<Reminder>, now: Long): MedicineState {
        val todayStart = DateUtils.startOfDay(now)
        val todayKey = DateUtils.dayKey(now)
        val today = meds.mapNotNull { m ->
            val occ = Recurrence.occurrenceOnDay(m, todayStart) ?: return@mapNotNull null
            val taken = ReminderMeta.isDoneOn(m, todayKey)
            val st = when {
                taken -> MedState.TAKEN
                occ <= now -> MedState.MISSED
                else -> MedState.UPCOMING
            }
            MedItem(m, occ, DateUtils.timeFa(occ), ReminderMeta.str(m, "dose") ?: "", st)
        }.sortedBy { it.occurrence }

        var scheduledSum = 0
        var takenSum = 0
        for (i in 0 until 7) {
            val day = DateUtils.addDays(todayStart, -i)
            if (i == 0) {
                val due = today.filter { it.occurrence <= now || it.state == MedState.TAKEN }
                scheduledSum += due.size
                takenSum += due.count { it.state == MedState.TAKEN }
            } else {
                val (s, t) = dayStats(meds, day)
                scheduledSum += s
                takenSum += t
            }
        }
        val percent = if (scheduledSum == 0) 0 else (takenSum * 100) / scheduledSum

        var streak = 0
        val (ts, tt) = dayStats(meds, todayStart)
        var i = if (ts > 0 && ts == tt) 0 else 1
        while (i < 366) {
            val (s, t) = dayStats(meds, DateUtils.addDays(todayStart, -i))
            if (s == 0 || s != t) break
            streak++
            i++
        }

        val j = JalaliConverter.fromMillis(now)
        val weekStart = DateUtils.addDays(todayStart, -j.weekday)
        val todayEpoch = DateUtils.epochDay(now)
        val week = (0 until 7).map { k ->
            val d = DateUtils.addDays(weekStart, k)
            val e = DateUtils.epochDay(d)
            val (s, t) = dayStats(meds, d)
            AdherenceDay(
                label = JalaliConverter.weekdayShortFa(k),
                complete = e <= todayEpoch && s > 0 && s == t,
                isToday = e == todayEpoch,
                isFuture = e > todayEpoch
            )
        }
        return MedicineState(today, percent, streak, week)
    }

    /** مصرف هوشمند: اگر رکورد مال همسر باشد، از سرور به‌روز می‌شود */
    fun setTakenSmart(item: MedItem, taken: Boolean) {
        viewModelScope.launch {
            if (HouseholdSyncManager.isForeign(item.reminder)) {
                HouseholdSyncManager.toggleDoneRemote(item.reminder, taken)
            } else {
                repo.setDoneOn(item.reminder, DateUtils.dayKey(item.occurrence), taken)
            }
        }
    }
}

@Composable
fun MedicineScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    vm: MedicineViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showInfo by remember { mutableStateOf(false) }
    val mood = when {
        state.weekPercent >= 80 -> "عالیه!"
        state.weekPercent >= 50 -> "خوبه!"
        else -> "ادامه بده!"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "یادآوری دارو",
                subtitle = "${state.today.size.fa()} دارو برای امروز",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
                actions = { HeaderIconButton(icon = YIcons.Plus, contentDescription = "داروی جدید", onClick = onAdd) }
            )
        }
        item {
            AppCard(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth(),
                brush = YBrush.HeroCard
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "پایداری این هفته", style = YText.Sub)
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    text = PersianDigits.percent(state.weekPercent),
                                    style = YText.Title.copy(fontSize = 26.sp, color = YColors.Gold1)
                                )
                                Text(text = "  $mood", style = YText.CardTitle, modifier = Modifier.padding(bottom = 3.dp))
                            }
                        }
                        Chip(label = "${state.streak.fa()} روز متوالی", color = YColors.Success, icon = YIcons.Sparkle)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        state.week.forEach { d -> AdherenceCircle(d) }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(YRadius.Md))
                    .background(YColors.Card)
                    .border(1.dp, YColors.Line, RoundedCornerShape(YRadius.Md))
                    .clickable { showInfo = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconTile(color = YColors.Info, icon = YIcons.Medicine, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("راهنمای یادآوری دارو", style = YText.Name)
                    Text("چطور کار می‌کند؟ ضربه بزن", style = YText.Meta)
                }
                Icon(YIcons.ChevronLeft, null, tint = YColors.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
        item { SectionTitle(title = "داروهای امروز", trailing = "${state.today.count { it.state == MedState.TAKEN }.fa()} از ${state.today.size.fa()} مصرف شد") }
        if (state.today.isEmpty()) {
            item { EmptyState(message = "دارویی برای امروز ثبت نشده", icon = YIcons.Medicine) }
        } else {
            items(state.today, key = { it.reminder.id }) { m ->
                MedicineRow(
                    item = m,
                    onTake = { vm.setTakenSmart(m, true) },
                    onUndo = { vm.setTakenSmart(m, false) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = YColors.Card,
            title = { Text("راهنمای یادآوری دارو", style = YText.CardTitle, color = YColors.Gold1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• هر دارو را با دوز و ساعت مصرف ثبت کن؛ یادمان سر ساعت یادآوری می‌کند.", style = YText.Meta)
                    Text("• با «مصرف کردم» آن روز تیک می‌خورد و در پایداری هفته حساب می‌شود.", style = YText.Meta)
                    Text("• اگر ساعت گذشت و تیک نزنی، دارو «جامانده» قرمز می‌شود.", style = YText.Meta)
                    Text("• روزهای متوالیِ کامل، رکورد «روز متوالی» می‌سازند.", style = YText.Meta)
                    Text("• داروها فقط روی گوشی تو می‌مانند؛ مگر سینک یا پشتیبان فعال باشد.", style = YText.Meta)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("باشه", style = YText.Name, color = YColors.Gold2) }
            }
        )
    }
}

@Composable
private fun AdherenceCircle(d: AdherenceDay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .then(
                    when {
                        d.complete -> Modifier.background(YBrush.GoldSoft)
                        d.isToday -> Modifier
                            .background(YColors.Gold2.copy(alpha = 0.12f))
                            .border(1.5.dp, YColors.Gold2, CircleShape)
                        else -> Modifier
                            .background(YColors.Card2)
                            .border(1.dp, YColors.Line, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (d.complete) {
                Icon(imageVector = YIcons.CheckMark, contentDescription = null, tint = YColors.OnGold, modifier = Modifier.size(18.dp))
            }
        }
        Text(text = d.label, style = YText.Label, color = if (d.isToday) YColors.Gold2 else YColors.TextMuted)
    }
}

@Composable
private fun MedicineRow(item: MedItem, onTake: () -> Unit, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
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
        IconTile(color = YColors.CatMedicine, icon = YIcons.Medicine, size = 44.dp, cornerRadius = 14.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.reminder.title,
                style = YText.Name,
                color = if (item.state == MedState.TAKEN) YColors.TextMuted else YColors.TextPrimary,
                maxLines = 1
            )
            if (item.dose.isNotBlank()) {
                Text(text = item.dose, style = YText.Meta, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    label = item.time,
                    color = if (item.state == MedState.MISSED) YColors.Danger else YColors.CatMedicine,
                    icon = YIcons.Clock
                )
                // چیپ مالکیت در گروه دونفره
                HouseholdOwnerChip(reminder = item.reminder)
            }
        }
        when (item.state) {
            MedState.TAKEN -> GhostButton(text = "مصرف شد ✓", onClick = onUndo)
            MedState.MISSED -> GoldButton(text = "مصرف شد؟", onClick = onTake, small = true)
            MedState.UPCOMING -> GhostButton(text = "مصرف کردم", onClick = onTake)
        }
    }
}