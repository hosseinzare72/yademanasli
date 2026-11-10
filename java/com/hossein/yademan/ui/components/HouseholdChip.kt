// مسیر: app/src/main/java/com/hossein/yademan/ui/components/HouseholdChip.kt
package com.hossein.yademan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.services.HouseholdSyncManager
import com.hossein.yademan.ui.theme.YColors

/**
 * چیپ کوچک برای نمایش «مال همسر» در گروه دونفره.
 * اگر رکورد مال خود کاربر باشد، هیچ چیزی نشان نمی‌دهد (تمیز می‌ماند).
 */
@Composable
fun HouseholdOwnerChip(reminder: Reminder, modifier: Modifier = Modifier) {
    val ownerName = ReminderMeta.ownerDisplayName(reminder) ?: return
    val myId = HouseholdSyncManager.myUserId
    val ownerId = ReminderMeta.ownerUserId(reminder)
    val isMine = myId == null || ownerId == null || ownerId == myId
    if (isMine) return

    Chip(
        label = "مال $ownerName",
        color = YColors.Info,
        icon = YIcons.Note,
        modifier = modifier
    )
}