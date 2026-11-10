// مسیر: app/src/main/java/com/hossein/yademan/services/RemindersWidgetService.kt
package com.hossein.yademan.services

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.JsonParser
import com.hossein.yademan.R
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.Recurrence
import kotlinx.coroutines.runBlocking

class RemindersWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)
}

private class Factory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private data class Row(val id: String, val title: String, val label: String, val whenText: String, val occ: Long)
    private var rows: List<Row> = emptyList()

    override fun onCreate() {}
    override fun onDestroy() {}

    override fun onDataSetChanged() {
        rows = runBlocking { buildRows() }
    }

    private suspend fun buildRows(): List<Row> {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val todayKey = DateUtils.dayKey(now)
        val tomorrowStart = todayStart + DateUtils.DAY
        val customLabels = parseCustomHubLabels(ServiceLocator.meta.get(MetaKeys.CUSTOM_HUBS))

        return ServiceLocator.reminders.getAll()
            .filter { r ->
                !r.isDeleted &&
                        ReminderMeta.type(r) != ReminderType.SHOPPING &&
                        ReminderMeta.type(r) != ReminderType.NOTE
            }
            .mapNotNull { r ->
                val doneToday = ReminderMeta.isDoneOn(r, todayKey)
                val todayOcc = Recurrence.occurrenceOnDay(r, todayStart)

                // ✅ اولویت ۱: مورد امروزِ انجام‌نشده (حتی اگر ساعتش گذشته باشد)
                // ✅ اولویت ۲: نزدیک‌ترین occurrence آینده
                var occ: Long? = when {
                    todayOcc != null && !doneToday -> todayOcc
                    else -> {
                        var o = Recurrence.nextOccurrence(r, now)
                        if (o != null && DateUtils.epochDay(o) == DateUtils.epochDay(now) && doneToday) {
                            o = Recurrence.nextOccurrence(r, tomorrowStart)
                        }
                        o
                    }
                }
                // ✅ اولویت ۳: مورد یک‌بارمصرف عقب‌افتادهٔ انجام‌نشده
                if (occ == null) {
                    val due = DateUtils.isoToMillis(r.dueAt) ?: 0L
                    if (!r.isDone && r.repeatRule == "none" && due < now) occ = due
                }
                if (occ == null) null
                else Row(
                    id = r.id,
                    title = r.title,
                    label = labelFor(r, customLabels),
                    whenText = whenText(occ, now, todayStart),
                    occ = occ
                )
            }
            .sortedBy { it.occ }
    }

    private fun whenText(occ: Long, now: Long, todayStart: Long): String {
        val time = DateUtils.timeFa(occ)
        return when {
            occ < todayStart -> "عقب‌افتاده: ${DateUtils.dateShortFa(occ)} • $time"
            DateUtils.epochDay(occ) == DateUtils.epochDay(now) -> "امروز • $time"
            DateUtils.epochDay(occ) == DateUtils.epochDay(now) + 1 -> "فردا • $time"
            else -> "${DateUtils.dateShortFa(occ)} • $time"
        }
    }

    private fun labelFor(r: Reminder, customLabels: Map<String, String>): String {
        val hub = ReminderMeta.str(r, "hub")
        if (hub != null) {
            return when (hub) {
                "bills" -> "قبض"
                "building" -> "شارژ ساختمان"
                "travel" -> "مسافرت"
                "insurance" -> "بیمه"
                "car" -> "سرویس خودرو"
                "salary" -> "حقوق"
                "pocket" -> "پول توجیبی"
                "birthday" -> "تولد"
                "anniversary" -> "سالگرد"
                else -> customLabels[hub] ?: "بخش سفارشی"
            }
        }
        return when (ReminderMeta.type(r)) {
            ReminderType.INSTALLMENT -> "قسط"
            ReminderType.CHECK -> "چک"
            ReminderType.SUBSCRIPTION -> "اشتراک"
            ReminderType.MEETING -> "قرار"
            ReminderType.MEDICINE -> "دارو"
            ReminderType.TRUST -> "امانت"
            else -> "یادآوری"
        }
    }

    private fun parseCustomHubLabels(json: String?): Map<String, String> = try {
        val el = JsonParser.parseString(json ?: "[]")
        if (!el.isJsonArray) emptyMap()
        else el.asJsonArray.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val o = item.asJsonObject
            val k = o.get("key")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
            val l = o.get("label")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
            k to l
        }.toMap()
    } catch (_: Exception) { emptyMap() }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val v = RemoteViews(context.packageName, R.layout.widget_yademan_item)
        v.setTextViewText(R.id.widget_item_title, row.title)
        v.setTextViewText(R.id.widget_item_label, row.label)
        v.setTextViewText(R.id.widget_item_time, row.whenText)
        v.setOnClickFillInIntent(R.id.widget_item_root, Intent())
        return v
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}