package com.hossein.yademan.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.services.AlarmScheduler
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.services.SubscriptionManager
import com.hossein.yademan.services.SyncManager
import com.hossein.yademan.services.SyncResult
import com.hossein.yademan.services.SyncWorker
import com.hossein.yademan.ui.components.AppCard
import com.hossein.yademan.ui.components.Chip
import com.hossein.yademan.ui.components.ConfirmDialog
import com.hossein.yademan.ui.components.GhostButton
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.IconTile
import com.hossein.yademan.ui.components.PaywallDialog
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.SectionTitle
import com.hossein.yademan.ui.components.SoundPickerDialog
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ... بقیه کد ProfileScreen بدون تغییر ...

data class ProfileState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val username: String? = null,
    val lastSync: String? = null,
    val lastError: String? = null
)

class ProfileViewModel : ViewModel() {
    private val meta = ServiceLocator.meta
    private val auth = ServiceLocator.auth
    private val authVersion = MutableStateFlow(0)
    var syncing by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    val state: StateFlow<ProfileState> = combine(
        meta.observe(MetaKeys.DISPLAY_NAME),
        meta.observe("user_phone"),
        meta.observe("user_email"),
        meta.observe(MetaKeys.LAST_SYNC_LOCAL),
        meta.observe(MetaKeys.LAST_SYNC_ERROR),
        authVersion
    ) { values ->
        ProfileState(
            name = (values[0] as? String)?.takeIf { it.isNotBlank() } ?: auth.username ?: "دوست من",
            phone = values[1] as? String ?: "",
            email = values[2] as? String ?: "",
            username = auth.username?.takeIf { auth.isLoggedIn() } ?: if (auth.isLoggedIn()) "" else null,
            lastSync = DateUtils.isoToMillis(values[3] as? String)?.let { DateUtils.relativeFa(it) },
            lastError = values[4] as? String
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    fun refreshAuth() { authVersion.value = authVersion.value + 1 }

    fun updateProfile(name: String, phone: String, email: String) {
        viewModelScope.launch {
            meta.set(MetaKeys.DISPLAY_NAME, name.trim())
            meta.set("user_phone", phone.trim())
            meta.set("user_email", email.trim())
            val success = SubscriptionManager.updateProfile(name, phone, email)
            message = if (success) "پروفایل با موفقیت ذخیره شد ✓" else "خطا در ارتباط با سرور"
        }
    }

    fun syncNow() {
        if (syncing) return
        syncing = true
        viewModelScope.launch {
            val result = SyncManager.sync()
            message = when (result) {
                is SyncResult.Success -> "همگام‌سازی انجام شد ✓"
                is SyncResult.NotLoggedIn -> "اول وارد حساب شو"
                is SyncResult.NetworkError -> "اینترنت در دسترس نیست؛ بعد از اتصال خودکار سینک می‌شود"
                is SyncResult.ServerError -> result.message
            }
            if (result is SyncResult.NetworkError) SyncWorker.requestSoon(ServiceLocator.app, 5)
            syncing = false
        }
    }

    fun logout() {
        auth.logout()
        viewModelScope.launch {
            meta.delete(MetaKeys.LAST_SYNC)
            meta.delete(MetaKeys.LAST_SYNC_LOCAL)
            meta.delete(MetaKeys.LAST_SYNC_ERROR)
            refreshAuth()
            message = "از حساب خارج شدی. داده‌ها روی گوشی باقی می‌مانند."
        }
    }
}

private data class Health(val notifications: Boolean, val exact: Boolean, val battery: Boolean)

private fun readHealth(context: Context): Health {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return Health(
        notifications = NotificationHelper.areEnabled(context),
        exact = AlarmScheduler.canScheduleExact(context),
        battery = pm.isIgnoringBatteryOptimizations(context.packageName)
    )
}

@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenHousehold: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var health by remember { mutableStateOf(readHealth(context)) }
    var editName by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var subUntil by remember { mutableStateOf(0L) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        health = readHealth(context)
        vm.refreshAuth()
        scope.launch { subUntil = SubscriptionManager.activeUntil() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        health = readHealth(context)
    }

    fun start(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun fixNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            start(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        } else {
            start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    fun fixExact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            start(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }

    @Suppress("BatteryLife")
    fun fixBattery() {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        val ok = runCatching { context.startActivity(direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!ok) start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item { ScreenHeader(title = "من", subtitle = "حساب کاربری و تنظیمات") }
        item {
            AppCard(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth(),
                brush = YBrush.HeroCard,
                onClick = { editName = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(YBrush.Gold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.name.take(1), style = YText.Title, color = YColors.OnGold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val user = state.username
                        Text(text = state.name, style = YText.CardTitle)
                        Text(
                            text = when {
                                user == null -> "وارد نشده • داده‌ها فقط روی این گوشی"
                                user.isBlank() -> "وارد شده"
                                else -> "@$user"
                            },
                            style = YText.Sub
                        )
                    }
                    Icon(imageVector = YIcons.Edit, contentDescription = "ویرایش پروفایل", tint = YColors.TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
                val active = subUntil > System.currentTimeMillis()
                val daysLeft = ((subUntil - System.currentTimeMillis()) / 86_400_000L) + 1
                InfoRow(
                    icon = YIcons.Subscription,
                    color = if (active) YColors.Success else YColors.Warning,
                    title = if (active) "اشتراک ویژه فعال" else "اشتراک فعال نداری",
                    subtitle = if (active) "${daysLeft.toInt().fa()} روز مانده • تا ${DateUtils.dateShortFa(subUntil)}" else "از ردیف «اشتراک ویژه» فعالش کن"
                )
            }
        }
        item { SectionTitle(title = "همگام‌سازی") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.username == null) {
                    InfoRow(
                        icon = YIcons.Sync,
                        color = YColors.Info,
                        title = "سینک بین دستگاه‌ها",
                        subtitle = "با حساب kafezare.ir وارد شو تا یادآورها پشتیبان‌گیری شوند"
                    )
                    GoldButton(text = "ورود / ثبت‌نام", onClick = onLogin)
                } else {
                    InfoRow(
                        icon = YIcons.Sync,
                        color = if (state.lastError != null) YColors.Warning else YColors.Success,
                        title = if (vm.syncing) "در حال همگام‌سازی…" else "آخرین همگام‌سازی: ${state.lastSync ?: "هنوز انجام نشده"}",
                        subtitle = state.lastError ?: "سینک خودکار هر ۱۵ دقیقه و بعد از هر تغییر"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton(
                            text = "همگام‌سازی الان",
                            onClick = vm::syncNow,
                            enabled = !vm.syncing,
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(text = "خروج", onClick = { confirmLogout = true }, modifier = Modifier.height(48.dp))
                    }
                }
                vm.message?.let { Text(text = it, style = YText.Meta, color = YColors.Gold2) }
            }
        }
        item { SectionTitle(title = "سلامت یادآورها") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HealthRow(
                    icon = YIcons.Bell, title = "اجازه نمایش اعلان", ok = health.notifications,
                    okText = "فعال", fixText = "فعال‌سازی", onFix = { fixNotifications() }
                )
                HealthRow(
                    icon = YIcons.Clock, title = "آلارم دقیق", ok = health.exact,
                    okText = "مجاز", fixText = "اجازه بده", onFix = { fixExact() }
                )
                HealthRow(
                    icon = YIcons.Battery, title = "بهینه‌سازی باتری",
                    subtitle = "برای شیائومی، سامسونگ و هواوی ضروری است",
                    ok = health.battery, okText = "معاف", fixText = "معاف کن", onFix = { fixBattery() }
                )
                QuickLinkRow(icon = YIcons.Settings, title = "تنظیمات نمایش تاریخ", onClick = onOpenSettings)
                QuickLinkRow(icon = YIcons.Bell, title = "اعلان‌ها و تنظیمات یادآوری", onClick = onOpenNotifications)
                QuickLinkRow(icon = YIcons.Bell, title = "صدای اعلان", onClick = { showSoundPicker = true })
                QuickLinkRow(icon = YIcons.Subscription, title = "اشتراک ویژه", onClick = { showPaywall = true })
                QuickLinkRow(icon = YIcons.Lock, title = "تغییر رمز عبور", onClick = { showChangePassword = true })
                QuickLinkRow(icon = YIcons.Sync, title = "پشتیبان‌گیری و بازیابی", onClick = onOpenBackup)
                QuickLinkRow(icon = YIcons.Meeting, title = "حساب مشترک دونفره", onClick = onOpenHousehold)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { Box(Modifier.padding(horizontal = 20.dp)) { AboutDeveloperCard() } }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (editName) {
        var nameVal by remember { mutableStateOf(state.name) }
        var phoneVal by remember { mutableStateOf(state.phone) }
        var emailVal by remember { mutableStateOf(state.email) }
        AlertDialog(
            onDismissRequest = { editName = false },
            containerColor = YColors.Card,
            title = { Text(text = "ویرایش پروفایل", style = YText.CardTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    YTextField(value = nameVal, onValueChange = { nameVal = it.take(30) }, placeholder = "نام نمایشی")
                    YTextField(
                        value = phoneVal,
                        onValueChange = { phoneVal = it.filter { c -> c.isDigit() || c in "۰۱۲۳۴۵۶۷۸۹" }.take(11) },
                        placeholder = "شماره موبایل (برای پیامک)",
                        keyboardType = KeyboardType.Phone
                    )
                    YTextField(
                        value = emailVal,
                        onValueChange = { emailVal = it },
                        placeholder = "ایمیل (برای یادآوری ایمیلی)",
                        keyboardType = KeyboardType.Email
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateProfile(nameVal, phoneVal, emailVal)
                    editName = false
                }) { Text(text = "ذخیره", style = YText.Name, color = YColors.Gold2) }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) { Text(text = "انصراف", style = YText.Name, color = YColors.TextSecondary) }
            }
        )
    }

    if (showChangePassword) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }
        var passLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!passLoading) showChangePassword = false },
            containerColor = YColors.Card,
            title = { Text(text = "تغییر رمز عبور", style = YText.CardTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    YTextField(value = currentPass, onValueChange = { currentPass = it }, placeholder = "رمز عبور فعلی", keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation())
                    YTextField(value = newPass, onValueChange = { newPass = it }, placeholder = "رمز عبور جدید (حداقل ۶ کاراکتر)", keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation())
                    YTextField(value = confirmPass, onValueChange = { confirmPass = it }, placeholder = "تکرار رمز عبور جدید", keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation())
                    passError?.let { Text(text = it, style = YText.Meta, color = YColors.Danger) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPass.length < 6) { passError = "رمز عبور جدید باید حداقل ۶ کاراکتر باشد"; return@TextButton }
                    if (newPass != confirmPass) { passError = "رمز عبور جدید و تکرار آن مطابقت ندارند"; return@TextButton }
                    passLoading = true; passError = null
                    scope.launch {
                        val success = SubscriptionManager.changePassword(currentPass, newPass)
                        if (success) { showChangePassword = false; vm.message = "رمز عبور با موفقیت تغییر کرد ✓" }
                        else { passError = "رمز عبور فعلی اشتباه است یا خطایی رخ داده" }
                        passLoading = false
                    }
                }, enabled = !passLoading) { Text(text = if (passLoading) "لطفاً صبر کنید…" else "تغییر رمز", style = YText.Name, color = YColors.Gold2) }
            },
            dismissButton = { TextButton(onClick = { if (!passLoading) showChangePassword = false }, enabled = !passLoading) { Text(text = "انصراف", style = YText.Name, color = YColors.TextSecondary) } }
        )
    }

    if (showSoundPicker) {
        SoundPickerDialog(onDismiss = { showSoundPicker = false })
    }
    if (showPaywall) {
        PaywallDialog(onDismiss = { showPaywall = false })
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب؟",
            message = "داده‌های روی گوشی پاک نمی‌شوند ولی دیگر سینک نمی‌شوند.",
            confirmText = "خروج",
            onConfirm = {
                confirmLogout = false
                vm.logout()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, color: Color, title: String, subtitle: String) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = color, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp, bgAlpha = 0.16f)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = YText.Name)
            Text(text = subtitle, style = YText.Meta)
        }
    }
}

@Composable
private fun HealthRow(
    icon: ImageVector, title: String, ok: Boolean, okText: String, fixText: String, onFix: () -> Unit, subtitle: String? = null
) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = if (ok) YColors.Success else YColors.Danger, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp, bgAlpha = 0.16f)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = YText.Name)
            if (subtitle != null) Text(text = subtitle, style = YText.Meta)
        }
        if (ok) Chip(label = okText, color = YColors.Success, icon = YIcons.CheckMark)
        else GoldButton(text = fixText, onClick = onFix, small = true)
    }
}

@Composable
private fun QuickLinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = YColors.Gold2, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp, bgAlpha = 0.16f)
        Text(text = title, style = YText.Name, modifier = Modifier.weight(1f))
        Icon(imageVector = YIcons.ChevronLeft, contentDescription = null, tint = YColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}