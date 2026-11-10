// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/EditReminderScreen.kt
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
import androidx.compose.foundation.layout.height
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
import com.hossein.yademan.ui.components.GhostButton
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.IconTile
import com.hossein.yademan.ui.components.JalaliDatePickerDialog
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.SegmentedRow
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.JalaliConverter
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch

private val typeOptions = listOf(
    Triple(ReminderType.INSTALLMENT, "قسط", YColors.CatInstallment),
    Triple(ReminderType.CHECK, "چک", YColors.CatCheck),
    Triple(ReminderType.SUBSCRIPTION, "اشتراک", YColors.CatSubscription),
    Triple(ReminderType.MEETING, "قرار", YColors.CatMeeting),
    Triple(ReminderType.MEDICINE, "دارو", YColors.CatMedicine),
    Triple(ReminderType.SHOPPING, "خرید", YColors.CatShopping),
    Triple(ReminderType.TRUST, "امانت", YColors.Info),
    Triple(ReminderType.OTHER, "سایر", YColors.Gold3)
)

private val repeatRules = listOf("none", "daily", "weekly", "monthly", "yearly", "hourly")
private val beforeMinutes = listOf(15, 60, 1440)

class EditReminderViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    var type by mutableStateOf(ReminderType.OTHER); private set
    var title by mutableStateOf("")
    var amount by mutableStateOf("")
    var detail by mutableStateOf("")
    var dayMillis by mutableLongStateOf(DateUtils.todayStart())
    var hour by mutableIntStateOf(9)
    var minute by mutableIntStateOf(0)
    var repeatIndex by mutableIntStateOf(0)
    var repeatTouched by mutableStateOf(false)
    var countText by mutableStateOf("")
    var beforeIndex by mutableIntStateOf(0)
    var priorityIndex by mutableIntStateOf(1)
    var trustKindIndex by mutableIntStateOf(0)
    var error by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false); private set
    private var preservedRule: String? = null
    private var loadedReminder: Reminder? = null
    private var initialized = false

    fun load(reminderId: String?) {
        if (initialized || reminderId.isNullOrBlank()) return
        initialized = true
        val r = kotlinx.coroutines.runBlocking { repo.getById(reminderId) } ?: return
        loadedReminder = r
        type = ReminderMeta.type(r)
        title = r.title
        amount = if (r.amountToman > 0) r.amountToman.toString() else ""
        dayMillis = DateUtils.startOfDay(DateUtils.isoToMillis(r.dueAt) ?: DateUtils.todayStart())
        val at = DateUtils.isoToMillis(r.dueAt) ?: System.currentTimeMillis()
        hour = DateUtils.hourOf(at)
        minute = DateUtils.minuteOf(at)
        val ruleIdx = repeatRules.indexOf(r.repeatRule)
        if (ruleIdx >= 0) {
            repeatIndex = ruleIdx
        } else {
            preservedRule = r.repeatRule
            repeatIndex = 0
        }
        beforeIndex = beforeMinutes.indexOf(r.alarmOffsetMinutes).takeIf { it >= 0 } ?: 0
        priorityIndex = r.priority.coerceIn(0, 2)

        val allExtras = ReminderMeta.allExtras(r)
        detail = when (type) {
            ReminderType.TRUST -> allExtras["holder"] as? String ?: ""
            ReminderType.INSTALLMENT, ReminderType.CHECK -> allExtras["bank"] as? String ?: ""
            ReminderType.SUBSCRIPTION -> allExtras["plan"] as? String ?: ""
            ReminderType.MEDICINE -> allExtras["dose"] as? String ?: ""
            ReminderType.SHOPPING -> allExtras["group"] as? String ?: ""
            else -> allExtras["location"] as? String ?: ""
        }
        val kind = allExtras["kind"] as? String
        trustKindIndex = ReminderType.TRUST_KINDS.indexOf(kind ?: "").takeIf { it >= 0 } ?: 0
        val totalRaw = allExtras["total_count"]
        countText = when (totalRaw) {
            is String -> totalRaw.toIntOrNull()?.toString() ?: ""
            is Number -> totalRaw.toInt().toString()
            else -> ""
        }
    }

    fun selectType(t: String) { type = t }

    fun shiftDay(d: Int) {
        val c = DateUtils.addDays(dayMillis, d)
        if (DateUtils.epochDay(c) >= DateUtils.epochDay(System.currentTimeMillis())) dayMillis = c
    }

    fun pickDayMillis(m: Long) {
        val c = DateUtils.startOfDay(m)
        if (DateUtils.epochDay(c) >= DateUtils.epochDay(System.currentTimeMillis())) dayMillis = c
    }

    fun setTime(h: Int, m: Int) { hour = h; minute = m }

    private val dueMillis: Long get() = DateUtils.atTime(dayMillis, hour, minute)

    fun save(onDone: () -> Unit) {
        if (saving) return
        val t = title.trim()
        if (t.isEmpty()) { error = "عنوان را وارد کن"; return }
        error = null
        val base = loadedReminder ?: return
        val due = if (type == ReminderType.SHOPPING) System.currentTimeMillis() else dueMillis

        // ✅ شروع از extras قبلی تا hub و بقیه کلیدها حفظ شوند
        val extras: MutableMap<String, Any?> = ReminderMeta.allExtras(base).toMutableMap()
        when (type) {
            ReminderType.INSTALLMENT, ReminderType.CHECK -> extras["bank"] = detail.trim()
            ReminderType.SUBSCRIPTION -> extras["plan"] = detail.trim()
            ReminderType.MEETING, ReminderType.OTHER -> extras["location"] = detail.trim()
            ReminderType.MEDICINE -> extras["dose"] = detail.trim()
            ReminderType.SHOPPING -> extras["group"] = detail.trim().ifBlank { "عمومی" }
            ReminderType.TRUST -> {
                extras["holder"] = detail.trim()
                extras["kind"] = ReminderType.TRUST_KINDS[trustKindIndex]
            }
            else -> Unit
        }

        val rule = when {
            !repeatTouched && preservedRule != null -> preservedRule!!
            type == ReminderType.SHOPPING || type == ReminderType.TRUST -> "none"
            else -> repeatRules[repeatIndex]
        }
        val count = if (rule == "none") null else PersianDigits.parseLong(countText)?.toInt()?.takeIf { it in 1..999 }
        if (count != null) extras["total_count"] = count else extras.remove("total_count")

        saving = true
        viewModelScope.launch {
            val built = ReminderMeta.parse(ReminderMeta.build(type, extras))
            val oldMeta = ReminderMeta.parse(base.metaJson)
            // ✅ حفظ سابقه پرداخت‌ها
            if (oldMeta.has("done_dates")) built.add("done_dates", oldMeta.get("done_dates"))

            val draft = base.copy(
                title = t,
                dueAt = DateUtils.millisToIso(due),
                repeatRule = rule,
                alarmEnabled = type != ReminderType.SHOPPING,
                alarmOffsetMinutes = beforeMinutes[beforeIndex],
                priority = priorityIndex,
                amountToman = PersianDigits.parseLong(amount) ?: 0L,
                metaJson = built.toString(),
                updatedAt = DateUtils.nowIso()
            )
            val rec = if (count != null) {
                val last = Recurrence.nthOccurrence(draft, count)
                if (last != null) draft.copy(endAt = DateUtils.millisToIso(DateUtils.addDays(DateUtils.startOfDay(last), 1) - 1000)) else draft
            } else draft.copy(endAt = null)
            repo.update(rec)
            saving = false
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val base = loadedReminder ?: return
        saving = true
        viewModelScope.launch {
            repo.delete(base.id)
            saving = false
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    reminderId: String?,
    onClose: () -> Unit,
    vm: EditReminderViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.load(reminderId) }
    val isShopping = vm.type == ReminderType.SHOPPING
    val isTrust = vm.type == ReminderType.TRUST
    val showAmount = vm.type == ReminderType.INSTALLMENT || vm.type == ReminderType.CHECK ||
            vm.type == ReminderType.SUBSCRIPTION || isShopping || isTrust ||
            vm.type == ReminderType.OTHER
    val detailPlaceholder = when (vm.type) {
        ReminderType.INSTALLMENT, ReminderType.CHECK -> "بانک (مثلاً بانک ملت)"
        ReminderType.SUBSCRIPTION -> "سرویس یا پلن"
        ReminderType.MEDICINE -> "دوز"
        ReminderType.SHOPPING -> "گروه"
        ReminderType.MEETING -> "مکان (اختیاری)"
        ReminderType.TRUST -> "نزد چه کسی/کجا؟"
        else -> "توضیح یا مکان (اختیاری)"
    }
    val j = JalaliConverter.fromMillis(vm.dayMillis)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ScreenHeader(
            title = "ویرایش یادآوری",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onClose) }
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            typeOptions.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (t, label, color) ->
                        EditTypeCard(
                            type = t, label = label, color = color,
                            selected = vm.type == t,
                            onClick = { vm.selectType(t) },
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
                    onValueChange = { v -> vm.amount = v.filter { it.isDigit() || it in "۰۱۲۴۵۶۷۸۹" }.take(13) },
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
                EditDateCard(
                    value = "${JalaliConverter.weekdayNameFa(j.weekday)} ${j.formatShort()}",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditJumpChip("روز −", Modifier.weight(1f)) { vm.shiftDay(-1) }
                    EditJumpChip("روز +", Modifier.weight(1f)) { vm.shiftDay(1) }
                    EditJumpChip("هفته +", Modifier.weight(1f)) { vm.shiftDay(7) }
                    EditJumpChip("ماه +", Modifier.weight(1f)) { vm.shiftDay(30) }
                    EditJumpChip("سال +", Modifier.weight(1f)) { vm.shiftDay(365) }
                }
                EditTimeCard(
                    value = "%02d:%02d".format(java.util.Locale.US, vm.hour, vm.minute).fa(),
                    onClick = { showTimePicker = true }
                )
                Text(text = "تکرار", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(
                    listOf("یک‌بار", "روزانه", "هفتگی", "ماهانه", "سالانه", "ساعتی"),
                    vm.repeatIndex,
                    { vm.repeatIndex = it; vm.repeatTouched = true }
                )
                if (vm.repeatIndex != 0) {
                    Text(text = "تعداد دفعات", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                    YTextField(
                        value = vm.countText,
                        onValueChange = { v -> vm.countText = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(3) },
                        placeholder = "خالی = نامحدود",
                        keyboardType = KeyboardType.Number
                    )
                }
                Text(text = "یادآوری قبل", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(listOf("${15.fa()} دقیقه", "${1.fa()} ساعت", "${1.fa()} روز"), vm.beforeIndex, { vm.beforeIndex = it })
                Text(text = "اولویت", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(listOf("کم", "متوسط", "زیاد"), vm.priorityIndex, { vm.priorityIndex = it })
            }
            GoldButton(
                text = "ذخیره تغییرات",
                onClick = { vm.save(onClose) },
                enabled = !vm.saving,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
            )
            GhostButton(
                text = "حذف یادآوری",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(44.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialMillis = vm.dayMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { m -> vm.pickDayMillis(m); showDatePicker = false }
        )
    }
    if (showTimePicker) {
        val ts = rememberTimePickerState(initialHour = vm.hour, initialMinute = vm.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = YColors.Card,
            title = { Text("انتخاب ساعت", style = YText.CardTitle, color = YColors.TextPrimary) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = ts,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = YColors.Card2,
                            selectorColor = YColors.Gold2,
                            containerColor = YColors.Card,
                            clockDialSelectedContentColor = YColors.OnGold,
                            clockDialUnselectedContentColor = YColors.TextPrimary,
                            timeSelectorSelectedContainerColor = YColors.Gold2,
                            timeSelectorUnselectedContainerColor = YColors.Card2
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.setTime(ts.hour, ts.minute); showTimePicker = false }) {
                    Text("تأیید", color = YColors.Gold2)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("انصراف", color = YColors.TextSecondary) }
            }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = YColors.Card,
            title = { Text("حذف یادآوری؟", style = YText.CardTitle) },
            text = { Text("این یادآوری برای همیشه حذف می‌شود.", style = YText.Meta) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onClose) }) {
                    Text("حذف", color = YColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("انصراف", color = YColors.TextSecondary) }
            }
        )
    }
}

@Composable
private fun EditTypeCard(
    type: String, label: String, color: Color, selected: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(YRadius.Md)
    val icon = when (type) {
        ReminderType.INSTALLMENT -> YIcons.Installment
        ReminderType.CHECK -> YIcons.Check
        ReminderType.SUBSCRIPTION -> YIcons.Subscription
        ReminderType.MEETING -> YIcons.Meeting
        ReminderType.MEDICINE -> YIcons.Medicine
        ReminderType.SHOPPING -> YIcons.Cart
        else -> YIcons.Other
    }
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
        IconTile(color = color, icon = icon, size = 42.dp, cornerRadius = 14.dp, iconSize = 21.dp)
        Text(text = label, style = YText.Name, color = if (selected) YColors.Gold1 else YColors.TextSecondary)
    }
}

@Composable
private fun EditDateCard(value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier.fillMaxWidth().clip(shape).background(YColors.Card)
            .border(1.dp, YColors.Line, shape).clickable(onClick = onClick).padding(12.dp)
    ) {
        Text(text = "تاریخ", style = YText.Meta)
        Text(text = value, style = YText.CardTitle.copy(color = YColors.Gold1))
    }
}

@Composable
private fun EditTimeCard(value: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(YColors.Card)
            .border(1.dp, YColors.Line, shape).clickable(onClick = onClick).padding(12.dp)
    ) {
        Text(text = "ساعت", style = YText.Meta)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = YText.CardTitle.copy(color = YColors.Gold1), modifier = Modifier.weight(1f))
            Icon(YIcons.Clock, null, tint = YColors.Gold2, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EditJumpChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(YColors.Card2)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = YText.Chip, color = YColors.Gold2, maxLines = 1)
    }
}