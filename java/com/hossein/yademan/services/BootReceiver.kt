// مسیر: app/src/main/java/com/hossein/yademan/services/BootReceiver.kt
package com.hossein.yademan.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hossein.yademan.ServiceLocator
import kotlinx.coroutines.launch

/**
 * بعد از ریبوت، آپدیت اپ، تغییر ساعت/منطقه زمانی یا تغییر مجوز آلارم دقیق،
 * همه آلارم‌ها را دوباره زمان‌بندی می‌کند (AlarmManager با ریبوت پاک می‌شود).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        ServiceLocator.appScope.launch {
            try {
                NotificationHelper.createChannels(appContext)
                AlarmScheduler.rescheduleAll(appContext)
                DailyAlarmRescheduleWorker.schedule(appContext)
                SyncWorker.schedulePeriodic(appContext)
            } catch (e: Exception) {
                // بی‌صدا؛ Worker دوره‌ای دوباره تلاش می‌کند
            } finally {
                pending.finish()
            }
        }
    }
}
