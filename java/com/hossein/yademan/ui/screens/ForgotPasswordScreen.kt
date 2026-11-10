package com.hossein.yademan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.services.ApiClient
import com.hossein.yademan.ui.components.GoldButton
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.components.YTextField
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    var identifier by mutableStateOf("") // شماره موبایل یا نام کاربری
    var isLoading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var isError by mutableStateOf(false)

    fun resetPassword(onSuccess: () -> Unit) {
        if (identifier.isBlank()) {
            message = "لطفاً شماره موبایل یا نام کاربری را وارد کنید"
            isError = true
            return
        }
        isLoading = true
        message = null
        viewModelScope.launch {
            try {
                val isPhone = identifier.all { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }
                val body = com.google.gson.JsonObject().apply {
                    if (isPhone) addProperty("phone", identifier)
                    else addProperty("username", identifier)
                }
                val response = ApiClient.post("auth/forgot_password", body)
                message = response.get("message")?.asString ?: "رمز جدید پیامک شد"
                isError = false
                if (response.get("ok")?.asBoolean == true) {
                    onSuccess()
                }
            } catch (e: Exception) {
                message = "خطا در ارتباط با سرور"
                isError = true
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, vm: ForgotPasswordViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(
            title = "بازیابی رمز عبور",
            leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) }
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "شماره موبایل یا نام کاربری خود را وارد کنید تا رمز عبور جدید برایتان پیامک شود.",
            style = YText.Meta,
            color = YColors.TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        YTextField(
            value = vm.identifier,
            onValueChange = { vm.identifier = it },
            placeholder = "مثال: 09123456789 یا hossein",
            keyboardType = KeyboardType.Text
        )

        if (vm.message != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = vm.message!!,
                style = YText.Meta,
                color = if (vm.isError) YColors.Danger else YColors.Success
            )
        }

        Spacer(Modifier.height(32.dp))

        GoldButton(
            text = if (vm.isLoading) "در حال ارسال..." else "ارسال رمز جدید",
            onClick = { vm.resetPassword(onBack) },
            enabled = !vm.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}