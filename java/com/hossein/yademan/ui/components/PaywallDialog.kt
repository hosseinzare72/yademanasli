// مسیر: app/src/main/java/com/hossein/yademan/ui/components/PaywallDialog.kt
package com.hossein.yademan.ui.components

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.hossein.yademan.services.BazaarBilling
import com.hossein.yademan.services.OfferManager
import com.hossein.yademan.services.SubscriptionManager
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private val features = listOf(
    "یادآور نامحدود (فراتر از ۲۰ خدمت رایگان)",
    "۳ صدای اعلان طلایی ویژه",
    "پشتیبان‌گیری ابری رمزنگاری‌شده",
    "حساب مشترک دونفره (من و همسر)",
    "پشتیبانی اولویت‌دار"
)

private const val TAG = "PaywallDialog"

@Composable
fun PaywallDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var offPercent by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            BazaarBilling.bind(context)
            delay(500)
            SubscriptionManager.refreshFromServer()
            active = SubscriptionManager.isActive()
            offPercent = OfferManager.activePercent()
        } catch (e: Exception) {
            Log.e(TAG, "Initial load failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { BazaarBilling.unbind(context) } }
    }

    val purchaseLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val purchaseData = result.data?.getStringExtra("INAPP_PURCHASE_DATA") ?: ""
            val signature = result.data?.getStringExtra("INAPP_DATA_SIGNATURE") ?: ""
            if (purchaseData.isBlank()) {
                message = "خطا: اطلاعات خرید دریافت نشد"
                busy = false
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                busy = true
                try {
                    val jo = runCatching { JSONObject(purchaseData) }.getOrNull()
                    val sku = jo?.optString("productId") ?: ""
                    val token = jo?.optString("purchaseToken") ?: ""
                    if (sku.isBlank() || token.isBlank()) {
                        message = "خطا: اطلاعات خرید ناقص است"
                        busy = false
                        return@launch
                    }
                    val ok = SubscriptionManager.verifyBazaarPurchase(
                        sku, token, signature, purchaseData,
                        OfferManager.appliedOfferId()
                    )
                    busy = false
                    if (ok) {
                        BazaarBilling.consume(context, token)
                        active = true
                        val bonus = SubscriptionManager.plans.firstOrNull { it.productId == sku }
                            ?.let { SubscriptionManager.bonusDaysFor(it, offPercent) } ?: 0
                        message = if (bonus > 0) "خرید تأیید شد ✓ + ${bonus.fa()} روز هدیه 🎁"
                        else "خرید تأیید شد و اشتراک فعال است ✓"
                    } else {
                        message = "تأیید خرید ناموفق؛ با پشتیبانی تماس بگیرید"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Verify failed", e)
                    busy = false
                    message = "خطا در پردازش خرید"
                }
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            message = "خرید لغو شد"
            busy = false
        } else {
            message = "خطا در خرید (کد: ${result.resultCode})"
            busy = false
        }
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
                Text("اشتراک ویژه یادمان", style = YText.CardTitle, color = YColors.Gold1)
                Spacer(Modifier.height(4.dp))
                Text("با اشتراک ویژه، خیالت راحت و همه‌چیز فعال:", style = YText.Meta, color = YColors.TextSecondary)
                Spacer(Modifier.height(10.dp))
                features.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(YColors.Gold2))
                        Spacer(Modifier.width(8.dp))
                        Text(f, style = YText.Name, color = YColors.TextPrimary)
                    }
                }

                Spacer(Modifier.height(12.dp))
                OfferCodeField(onApplied = { offPercent = it })
                Spacer(Modifier.height(10.dp))

                SubscriptionManager.plans.forEach { p ->
                    val special = p.tag == "پیشنهاد ویژه"
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(shape)
                            .background(if (special) YColors.Gold2.copy(alpha = 0.10f) else YColors.Card2)
                            .border(
                                if (special) 1.5.dp else 1.dp,
                                if (special) YColors.Gold2 else YColors.Line,
                                shape
                            )
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.title, style = YText.Name, color = YColors.TextPrimary)
                                p.tag?.let { Text(it, style = YText.Meta, color = YColors.Gold2) }
                                if (offPercent > 0) {
                                    Text(
                                        "🎁 +${SubscriptionManager.bonusDaysFor(p, offPercent).fa()} روز هدیه با کد فعال",
                                        style = YText.Meta,
                                        color = YColors.Success
                                    )
                                }
                            }
                            Text(
                                PersianDigits.toman(p.priceToman),
                                style = YText.Name,
                                color = if (special) YColors.Gold1 else YColors.TextPrimary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        // ✅ دکمه دقیقاً مثل نسخه اصلیِ سالم: همیشه «خرید از کافه‌بازار» و فقط وقتی busy است غیرفعال
                        GoldButton(
                            text = if (busy) "در حال پردازش..." else "خرید از کافه‌بازار",
                            onClick = {
                                busy = true
                                message = null
                                scope.launch {
                                    try {
                                        BazaarBilling.bind(context)
                                        val connected = BazaarBilling.awaitConnection()
                                        if (!connected) {
                                            busy = false
                                            message = if (BazaarBilling.isBazaarInstalled(context))
                                                "بازار نصب است ولی اتصال برقرار نشد. یک‌بار اپ بازار را باز کن، وارد شو و دوباره تلاش کن."
                                            else
                                                "کافه‌بازار روی این دستگاه نصب نیست؛ اول آن را از بازار نصب کن."
                                            return@launch
                                        }
                                        BazaarBilling.buy(
                                            context, p.productId,
                                            onLaunch = { req ->
                                                busy = false
                                                purchaseLauncher.launch(req)
                                            },
                                            onError = { msg ->
                                                busy = false
                                                message = msg
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Purchase flow failed", e)
                                        busy = false
                                        message = "خطا در شروع خرید"
                                    }
                                }
                            },
                            small = true,
                            enabled = !busy
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            SubscriptionManager.refreshFromServer()
                            active = SubscriptionManager.isActive()
                            message = if (active) "اشتراک فعال است ✓" else "اشتراک فعالی یافت نشد"
                        }
                    }) { Text("بررسی وضعیت", style = YText.Meta, color = YColors.Info) }
                }
                if (active) {
                    Spacer(Modifier.height(6.dp))
                    Text("اشتراک ویژه فعال است ✓", style = YText.Name, color = YColors.Success)
                }
                message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = YText.Meta, color = YColors.Warning)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("بستن", style = YText.Name, color = YColors.Gold2)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCodeField(onApplied: (Int) -> Unit) {
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                placeholder = "کد تخفیف",
                modifier = Modifier.weight(1f)
            )
            GoldButton(
                text = "اعمال",
                onClick = {
                    scope.launch {
                        val result = OfferManager.applyCode(code.trim())
                        if (result > 0) {
                            message = "کد تخفیف ${result}% اعمال شد ✓"
                            onApplied(result)
                        } else {
                            message = "کد نامعتبر یا منقضی شده"
                        }
                    }
                },
                small = true
            )
        }
        message?.let {
            Text(
                it,
                style = YText.Meta,
                color = if (it.contains("✓")) YColors.Success else YColors.Danger,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}