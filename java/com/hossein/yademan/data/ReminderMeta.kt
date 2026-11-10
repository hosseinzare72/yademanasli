// مسیر: app/src/main/java/com/hossein/yademan/data/ReminderMeta.kt
package com.hossein.yademan.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hossein.yademan.utils.DateUtils

object ReminderType {
    const val INSTALLMENT = "installment"
    const val CHECK = "check"
    const val SUBSCRIPTION = "subscription"
    const val MEETING = "meeting"
    const val MEDICINE = "medicine"
    const val SHOPPING = "shopping"
    const val NOTE = "note"
    const val OTHER = "other"
    const val TRUST = "trust"

    val TRUST_KINDS = listOf("چک امانی", "سفته امانی", "سند/مدرک", "ضمانت دیگر")
    const val CAT_INSTALLMENT = "cat_installment"
    const val CAT_CHECK = "cat_check"
    const val CAT_SUBSCRIPTION = "cat_subscription"
    const val CAT_MEETING = "cat_meeting"
    const val CAT_MEDICINE = "cat_medicine"
    const val CAT_SHOPPING = "cat_shopping"
    const val CAT_NOTE = "cat_note"

    fun categoryId(type: String): String? = when (type) {
        INSTALLMENT -> CAT_INSTALLMENT
        CHECK -> CAT_CHECK
        SUBSCRIPTION -> CAT_SUBSCRIPTION
        MEETING -> CAT_MEETING
        MEDICINE -> CAT_MEDICINE
        SHOPPING -> CAT_SHOPPING
        NOTE -> CAT_NOTE
        else -> null
    }

    fun fromCategory(categoryId: String?): String = when (categoryId) {
        CAT_INSTALLMENT -> INSTALLMENT
        CAT_CHECK -> CHECK
        CAT_SUBSCRIPTION -> SUBSCRIPTION
        CAT_MEETING -> MEETING
        CAT_MEDICINE -> MEDICINE
        CAT_SHOPPING -> SHOPPING
        CAT_NOTE -> NOTE
        else -> OTHER
    }

    fun colorHex(type: String): String = when (type) {
        INSTALLMENT -> "#E8B84B"
        CHECK -> "#9D8CFF"
        SUBSCRIPTION -> "#FF8F5C"
        MEETING -> "#4CC3FF"
        MEDICINE -> "#FF7A9E"
        SHOPPING -> "#3ED598"
        NOTE -> "#FFB84D"
        TRUST -> "#7FB5FF"
        else -> "#D9B45B"
    }

    fun labelFa(type: String): String = when (type) {
        INSTALLMENT -> "قسط"
        CHECK -> "چک"
        SUBSCRIPTION -> "اشتراک"
        MEETING -> "قرار"
        MEDICINE -> "دارو"
        SHOPPING -> "خرید"
        NOTE -> "یادداشت"
        TRUST -> "امانت"
        else -> "سایر"
    }
}

data class NoteItem(val text: String, val done: Boolean)

object ReminderMeta {
    private const val MAX_DONE_DATES = 120

    internal fun parse(json: String?): JsonObject = try {
        val el = JsonParser.parseString(json ?: "{}")
        if (el.isJsonObject) el.asJsonObject else JsonObject()
    } catch (e: Exception) {
        JsonObject()
    }

    private val readCache = java.util.concurrent.ConcurrentHashMap<String, JsonObject>()
    private val doneCache = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()
    private const val CACHE_LIMIT = 3000

    private fun read(json: String?): JsonObject {
        val key = json ?: "{}"
        readCache[key]?.let { return it }
        val o = parse(key)
        if (readCache.size > CACHE_LIMIT) readCache.clear()
        readCache[key] = o
        return o
    }

    fun type(r: Reminder): String =
        str(r, "type")?.takeIf { it.isNotBlank() }?.trim() ?: ReminderType.fromCategory(r.categoryId)

    fun str(r: Reminder, key: String): String? {
        val el = read(r.metaJson).get(key) ?: return null
        return if (el.isJsonPrimitive) el.asString else null
    }

    fun bool(r: Reminder, key: String): Boolean {
        val el = read(r.metaJson).get(key) ?: return false
        return try { el.isJsonPrimitive && el.asBoolean } catch (e: Exception) { false }
    }

    fun with(r: Reminder, key: String, value: Any?): Reminder {
        val o = parse(r.metaJson)
        when (value) {
            null -> o.remove(key)
            is String -> o.addProperty(key, value)
            is Boolean -> o.addProperty(key, value)
            is Number -> o.addProperty(key, value)
            else -> o.addProperty(key, value.toString())
        }
        return r.copy(metaJson = o.toString())
    }

    fun int(r: Reminder, key: String): Int? {
        val el = read(r.metaJson).get(key) ?: return null
        return try { if (el.isJsonPrimitive) el.asString.trim().toIntOrNull() else null } catch (e: Exception) { null }
    }

    fun doneDates(r: Reminder): Set<String> {
        doneCache[r.metaJson]?.let { return it }
        val arr = read(r.metaJson).get("done_dates")
        val set: Set<String> = if (arr == null || !arr.isJsonArray) emptySet()
        else arr.asJsonArray.mapNotNull { if (it.isJsonPrimitive) it.asString else null }.toHashSet()
        if (doneCache.size > CACHE_LIMIT) doneCache.clear()
        doneCache[r.metaJson] = set
        return set
    }

    fun totalCount(r: Reminder): Int? = int(r, "total_count")?.takeIf { it > 0 }

    fun isDoneOn(r: Reminder, dayKey: String): Boolean =
        if (r.repeatRule == "none" || r.repeatRule.isBlank()) r.isDone else dayKey in doneDates(r)

    fun withDoneOn(r: Reminder, dayKey: String, done: Boolean): Reminder {
        if (r.repeatRule == "none" || r.repeatRule.isBlank()) {
            return r.copy(isDone = done, doneAt = if (done) DateUtils.nowIso() else null)
        }
        val set = doneDates(r).toMutableSet()
        if (done) set.add(dayKey) else set.remove(dayKey)
        val trimmed = set.sorted().takeLast(MAX_DONE_DATES)
        val o = parse(r.metaJson)
        val arr = JsonArray()
        trimmed.forEach { arr.add(it) }
        o.add("done_dates", arr)
        return r.copy(metaJson = o.toString())
    }

    fun noteItems(r: Reminder): List<NoteItem> {
        val arr = read(r.metaJson).get("items")
        if (arr == null || !arr.isJsonArray) return emptyList()
        return arr.asJsonArray.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val text = o.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
            val done = try { o.get("done")?.asBoolean ?: false } catch (e: Exception) { false }
            NoteItem(text, done)
        }
    }

    fun withNoteItems(r: Reminder, items: List<NoteItem>): Reminder {
        val o = parse(r.metaJson)
        val arr = JsonArray()
        items.forEach { item ->
            arr.add(JsonObject().apply {
                addProperty("text", item.text)
                addProperty("done", item.done)
            })
        }
        o.add("items", arr)
        return r.copy(metaJson = o.toString())
    }

    fun build(type: String, extras: Map<String, Any?> = emptyMap()): String {
        val o = JsonObject()
        o.addProperty("type", type)
        extras.forEach { (k, v) ->
            when (v) {
                null -> Unit
                is String -> if (v.isNotBlank()) o.addProperty(k, v)
                is Boolean -> o.addProperty(k, v)
                is Number -> o.addProperty(k, v)
                is List<*> -> {
                    val arr = JsonArray()
                    v.forEach { item -> if (item is String) arr.add(item) }
                    o.add(k, arr)
                }
                else -> o.addProperty(k, v.toString())
            }
        }
        return o.toString()
    }

    fun allExtras(r: Reminder): Map<String, Any?> {
        val o = read(r.metaJson)
        val map = mutableMapOf<String, Any?>()
        o.entrySet().forEach { (key, value) ->
            if (key != "type" && key != "done_dates") {
                map[key] = when {
                    value.isJsonNull -> null
                    value.isJsonPrimitive -> if (value.asString.isEmpty()) null else value.asString
                    value.isJsonObject -> value.asJsonObject.toString()
                    value.isJsonArray -> value.asJsonArray.toString()
                    else -> null
                }
            }
        }
        return map
    }

    fun ownerUserId(r: Reminder): String? = str(r, "owner_user_id")
    fun ownerDisplayName(r: Reminder): String? = str(r, "owner_display_name")
    fun lastActorDisplayName(r: Reminder): String? = str(r, "last_actor_display_name")
}