package com.hossein.yademan.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText

@Composable
fun AboutDeveloperCard() {
    val context = LocalContext.current
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "۱٫۲٫۰"

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(YColors.Gold2.copy(alpha = 0.08f))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(YBrush.Gold),
                contentAlignment = Alignment.Center
            ) {
                Text("ح", style = YText.Title, color = YColors.OnGold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "طراح و توسعه‌دهنده: حسین زارع",
                    style = YText.Name,
                    color = YColors.TextPrimary
                )
                Text(
                    "یادمان • نسخه $version",
                    style = YText.Meta,
                    color = YColors.TextMuted
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        DevInfoRow("ایمیل پشتیبانی", "hosseinzarec@gmail.com") {
            open("mailto:hosseinzarec@gmail.com")
        }
        DevInfoRow("تماس مستقیم", "۰۹۳۵۱۰۶۳۶۶۸") {
            open("tel:09351063668")
        }
        DevInfoRow("دیگر برنامه‌های ما", "ZSA Studio") {
            open("https://kafezare.ir/zsa-studio/")
        }
    }
}

@Composable
private fun DevInfoRow(label: String, value: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = YText.Meta, color = YColors.TextSecondary, textAlign = TextAlign.Right)
            Text(value, style = YText.Name, color = YColors.Gold2, textAlign = TextAlign.Left)
        }
    }
}