// مسیر: app/src/main/java/com/hossein/yademan/services/AuthManager.kt
package com.hossein.yademan.services

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * نگهداری امن توکن با EncryptedSharedPreferences.
 * اگر Keystore روی دستگاهی خراب باشد (مشکل شناخته‌شده بعضی گوشی‌ها)، به‌جای کرش
 * به SharedPreferences معمولی برمی‌گردیم تا اپ قابل استفاده بماند.
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    private fun createPrefs(context: Context): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "yademan_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("yademan_auth_fallback", Context.MODE_PRIVATE)
    }

    /** شناسه یکتای دستگاه (یک‌بار ساخته و ذخیره می‌شود) */
    val deviceId: String
        @Synchronized get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val id = "android-" + UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            return id
        }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val userId: String? get() = prefs.getString(KEY_USER_ID, null)
    val username: String? get() = prefs.getString(KEY_USERNAME, null)

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()

    fun saveAuth(token: String, userId: String?, username: String?) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            if (userId != null) putString(KEY_USER_ID, userId)
            if (username != null) putString(KEY_USERNAME, username)
        }.apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
