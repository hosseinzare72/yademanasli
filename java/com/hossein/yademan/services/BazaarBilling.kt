// مسیر: app/src/main/java/com/hossein/yademan/services/BazaarBilling.kt
package com.hossein.yademan.services

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.android.vending.billing.IInAppBillingService
import kotlinx.coroutines.delay

/** ارتباط با سرویس پرداخت درون‌برنامه‌ای کافه‌بازار (مطابق مستندات رسمی) */
object BazaarBilling {

    private const val TAG = "YademanIAB"
    private const val BAZAAR_PKG = "com.farsitel.bazaar"
    /** اکشن رسمی سرویس پرداخت بازار */
    private const val BIND_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
    /** اکشن قدیمی (فقط برای fallback) */
    private const val BIND_ACTION_LEGACY = "com.farsitel.bazaar.inappbilling.service.BIND"
    private const val API = 3

    private var service: IInAppBillingService? = null
    private var bound = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, s: IBinder?) {
            Log.w(TAG, "service CONNECTED")
            service = IInAppBillingService.Stub.asInterface(s)
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "service DISCONNECTED")
            service = null
            bound = false
        }
    }

    fun bind(context: Context) {
        if (bound && service != null) return
        val intents = listOf(
            Intent(BIND_ACTION).setPackage(BAZAAR_PKG),
            Intent(BIND_ACTION_LEGACY).setPackage(BAZAAR_PKG)
        )
        for (intent in intents) {
            val ok = runCatching {
                context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            }.getOrElse { false }
            Log.w(TAG, "bind ${intent.action} -> $ok")
            if (ok) {
                bound = true
                return
            }
        }
    }

    /** تا برقراری اتصال صبر می‌کند (حداکثر ۶ ثانیه) */
    suspend fun awaitConnection(timeoutMs: Long = 6000): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (service != null) return true
            delay(200)
        }
        Log.w(TAG, "awaitConnection TIMEOUT | bound=$bound")
        return false
    }

    /** آیا کافه‌بازار روی دستگاه نصب است؟ */
    fun isBazaarInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(BAZAAR_PKG, 0)
        true
    }.getOrDefault(false)

    fun isBillingSupported(context: Context): Boolean = runCatching {
        service?.isBillingSupported(API, context.packageName, "inapp") == 0
    }.getOrDefault(false)

    fun unbind(context: Context) {
        runCatching { if (bound) context.unbindService(conn) }
        bound = false
        service = null
    }

    /** شروع خرید */
    fun buy(
        context: Context,
        sku: String,
        onLaunch: (IntentSenderRequest) -> Unit,
        onError: (String) -> Unit
    ) {
        val svc = service
        if (svc == null) {
            onError("اتصال به بازار برقرار نشد")
            return
        }
        try {
            val support = svc.isBillingSupported(API, context.packageName, "inapp")
            Log.w(TAG, "isBillingSupported: $support")
            if (support != 0) {
                onError("پرداخت درون‌برنامه‌ای فعال نیست (کد $support)")
                return
            }
            val bundle: Bundle = svc.getBuyIntent(API, context.packageName, sku, "inapp", "")
            val code = bundle.getInt("RESPONSE_CODE")
            val pending = bundle.getParcelable<PendingIntent>("BUY_INTENT")
            Log.w(TAG, "getBuyIntent: code=$code pending=${pending != null}")
            if (code == 0 && pending != null) {
                onLaunch(IntentSenderRequest.Builder(pending.intentSender).build())
            } else {
                onError("بازار خطا داد (کد $code)؛ دوباره تلاش کنید")
            }
        } catch (e: Exception) {
            onError("خطا در شروع خرید: ${e.message}")
        }
    }

    /** مصرف خرید */
    fun consume(context: Context, token: String) {
        runCatching { service?.consumePurchase(API, context.packageName, token) }
    }
}