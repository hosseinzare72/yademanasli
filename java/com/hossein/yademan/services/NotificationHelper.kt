// مسیر: app/src/main/java/com/hossein/yademan/services/NotificationHelper.kt
package com.hossein.yademan.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hossein.yademan.MainActivity
import com.hossein.yademan.R
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType

object NotificationHelper {
    const val CHANNEL_MAIN = "yademan_reminders"
    const val CHANNEL_SILENT = "yademan_reminders_silent"
    const val EXTRA_OPEN_ROUTE = "open_route"
    private const val SND_VERSION = "v3"

    fun soundRes(key: String): Int = when (key) {
        "gentle" -> R.raw.notif_gentle
        "urgent" -> R.raw.notif_urgent
        "luxury1" -> R.raw.notif_luxury1
        "luxury2" -> R.raw.notif_luxury2
        "luxury3" -> R.raw.notif_luxury3
        else -> R.raw.notif_default
    }

    private fun soundUri(context: Context, key: String): Uri =
        Uri.parse("android.resource://${context.packageName}/${soundRes(key)}")

    private fun channelIdFor(key: String): String =
        if (key == "default") CHANNEL_MAIN else "yademan_snd_${SND_VERSION}_$key"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (nm.getNotificationChannel(CHANNEL_MAIN) == null) {
            val main = NotificationChannel(CHANNEL_MAIN, "یادآوری‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "اعلان یادآورها، اقساط، داروها و قرارها"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFDDAF4B.toInt()
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri(context, "default"), attrs)
                setBypassDnd(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            nm.createNotificationChannel(main)
        }

        if (nm.getNotificationChannel(CHANNEL_SILENT) == null) {
            val silent = NotificationChannel(CHANNEL_SILENT, "یادآوری‌های بی‌صدا", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "وقتی صدا خاموش است یا حالت مزاحم نشو فعال است"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(silent)
        }
    }

    private fun ensureSoundChannel(context: Context, key: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val id = channelIdFor(key)
        if (nm.getNotificationChannel(id) != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val ch = NotificationChannel(id, "یادآوری‌ها • صدای اختصاصی", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFFDDAF4B.toInt()
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(soundUri(context, key), attrs)
            setBypassDnd(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(true)
            }
        }
        nm.createNotificationChannel(ch)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun areEnabled(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    suspend fun show(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        silent: Boolean,
        reminderId: String?,
        occurrence: Long,
        doneLabel: String?,
        typeKey: String? = null
    ): Boolean {
        createChannels(context)
        if (!hasPermission(context)) return false

        val meta = ServiceLocator.meta
        val selected = meta.get("notif_sound") ?: "auto"
        val subActive = (meta.get("sub_active_until")?.toLongOrNull() ?: 0L) > System.currentTimeMillis()
        val resolvedType = typeKey ?: reminderId?.let {
            ServiceLocator.reminders.getById(it)?.let { r -> ReminderMeta.type(r) }
        }
        val autoKey = when (resolvedType) {
            ReminderType.MEDICINE -> "gentle"
            ReminderType.INSTALLMENT, ReminderType.CHECK -> "urgent"
            else -> "default"
        }
        var soundKey = if (selected == "auto") autoKey else selected
        if (soundKey.startsWith("luxury") && !subActive) soundKey = autoKey
        if (!silent) ensureSoundChannel(context, soundKey)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, "notifications")
        }
        val contentPi = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = if (silent) CHANNEL_SILENT else channelIdFor(soundKey)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setColor(0xFFDDAF4B.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(contentPi)
            .setOnlyAlertOnce(false)
            .setOngoing(false)

        if (silent) {
            builder.setSilent(true)
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setSound(soundUri(context, soundKey))
            }
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
        }

        // دکمه «انجام شد / پرداخت شد / مصرف شد»
        if (reminderId != null && doneLabel != null) {
            val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_DONE
                data = Uri.parse("yademan://done/${Uri.encode(reminderId)}")
                putExtra(AlarmScheduler.EXTRA_ID, reminderId)
                putExtra(AlarmScheduler.EXTRA_OCCURRENCE, occurrence)
            }
            val donePi = PendingIntent.getBroadcast(
                context, notificationId, doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, doneLabel, donePi)
        }

        // ✅ دکمه «چرت زدن» (Snooze) — ۴ ساعت تأخیر
        if (reminderId != null) {
            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SNOOZE
                data = Uri.parse("yademan://snooze/${Uri.encode(reminderId)}")
                putExtra(AlarmScheduler.EXTRA_ID, reminderId)
                putExtra(AlarmScheduler.EXTRA_OCCURRENCE, occurrence)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val snoozePi = PendingIntent.getBroadcast(
                context, notificationId + 5000, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "چرت ۴ ساعته ⏰", snoozePi)
        }

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}