// مسیر: app/src/main/java/com/hossein/yademan/services/RemindersWidgetProvider.kt
package com.hossein.yademan.services

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hossein.yademan.MainActivity
import com.hossein.yademan.R

class RemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateOne(context, manager, id)
    }

    companion object {
        fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_yademan)
            val svc = Intent(context, RemindersWidgetService::class.java)
            views.setRemoteAdapter(R.id.widget_list, svc)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_header, pi)
            views.setPendingIntentTemplate(R.id.widget_list, pi)
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }

        /** از داخل اپ صدا زده می‌شود تا ویجت فوراً تازه شود */
        fun refresh(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context.applicationContext)
                val ids = manager.getAppWidgetIds(ComponentName(context.applicationContext, RemindersWidgetProvider::class.java))
                for (id in ids) updateOne(context.applicationContext, manager, id)
            } catch (_: Exception) { }
        }
    }
}