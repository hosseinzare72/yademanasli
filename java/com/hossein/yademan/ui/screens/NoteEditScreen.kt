// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/NoteEditScreen.kt
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
import com.hossein.yademan.data.NoteItem

private val noteColors = listOf("#FFB84D", "#E8B84B", "#4CC3FF", "#FF7A9E", "#3ED598", "#9D8CFF")

/** فرم ساخت/ویرایش یادداشت با چک‌لیست */
class NoteEditViewModel(private val noteId: String?) : ViewModel() {
    private val repo = ServiceLocator.reminders

    var title by mutableStateOf("")
    var body by mutableStateOf("")
    var color by mutableStateOf(noteColors.first())
    var pinned by mutableStateOf(false)
    val items = mutableStateListOf<NoteItem>()
    var loaded by mutableStateOf(noteId == null)
        private set

    private var original: Reminder? = null
    /** جلوگیری از ذخیره/حذف دوباره (و popBackStack دوباره) با لمس سریع */
    private var busy = false

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val r = repo.getById(noteId)
                if (r != null) {
                    original = r
                    title = r.title
                    body = r.note
                    color = r.color
                    pinned = ReminderMeta.bool(r, "pinned")
                    items.clear()
                    items.addAll(ReminderMeta.noteItems(r))
                }
                loaded = true
            }
        }
    }

    val isNew: Boolean get() = noteId == null

    fun addItem(text: String) {
        if (text.isNotBlank()) items.add(NoteItem(text.trim(), false))
    }

    fun toggleItem(i: Int) {
        if (i in items.indices) items[i] = items[i].copy(done = !items[i].done)
    }

    fun removeItem(i: Int) {
        if (i in items.indices) items.removeAt(i)
    }

    fun save(onDone: () -> Unit) {
        if (busy) return
        busy = true
        val t = title.trim().ifBlank { "یادداشت بدون عنوان" }
        viewModelScope.launch {
            val base = original ?: repo.newReminder(ReminderType.NOTE, t, System.currentTimeMillis())
                .copy(alarmEnabled = false)
            var rec = base.copy(title = t, note = body.trim(), color = color)
            rec = ReminderMeta.with(rec, "type", ReminderType.NOTE)
            rec = ReminderMeta.with(rec, "pinned", pinned)
            rec = ReminderMeta.withNoteItems(rec, items.toList())
            if (original == null) repo.create(rec) else repo.update(rec)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        val id = original?.id ?: return onDone()
        busy = true
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }
}

@Composable
fun NoteEditScreen(noteId: String?, onClose: () -> Unit) {
    val vm: NoteEditViewModel = viewModel(key = "note_${noteId ?: "new"}") { NoteEditViewModel(noteId) }
    var newItem by rememberSaveable { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            ScreenHeader(
                title = if (vm.isNew) "یادداشت جدید" else "ویرایش یادداشت",
                actions = {
                    if (!vm.isNew) {
                        HeaderIconButton(icon = YIcons.Trash, contentDescription = "حذف", onClick = { confirmDelete = true })
                    }
                    HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onClose)
                }
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                YTextField(value = vm.title, onValueChange = { vm.title = it }, placeholder = "عنوان یادداشت")
                YTextField(
                    value = vm.body,
                    onValueChange = { vm.body = it },
                    placeholder = "متن یادداشت (اختیاری)",
                    singleLine = false,
                    minLines = 3
                )
                Text(text = "رنگ", style = YText.Section)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    noteColors.forEach { hex ->
                        val selected = vm.color.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(hexColor(hex))
                                .border(if (selected) 3.dp else 0.dp, if (selected) YColors.TextPrimary else Color.Transparent, CircleShape)
                                .clickable { vm.color = hex }
                        )
                    }
                }
                ToggleRow(icon = YIcons.Pin, label = "سنجاق کردن بالای لیست", isOn = vm.pinned, onToggle = { vm.pinned = it })
                Text(text = "چک‌لیست", style = YText.Section, modifier = Modifier.padding(top = 4.dp))
            }
        }
        itemsIndexed(vm.items) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(YRadius.Sm))
                    .background(YColors.Card)
                    .border(1.dp, YColors.Line, RoundedCornerShape(YRadius.Sm))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CheckCircle(checked = item.done, onToggle = { vm.toggleItem(index) }, size = 22.dp)
                Text(
                    text = item.text,
                    style = YText.Body,
                    color = if (item.done) YColors.TextMuted else YColors.TextPrimary,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = YIcons.Close,
                    contentDescription = "حذف مورد",
                    tint = YColors.TextMuted,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { vm.removeItem(index) }
                        .padding(5.dp)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    placeholder = "مورد جدید چک‌لیست",
                    modifier = Modifier.weight(1f),
                    onImeAction = {
                        vm.addItem(newItem)
                        newItem = ""
                    }
                )
                GhostButton(text = "افزودن", onClick = {
                    vm.addItem(newItem)
                    newItem = ""
                })
            }
        }
        item {
            GoldButton(
                text = "ذخیره یادداشت",
                onClick = {
                    if (newItem.isNotBlank()) {
                        vm.addItem(newItem)
                        newItem = ""
                    }
                    vm.save(onClose)
                },
                enabled = vm.loaded,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "حذف یادداشت؟",
            message = "این یادداشت برای همیشه حذف می‌شود.",
            onConfirm = {
                confirmDelete = false
                vm.delete(onClose)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
