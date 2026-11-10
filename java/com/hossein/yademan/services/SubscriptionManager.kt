package com.hossein.yademan.services

import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SubscriptionManager {
    const val KEY_UNTIL = "sub_active_until"
    const val FREE_LIMIT = 20
    private const val BASE = "https://kafezare.ir/wp-json/lifeplanner/v1/"

    const val PRODUCT_M1 = "yademan_m1"
    const val PRODUCT_M3 = "yademan_m3"
    const val PRODUCT_M6 = "yademan_m6"
    const val PRODUCT_M12 = "yademan_m12"

    data class Plan(
        val id: String,
        val title: String,
        val priceToman: Long,
        val tag: String?,
        val productId: String
    )

    val plans = listOf(
        Plan("m1", "یک ماهه", 45_000, null, PRODUCT_M1),
        Plan("m3", "سه ماهه", 99_000, "۱۰٪ تخفیف", PRODUCT_M3),
        Plan("m6", "شش ماهه", 199_000, "۲۶٪ تخفیف", PRODUCT_M6),
        Plan("m12", "یک ساله", 349_000, "پیشنهاد ویژه", PRODUCT_M12)
    )

    fun monthsOf(p: Plan): Int = when (p.id) {
        "m1" -> 1
        "m3" -> 3
        "m6" -> 6
        else -> 12
    }

    fun bonusDaysFor(p: Plan, percent: Int): Int =
        if (percent <= 0) 0 else Math.round(monthsOf(p) * 30f * percent / 100f).toInt()

    suspend fun activeUntil(): Long =
        ServiceLocator.meta.get(KEY_UNTIL)?.toLongOrNull() ?: 0L

    suspend fun isActive(): Boolean = activeUntil() > System.currentTimeMillis()

    suspend fun serviceCount(): Int = ServiceLocator.reminders.getAll().count {
        !it.isDeleted && ReminderMeta.type(it) != ReminderType.SHOPPING
    }

    suspend fun canCreateMore(): Boolean =
        if (isActive()) true else serviceCount() < FREE_LIMIT

    private fun parseMysqlUtc(s: String): Long = try {
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        f.parse(s)?.time ?: 0L
    } catch (_: Exception) {
        0L
    }

    private fun buildAuthBody(): JSONObject {
        val jo = JSONObject()
        val u = runCatching { ServiceLocator.auth.username }.getOrNull()
        val t = runCatching { ServiceLocator.auth.token }.getOrNull()
        if (!u.isNullOrBlank()) jo.put("username", u)
        if (!t.isNullOrBlank()) jo.put("token", t)
        return jo
    }

    private suspend fun post(path: String, body: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(BASE + path).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.readText() ?: return@withContext null
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshFromServer(): Boolean {
        val auth = buildAuthBody()
        if (auth.length() == 0) return false
        val jo = post("subscription/status", auth.toString()) ?: return false
        return if (jo.optBoolean("active")) {
            ServiceLocator.meta.set(KEY_UNTIL, parseMysqlUtc(jo.optString("ends_at")).toString())
            true
        } else {
            if (activeUntil() > 0) ServiceLocator.meta.set(KEY_UNTIL, "0")
            false
        }
    }

    suspend fun verifyBazaarPurchase(
        productId: String,
        purchaseToken: String,
        signature: String,
        signedData: String,
        offerId: String? = null
    ): Boolean {
        val auth = buildAuthBody()
        auth.put("product_id", productId)
        auth.put("purchase_token", purchaseToken)
        auth.put("signature", signature)
        auth.put("signed_data", signedData)
        if (!offerId.isNullOrBlank()) auth.put("offer_id", offerId)
        val jo = post("subscription/bazaar_verify", auth.toString()) ?: return false
        if (jo.optBoolean("ok")) {
            refreshFromServer()
            return true
        }
        return false
    }

    suspend fun updateProfile(displayName: String, phone: String, email: String): Boolean {
        val auth = buildAuthBody()
        auth.put("display_name", displayName)
        auth.put("phone", phone)
        auth.put("email", email)
        val jo = post("profile/update", auth.toString()) ?: return false
        return jo.optBoolean("ok")
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Boolean {
        val auth = buildAuthBody()
        auth.put("current_password", currentPassword)
        auth.put("new_password", newPassword)
        val jo = post("profile/change_password", auth.toString()) ?: return false
        return jo.optBoolean("ok")
    }
}