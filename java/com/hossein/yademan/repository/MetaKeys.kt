// مسیر: app/src/main/java/com/hossein/yademan/repository/MetaKeys.kt
package com.hossein.yademan.repository

object MetaKeys {
    // سینک
    const val LAST_SYNC = "last_sync"
    const val LAST_SYNC_LOCAL = "last_sync_local"
    const val LAST_SYNC_ERROR = "last_sync_error"

    // Seed (داده‌های اولیه)
    const val SEEDED = "seeded"
    const val DISPLAY_NAME = "display_name"
    const val NOTIF_PERMISSION_ASKED = "notif_permission_asked"

    // تنظیمات کاربر
    const val DATE_FORMAT = "date_format"
    const val CUSTOM_HUBS = "custom_hubs"

    // اعلان‌ها
    const val FIRED_PREFIX = "fired_"
    const val NOTIF_SMART = "notif_smart"
    const val NOTIF_SOUND = "notif_sound"
    const val NOTIF_DND = "notif_dnd"
}