// مسیر: app/src/main/java/com/hossein/yademan/YademanApp.kt
package com.hossein.yademan

import android.app.Application
import com.hossein.yademan.services.AlarmScheduler
import com.hossein.yademan.services.BazaarBilling
import com.hossein.yademan.services.DailyAlarmRescheduleWorker
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.services.SyncWorker
import kotlinx.coroutines.launch

/**
 * کلاس Application: راه‌اندازی DI، کانال نوتیفیکیشن،
 * بازچینش آلارم‌ها و زمان‌بندی سینک/Workerها.
 * (Seed داده نمونه برای انتشار غیرفعال شد)
 */
class YademanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.createChannels(this)
        BazaarBilling.bind(this)

        ServiceLocator.appScope.launch {
            // Seed.seedIfNeeded()  ← غیرفعال: اپ برای کاربران کاملاً خام باز می‌شود
            AlarmScheduler.rescheduleAll(this@YademanApp)
        }

        DailyAlarmRescheduleWorker.schedule(this)
        SyncWorker.schedulePeriodic(this)
        SyncWorker.requestSoon(this, delaySeconds = 2)
    }
}