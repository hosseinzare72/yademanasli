// مسیر: app/src/main/java/com/hossein/yademan/ui/components/SoundPickerDialog.kt
package com.hossein.yademan.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import kotlinx.coroutines.launch

private data class SoundOption(val key: String, val label: String, val premium: Boolean)

private val soundOptions = listOf(
    SoundOption("auto", "خودکار (بر اساس نوع یادآور)", false),
    SoundOption("default", "زنگ کلاسیک", false),
    SoundOption("gentle", "ملایم (مناسب دارو)", false),
    SoundOption("urgent", "جدی (مناسب قسط و چک)", false),
    SoundOption("luxury1", "طلایی ویژه ۱", true),
    SoundOption("luxury2", "طلایی ویژه ۲", true),
    SoundOption("luxury3", "طلایی ویژه ۳", true)
)

/** پنجره انتخاب صدای اعلان با پیش‌شنوند و قفل اشتراک برای صداهای ویژه */
@Composable
fun SoundPickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("auto") }
    var subActive by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(Unit) {
        current = ServiceLocator.meta.get("notif_sound") ?: "auto"
        subActive = (ServiceLocator.meta.get("sub_active_until")?.toLongOrNull() ?: 0L) >
                System.currentTimeMillis()
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { player?.release() }
        }
    }

    fun preview(key: String) {
        runCatching { player?.release() }
        player = MediaPlayer.create(context, NotificationHelper.soundRes(key))
        player?.start()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(YColors.Card)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("صدای اعلان", style = YText.CardTitle, color = YColors.Gold1)
                Spacer(Modifier.height(6.dp))
                Text(
                    "صداهای ویژه فقط با اشتراک فعال باز می‌شوند",
                    style = YText.Meta,
                    color = YColors.TextMuted
                )
                message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = YText.Meta, color = YColors.Warning)
                }
                Spacer(Modifier.height(12.dp))

                soundOptions.forEach { opt ->
                    val locked = opt.premium && !subActive
                    val selected = current == opt.key
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(shape)
                            .background(if (selected) YColors.Gold2.copy(alpha = 0.10f) else YColors.Card2)
                            .border(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) YColors.Gold2 else YColors.Line,
                                shape
                            )
                            .clickable {
                                if (locked) {
                                    message = "این صدا مخصوص اشتراک ویژه است"
                                    return@clickable
                                }
                                message = null
                                scope.launch {
                                    ServiceLocator.meta.set("notif_sound", opt.key)
                                    current = opt.key
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (selected) YColors.Gold2 else YColors.Card)
                                .border(1.dp, if (selected) YColors.Gold2 else YColors.Line, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(YColors.OnGold)
                                )
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(opt.label, style = YText.Name, color = YColors.TextPrimary)
                            if (opt.premium) {
                                Text("ویژه • اشتراک لازم است", style = YText.Meta, color = YColors.Gold2)
                            }
                        }
                        if (locked) {
                            Icon(YIcons.Bell, null, tint = YColors.TextMuted, modifier = Modifier.size(18.dp))
                        } else if (opt.key != "auto") {
                            IconButton(onClick = { preview(opt.key) }) {
                                Icon(YIcons.Sound, "پخش نمونه", tint = YColors.Gold2, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("تمام", style = YText.Name, color = YColors.Gold2)
                    }
                }
            }
        }
    }
}