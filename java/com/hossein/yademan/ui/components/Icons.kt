// مسیر: app/src/main/java/com/hossein/yademan/ui/components/Icons.kt
package com.hossein.yademan.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * آیکن‌های خطی ۲۴×۲۴ با استروک 1.8 و سر/اتصال گرد.
 * pathها عیناً از SVGهای source-html/home.html مخزن کپی شده‌اند
 * (rect و circle به path تبدیل شده‌اند). رنگ با tint آیکن تعیین می‌شود.
 */
object YIcons {

    private fun icon(name: String, vararg paths: String, stroke: Float = 1.8f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            for (d in paths) {
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    /** معادل <rect x y width height rx> */
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
        "M${x + r},${y}H${x + w - r}A$r,$r 0 0 1 ${x + w},${y + r}" +
                "V${y + h - r}A$r,$r 0 0 1 ${x + w - r},${y + h}" +
                "H${x + r}A$r,$r 0 0 1 $x,${y + h - r}" +
                "V${y + r}A$r,$r 0 0 1 ${x + r},${y}Z"

    /** معادل <circle cx cy r> */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r},${cy}A$r,$r 0 1 0 ${cx + r},${cy}A$r,$r 0 1 0 ${cx - r},${cy}Z"

    // ---------- نوار ناوبری ----------
    val Home: ImageVector by lazy { icon("home", "M4 10.5L12 3.5l8 7", "M6 9v11.5h12V9") }
    val Bell: ImageVector by lazy {
        icon("bell", "M6 8.5a6 6 0 0 1 12 0c0 6.5 2.5 7.5 2.5 7.5h-17S4 15 4 8.5", "M10.3 20a2 2 0 0 0 3.4 0")
    }
    val Cart: ImageVector by lazy {
        icon("cart", circle(9f, 20f, 1.4f), circle(17.5f, 20f, 1.4f), "M3 4h2.2l2.5 11.5h10.6L21 8H7")
    }
    val User: ImageVector by lazy {
        icon("user", circle(12f, 8f, 4f), "M4.5 20.5c1.4-3.8 4.6-5.3 7.5-5.3s6.1 1.5 7.5 5.3")
    }
    val Plus: ImageVector by lazy { icon("plus", "M12 5v14M5 12h14") }
    val PlusBold: ImageVector by lazy { icon("plus_bold", "M12 5v14M5 12h14", stroke = 2.4f) }

    // ---------- هاب خدمات ----------
    val Installment: ImageVector by lazy {
        icon("installment", rect(3f, 6.5f, 18f, 12f, 2.5f), circle(12f, 12.5f, 2.6f), "M6.5 10v.01M17.5 15v.01")
    }
    val Check: ImageVector by lazy { icon("check", "M6 2.5h9l4.5 4.5v14.5H6z", "M9 13l2.2 2.2L15.5 11") }
    /** اشتراک: کارت با فلش تمدید */
    val Subscription: ImageVector by lazy {
        icon("subscription", rect(3f, 5.5f, 18f, 13f, 2.5f), "M3 9.5h18", "M14.5 15.5a2.5 2.5 0 1 0 .8-1.8", "M15.6 12.4v1.5h-1.5")
    }
    val Bill: ImageVector by lazy { icon("bill", "M6 2.5h12v19l-3-2-3 2-3-2-3 2z", "M13 7.5l-2.8 4.5h3.6L11 16.5") }
    val Building: ImageVector by lazy {
        icon("building", rect(5f, 3f, 14f, 18f, 2f), "M9 7.5h2M13 7.5h2M9 11.5h2M13 11.5h2M10 21v-4h4v4")
    }
    val Medicine: ImageVector by lazy {
        icon("medicine", "M10.5 20.5l-7-7a4.95 4.95 0 1 1 7-7l7 7a4.95 4.95 0 1 1-7 7Z", "M7 7l7 7")
    }
    val Meeting: ImageVector by lazy { icon("meeting", circle(12f, 12f, 8.5f), "M12 7.5V12l3.2 2") }
    val Travel: ImageVector by lazy { icon("travel", "M21.5 2.5L11 13", "M21.5 2.5l-6.8 19-3.7-8.5-8.5-3.7z") }
    val Note: ImageVector by lazy { icon("note", rect(5f, 3f, 14f, 18f, 2.5f), "M9 8.5h6M9 12.5h6M9 16.5h3.5") }
    val Insurance: ImageVector by lazy {
        icon("insurance", "M12 3l8 3v5.5c0 5-3.4 8.2-8 9.5-4.6-1.3-8-4.5-8-9.5V6z", "M9 12l2.2 2.2L15.5 10")
    }
    val Car: ImageVector by lazy {
        icon(
            "car",
            "M4 16.5l1.8-5.2a2 2 0 0 1 1.9-1.3h8.6a2 2 0 0 1 1.9 1.3l1.8 5.2",
            rect(3f, 16.5f, 18f, 4.5f, 1.5f),
            "M7 21v1M17 21v1"
        )
    }
    val Salary: ImageVector by lazy {
        icon(
            "salary",
            rect(3f, 6f, 18f, 13f, 3f),
            "M3 9.5V8a2 2 0 0 1 2-2h13",
            "M15.5 12.5h5.5v4h-5.5a2 2 0 0 1 0-4z"
        )
    }
    val PocketMoney: ImageVector by lazy { icon("pocket_money", circle(9f, 9f, 5.5f), "M14.9 7.1a5.5 5.5 0 1 1-7.8 7.8") }
    val Birthday: ImageVector by lazy {
        icon(
            "birthday",
            "M4 21h16",
            "M5 21v-5.5A3.5 3.5 0 0 1 8.5 12h7a3.5 3.5 0 0 1 3.5 3.5V21",
            "M12 12V9.5",
            "M12 6.5a1.6 1.6 0 0 0 1.6-1.6C13.6 3.6 12 2.5 12 2.5s-1.6 1.1-1.6 2.4A1.6 1.6 0 0 0 12 6.5z"
        )
    }
    val Anniversary: ImageVector by lazy {
        icon(
            "anniversary",
            "M12 20s-7.2-4.6-9.2-9.1C1.5 7.9 3.6 4 7.3 4c2 0 3.6 1 4.7 2.6C13.1 5 14.7 4 16.7 4c3.7 0 5.8 3.9 4.5 6.9C19.2 15.4 12 20 12 20z"
        )
    }

    // ---------- عمومی (موقت؛ در فاز ۳ با SVG صفحات دیگر مخزن تطبیق داده می‌شوند) ----------
    val Close: ImageVector by lazy { icon("close", "M6 6l12 12M18 6L6 18") }
    val CheckMark: ImageVector by lazy { icon("check_mark", "M5 12.5l4.5 4.5L19 7.5") }
    val Search: ImageVector by lazy { icon("search", circle(11f, 11f, 7f), "M20 20l-3.5-3.5") }
    val Trash: ImageVector by lazy { icon("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3") }
    val Filter: ImageVector by lazy { icon("filter", "M4 5h16l-6 8v6l-4-2v-4z") }
    val ChevronLeft: ImageVector by lazy { icon("chevron_left", "M15 5l-7 7 7 7") }
    val ChevronRight: ImageVector by lazy { icon("chevron_right", "M9 5l7 7-7 7") }
    val Settings: ImageVector by lazy {
        icon("settings", "M4 7h9M17 7h3M4 17h3M11 17h9", circle(15f, 7f, 2f), circle(9f, 17f, 2f))
    }
    val Moon: ImageVector by lazy { icon("moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z") }
    val Sound: ImageVector by lazy {
        icon("sound", "M4 9.5h3.5L12 5.5v13l-4.5-4H4z", "M16 9a4 4 0 0 1 0 6", "M18.5 6.5a7.5 7.5 0 0 1 0 11")
    }
    val Sparkle: ImageVector by lazy {
        icon(
            "sparkle",
            "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z",
            "M19 16l.7 1.8 1.8.7-1.8.7L19 21l-.7-1.8-1.8-.7 1.8-.7z"
        )
    }
    val Pin: ImageVector by lazy { icon("pin", "M9 3.5h6l-1 5 3.5 3.5h-11L10 8.5z", "M12 12v8.5") }
    val Edit: ImageVector by lazy { icon("edit", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4") }
    val Logout: ImageVector by lazy {
        icon("logout", "M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4", "M10 16l-4-4 4-4", "M6 12h9")
    }
    val Sync: ImageVector by lazy {
        icon("sync", "M20 11a8 8 0 0 0-14.5-4.5L4 8", "M4 4v4h4", "M4 13a8 8 0 0 0 14.5 4.5L20 16", "M20 20v-4h-4")
    }
    val Battery: ImageVector by lazy { icon("battery", rect(3f, 7f, 16f, 10f, 2.5f), "M21 10.5v3", "M7 10v4M11 10v4") }
    val Calendar: ImageVector by lazy {
        icon("calendar", rect(3.5f, 5f, 17f, 15.5f, 3f), "M3.5 10h17", "M8 3v4M16 3v4")
    }
    val Clock: ImageVector by lazy { icon("clock", circle(12f, 12f, 8.5f), "M12 7.5V12l3.2 2") }
    val Minus: ImageVector by lazy { icon("minus", "M5 12h14") }
    val Location: ImageVector by lazy {
        icon("location", "M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z", circle(12f, 10f, 2.3f))
    }
    val Other: ImageVector by lazy { icon("other", circle(6f, 12f, 1.2f), circle(12f, 12f, 1.2f), circle(18f, 12f, 1.2f)) }

    // ✅ آیکون قفل برای تغییر رمز عبور
    val Lock: ImageVector by lazy {
        icon("lock", rect(5f, 10f, 14f, 10f, 2f), "M8 10V7.5a4 4 0 0 1 8 0V10")
    }

    // ✅ آیکون اجاره (خانه با کلید)
    val Rent: ImageVector by lazy {
        icon(
            "rent",
            "M3 12l9-9 9 9",
            "M5 10v10h14V10",
            "M9 20v-5h6v5",
            "M15 14a2 2 0 1 1-2-2 2 2 0 0 1 2 2z"
        )
    }
}