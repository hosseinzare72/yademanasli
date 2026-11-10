// مسیر: app/src/main/java/com/hossein/yademan/services/ApiClient.kt
package com.hossein.yademan.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** خطای منطقی سرور (پاسخ رسید ولی ok=false یا HTTP خطا) */
class ApiException(message: String, val httpCode: Int = -1) : Exception(message)

/**
 * کلاینت REST بک‌اند وردپرس (بدون تغییر در سرور).
 * Base URL: https://kafezare.ir/wp-json/lifeplanner/v1/
 * همه درخواست‌ها POST با بدنه JSON هستند و توکن داخل بدنه ارسال می‌شود.
 */
object ApiClient {
    const val BASE_URL = "https://kafezare.ir/wp-json/lifeplanner/v1/"

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** ارسال POST و برگرداندن JSON پاسخ؛ IOException = مشکل شبکه، ApiException = خطای سرور */
    @Throws(IOException::class, ApiException::class)
    suspend fun post(path: String, body: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .post(body.toString().toRequestBody(JSON_TYPE))
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json: JsonObject? = try {
                val el = JsonParser.parseString(text)
                if (el.isJsonObject) el.asJsonObject else null
            } catch (e: Exception) {
                null
            }
            if (!response.isSuccessful) {
                val msg = json?.str("message") ?: json?.str("error") ?: "خطای سرور (${response.code})"
                throw ApiException(msg, response.code)
            }
            if (json == null) throw ApiException("پاسخ نامعتبر از سرور", response.code)
            if (json.bool("ok") == false) {
                throw ApiException(json.str("message") ?: json.str("error") ?: "درخواست ناموفق بود", response.code)
            }
            json
        }
    }

    suspend fun register(username: String, password: String, deviceId: String): JsonObject =
        post("auth/register", JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
            addProperty("device_id", deviceId)
        })

    suspend fun login(username: String, password: String, deviceId: String): JsonObject =
        post("auth/login", JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
            addProperty("device_id", deviceId)
        })

    suspend fun verify(token: String): JsonObject =
        post("auth/verify", JsonObject().apply { addProperty("token", token) })

    suspend fun push(token: String, deviceId: String, records: JsonArray, deleted: JsonArray, lastSync: String): JsonObject =
        post("sync/push", JsonObject().apply {
            addProperty("token", token)
            addProperty("device_id", deviceId)
            add("records", records)
            add("deleted", deleted)
            addProperty("last_sync", lastSync)
        })

    suspend fun pull(token: String, deviceId: String, lastSync: String): JsonObject =
        post("sync/pull", JsonObject().apply {
            addProperty("token", token)
            addProperty("device_id", deviceId)
            addProperty("last_sync", lastSync)
        })

    suspend fun resolve(token: String, conflicts: JsonArray): JsonObject =
        post("sync/resolve", JsonObject().apply {
            addProperty("token", token)
            add("conflicts", conflicts)
        })
}
