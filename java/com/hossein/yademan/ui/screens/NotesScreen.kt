// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/NotesScreen.kt
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
import com.hossein.yademan.data.NoteItem

data class NoteCardState(
    val reminder: Reminder,
    val pinned: Boolean,
    val items: List<NoteItem>,
    val color: Color,
    val createdMillis: Long
)

data class NotesState(
    val notes: List<NoteCardState> = emptyList(),
    val total: Int = 0,
    val pinnedCount: Int = 0,
    val query: String = ""
)

class NotesViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    private val query = MutableStateFlow("")

    val state: StateFlow<NotesState> = combine(repo.observeAll(), query, TimeTicker.now) { list, q, _ ->
        val notes = list.filter { ReminderMeta.type(it) == ReminderType.NOTE }.map {
            NoteCardState(
                reminder = it,
                pinned = ReminderMeta.bool(it, "pinned"),
                items = ReminderMeta.noteItems(it),
                color = hexColor(it.color, YColors.CatNote),
                createdMillis = DateUtils.isoToMillis(it.createdAt) ?: 0L
            )
        }.sortedWith(compareByDescending<NoteCardState> { it.pinned }.thenByDescending { it.reminder.updatedAt })
        val filtered = if (q.isBlank()) notes else notes.filter { n ->
            n.reminder.title.contains(q, ignoreCase = true) ||
                    n.reminder.note.contains(q, ignoreCase = true) ||
                    n.items.any { it.text.contains(q, ignoreCase = true) }
        }
        NotesState(filtered, notes.size, notes.count { it.pinned }, q)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesState())

    fun setQuery(q: String) {
        query.value = q
    }

    /** تیک زدن هوشمند: اگر یادداشت مال همسر باشد، از سرور به‌روز می‌شود */
    fun toggleItemSmart(note: NoteCardState, index: Int) {
        viewModelScope.launch {
            val fresh = repo.getById(note.reminder.id) ?: return@launch
            val items = ReminderMeta.noteItems(fresh).toMutableList()
            if (index !in items.indices) return@launch
            items[index] = items[index].copy(done = !items[index].done)
            val updated = ReminderMeta.withNoteItems(fresh, items)

            if (HouseholdSyncManager.isForeign(fresh)) {
                HouseholdSyncManager.toggleDoneRemote(updated, updated.isDone)
            } else {
                repo.update(updated)
            }
        }
    }
}

@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpen: (id: String?) -> Unit,
    vm: NotesViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "دفترچه یادداشت",
                subtitle = "${state.total.fa()} یادداشت • ${state.pinnedCount.fa()} سنجاق شده",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
                actions = {
                    HeaderIconButton(
                        icon = if (searching) YIcons.Close else YIcons.Search,
                        contentDescription = "جست‌وجو",
                        onClick = {
                            if (searching) vm.setQuery("")
                            searching = !searching
                        }
                    )
                }
            )
        }
        if (searching) {
            item {
                YTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    placeholder = "جست‌وجو در یادداشت‌ها…",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
        if (state.notes.isEmpty()) {
            item {
                EmptyState(
                    message = if (state.query.isBlank()) "هنوز یادداشتی نداری" else "چیزی پیدا نشد",
                    icon = YIcons.Note
                )
            }
        } else {
            items(state.notes, key = { it.reminder.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onOpen(note.reminder.id) },
                    onToggleItem = { idx -> vm.toggleItemSmart(note, idx) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
        item {
            GoldButton(
                text = "+ یادداشت جدید",
                onClick = { onOpen(null) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteCardState,
    onClick: () -> Unit,
    onToggleItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(YRadius.Md)
    val doneCount = note.items.count { it.done }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
    ) {
        // نوار رنگی عمودی ۸dp کنار کارت (سمت راست در RTL)
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(note.color)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = note.reminder.title, style = YText.CardTitle, maxLines = 1, modifier = Modifier.weight(1f))
                if (note.pinned) Chip(label = "سنجاق شده", color = YColors.Gold2, icon = YIcons.Pin)
            }
            val metaText = if (note.items.isNotEmpty()) {
                "${note.items.size.fa()} مورد • ${doneCount.fa()} انجام شده • ${DateUtils.relativeFa(note.createdMillis)}"
            } else {
                DateUtils.relativeFa(note.createdMillis)
            }
            Text(text = metaText, style = YText.Meta, modifier = Modifier.padding(top = 2.dp))

            // چیپ مالکیت در گروه دونفره
            HouseholdOwnerChip(
                reminder = note.reminder,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (note.reminder.note.isNotBlank()) {
                Text(
                    text = note.reminder.note,
                    style = YText.Body.copy(color = YColors.TextSecondary),
                    maxLines = 3,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (note.items.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.items.take(5).forEachIndexed { idx, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleItem(idx) }
                                .padding(vertical = 2.dp)
                        ) {
                            CheckCircle(checked = item.done, onToggle = { onToggleItem(idx) }, size = 20.dp)
                            Text(
                                text = item.text,
                                style = YText.Body,
                                color = if (item.done) YColors.TextMuted else YColors.TextPrimary,
                                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1
                            )
                        }
                    }
                    if (note.items.size > 5) {
                        Text(text = "+ ${(note.items.size - 5).fa()} مورد دیگر", style = YText.Meta)
                    }
                }
            }
        }
    }
}