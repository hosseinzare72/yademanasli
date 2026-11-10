// مسیر: app/src/main/java/com/hossein/yademan/services/RentStore.kt
package com.hossein.yademan.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class RentItem(
    val id: String,
    val title: String,
    val kind: String,          // "pay" = پرداختی | "get" = دریافتی
    val amountToman: Long,
    val dueDay: Int,           // روز سررسید ماهانه (۱ تا ۳۱)
    val ownerName: String,
    val phone: String,
    val note: String,
    /** ✅ کلید ماه‌هایی که پرداخت شده‌اند، مثل "2026-11" */
    val paidKeys: List<String> = emptyList()
)

object RentStore {
    private const val PREF = "yademan_rent"
    private const val KEY = "rent_items_v1"

    private fun pref(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun all(ctx: Context): List<RentItem> = try {
        val arr = JSONArray(pref(ctx).getString(KEY, "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val paidArr = o.optJSONArray("paid")
            val paid = if (paidArr == null) emptyList()
            else (0 until paidArr.length()).mapNotNull { paidArr.optString(it) }
            RentItem(
                id = o.optString("id"),
                title = o.optString("title"),
                kind = o.optString("kind", "pay"),
                amountToman = o.optLong("amount", 0),
                dueDay = o.optInt("dueDay", 1),
                ownerName = o.optString("owner", ""),
                phone = o.optString("phone", ""),
                note = o.optString("note", ""),
                paidKeys = paid
            )
        }
    } catch (_: Exception) { emptyList() }

    fun save(ctx: Context, items: List<RentItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("title", it.title); put("kind", it.kind)
                put("amount", it.amountToman); put("dueDay", it.dueDay)
                put("owner", it.ownerName); put("phone", it.phone); put("note", it.note)
                put("paid", JSONArray(it.paidKeys))
            })
        }
        pref(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(ctx: Context, item: RentItem) {
        val list = all(ctx).toMutableList()
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx >= 0) list[idx] = item else list.add(item)
        save(ctx, list)
        schedule(ctx, item)
    }

    fun delete(ctx: Context, id: String) {
        save(ctx, all(ctx).filterNot { it.id == id })
        cancel(ctx, id)
    }

    // ---------- ✅ وضعیت پرداخت ماه جاری ----------

    /** کلید ماه جاری، مثل "2026-11" */
    fun cycleKey(now: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        return String.format("%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    /** آیا اجارهٔ این ماه پرداخت شده؟ */
    fun isPaid(item: RentItem, now: Long = System.currentTimeMillis()): Boolean =
        cycleKey(now) in item.paidKeys

    /** ثبت یا لغو پرداخت ماه جاری */
    fun setPaid(ctx: Context, item: RentItem, paid: Boolean) {
        val key = cycleKey()
        val keys = item.paidKeys.toMutableSet()
        if (paid) keys.add(key) else keys.remove(key)
        val updated = item.copy(paidKeys = keys.toList().sorted())
        val list = all(ctx).toMutableList()
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx >= 0) list[idx] = updated else list.add(updated)
        save(ctx, list)
        schedule(ctx, updated)
    }

    /** روزهای مانده تا سررسید این ماه (منفی = گذشته) */
    fun daysLeft(item: RentItem, now: Long = System.currentTimeMillis()): Int {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, minOf(item.dueDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return ((c.timeInMillis - now) / 86_400_000L).toInt()
    }

    /** prochaine سررسید آینده (برای آلارم) */
    fun nextDueMillis(item: RentItem): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, minOf(item.dueDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.before(now)) cal.add(Calendar.MONTH, 1)
        return cal.timeInMillis
    }

    // ---------- آلارم سازگار با همه اندرویدها ----------
    fun schedule(ctx: Context, item: RentItem) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, item.id, item.title, item.amountToman)
        val at = nextDueMillis(item)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms() ->
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else -> am.set(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun scheduleAll(ctx: Context) { all(ctx).forEach { schedule(ctx, it) } }

    fun cancel(ctx: Context, id: String) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx, id, "", 0))
    }

    private fun pending(ctx: Context, id: String, title: String, amount: Long): PendingIntent {
        val intent = Intent(ctx, RentReceiver::class.java).apply {
            putExtra("id", id); putExtra("title", title); putExtra("amount", amount)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(ctx, id.hashCode(), intent, flags)
    }
}