// مسیر: app/src/main/java/com/hossein/yademan/ui/components/Common.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YText

/** عنوان بخش (.sect) با اکشن متنی اختیاری در سمت چپ */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = YColors.TextMuted,
    onTrailingClick: (() -> Unit)? = null,
    trailingChip: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = YText.Section)
        if (trailingChip != null) {
            trailingChip()
        } else if (trailing != null) {
            Text(
                text = trailing,
                style = YText.MetaBold,
                color = trailingColor,
                modifier = if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier
            )
        }
    }
}

/** دیالوگ تأیید حذف با رنگ‌بندی تم */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "حذف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YColors.Card,
        titleContentColor = YColors.TextPrimary,
        textContentColor = YColors.TextSecondary,
        title = { Text(text = title, style = YText.CardTitle) },
        text = { Text(text = message, style = YText.Body, color = YColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, style = YText.Name, color = YColors.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "انصراف", style = YText.Name, color = YColors.TextSecondary)
            }
        }
    )
}

/** رنگ هر نوع یادآور */
fun typeColor(type: String): Color = when (type) {
    ReminderType.INSTALLMENT -> YColors.CatInstallment
    ReminderType.CHECK -> YColors.CatCheck
    ReminderType.SUBSCRIPTION -> YColors.CatSubscription
    ReminderType.MEETING -> YColors.CatMeeting
    ReminderType.MEDICINE -> YColors.CatMedicine
    ReminderType.SHOPPING -> YColors.CatShopping
    ReminderType.NOTE -> YColors.CatNote
    else -> YColors.Gold3
}

/** آیکن هر نوع یادآور */
fun typeIcon(type: String): ImageVector = when (type) {
    ReminderType.INSTALLMENT -> YIcons.Installment
    ReminderType.CHECK -> YIcons.Check
    ReminderType.SUBSCRIPTION -> YIcons.Subscription
    ReminderType.MEETING -> YIcons.Meeting
    ReminderType.MEDICINE -> YIcons.Medicine
    ReminderType.SHOPPING -> YIcons.Cart
    ReminderType.NOTE -> YIcons.Note
    else -> YIcons.Bell
}

/** رنگ از رشته هگز (#RRGGBB)؛ در صورت خطا طلایی */
fun hexColor(hex: String?, fallback: Color = YColors.Gold3): Color = try {
    if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    fallback
}
