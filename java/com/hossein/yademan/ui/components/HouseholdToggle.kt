// مسیر: app/src/main/java/com/hossein/yademan/ui/components/HouseholdToggle.kt
package com.hossein.yademan.ui.components

import com.hossein.yademan.data.Reminder
import com.hossein.yademan.services.HouseholdSyncManager

/**
 * toggle هوشمند is_done: اگر رکورد مال همسر باشد، از سرور hh_update می‌زند.
 * در غیر این صورت، تابع محلیِ داده‌شده را صدا می‌زند.
 *
 * مثال استفاده:
 *   toggleDoneSmart(item.reminder) { repo.setDoneOn(...) }
 */
suspend fun toggleDoneSmart(
    reminder: Reminder,
    localToggle: suspend () -> Unit
) {
    if (HouseholdSyncManager.isForeign(reminder)) {
        HouseholdSyncManager.toggleDoneRemote(reminder, !reminder.isDone)
    } else {
        localToggle()
    }
}