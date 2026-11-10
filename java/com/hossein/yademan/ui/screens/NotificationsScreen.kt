// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/NotificationsScreen.kt
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
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.*
import com.hossein.yademan.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.hossein.yademan.data.NotificationLog
import com.hossein.yademan.repository.MetaKeys

data class NotifRow(val log: NotificationLog, val type: String, val createdMillis: Long)

data class NotificationsState(
    val rows: List<NotifRow> = emptyList(),
    val unread: Int = 0,
    val smart: Boolean = true,
    val sound: Boolean = true,
    val dnd: Boolean = false
)

class NotificationsViewModel : ViewModel() {
    private val meta = ServiceLocator.meta
    private val notifs = ServiceLocator.notifications

    private val settings = combine(
        meta.observeBool(MetaKeys.NOTIF_SMART, true),
        meta.observeBool(MetaKeys.NOTIF_SOUND, true),
        meta.observeBool(MetaKeys.NOTIF_DND, false)
    ) { s, so, d -> Triple(s, so, d) }

    val state: StateFlow<NotificationsState> = combine(
        notifs.observeRecent(),
        ServiceLocator.reminders.observeAll(),
        settings,
        TimeTicker.now
    ) { logs, reminders, (smart, sound, dnd), _ ->
        val types = reminders.associate { it.id to ReminderMeta.type(it) }
        NotificationsState(
            rows = logs.map { NotifRow(it, types[it.sourceId] ?: ReminderType.OTHER, DateUtils.isoToMillis(it.createdAt) ?: 0L) },
            unread = logs.count { !it.isRead },
            smart = smart,
            sound = sound,
            dnd = dnd
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsState())

    fun markRead(row: NotifRow) {
        if (!row.log.isRead) viewModelScope.launch { notifs.markRead(row.log.id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notifs.markAllRead() }
    }

    fun setSetting(key: String, value: Boolean) {
        viewModelScope.launch { meta.setBool(key, value) }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, vm: NotificationsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun openSystemSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "اعلان‌ها",
                subtitle = "${state.unread.fa()} اعلان خوانده نشده",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
                actions = { HeaderIconButton(icon = YIcons.Settings, contentDescription = "تنظیمات اعلان", onClick = { openSystemSettings() }) }
            )
        }
        if (state.unread > 0) {
            item {
                SectionTitle(title = "اخیر", trailing = "خواندن همه", trailingColor = YColors.Gold2, onTrailingClick = vm::markAllRead)
            }
        } else {
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
        if (state.rows.isEmpty()) {
            item { EmptyState(message = "هنوز اعلانی نیامده. وقتی یادآوری‌ها زنگ بخورند اینجا می‌بینی.", icon = YIcons.Bell) }
        } else {
            items(state.rows, key = { it.log.id }) { row ->
                NotificationRowView(
                    row = row,
                    onClick = { vm.markRead(row) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
        item { SectionTitle(title = "تنظیمات یادآوری") }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToggleRow(
                    icon = YIcons.Sparkle,
                    label = "یادآوری هوشمند",
                    subtitle = "اگر انجام نشد، ۱۰ دقیقه بعد دوباره یادآوری کن",
                    isOn = state.smart,
                    onToggle = { vm.setSetting(MetaKeys.NOTIF_SMART, it) }
                )
                ToggleRow(
                    icon = YIcons.Sound,
                    label = "صدای اعلان",
                    subtitle = "پخش صدا و لرزش هنگام یادآوری",
                    isOn = state.sound,
                    onToggle = { vm.setSetting(MetaKeys.NOTIF_SOUND, it) },
                    iconColor = YColors.Info
                )
                ToggleRow(
                    icon = YIcons.Moon,
                    label = "مزاحم نشو",
                    subtitle = "از ساعت ۲۳ تا ۷ صبح اعلان‌ها بی‌صدا می‌آیند",
                    isOn = state.dnd,
                    onToggle = { vm.setSetting(MetaKeys.NOTIF_DND, it) },
                    iconColor = YColors.CatCheck
                )
            }
        }
    }
}

@Composable
private fun NotificationRowView(row: NotifRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    val read = row.log.isRead
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (read) YColors.Bg1 else YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(
            color = typeColor(row.type),
            icon = typeIcon(row.type),
            size = 42.dp,
            cornerRadius = 14.dp,
            iconSize = 21.dp,
            modifier = Modifier.alpha(if (read) 0.55f else 1f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.log.title,
                style = YText.Name,
                color = if (read) YColors.TextMuted else YColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = row.log.body,
                style = YText.Meta.copy(color = if (read) YColors.TextMuted else YColors.TextSecondary),
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = DateUtils.relativeFa(row.createdMillis),
                style = YText.MetaBold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (!read) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(YColors.Gold2)
            )
        }
    }
}
