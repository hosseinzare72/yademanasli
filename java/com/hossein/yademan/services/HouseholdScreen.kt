// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/HouseholdScreen.kt
package com.hossein.yademan.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.hossein.yademan.services.HouseholdManager
import com.hossein.yademan.services.SubscriptionManager
import com.hossein.yademan.ui.components.AppCard
import com.hossein.yademan.ui.components.GhostButton
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import kotlinx.coroutines.launch

@Composable
fun HouseholdScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<HouseholdManager.Status?>(null) }
    var joinCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isSubActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isSubActive = SubscriptionManager.isActive()
        status = HouseholdManager.status()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "حساب مشترک دونفره",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بازگشت", onClick = onBack) }
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            if (!isSubActive) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("اشتراک ویژه لازم است", style = YText.CardTitle, color = YColors.Warning)
                        Spacer(Modifier.height(6.dp))
                        Text("این قابلیت فقط برای کاربران دارای اشتراک ویژه فعال است.", style = YText.Meta, color = YColors.TextSecondary)
                    }
                }
                return@Column
            }

            val st = status
            if (st == null) {
                Text("در حال بارگذاری...", style = YText.Meta, color = YColors.TextMuted)
            } else if (!st.active) {
                // حالت ساخت یا پیوستن
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("شروع زندگی مشترک دیجیتال", style = YText.CardTitle, color = YColors.Gold1)
                        Text("با همسر خود یک گروه بسازید تا همه قسط‌ها، چک‌ها و قرارها بین دو گوشی شما سینک شوند و ببینید چه کسی چه کاری انجام داده.", style = YText.Meta, color = YColors.TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        GoldButton(text = "ساخت گروه جدید", onClick = {
                            scope.launch {
                                val r = HouseholdManager.create()
                                r.onSuccess { status = HouseholdManager.status(); message = "گروه ساخته شد؛ کد را به همسرت بده" }
                                r.onFailure { message = it.message }
                            }
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("پیوستن با کد دعوت", style = YText.CardTitle, color = YColors.Gold1)
                        Text("اگر همسرت قبلاً گروه ساخته، کد ۸ حرفی را وارد کن:", style = YText.Meta, color = YColors.TextSecondary)
                        YTextField(value = joinCode, onValueChange = { joinCode = it.take(8).uppercase() }, placeholder = "مثلاً A1B2C3D4")
                        GoldButton(text = "پیوستن", onClick = {
                            scope.launch {
                                val r = HouseholdManager.join(joinCode)
                                r.onSuccess { status = HouseholdManager.status(); message = "به گروه $it پیوستی ✓" }
                                r.onFailure { message = it.message }
                            }
                        }, small = true, enabled = joinCode.length >= 6)
                    }
                }
            } else {
                // در یک گروه هست
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("کد گروه شما", style = YText.Meta, color = YColors.TextMuted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(st.code ?: "-", style = YText.Title, color = YColors.Gold1)
                            GhostButton(text = "کپی", onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("household", st.code ?: ""))
                                message = "کد کپی شد ✓"
                            }, modifier = Modifier.height(40.dp))
                        }
                    }
                }

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اعضای گروه", style = YText.CardTitle, color = YColors.Gold1)
                        st.members.forEach { m ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val initial = m.displayName.take(1).ifEmpty { "?" }
                                val isMe = m.username == runCatching { com.hossein.yademan.ServiceLocator.auth.username }.getOrNull()
                                val color = if (m.role == "owner") YColors.Gold2 else YColors.Info
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(color),
                                    contentAlignment = Alignment.Center
                                ) { Text(initial, style = YText.Name, color = YColors.OnGold) }
                                Spacer(Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.displayName.ifBlank { m.username }, style = YText.Name, color = YColors.TextPrimary)
                                    Text(if (m.role == "owner") "سازنده" else "عضو", style = YText.Meta, color = YColors.TextMuted)
                                }
                                if (isMe) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(YColors.Gold2.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Text("من", style = YText.Meta, color = YColors.Gold2) }
                                }
                            }
                        }
                    }
                }

                message?.let {
                    Text(it, style = YText.Name, color = YColors.Success)
                }

                GhostButton(text = "ترک گروه", onClick = {
                    scope.launch {
                        if (HouseholdManager.leave()) {
                            status = HouseholdManager.status()
                            message = "از گروه خارج شدی"
                        } else message = "خطا در ترک گروه"
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}