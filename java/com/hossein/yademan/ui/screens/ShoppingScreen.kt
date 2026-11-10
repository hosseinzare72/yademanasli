// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/ShoppingScreen.kt
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
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.*
import com.hossein.yademan.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ShopItem(val reminder: Reminder, val group: String, val qty: String, val done: Boolean)

data class ShoppingState(
    val all: List<ShopItem> = emptyList(),
    val visible: List<ShopItem> = emptyList(),
    val groups: List<Pair<String, Int>> = emptyList(),
    val selectedGroup: String? = null,
    val doneCount: Int = 0,
    val openSum: Long = 0,
    val openCount: Int = 0
)

class ShoppingViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    private val selectedGroup = MutableStateFlow<String?>(null)

    val state: StateFlow<ShoppingState> = combine(repo.observeAll(), selectedGroup) { list, group ->
        val items = list.filter { ReminderMeta.type(it) == ReminderType.SHOPPING }
            .map {
                ShopItem(
                    reminder = it,
                    group = ReminderMeta.str(it, "group")?.takeIf { g -> g.isNotBlank() } ?: DEFAULT_GROUP,
                    qty = ReminderMeta.str(it, "qty") ?: "",
                    done = it.isDone
                )
            }
            .sortedWith(compareBy<ShopItem> { it.done }.thenBy { it.reminder.sortOrder }.thenBy { it.reminder.createdAt })
        val groups = items.groupBy { it.group }.map { (g, l) -> g to l.size }
        val validGroup = group?.takeIf { g -> groups.any { it.first == g } }
        val open = items.filter { !it.done }
        ShoppingState(
            all = items,
            visible = if (validGroup == null) items else items.filter { it.group == validGroup },
            groups = groups,
            selectedGroup = validGroup,
            doneCount = items.count { it.done },
            openSum = open.sumOf { it.reminder.amountToman },
            openCount = open.size
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingState())

    fun selectGroup(g: String?) {
        selectedGroup.value = g
    }

    /** افزودن قلم؛ اگر آخرین کلمه عدد باشد، قیمت در نظر گرفته می‌شود: «شیر ۴۵۰۰۰» */
    fun add(text: String) {
        val raw = text.trim()
        if (raw.isEmpty()) return
        val parts = raw.split(" ").filter { it.isNotBlank() }
        val price = if (parts.size > 1) PersianDigits.parseLong(parts.last())?.takeIf {
            PersianDigits.toLatin(parts.last()).all { c -> c.isDigit() || c == ',' || c == '٬' }
        } else null
        val title = if (price != null) parts.dropLast(1).joinToString(" ") else raw
        val group = selectedGroup.value ?: DEFAULT_GROUP
        viewModelScope.launch {
            val base = repo.newReminder(ReminderType.SHOPPING, title, System.currentTimeMillis())
            repo.create(
                base.copy(
                    alarmEnabled = false,
                    amountToman = price ?: 0L,
                    metaJson = ReminderMeta.build(ReminderType.SHOPPING, mapOf("group" to group))
                )
            )
        }
    }

    fun toggle(item: ShopItem) {
        viewModelScope.launch {
            val fresh = repo.getById(item.reminder.id) ?: return@launch
            repo.update(fresh.copy(isDone = !fresh.isDone, doneAt = if (!fresh.isDone) DateUtils.nowIso() else null))
        }
    }

    fun clearDone() {
        viewModelScope.launch { repo.deleteMany(state.value.all.filter { it.done }.map { it.reminder.id }) }
    }

    fun finishShopping() {
        viewModelScope.launch {
            state.value.all.filter { !it.done }.forEach { item ->
                val fresh = repo.getById(item.reminder.id) ?: return@forEach
                repo.update(fresh.copy(isDone = true, doneAt = DateUtils.nowIso()))
            }
        }
    }

    fun delete(item: ShopItem) {
        viewModelScope.launch { repo.delete(item.reminder.id) }
    }

    companion object {
        const val DEFAULT_GROUP = "متفرقه"
    }
}

@Composable
fun ShoppingScreen(vm: ShoppingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ShopItem?>(null) }
    val total = state.all.size
    val progress = if (total == 0) 0f else state.doneCount.toFloat() / total

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "لیست خرید",
                subtitle = "${state.doneCount.fa()} از ${total.fa()} قلم",
                actions = {
                    HeaderIconButton(
                        icon = YIcons.Trash,
                        contentDescription = "حذف خریدهای انجام‌شده",
                        onClick = { if (state.doneCount > 0) confirmClear = true }
                    )
                }
            )
        }
        item {
            LinearProgress(progress = progress, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "افزودن قلم (مثلاً: شیر ۴۵۰۰۰)",
                    modifier = Modifier.weight(1f),
                    onImeAction = {
                        vm.add(input)
                        input = ""
                    }
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(YRadius.Sm))
                        .background(YBrush.Gold)
                        .clickable {
                            vm.add(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = YIcons.PlusBold, contentDescription = "افزودن", tint = YColors.OnGold, modifier = Modifier.size(22.dp))
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(label = "همه ${total.fa()}", selected = state.selectedGroup == null, onClick = { vm.selectGroup(null) })
                state.groups.forEach { (g, n) ->
                    FilterPill(label = "$g ${n.fa()}", selected = state.selectedGroup == g, onClick = { vm.selectGroup(g) })
                }
            }
        }
        if (state.visible.isEmpty()) {
            item { EmptyState(message = "لیست خرید خالیه", icon = YIcons.Cart) }
        } else {
            items(state.visible, key = { it.reminder.id }) { item ->
                ShopRow(
                    item = item,
                    onToggle = { vm.toggle(item) },
                    onLongPress = { toDelete = item },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
        item {
            AppCard(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                    .fillMaxWidth(),
                brush = YBrush.HeroCard
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "جمع سبد", style = YText.Sub)
                        Text(
                            text = PersianDigits.toman(state.openSum),
                            style = YText.CardTitle.copy(color = YColors.Gold1),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(text = "${state.openCount.fa()} قلم باقی‌مانده", style = YText.Meta)
                    }
                    GoldButton(
                        text = "اتمام خرید",
                        onClick = vm::finishShopping,
                        small = true,
                        enabled = state.openCount > 0
                    )
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "حذف خریدهای انجام‌شده",
            message = "${state.doneCount.fa()} قلم خریداری‌شده از لیست حذف می‌شود.",
            onConfirm = {
                vm.clearDone()
                confirmClear = false
            },
            onDismiss = { confirmClear = false }
        )
    }
    toDelete?.let { item ->
        ConfirmDialog(
            title = "حذف «${item.reminder.title}»؟",
            message = "این قلم از لیست خرید حذف می‌شود.",
            onConfirm = {
                vm.delete(item)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(YBrush.GoldSoft)
                else Modifier
                    .background(YColors.Card)
                    .border(1.dp, YColors.Line, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text = label, style = YText.Chip, color = if (selected) YColors.OnGold else YColors.TextSecondary, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShopRow(item: ShopItem, onToggle: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CheckCircle(checked = item.done, onToggle = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.reminder.title,
                style = YText.Name,
                color = if (item.done) YColors.TextMuted else YColors.TextPrimary,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
            val meta = if (item.qty.isBlank()) item.group else "${item.group} • ${item.qty.fa()}"
            Text(text = meta, style = YText.Meta, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        if (item.reminder.amountToman > 0) {
            Text(
                text = item.reminder.amountToman.fa(),
                style = YText.Amount,
                color = if (item.done) YColors.TextMuted else YColors.TextPrimary
            )
        }
    }
}
