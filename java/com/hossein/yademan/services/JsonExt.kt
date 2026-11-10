// مسیر: app/src/main/java/com/hossein/yademan/services/JsonExt.kt
package com.hossein.yademan.services

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** دسترسی امن به فیلدهای JSON (سرور ممکن است نوع‌ها را رشته‌ای برگرداند) */
fun JsonObject.str(key: String): String? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    return if (el.isJsonPrimitive) el.asString else el.toString()
}

fun JsonObject.int(key: String): Int? {
    val el = get(key) ?: return null
    if (el.isJsonNull || !el.isJsonPrimitive) return null
    return try {
        el.asInt
    } catch (e: Exception) {
        el.asString.toDoubleOrNull()?.toInt()
    }
}

fun JsonObject.long(key: String): Long? {
    val el = get(key) ?: return null
    if (el.isJsonNull || !el.isJsonPrimitive) return null
    return try {
        el.asLong
    } catch (e: Exception) {
        el.asString.toDoubleOrNull()?.toLong()
    }
}

fun JsonObject.bool(key: String): Boolean? {
    val el = get(key) ?: return null
    if (el.isJsonNull || !el.isJsonPrimitive) return null
    val p = el.asJsonPrimitive
    return when {
        p.isBoolean -> p.asBoolean
        p.isNumber -> p.asInt != 0
        else -> when (p.asString.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no", "" -> false
            else -> null
        }
    }
}

/** شیء JSON؛ اگر سرور آن را به صورت رشته JSON فرستاده باشد هم پارس می‌شود */
fun JsonObject.obj(key: String): JsonObject? {
    val el = get(key) ?: return null
    return el.asObjectOrNull()
}

fun JsonObject.arr(key: String): JsonArray? {
    val el = get(key) ?: return null
    if (el.isJsonArray) return el.asJsonArray
    if (el.isJsonPrimitive) {
        return try {
            val parsed = JsonParser.parseString(el.asString)
            if (parsed.isJsonArray) parsed.asJsonArray else null
        } catch (e: Exception) {
            null
        }
    }
    return null
}

fun JsonElement.asObjectOrNull(): JsonObject? {
    if (isJsonObject) return asJsonObject
    if (isJsonPrimitive) {
        return try {
            val parsed = JsonParser.parseString(asString)
            if (parsed.isJsonObject) parsed.asJsonObject else null
        } catch (e: Exception) {
            null
        }
    }
    return null
}
