package com.hossein.yademan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.services.ApiClient
import com.hossein.yademan.services.ApiException
import com.hossein.yademan.services.SyncManager
import com.hossein.yademan.services.SyncWorker
import com.hossein.yademan.services.obj
import com.hossein.yademan.services.str
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.SegmentedRow
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import kotlinx.coroutines.launch
import java.io.IOException

class LoginViewModel : ViewModel() {
    var mode by mutableIntStateOf(0) // ۰=ورود، ۱=ثبت‌نام
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)

    fun submit(onSuccess: () -> Unit) {
        if (loading) return
        val u = username.trim()
        val p = password
        if (u.length < 3) { error = "نام کاربری حداقل ۳ حرف باشد"; return }
        if (p.length < 6) { error = "رمز عبور حداقل ۶ کاراکتر باشد"; return }

        loading = true; error = null
        viewModelScope.launch {
            try {
                val auth = ServiceLocator.auth
                val device = auth.deviceId
                var resp = if (mode == 1) ApiClient.register(u, p, device) else ApiClient.login(u, p, device)
                var token = resp.str("token")
                if (token.isNullOrBlank() && mode == 1) {
                    resp = ApiClient.login(u, p, device)
                    token = resp.str("token")
                }
                if (token.isNullOrBlank()) throw ApiException("توکن از سرور دریافت نشد")
                val userId = resp.str("user_id") ?: resp.obj("user")?.str("id")
                auth.saveAuth(token, userId, u)
                val meta = ServiceLocator.meta
                meta.delete(MetaKeys.LAST_SYNC)
                val userObj = resp.obj("user")
                if (userObj != null) {
                    meta.set("user_phone", userObj.str("phone") ?: "")
                    meta.set("user_email", userObj.str("email") ?: "")
                    if (meta.get(MetaKeys.DISPLAY_NAME).isNullOrBlank()) {
                        meta.set(MetaKeys.DISPLAY_NAME, userObj.str("display_name") ?: u)
                    }
                } else {
                    if (meta.get(MetaKeys.DISPLAY_NAME).isNullOrBlank()) meta.set(MetaKeys.DISPLAY_NAME, u)
                }
                SyncWorker.schedulePeriodic(ServiceLocator.app)
                SyncManager.sync()
                onSuccess()
            } catch (e: ApiException) { error = e.message ?: "خطای سرور" }
            catch (e: IOException) { error = "اتصال به اینترنت برقرار نیست" }
            catch (e: Exception) { error = e.message ?: "خطای ناشناخته" }
            finally { loading = false }
        }
    }
}

@Composable
fun LoginScreen(onClose: () -> Unit, onForgotPassword: (() -> Unit)? = null, vm: LoginViewModel = viewModel()) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeader(
            title = if (vm.mode == 0) "ورود به حساب" else "ساخت حساب",
            subtitle = "همگام‌سازی با kafezare.ir",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onClose) }
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(84.dp).clip(CircleShape).background(YColors.Gold2.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = YIcons.Bell, contentDescription = null, tint = YColors.Gold2, modifier = Modifier.size(40.dp))
            }
            Text(text = "یادمان", style = YText.Title.copy(fontSize = 28.sp, color = YColors.Gold1))
            Text(text = "یادآورهایت روی همه گوشی‌ها، همیشه همراهت", style = YText.Sub)

            SegmentedRow(options = listOf("ورود", "ثبت‌نام"), selectedIndex = vm.mode, onSelect = { vm.mode = it; vm.error = null }, modifier = Modifier.padding(top = 10.dp))
            YTextField(value = vm.username, onValueChange = { vm.username = it.trim() }, placeholder = "نام کاربری", imeAction = ImeAction.Next)
            YTextField(value = vm.password, onValueChange = { vm.password = it }, placeholder = "رمز عبور", keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation(), onImeAction = { vm.submit(onClose) })

            if (vm.mode == 0 && onForgotPassword != null) {
                TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                    Text(text = "رمز عبور را فراموش کرده‌اید؟", style = YText.Meta, color = YColors.Gold2)
                }
            }

            vm.error?.let { Text(text = it, style = YText.Meta, color = YColors.Danger) }

            GoldButton(
                text = when { vm.loading -> "لطفاً صبر کن…"; vm.mode == 0 -> "ورود"; else -> "ثبت‌نام" },
                onClick = { vm.submit(onClose) }, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()
            )
            Text(text = "بدون حساب هم می‌توانی از اپ استفاده کنی؛ داده‌ها روی گوشی ذخیره می‌شوند.", style = YText.Meta, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}