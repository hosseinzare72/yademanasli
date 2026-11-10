// مسیر: app/src/main/java/com/hossein/yademan/services/SyncWorker.kt
package com.hossein.yademan.services

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hossein.yademan.ServiceLocator
import java.util.concurrent.TimeUnit

/**
 * اجرای سینک در پس‌زمینه.
 * - دوره‌ای هر ۱۵ دقیقه (حداقل مجاز WorkManager)
 * - هنگام استارت اپ
 * - debounced چند ثانیه بعد از هر تغییر محلی (REPLACE → فقط آخرین درخواست اجرا می‌شود)
 * همه با شرط «اتصال به شبکه»؛ پس در حالت هواپیما منتظر می‌ماند و بعد از اتصال خودکار سینک می‌کند.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!ServiceLocator.auth.isLoggedIn()) return Result.success()
        return when (val result = SyncManager.sync()) {
            is SyncResult.Success, is SyncResult.NotLoggedIn -> Result.success()
            is SyncResult.NetworkError -> Result.retry()
            is SyncResult.ServerError ->
                if (result.authFailed || runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_NAME = "yademan_sync_periodic"
        private const val ONE_TIME_NAME = "yademan_sync_now"

        private val networkConstraint: Constraints
            get() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** سینک debounced؛ هر فراخوانی جدید، تایمر قبلی را جایگزین می‌کند */
        fun requestSoon(context: Context, delaySeconds: Long = 4) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_NAME)
        }
    }
}
