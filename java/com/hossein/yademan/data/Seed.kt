// مسیر: app/src/main/java/com/hossein/yademan/data/Seed.kt
package com.hossein.yademan.data

import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.utils.DateUtils

/**
 * داده دموی اولین اجرا (با گارد sync_meta["seeded_v1"]).
 * ۶ دسته، ۳ قسط/چک، ۳ قرار، ۵ داروی روزانه (فقط متفورمین امروز مصرف شده)، ۱۲ قلم خرید، ۶ یادداشت، ۱ یادآور سایر.
 */
object Seed {

    suspend fun seedIfNeeded() {
        val meta = ServiceLocator.meta
        if (meta.get(MetaKeys.SEEDED) != null) {
            ensureCategories()
            return
        }
        ServiceLocator.categories.insertSeed(categories())
        ServiceLocator.reminders.insertSeed(reminders())
        meta.set(MetaKeys.SEEDED, "true")
        if (meta.get(MetaKeys.DISPLAY_NAME) == null) meta.set(MetaKeys.DISPLAY_NAME, "حسین")
    }

    /** دسته‌های جدید (مثل «اشتراک‌ها» در v1.2) را برای نصب‌های قبلی اضافه می‌کند */
    private suspend fun ensureCategories() {
        val existing = ServiceLocator.categories.getAll().map { it.id }.toSet()
        val missing = categories().filter { it.id !in existing }
        if (missing.isNotEmpty()) ServiceLocator.categories.insertSeed(missing)
    }

    private fun categories(): List<Category> {
        val now = DateUtils.nowIso()
        val device = ServiceLocator.auth.deviceId
        fun c(id: String, name: String, color: String, order: Int) =
            Category(id, name, color, order, now, now, device, 1, SyncStatus.LOCAL_NEW)
        return listOf(
            c(ReminderType.CAT_INSTALLMENT, "اقساط", "#E8B84B", 0),
            c(ReminderType.CAT_CHECK, "چک‌ها", "#9D8CFF", 1),
            c(ReminderType.CAT_MEETING, "قرارها", "#4CC3FF", 2),
            c(ReminderType.CAT_MEDICINE, "داروها", "#FF7A9E", 3),
            c(ReminderType.CAT_SHOPPING, "خرید", "#3ED598", 4),
            c(ReminderType.CAT_NOTE, "یادداشت‌ها", "#FFB84D", 5),
            c(ReminderType.CAT_SUBSCRIPTION, "اشتراک‌ها", "#FF8F5C", 6)
        )
    }

    private fun reminders(): List<Reminder> {
        val nowMs = System.currentTimeMillis()
        val nowIso = DateUtils.nowIso()
        val device = ServiceLocator.auth.deviceId
        val today = DateUtils.todayStart()
        fun at(dayOffset: Int, h: Int, m: Int) = DateUtils.atTime(DateUtils.addDays(today, dayOffset), h, m)
        fun iso(ms: Long) = DateUtils.millisToIso(ms)
        val list = mutableListOf<Reminder>()
        var order = 0

        fun add(
            id: String,
            type: String,
            title: String,
            due: Long,
            repeat: String = "none",
            amount: Long = 0,
            offset: Int = 0,
            priority: Int = 1,
            alarm: Boolean = true,
            done: Boolean = false,
            note: String = "",
            extras: Map<String, Any?> = emptyMap(),
            created: Long = nowMs,
            color: String? = null
        ) {
            list.add(
                Reminder(
                    id = id,
                    title = title,
                    note = note,
                    dueAt = iso(due),
                    repeatRule = repeat,
                    priority = priority,
                    categoryId = ReminderType.categoryId(type),
                    isDone = done,
                    doneAt = if (done) nowIso else null,
                    alarmEnabled = alarm,
                    alarmOffsetMinutes = offset,
                    color = color ?: ReminderType.colorHex(type),
                    sortOrder = order++,
                    amountToman = amount,
                    metaJson = ReminderMeta.build(type, extras),
                    createdAt = iso(created),
                    updatedAt = iso(created),
                    deviceId = device,
                    syncStatus = SyncStatus.LOCAL_NEW
                )
            )
        }

        // ---------- اقساط و چک‌ها ----------
        add("seed_inst_home", ReminderType.INSTALLMENT, "قسط وام مسکن", at(3, 10, 0), "monthly",
            4_500_000, offset = 1440, priority = 2, extras = mapOf("bank" to "بانک ملت"))
        add("seed_inst_car", ReminderType.INSTALLMENT, "قسط خودرو", at(1, 10, 0), "monthly",
            2_800_000, offset = 1440, priority = 2, extras = mapOf("bank" to "بانک سامان"))
        add("seed_check_rent", ReminderType.CHECK, "چک اجاره", at(9, 12, 0), "none",
            12_000_000, offset = 1440, priority = 2, extras = mapOf("bank" to "بانک ملی", "check_number" to "۴۸۲۱۰۶"))

        // ---------- قرارها ----------
        add("seed_meet_team", ReminderType.MEETING, "جلسه با تیم طراحی", at(0, 11, 0), offset = 60,
            extras = mapOf("location" to "دفتر مرکزی"))
        add("seed_meet_dentist", ReminderType.MEETING, "دندانپزشکی", at(0, 17, 30), offset = 60,
            priority = 2, extras = mapOf("location" to "کلینیک آرمان"))
        add("seed_meet_birthday", ReminderType.MEETING, "جشن تولد مریم", at(1, 20, 0), offset = 1440,
            extras = mapOf("location" to "خانه مادربزرگ"))

        // ---------- داروها (روزانه؛ از یک ماه پیش شروع شده‌اند تا آمار هفته معنا داشته باشد) ----------
        val pastKeys = (1..6).map { DateUtils.dayKey(DateUtils.addDays(today, -it)) }
        val todayKey = DateUtils.dayKey(today)
        fun medDates(takenToday: Boolean, skipDay: Int?): List<String> {
            val keys = pastKeys.filterIndexed { i, _ -> skipDay == null || i + 1 != skipDay }.toMutableList()
            if (takenToday) keys.add(todayKey)
            return keys
        }
        val medStart = -30
        add("seed_med_metformin", ReminderType.MEDICINE, "متفورمین", at(medStart, 8, 0), "daily",
            offset = 0, extras = mapOf("dose" to "۵۰۰ میلی‌گرم • بعد از صبحانه", "done_dates" to medDates(true, null)))
        add("seed_med_aspirin", ReminderType.MEDICINE, "آسپرین", at(medStart, 8, 30), "daily",
            extras = mapOf("dose" to "۸۰ میلی‌گرم • یک عدد", "done_dates" to medDates(false, null)))
        add("seed_med_vitd", ReminderType.MEDICINE, "ویتامین D", at(medStart, 13, 0), "daily",
            extras = mapOf("dose" to "۱۰۰۰ واحد • با ناهار", "done_dates" to medDates(false, 6)))
        add("seed_med_omega", ReminderType.MEDICINE, "امگا ۳", at(medStart, 14, 0), "daily",
            extras = mapOf("dose" to "یک کپسول", "done_dates" to medDates(false, null)))
        add("seed_med_atorva", ReminderType.MEDICINE, "آتورواستاتین", at(medStart, 21, 0), "daily",
            extras = mapOf("dose" to "۲۰ میلی‌گرم • قبل از خواب", "done_dates" to medDates(false, 5)))

        // ---------- لیست خرید ----------
        data class Item(val title: String, val group: String, val qty: String, val price: Long, val done: Boolean)
        listOf(
            Item("شیر", "لبنیات", "۲ بطری", 64_000, true),
            Item("ماست", "لبنیات", "۱ سطل", 85_000, false),
            Item("پنیر", "لبنیات", "۱ بسته", 120_000, true),
            Item("نان سنگک", "نانوایی", "۳ عدد", 45_000, true),
            Item("گوجه‌فرنگی", "میوه و سبزی", "۲ کیلو", 70_000, false),
            Item("خیار", "میوه و سبزی", "۱ کیلو", 35_000, true),
            Item("سیب", "میوه و سبزی", "۲ کیلو", 110_000, false),
            Item("مرغ", "پروتئین", "۱ کیلو", 180_000, false),
            Item("تخم‌مرغ", "پروتئین", "۱ شانه", 240_000, false),
            Item("برنج", "خواربار", "۵ کیلو", 850_000, false),
            Item("روغن", "خواربار", "۱ بطری", 190_000, true),
            Item("مایع ظرفشویی", "بهداشتی", "۱ عدد", 95_000, false)
        ).forEachIndexed { i, item ->
            add("seed_shop_$i", ReminderType.SHOPPING, item.title, nowMs, amount = item.price, alarm = false,
                done = item.done, extras = mapOf("group" to item.group, "qty" to item.qty))
        }

        // ---------- یادداشت‌ها ----------
        fun items(vararg pairs: Pair<String, Boolean>) = pairs.toList()
        data class NoteSeed(val title: String, val color: String, val pinned: Boolean, val agoMinutes: Long,
                            val text: String, val items: List<Pair<String, Boolean>>)
        listOf(
            NoteSeed("برنامه سفر شمال", "#4CC3FF", true, 120, "",
                items("رزرو ویلا" to true, "چک کردن لاستیک‌ها" to true, "خرید تنقلات" to false, "شارژ پاوربانک" to false)),
            NoteSeed("کارهای خانه", "#E8B84B", true, 60 * 26, "",
                items("تعویض لامپ آشپزخانه" to false, "تماس با لوله‌کش" to true, "پرداخت قبض گاز" to false)),
            NoteSeed("ایده‌های تولد مریم", "#FF7A9E", false, 60 * 24 * 3, "",
                items("کیک شکلاتی" to true, "بادکنک طلایی" to false, "کارت دست‌ساز" to false)),
            NoteSeed("کتاب‌هایی که باید بخوانم", "#9D8CFF", false, 60 * 24 * 6, "",
                items("صد سال تنهایی" to true, "بوف کور" to false, "عادت‌های اتمی" to false)),
            NoteSeed("یادداشت جلسه هفتگی", "#3ED598", false, 60 * 24 * 9, "",
                items("بررسی بودجه" to true, "زمان‌بندی کمپین" to true)),
            NoteSeed("رمز وای‌فای مهمان", "#FFB84D", false, 60 * 24 * 20,
                "نام شبکه: Yademan-Guest\nرمز در کشوی میز", emptyList())
        ).forEachIndexed { i, n ->
            val created = nowMs - n.agoMinutes * DateUtils.MINUTE
            add("seed_note_$i", ReminderType.NOTE, n.title, created, alarm = false, note = n.text,
                extras = mapOf("pinned" to n.pinned), created = created, color = n.color)
            val last = list.removeAt(list.lastIndex)
            list.add(ReminderMeta.withNoteItems(last, n.items.map { NoteItem(it.first, it.second) }))
        }

        // ---------- سایر ----------
        add("seed_other_call", ReminderType.OTHER, "تماس با مشاور مالیاتی", at(0, 16, 0), offset = 15,
            extras = mapOf("location" to "تلفنی"))

        return list
    }
}
