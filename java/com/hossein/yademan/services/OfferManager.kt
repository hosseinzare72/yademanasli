// مسیر: app/src/main/java/com/hossein/yademan/services/OfferManager.kt
package com.hossein.yademan.services

import com.google.gson.JsonParser
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.JalaliConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class Offer(
    val id: String,
    val hash: String,
    val percent: Int,
    val starts: Long?,
    val ends: Long?,
    val label: String
)

object OfferManager {

    const val OFFERS_URL = "https://kafezare.ir/wp-json/lifeplanner/v1/offers/list"

    private const val CACHE_KEY = "offers_cache"
    private const val APPLIED_PERCENT = "offer_percent"
    private const val APPLIED_UNTIL = "offer_until"
    private const val APPLIED_LABEL = "offer_label"
    private const val APPLIED_ID = "offer_id"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var memory: List<Offer>? = null

    fun normalize(code: String): String = code.trim().uppercase(java.util.Locale.US)

    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    suspend fun refresh(): List<Offer> = withContext(Dispatchers.IO) {
        memory?.let { return@withContext it }
        val list = mutableListOf<Offer>()
        try {
            val req = Request.Builder().url(OFFERS_URL).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val text = resp.body?.string().orEmpty()
                    parse(text)?.let { parsed ->
                        list.addAll(parsed)
                        ServiceLocator.meta.set(CACHE_KEY, text)
                    }
                }
            }
        } catch (_: Exception) { }
        if (list.isEmpty()) parse(ServiceLocator.meta.get(CACHE_KEY))?.let { list.addAll(it) }
        memory = list
        list
    }

    private fun parse(json: String?): List<Offer>? {
        return try {
            val el = JsonParser.parseString(json ?: return null)
            if (!el.isJsonObject) return null
            val arr = el.asJsonObject.getAsJsonArray("offers") ?: return null
            arr.mapNotNull { item ->
                if (!item.isJsonObject) return@mapNotNull null
                val o = item.asJsonObject
                fun s(k: String) = o.get(k)?.takeIf { it.isJsonPrimitive }?.asString
                val id = s("id") ?: return@mapNotNull null
                val hash = s("hash") ?: return@mapNotNull null
                val percent = o.get("percent")?.takeIf { it.isJsonPrimitive }?.asInt ?: return@mapNotNull null
                Offer(
                    id = id,
                    hash = hash.lowercase(),
                    percent = percent.coerceIn(1, 90),
                    starts = s("starts")?.let { parseDay(it, true) },
                    ends = s("ends")?.let { parseDay(it, false) },
                    label = s("label") ?: "کد تخفیف"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDay(raw: String, startOfDay: Boolean): Long? {
        return try {
            val s = raw.trim()
            val h = if (startOfDay) 0 else 23
            val m = if (startOfDay) 0 else 59
            if (s.startsWith("j", ignoreCase = true)) {
                val p = s.substring(1).split("-")
                JalaliConverter.toMillis(p[0].toInt(), p[1].toInt(), p[2].toInt(), h, m)
            } else {
                val p = s.split("-")
                Calendar.getInstance().apply {
                    set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), h, m, if (startOfDay) 0 else 59)
                }.timeInMillis
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun validate(code: String): Offer? {
        if (code.isBlank()) return null
        val h = sha256(normalize(code))
        val now = System.currentTimeMillis()
        return refresh().firstOrNull { o ->
            o.hash == h &&
                    (o.starts == null || now >= o.starts) &&
                    (o.ends == null || now <= o.ends)
        }
    }

    suspend fun apply(offer: Offer) {
        val now = System.currentTimeMillis()
        val until = offer.ends ?: (now + 3 * DateUtils.DAY)
        ServiceLocator.meta.set(APPLIED_PERCENT, offer.percent.toString())
        ServiceLocator.meta.set(APPLIED_UNTIL, until.toString())
        ServiceLocator.meta.set(APPLIED_LABEL, offer.label)
        ServiceLocator.meta.set(APPLIED_ID, offer.id)
    }

    /** ✅ تابع جدید: کد را اعتبارسنجی و اعمال می‌کند، درصد تخفیف را برمی‌گرداند */
    suspend fun applyCode(code: String): Int {
        val offer = validate(code) ?: return 0
        apply(offer)
        return offer.percent
    }

    suspend fun activePercent(): Int {
        val until = ServiceLocator.meta.get(APPLIED_UNTIL)?.toLongOrNull() ?: 0L
        if (System.currentTimeMillis() > until) return 0
        return ServiceLocator.meta.get(APPLIED_PERCENT)?.toIntOrNull()?.coerceIn(0, 90) ?: 0
    }

    suspend fun activeLabel(): String? =
        if (activePercent() > 0) ServiceLocator.meta.get(APPLIED_LABEL) else null

    suspend fun appliedOfferId(): String? =
        if (activePercent() > 0) ServiceLocator.meta.get(APPLIED_ID) else null

    suspend fun clear() {
        ServiceLocator.meta.delete(APPLIED_PERCENT)
        ServiceLocator.meta.delete(APPLIED_UNTIL)
        ServiceLocator.meta.delete(APPLIED_LABEL)
        ServiceLocator.meta.delete(APPLIED_ID)
    }
}