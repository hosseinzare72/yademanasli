// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/BackupScreen.kt
package com.hossein.yademan.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hossein.yademan.services.BackupManager
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var messageColor by remember { mutableStateOf(YColors.Gold2) }
    var showPasswordDialog by remember { mutableStateOf<String?>(null) } // "export" یا "import"
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        showPasswordDialog = "export:$uri"
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        showPasswordDialog = "import"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 0.dp)
    ) {
        ScreenHeader(
            title = "پشتیبان‌گیری و بازیابی",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بازگشت", onClick = onBack) }
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("پشتیبان‌گیری", style = YText.CardTitle, color = YColors.Gold1)
                    Text(
                        "یک کپی رمزنگاری‌شده از همه داده‌ها می‌گیرد. رمز عبوری که انتخاب می‌کنی، تنها راه بازیابی این فایل است.",
                        style = YText.Meta, color = YColors.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    GoldButton(
                        text = "ذخیره فایل پشتیبان",
                        onClick = {
                            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
                                .format(java.util.Date())
                            exportLauncher.launch("yademan-backup-$stamp.ydbk")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("بازیابی", style = YText.CardTitle, color = YColors.Gold1)
                    Text(
                        "داده‌های فعلی با محتویات فایل جایگزین می‌شوند. رمز عبوری که هنگام پشتیبان‌گیری وارد کرده‌ای را نیاز داری.",
                        style = YText.Meta, color = YColors.Warning
                    )
                    Spacer(Modifier.height(6.dp))
                    GhostButton(
                        text = "انتخاب فایل و بازیابی",
                        onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔒 امنیت", style = YText.Name, color = YColors.TextPrimary)
                    Text(
                        "فایل با AES-256 و الگوریتم PBKDF2 رمزنگاری می‌شود. رمز عبور فقط در ذهن شماست و روی سرور ذخیره نمی‌شود. اگر رمز را فراموش کنی، فایل قابل بازیابی نیست!",
                        style = YText.Meta, color = YColors.TextSecondary
                    )
                }
            }

            message?.let {
                Text(it, style = YText.Name, color = messageColor)
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    // دیالوگ رمز عبور
    showPasswordDialog?.let { mode ->
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val isExport = mode.startsWith("export")
        val uri = if (isExport) Uri.parse(mode.substringAfter("export:")) else null

        AlertDialog(
            onDismissRequest = { showPasswordDialog = null; pendingImportUri = null },
            containerColor = YColors.Card,
            title = {
                Text(
                    if (isExport) "رمز عبور پشتیبان" else "رمز فایل پشتیبان",
                    style = YText.CardTitle
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (isExport)
                            "رمزی که هنگام بازیابی از شما پرسیده می‌شود. حتماً آن را به خاطر بسپار!"
                        else
                            "رمزی که هنگام ساخت این فایل وارد کرده‌ای.",
                        style = YText.Meta, color = YColors.TextSecondary
                    )
                    YTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "رمز عبور",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (isExport) {
                        YTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            placeholder = "تکرار رمز عبور",
                            keyboardType = KeyboardType.Password,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isExport) {
                        if (password != confirm) {
                            message = "رمز و تکرار آن یکسان نیستند"
                            messageColor = YColors.Danger
                            return@TextButton
                        }
                        if (password.length < 4) {
                            message = "رمز حداقل ۴ کاراکتر"
                            messageColor = YColors.Danger
                            return@TextButton
                        }
                        showPasswordDialog = null
                        uri?.let { u ->
                            scope.launch {
                                message = "در حال پشتیبان‌گیری..."
                                messageColor = YColors.Gold2
                                val out = context.contentResolver.openOutputStream(u)
                                if (out == null) {
                                    message = "خطا در باز کردن فایل"
                                    messageColor = YColors.Danger
                                    return@launch
                                }
                                out.use {
                                    val result = BackupManager.export(context, it, password)
                                    message = if (result.isSuccess) {
                                        "✓ پشتیبان‌گیری انجام شد (${result.getOrDefault(0).fa()} بایت)"
                                    } else {
                                        "✗ خطا: ${result.exceptionOrNull()?.message ?: "نامشخص"}"
                                    }
                                    messageColor = if (result.isSuccess) YColors.Success else YColors.Danger
                                }
                            }
                        }
                    } else {
                        val u = pendingImportUri
                        if (u == null) {
                            showPasswordDialog = null
                            return@TextButton
                        }
                        showPasswordDialog = null
                        scope.launch {
                            message = "در حال بازیابی..."
                            messageColor = YColors.Gold2
                            val input = context.contentResolver.openInputStream(u)
                            if (input == null) {
                                message = "خطا در باز کردن فایل"
                                messageColor = YColors.Danger
                                return@launch
                            }
                            input.use {
                                val result = BackupManager.restore(context, it, password)
                                message = if (result.isSuccess) {
                                    "✓ بازیابی انجام شد (${result.getOrDefault(0).fa()} بایت)"
                                } else {
                                    "✗ خطا: ${result.exceptionOrNull()?.message ?: "نامشخص"}"
                                }
                                messageColor = if (result.isSuccess) YColors.Success else YColors.Danger
                            }
                            pendingImportUri = null
                        }
                    }
                }) {
                    Text(
                        if (isExport) "پشتیبان‌گیری" else "بازیابی",
                        style = YText.Name,
                        color = YColors.Gold2
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = null
                    pendingImportUri = null
                }) {
                    Text("انصراف", style = YText.Name, color = YColors.TextSecondary)
                }
            }
        )
    }
}