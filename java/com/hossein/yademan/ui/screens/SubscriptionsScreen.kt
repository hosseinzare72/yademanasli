// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/SubscriptionsScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.*
import com.hossein.yademan.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubscriptionsState(
    val activeCount: Int = 0,
    val monthlyCost: Long = 0,
    val yearlyCost: Long = 0,
    val nearCount: Int = 0,
    val items: List<BillInfo> = emptyList()
)

/** بخش «اشتراک‌ها»: فیلیمو، نتفلیکس، اینترنت، VPN، اسپاتیفای، باشگاه و ... با تمدید دوره‌ای */
class SubscriptionsViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders

    val state: StateFlow<SubscriptionsState> = combine(repo.observeAll(), TimeTicker.now) { list, now ->
        val items = list.filter { BillsLogic.isSubscription(it) }.map { BillsLogic.info(it, now) }
            .sortedWith(compareBy<BillInfo> { it.finished }.thenBy { it.paid }.thenBy { it.occurrence })
        val active = items.filter { !it.finished }
        val monthly = active.sumOf { monthlyEquivalent(it.reminder) }
        SubscriptionsState(
            activeCount = active.size,
            monthlyCost = monthly,
            yearlyCost = monthly * 12,
            nearCount = active.count { !it.paid && it.daysLeft in 0..7 },
            items = items
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsState())

    /** هزینه معادل ماهانه بر اساس دوره تمدید */
    private fun monthlyEquivalent(r: Reminder): Long {
        val n = r.repeatInterval.coerceAtLeast(1)
        return when (r.repeatRule) {
            Recurrence.MONTHLY -> r.amountToman / n
            Recurrence.YEARLY -> r.amountToman / (12L * n)
            Recurrence.WEEKLY -> r.amountToman * 52 / (12L * n)
            Recurrence.DAILY -> r.amountToman * 30 / n
            else -> 0L
        }
    }

    fun renew(b: BillInfo) {
        viewModelScope.launch { repo.setDoneOn(b.reminder, DateUtils.dayKey(b.occurrence), true) }
    }

    fun undo(b: BillInfo) {
        viewModelScope.launch { repo.setDoneOn(b.reminder, DateUtils.dayKey(b.occurrence), false) }
    }

    fun delete(b: BillInfo) {
        viewModelScope.launch { repo.delete(b.reminder.id) }
    }
}

@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    vm: SubscriptionsViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<BillInfo?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "اشتراک‌ها",
                subtitle = "${state.activeCount.fa()} اشتراک فعال",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) }
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
                    Text(text = "هزینه ماهانه اشتراک‌ها", style = YText.Sub)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                        Text(text = state.monthlyCost.fa(), style = YText.Title.copy(fontSize = 26.sp, color = YColors.Gold1))
                        Text(text = " تومان", style = YText.Sub, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "سالانه حدود ${PersianDigits.toman(state.yearlyCost)}", style = YText.MetaBold)
                        Chip(
                            label = "${state.nearCount.fa()} تمدید نزدیک",
                            color = if (state.nearCount > 0) YColors.Warning else YColors.Success
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        if (state.items.isEmpty()) {
            item {
                EmptyState(
                    message = "هنوز اشتراکی ثبت نشده. فیلیمو، اینترنت، VPN، باشگاه… رو اضافه کن تا قبل از تمدید خبرت کنم",
                    icon = YIcons.Subscription
                )
            }
        } else {
            items(state.items, key = { it.reminder.id }) { b ->
                SubscriptionRow(
                    b = b,
                    onRenew = { vm.renew(b) },
                    onUndo = { vm.undo(b) },
                    onLongPress = { toDelete = b },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
        item {
            GhostButton(
                text = "+ اشتراک جدید",
                onClick = onAdd,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            )
        }
    }

    toDelete?.let { b ->
        ConfirmDialog(
            title = "حذف «${b.reminder.title}»؟",
            message = "این اشتراک و یادآورهای تمدیدش حذف می‌شود.",
            onConfirm = {
                vm.delete(b)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubscriptionRow(
    b: BillInfo,
    onRenew: () -> Unit,
    onUndo: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val r = b.reminder
    val chipColor = when {
        b.paid -> YColors.Success
        b.daysLeft <= 0 -> YColors.Danger
        b.daysLeft <= 3 -> YColors.Warning
        else -> YColors.Info
    }
    val plan = ReminderMeta.str(r, "plan")?.takeIf { it.isNotBlank() } ?: ReminderType.labelFa(b.type)
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(color = YColors.CatSubscription, icon = YIcons.Subscription, size = 44.dp, cornerRadius = 14.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = r.title, style = YText.Name, maxLines = 1)
                Text(
                    text = "$plan • ${Recurrence.labelFa(r)} • ${DateUtils.dateShortFa(b.occurrence)}",
                    style = YText.Meta,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = r.amountToman.fa(), style = YText.Amount)
                Text(text = "تومان", style = YText.Meta)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
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
                GoldButton(text = "تمدید شد", onClick = onRenew, small = true)
            }
        }
    }
}
