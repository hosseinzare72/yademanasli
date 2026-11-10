// مسیر: app/src/main/java/com/hossein/yademan/services/DailyAlarmRescheduleWorker.kt
package com.hossein.yademan.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * بازچینش دوره‌ای همه آلارم‌ها (هر ۱۲ ساعت) — لایه اطمینان در برابر Doze،
 * پاک شدن آلارم‌ها توسط «بهینه‌ساز» سازنده‌ها یا force-stop.
 */
class DailyAlarmRescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        AlarmScheduler.rescheduleAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val NAME = "yademan_alarm_reschedule"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyAlarmRescheduleWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
