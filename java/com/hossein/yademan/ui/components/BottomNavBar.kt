// مسیر: app/src/main/java/com/hossein/yademan/ui/components/BottomNavBar.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.hossein.yademan.ui.theme.YText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YBrush
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius

/** تب‌های نوار پایین؛ به ترتیب از راست به چپ (RTL) */
enum class YTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "خانه", YIcons.Home),
    REMINDERS("reminders", "یادآوری‌ها", YIcons.Bell),
    SHOPPING("shopping", "خرید", YIcons.Cart),
    PROFILE("profile", "من", YIcons.User)
}

/**
 * نوار شناور پایین (.nav): فاصله ۱۶ از طرفین و ۱۴ از پایین، ارتفاع ۷۶، گوشه ۲۶،
 * پس‌زمینه card با شفافیت ۹۲٪. تب فعال = پیله طلایی ۱۶٪ پشت آیکن طلایی.
 * FAB طلایی ۵۶ با حاشیه ۴ به رنگ پس‌زمینه و سایه طلایی، ۱۷dp بالاتر از مرکز نوار.
 */
@Composable
fun BottomNavBar(
    currentTab: YTab?,
    onTabClick: (YTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(YRadius.Lg)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .height(76.dp)
    ) {
        // لایه پس‌زمینه جدا تا FAB بیرون‌زده بریده نشود
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(elevation = 20.dp, shape = barShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(barShape)
                .background(YColors.Card.copy(alpha = 0.92f))
                .border(1.dp, YColors.Line, barShape)
        )
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavSlot(YTab.HOME, currentTab == YTab.HOME, onTabClick)
            NavSlot(YTab.REMINDERS, currentTab == YTab.REMINDERS, onTabClick)
            Box(
                modifier = Modifier
                    .offset(y = (-17).dp)
                    .size(56.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = YColors.Gold2,
                        spotColor = YColors.Gold2
                    )
                    .clip(CircleShape)
                    .background(YBrush.Gold)
                    .border(4.dp, YColors.Bg0, CircleShape)
                    .clickable(onClick = onFabClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = YIcons.PlusBold,
                    contentDescription = "یادآوری جدید",
                    tint = YColors.OnGold,
                    modifier = Modifier.size(24.dp)
                )
            }
            NavSlot(YTab.SHOPPING, currentTab == YTab.SHOPPING, onTabClick)
            NavSlot(YTab.PROFILE, currentTab == YTab.PROFILE, onTabClick)
        }
    }
}

@Composable
private fun NavSlot(tab: YTab, selected: Boolean, onTabClick: (YTab) -> Unit) {
    val shape = RoundedCornerShape(50)
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onTabClick(tab) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // تب فعال = پیله طلایی پشت آیکن + برچسب طلایی
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 30.dp)
                .clip(shape)
                .background(if (selected) YColors.Gold2.copy(alpha = 0.16f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = if (selected) YColors.Gold2 else YColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = tab.label,
            style = YText.Label.copy(fontSize = 9.5.sp),
            color = if (selected) YColors.Gold2 else YColors.TextMuted,
            maxLines = 1
        )
    }
}
