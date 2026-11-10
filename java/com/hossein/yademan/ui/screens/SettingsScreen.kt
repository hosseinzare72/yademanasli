// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/SettingsScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.ui.components.HeaderIconButton
import com.hossein.yademan.ui.components.IconTile
import com.hossein.yademan.ui.components.ScreenHeader
import com.hossein.yademan.ui.components.YIcons
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText
import com.hossein.yademan.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var currentFormatId by remember { mutableStateOf(DateUtils.DateFormat.FULL.id) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // خواندن مقدار ذخیره‌شده از دیتابیس هنگام ورود به صفحه
    LaunchedEffect(Unit) {
        val saved = ServiceLocator.meta.get(MetaKeys.DATE_FORMAT)
        if (saved != null) {
            currentFormatId = saved
        }
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            ScreenHeader(
                title = "تنظیمات",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) }
            )
        }
        item {
            Text(
                text = "نمایش تاریخ",
                style = YText.Section,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "در حال بارگذاری...", style = YText.Meta, color = YColors.TextMuted)
                }
            }
        } else {
            items(DateUtils.DateFormat.entries) { format ->
                val isSelected = currentFormatId == format.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(YRadius.Md))
                        .background(if (isSelected) YColors.Gold2.copy(alpha = 0.1f) else YColors.Card)
                        .border(1.dp, if (isSelected) YColors.Gold2 else YColors.Line, RoundedCornerShape(YRadius.Md))
                        .clickable {
                            currentFormatId = format.id
                            scope.launch {
                                // استفاده از تابع بهینه‌شده که هم ذخیره می‌کند و هم StateFlow را به‌روز می‌کند
                                DateUtils.updatePreferredFormat(format)
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconTile(
                        color = if (isSelected) YColors.Gold2 else YColors.TextMuted,
                        icon = if (isSelected) YIcons.CheckMark else YIcons.Clock,
                        size = 36.dp,
                        cornerRadius = 12.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = format.title, style = YText.Name, color = if (isSelected) YColors.Gold1 else YColors.TextPrimary)
                        Text(
                            text = "پیش‌نمایش: ${DateUtils.formatDateFa(System.currentTimeMillis(), format)}",
                            style = YText.Meta,
                            color = YColors.TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "این تغییر بلافاصله در تمام صفحات اپ (اقساط، دارو، تقویم و...) اعمال می‌شود.",
                style = YText.Meta,
                color = YColors.TextMuted,
                modifier = Modifier.padding(20.dp).fillMaxWidth()
            )
        }
    }
}