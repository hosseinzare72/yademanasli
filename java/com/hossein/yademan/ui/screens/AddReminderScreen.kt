// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/AddReminderScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.IconTile
import com.hossein.yademan.ui.components.JalaliDatePickerDialog
import com.hossein.yademan.ui.components.PaywallDialog
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.SegmentedRow
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.services.SubscriptionManager
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.JalaliConverter
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch

private data class TypeOption(val type: String, val label: String, val icon: ImageVector, val color: Color)

private val typeOptions = listOf(
    TypeOption(ReminderType.INSTALLMENT, "قسط", YIcons.Installment, YColors.CatInstallment),
    TypeOption(ReminderType.CHECK, "چک", YIcons.Check, YColors.CatCheck),
    TypeOption(ReminderType.SUBSCRIPTION, "اشتراک", YIcons.Subscription, YColors.CatSubscription),
    TypeOption(ReminderType.MEETING, "قرار", YIcons.Meeting, YColors.CatMeeting),
    TypeOption(ReminderType.MEDICINE, "دارو", YIcons.Medicine, YColors.CatMedicine),
    TypeOption(ReminderType.SHOPPING, "خرید", YIcons.Cart, YColors.CatShopping),
    TypeOption(ReminderType.TRUST, "امانت", YIcons.Note, YColors.Info),
    TypeOption(ReminderType.OTHER, "سایر", YIcons.Other, YColors.Gold3)
)

private val repeatRules = listOf("none", "daily", "weekly", "monthly", "yearly", "hourly")
private val beforeMinutes = listOf(15, 60, 1440)

class AddReminderViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    var type by mutableStateOf(ReminderType.OTHER)
        private set
    var title by mutableStateOf("")
    var amount by mutableStateOf("")
    var detail by mutableStateOf("")
    var dayMillis by mutableLongStateOf(DateUtils.todayStart())
    var hour by mutableIntStateOf(9)
    var minute by mutableIntStateOf(0)
    var repeatIndex by mutableIntStateOf(0)
    var countText by mutableStateOf("")
    var beforeIndex by mutableIntStateOf(0)
    var priorityIndex by mutableIntStateOf(1)
    var trustKindIndex by mutableIntStateOf(0)
    var customIntervalText by mutableStateOf("")
    var doseIntervalText by mutableStateOf("8")
    var isHourlyRepeat by mutableStateOf(false)
    var hubKey by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)
        private set
    private var initialized = false

    fun setup(initialType: String?, initialTitle: String?, initialDay: Long?, initialHub: String?) {
        if (initialized) return
        initialized = true
        hubKey = initialHub?.takeIf { it.isNotBlank() }
        val now = System.currentTimeMillis()
        val next = now + DateUtils.HOUR
        hour = DateUtils.hourOf(next)
        minute = (DateUtils.minuteOf(next) / 5) * 5
        dayMillis = if (initialDay != null && initialDay > 0) DateUtils.startOfDay(initialDay) else DateUtils.startOfDay(next)
        selectType(initialType ?: ReminderType.OTHER)
        if (!initialTitle.isNullOrBlank()) title = initialTitle
    }

    fun selectType(t: String) {
        type = t
        when (t) {
            ReminderType.INSTALLMENT -> { repeatIndex = 3; beforeIndex = 2; priorityIndex = 2; isHourlyRepeat = false }
            ReminderType.CHECK -> { repeatIndex = 0; beforeIndex = 2; priorityIndex = 2; isHourlyRepeat = false }
            ReminderType.SUBSCRIPTION -> { repeatIndex = 3; beforeIndex = 2; priorityIndex = 1; isHourlyRepeat = false }
            ReminderType.MEDICINE -> { repeatIndex = 1; beforeIndex = 0; priorityIndex = 1; isHourlyRepeat = false }
            ReminderType.MEETING -> { repeatIndex = 0; beforeIndex = 1; priorityIndex = 1; isHourlyRepeat = false }
            ReminderType.TRUST -> { repeatIndex = 0; beforeIndex = 2; priorityIndex = 2; isHourlyRepeat = false }
            else -> Unit
        }
    }

    fun shiftDay(d: Int) {
        val candidate = DateUtils.addDays(dayMillis, d)
        if (DateUtils.epochDay(candidate) >= DateUtils.epochDay(System.currentTimeMillis())) dayMillis = candidate
    }

    fun pickDayMillis(millis: Long) {
        val candidate = DateUtils.startOfDay(millis)
        if (DateUtils.epochDay(candidate) >= DateUtils.epochDay(System.currentTimeMillis())) dayMillis = candidate
    }

    fun setTime(h: Int, m: Int) {
        hour = h
        minute = m
    }

    val dueMillis: Long get() = DateUtils.atTime(dayMillis, hour, minute)

    fun lastOccurrencePreview(count: Int): Long? {
        val rule = repeatRules.getOrNull(repeatIndex) ?: return null
        if (rule == "none") return null
        val interval = when {
            isHourlyRepeat && type == ReminderType.MEDICINE -> PersianDigits.parseLong(doseIntervalText)?.toInt()?.coerceIn(1, 23) ?: 8
            (repeatIndex == 3 || repeatIndex == 4) && customIntervalText.isNotBlank() -> PersianDigits.parseLong(customIntervalText)?.toInt()?.coerceIn(1, 12) ?: 1
            else -> 1
        }
        val probe = Reminder(
            id = "preview", title = "", dueAt = DateUtils.millisToIso(dueMillis),
            repeatRule = if (isHourlyRepeat && type == ReminderType.MEDICINE) "hourly" else rule,
            repeatInterval = interval,
            createdAt = "", updatedAt = "", deviceId = ""
        )
        return Recurrence.nthOccurrence(probe, count)
    }

    fun save(onDone: () -> Unit) {
        if (saving) return
        val t = title.trim()
        if (t.isEmpty()) {
            error = "عنوان را وارد کن"
            return
        }
        error = null
        val due = if (type == ReminderType.SHOPPING) System.currentTimeMillis() else dueMillis

        val extras: Map<String, Any?> = when (type) {
            ReminderType.INSTALLMENT, ReminderType.CHECK -> mapOf("bank" to detail.trim())
            ReminderType.SUBSCRIPTION -> mapOf("plan" to detail.trim())
            ReminderType.MEETING, ReminderType.OTHER -> mapOf("location" to detail.trim())
            ReminderType.MEDICINE -> mapOf("dose" to detail.trim())
            ReminderType.SHOPPING -> mapOf("group" to detail.trim().ifBlank { ShoppingViewModel.DEFAULT_GROUP })
            ReminderType.TRUST -> mapOf(
                "holder" to detail.trim(),
                "kind" to ReminderType.TRUST_KINDS[trustKindIndex]
            )
            else -> emptyMap()
        }

        val rule = when {
            isHourlyRepeat && type == ReminderType.MEDICINE -> "hourly"
            type == ReminderType.SHOPPING || type == ReminderType.TRUST -> "none"
            else -> repeatRules[repeatIndex]
        }

        val interval = when {
            isHourlyRepeat && type == ReminderType.MEDICINE -> PersianDigits.parseLong(doseIntervalText)?.toInt()?.coerceIn(1, 23) ?: 8
            (repeatIndex == 3 || repeatIndex == 4) && customIntervalText.isNotBlank() -> PersianDigits.parseLong(customIntervalText)?.toInt()?.coerceIn(1, 12) ?: 1
            else -> 1
        }

        val count = if (rule == "none") null else PersianDigits.parseLong(countText)?.toInt()?.takeIf { it in 1..999 }
        val allExtras = if (count != null) extras + ("total_count" to count) else extras
        val finalExtras = if (hubKey != null) allExtras + ("hub" to hubKey) else allExtras

        val base = repo.newReminder(type, t, due)
        val draft = base.copy(
            repeatRule = rule,
            repeatInterval = interval,
            alarmEnabled = type != ReminderType.SHOPPING,
            alarmOffsetMinutes = beforeMinutes[beforeIndex],
            priority = priorityIndex,
            amountToman = PersianDigits.parseLong(amount) ?: 0L,
            metaJson = ReminderMeta.build(type, finalExtras)
        )
        val rec = if (count != null) {
            val last = Recurrence.nthOccurrence(draft, count)
            if (last != null) draft.copy(endAt = DateUtils.millisToIso(DateUtils.addDays(DateUtils.startOfDay(last), 1) - 1000)) else draft
        } else draft
        saving = true
        viewModelScope.launch {
            try {
                repo.create(rec)
                onDone()
            } finally {
                saving = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    initialType: String?,
    initialTitle: String?,
    initialDay: Long?,
    initialHub: String? = null,
    onClose: () -> Unit,
    vm: AddReminderViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.setup(initialType, initialTitle, initialDay, initialHub) }
    val isShopping = vm.type == ReminderType.SHOPPING
    val isTrust = vm.type == ReminderType.TRUST
val showAmount = vm.type == ReminderType.INSTALLMENT || vm.type == ReminderType.CHECK ||
        vm.type == ReminderType.SUBSCRIPTION || isShopping || isTrust ||
        vm.type == ReminderType.OTHER || vm.hubKey != null
    val detailPlaceholder = when (vm.type) {
        ReminderType.INSTALLMENT, ReminderType.CHECK -> "بانک (مثلاً بانک ملت)"
        ReminderType.SUBSCRIPTION -> "سرویس یا پلن (مثلاً فیلیمو خانوادگی)"
        ReminderType.MEDICINE -> "دوز (مثلاً ۵۰۰ میلی‌گرم بعد از غذا)"
        ReminderType.SHOPPING -> "گروه (مثلاً لبنیات)"
        ReminderType.TRUST -> "نزد چه کسی/کجا؟ (مثلاً شرکت بیمه ایران)"
        ReminderType.MEETING -> "مکان (اختیاری)"
        else -> "توضیح یا مکان (اختیاری)"
    }
    val j = JalaliConverter.fromMillis(vm.dayMillis)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ScreenHeader(
            title = "یادآوری جدید",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onClose) }
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            typeOptions.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { opt ->
                        TypeCard(
                            option = opt,
                            selected = vm.type == opt.type,
                            onClick = { vm.selectType(opt.type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Text(text = "عنوان", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
            YTextField(value = vm.title, onValueChange = { vm.title = it; vm.error = null }, placeholder = "مثلاً قسط وام مسکن")
            if (vm.error != null) Text(text = vm.error ?: "", style = YText.Meta, color = YColors.Danger)
            if (showAmount) {
                YTextField(
                    value = vm.amount,
                    onValueChange = { v -> vm.amount = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(13) },
                    placeholder = if (isShopping) "قیمت (تومان)" else "مبلغ (تومان)",
                    keyboardType = KeyboardType.Number,
                    trailing = {
                        val n = PersianDigits.parseLong(vm.amount)
                        if (n != null) Text(text = n.fa(), style = YText.MetaBold, color = YColors.Gold2)
                    }
                )
            }
            YTextField(value = vm.detail, onValueChange = { vm.detail = it }, placeholder = detailPlaceholder)
            if (isTrust) {
                Text(text = "نوع امانت", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(ReminderType.TRUST_KINDS, vm.trustKindIndex, { vm.trustKindIndex = it })
            }
            if (!isShopping) {
                DateCard(
                    value = "${JalaliConverter.weekdayNameFa(j.weekday)} ${j.formatShort()}",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JumpChip("روز −", Modifier.weight(1f)) { vm.shiftDay(-1) }
                    JumpChip("روز +", Modifier.weight(1f)) { vm.shiftDay(1) }
                    JumpChip("هفته +", Modifier.weight(1f)) { vm.shiftDay(7) }
                    JumpChip("ماه +", Modifier.weight(1f)) { vm.shiftDay(30) }
                    JumpChip("سال +", Modifier.weight(1f)) { vm.shiftDay(365) }
                }
                TimeCard(
                    value = "%02d:%02d".format(java.util.Locale.US, vm.hour, vm.minute).fa(),
                    onClick = { showTimePicker = true }
                )
                if (!isTrust) {
                    Text(text = "تکرار", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                    SegmentedRow(listOf("یک‌بار", "روزانه", "هفتگی", "ماهانه", "سالانه"), vm.repeatIndex, { vm.repeatIndex = it })

                    if ((vm.repeatIndex == 3 || vm.repeatIndex == 4) &&
                        (vm.type == ReminderType.INSTALLMENT || vm.type == ReminderType.CHECK)) {
                        Text(text = "هر چند وقت یکبار؟", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                        YTextField(
                            value = vm.customIntervalText,
                            onValueChange = { v -> vm.customIntervalText = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(2) },
                            placeholder = "مثلاً 2 (هر 2 ماه یکبار)",
                            keyboardType = KeyboardType.Number
                        )
                    }

                    if (vm.type == ReminderType.MEDICINE && vm.repeatIndex != 0) {
                        SegmentedRow(listOf("روزانه", "ساعتی"), if (vm.isHourlyRepeat) 1 else 0, { vm.isHourlyRepeat = it == 1 })
                        if (vm.isHourlyRepeat) {
                            Text(text = "هر چند ساعت یکبار؟", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                            YTextField(
                                value = vm.doseIntervalText,
                                onValueChange = { v -> vm.doseIntervalText = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(2) },
                                placeholder = "مثلاً 8 (هر 8 ساعت)",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }
                }
                if (vm.repeatIndex != 0) {
                    val unit = when (vm.type) {
                        ReminderType.INSTALLMENT -> "قسط"
                        ReminderType.CHECK -> "چک"
                        ReminderType.SUBSCRIPTION -> "دوره"
                        else -> "بار"
                    }
                    Text(text = "تعداد دفعات", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                    YTextField(
                        value = vm.countText,
                        onValueChange = { v -> vm.countText = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(3) },
                        placeholder = "خالی = نامحدود (مثلاً 10 $unit)",
                        keyboardType = KeyboardType.Number,
                        trailing = {
                            val n = PersianDigits.parseLong(vm.countText)
                            if (n != null && n > 0) Text(text = "${n.fa()} $unit", style = YText.MetaBold, color = YColors.Gold2)
                        }
                    )
                    val n = PersianDigits.parseLong(vm.countText)?.toInt()
                    if (n != null && n > 0) {
                        val last = vm.lastOccurrencePreview(n)
                        if (last != null) {
                            Text(
                                text = "آخرین $unit: ${DateUtils.dateShortFa(last)} • بعد از پرداخت آن، تکرار و هشدار خودکار تمام می‌شود",
                                style = YText.Meta,
                                color = YColors.TextSecondary
                            )
                        }
                    }
                }
                Text(text = "یادآوری قبل", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(
                    listOf("${15.fa()} دقیقه", "${1.fa()} ساعت", "${1.fa()} روز"),
                    vm.beforeIndex,
                    { vm.beforeIndex = it }
                )
                Text(text = "اولویت", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(listOf("کم", "متوسط", "زیاد"), vm.priorityIndex, { vm.priorityIndex = it })
            }
            GoldButton(
                text = "ذخیره یادآوری",
                onClick = {
                    scope.launch {
                        if (!SubscriptionManager.canCreateMore()) showPaywall = true
                        else vm.save(onClose)
                    }
                },
                enabled = !vm.saving,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }
    }
    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialMillis = vm.dayMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                vm.pickDayMillis(millis)
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = vm.hour,
            initialMinute = vm.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = YColors.Card,
            title = { Text("انتخاب ساعت", style = YText.CardTitle, color = YColors.TextPrimary) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timeState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = YColors.Card2,
                            selectorColor = YColors.Gold2,
                            containerColor = YColors.Card,
                            periodSelectorBorderColor = YColors.Line,
                            clockDialSelectedContentColor = YColors.OnGold,
                            clockDialUnselectedContentColor = YColors.TextPrimary,
                            timeSelectorSelectedContainerColor = YColors.Gold2,
                            timeSelectorUnselectedContainerColor = YColors.Card2
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("تأیید", color = YColors.Gold2) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("انصراف", color = YColors.TextSecondary)
                }
            }
        )
    }
    if (showPaywall) {
        PaywallDialog(onDismiss = { showPaywall = false })
    }
}

@Composable
private fun TypeCard(option: TypeOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) YColors.Gold2.copy(alpha = 0.08f) else YColors.Card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) YColors.Gold2 else YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconTile(color = option.color, icon = option.icon, size = 42.dp, cornerRadius = 14.dp, iconSize = 21.dp)
        Text(
            text = option.label,
            style = YText.Name,
            color = if (selected) YColors.Gold1 else YColors.TextSecondary
        )
    }
}

@Composable
private fun DateCard(value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(text = "تاریخ", style = YText.Meta)
        Text(text = value, style = YText.CardTitle.copy(color = YColors.Gold1))
        Text(text = "برای انتخاب از تقویم ضربه بزن", style = YText.Meta, color = YColors.TextMuted)
    }
}

@Composable
private fun TimeCard(value: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(text = "ساعت", style = YText.Meta)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = YText.CardTitle.copy(color = YColors.Gold1), modifier = Modifier.weight(1f))
            Icon(YIcons.Clock, null, tint = YColors.Gold2, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun JumpChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(YColors.Card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = YText.Chip, color = YColors.Gold2, maxLines = 1)
    }
}