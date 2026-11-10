// مسیر: app/src/main/java/com/hossein/yademan/ui/components/OfferCodeField.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hossein.yademan.services.OfferManager
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch

@Composable
fun OfferCodeField(modifier: Modifier = Modifier, onApplied: ((Int) -> Unit)? = null) {
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var appliedPercent by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        appliedPercent = OfferManager.activePercent()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (appliedPercent > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "تخفیف ${appliedPercent.fa()}٪ فعال شد ✅",
                    style = YText.MetaBold,
                    color = YColors.Success
                )
                TextButton(onClick = {
                    scope.launch {
                        OfferManager.clear()
                        appliedPercent = 0
                        onApplied?.invoke(0)
                    }
                }) {
                    Text("حذف", style = YText.Meta, color = YColors.TextSecondary)
                }
            }
        } else {
            YTextField(
                value = code,
                onValueChange = { code = it; msg = null },
                placeholder = "کد تخفیف داری؟ وارد کن"
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton(
                    text = if (busy) "بررسی..." else "اعمال کد",
                    small = true,
                    enabled = !busy && code.isNotBlank(),
                    onClick = {
                        scope.launch {
                            busy = true
                            val offer = OfferManager.validate(code)
                            if (offer == null) {
                                msg = "کد نامعتبر است یا تاریخش گذشته"
                            } else {
                                OfferManager.apply(offer)
                                appliedPercent = offer.percent
                                msg = null
                                onApplied?.invoke(offer.percent)
                            }
                            busy = false
                        }
                    }
                )
                msg?.let { Text(it, style = YText.Meta, color = YColors.Danger) }
            }
        }
    }
}