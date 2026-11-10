package com.hossein.yademan.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class RentReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // ✅ این خط را اضافه کنید تا هشدار IDE برطرف شود
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val title = intent.getStringExtra("title") ?: "اجاره"
        val amount = intent.getStringExtra("amount")?.toLongOrNull() ?: 0L

        val channelId = "rent_due"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "سررسید اجاره", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }

        val money = if (amount > 0) " — مبلغ: ${String.format("%,d", amount)} تومان" else ""
        val n = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("یادمان: سررسید اجاره")
            .setContentText(title + money)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(ctx).notify(id.hashCode(), n) }

        // سررسید ماه بعد را خودکار تنظیم کن
        RentStore.all(ctx).firstOrNull { it.id == id }?.let { RentStore.schedule(ctx, it) }
    }
}