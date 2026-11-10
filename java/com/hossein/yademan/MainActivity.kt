// مسیر: app/src/main/java/com/hossein/yademan/MainActivity.kt
package com.hossein.yademan

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.ui.navigation.YademanNavGraph
import com.hossein.yademan.ui.theme.YademanTheme

class MainActivity : ComponentActivity() {

    /** مسیری که باید باز شود (مثلاً با لمس نوتیفیکیشن) */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // نوار وضعیت و ناوبری شفاف با آیکن‌های روشن (تم تیره)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // فقط در اولین ساخت؛ با چرخش صفحه دوباره به اعلان‌ها پرت نشویم
        if (savedInstanceState == null) {
            pendingRoute.value = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_ROUTE)
        }
        setContent {
            YademanTheme {
                // راست‌چین کامل در ریشه درخت Compose
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    YademanNavGraph(
                        pendingRoute = pendingRoute.value,
                        onRouteConsumed = { pendingRoute.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_OPEN_ROUTE)?.let { pendingRoute.value = it }
    }
}
