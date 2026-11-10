// مسیر: app/src/main/java/com/hossein/yademan/services/BackupManager.kt
package com.hossein.yademan.services

import android.content.Context
import com.hossein.yademan.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {
    private const val MAGIC = "YDBK"
    private const val VERSION = 3  // نسخه ۳: رمز عبور کاربر
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_SIZE_BITS = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    private fun dbFile(context: Context): File = context.getDatabasePath("yademan.db")

    /** export با رمز عبور کاربر */
    suspend fun export(context: Context, out: OutputStream, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (password.length < 4) return@withContext Result.failure(Exception("رمز حداقل ۴ کاراکتر"))

            ServiceLocator.db.close()
            val src = dbFile(context)
            if (!src.exists()) return@withContext Result.failure(Exception("دیتابیس یافت نشد"))

            val plain = src.readBytes()
            val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
            val key = deriveKey(password, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val ct = cipher.doFinal(plain)

            out.write(MAGIC.toByteArray(Charsets.US_ASCII))
            out.write(byteArrayOf(VERSION.toByte(), 0, 0, 0))
            out.write(salt)
            out.write(iv)
            out.write(ct)
            out.flush()
            Result.success(plain.size)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            ServiceLocator.db.openHelper.writableDatabase
        }
    }

    /** import با رمز عبور کاربر */
    suspend fun restore(context: Context, input: InputStream, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val all = input.readBytes()
            if (all.size < 36 || String(all.copyOfRange(0, 4), Charsets.US_ASCII) != MAGIC)
                return@withContext Result.failure(Exception("فایل نامعتبر است"))

            val version = all[4].toInt()
            if (version != VERSION) {
                return@withContext Result.failure(Exception("این فایل با نسخه قدیمی ساخته شده. لطفاً بکاپ جدید بگیرید."))
            }

            val salt = all.copyOfRange(8, 8 + SALT_SIZE)
            val iv = all.copyOfRange(8 + SALT_SIZE, 8 + SALT_SIZE + IV_SIZE)
            val ct = all.copyOfRange(8 + SALT_SIZE + IV_SIZE, all.size)

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

            val plain = try {
                cipher.doFinal(ct)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("رمز عبور اشتباه است"))
            }

            ServiceLocator.db.close()
            val dst = dbFile(context)
            dst.writeBytes(plain)
            ServiceLocator.db.openHelper.writableDatabase
            AlarmScheduler.rescheduleAll(context)
            Result.success(plain.size)
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }
}