// مسیر: app/src/main/java/com/hossein/yademan/ServiceLocator.kt
package com.hossein.yademan

import android.app.Application
import com.hossein.yademan.data.AppDatabase
import com.hossein.yademan.repository.CategoryRepository
import com.hossein.yademan.repository.NotificationRepository
import com.hossein.yademan.repository.ReminderRepository
import com.hossein.yademan.repository.SyncRepository
import com.hossein.yademan.services.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * DI دستی با object singleton (بدون Hilt).
 * در YademanApp.onCreate مقداردهی می‌شود؛ چون Application قبل از هر Receiver/Worker ساخته می‌شود،
 * در همه نقاط ورود (اپ بسته، بعد از ریبوت، آلارم) در دسترس است.
 */
object ServiceLocator {
    lateinit var app: Application
        private set

    /** اسکوپ سراسری برای کارهای پس‌زمینه‌ای که نباید با بسته شدن صفحه لغو شوند */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(application: Application) {
        app = application
    }

    val db: AppDatabase by lazy { AppDatabase.getInstance(app) }
    val auth: AuthManager by lazy { AuthManager(app) }
    val reminders: ReminderRepository by lazy { ReminderRepository(db.reminderDao(), app) }
    val categories: CategoryRepository by lazy { CategoryRepository(db.categoryDao()) }
    val notifications: NotificationRepository by lazy { NotificationRepository(db.notificationLogDao()) }
    val meta: SyncRepository by lazy { SyncRepository(db.syncMetaDao()) }
}
