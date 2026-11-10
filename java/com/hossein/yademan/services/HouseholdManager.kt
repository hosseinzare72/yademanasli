// مسیر: app/src/main/java/com/hossein/yademan/services/HouseholdManager.kt
package com.hossein.yademan.services

import com.hossein.yademan.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** مدیریت گروه دونفره (من و همسر) - سینک مشترک */
object HouseholdManager {

    private const val BASE = "https://kafezare.ir/wp-json/lifeplanner/v1/"

    data class Member(
        val id: Int,
        val username: String,
        val displayName: String,
        val phone: String?,
        val role: String
    )

    data class Status(
        val active: Boolean,
        val code: String?,
        val myRole: String?,
        val members: List<Member>
    ) {
        val partnerName: String?
            get() = members.firstOrNull { it.role != myRole }?.displayName
    }

    private fun authBody(): JSONObject {
        val jo = JSONObject()
        runCatching { ServiceLocator.auth.username }?.getOrNull()?.takeIf { it.isNotBlank() }?.let { jo.put("username", it) }
        runCatching { ServiceLocator.auth.token }?.getOrNull()?.takeIf { it.isNotBlank() }?.let { jo.put("token", it) }
        return jo
    }

    private suspend fun post(path: String, body: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val c = URL(BASE + path).openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.connectTimeout = 15000; c.readTimeout = 15000
            c.doOutput = true; c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
            stream?.bufferedReader()?.readText()?.let { JSONObject(it) }
        } catch (_: Exception) { null }
    }

    suspend fun status(): Status {
        val auth = authBody(); if (auth.length() == 0) return Status(false, null, null, emptyList())
        val jo = post("household/status", auth.toString()) ?: return Status(false, null, null, emptyList())
        if (!jo.optBoolean("active")) return Status(false, null, null, emptyList())
        val arr = jo.optJSONArray("members") ?: return Status(false, null, null, emptyList())
        val list = mutableListOf<Member>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Member(o.optInt("id"), o.optString("username"), o.optString("display_name"),
                o.optString("phone"), o.optString("role")))
        }
        return Status(true, jo.optString("code"), jo.optString("my_role"), list)
    }

    suspend fun create(): Result<String> {
        val auth = authBody(); if (auth.length() == 0) return Result.failure(Exception("وارد حساب شوید"))
        val jo = post("household/create", auth.toString()) ?: return Result.failure(Exception("ارتباط با سرور ناموفق"))
        return if (jo.optBoolean("ok")) Result.success(jo.optString("code"))
        else Result.failure(Exception(jo.optString("message", "خطا")))
    }

    suspend fun join(code: String): Result<String> {
        val auth = authBody().apply { put("code", code.trim().uppercase()) }
        val jo = post("household/join", auth.toString()) ?: return Result.failure(Exception("ارتباط ناموفق"))
        return if (jo.optBoolean("ok")) Result.success(jo.optString("partner_name", "همسر"))
        else Result.failure(Exception(jo.optString("message", "خطا")))
    }

    suspend fun leave(): Boolean {
        val auth = authBody()
        val jo = post("household/leave", auth.toString()) ?: return false
        return jo.optBoolean("ok")
    }
}