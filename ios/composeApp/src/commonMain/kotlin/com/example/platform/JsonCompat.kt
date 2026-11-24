package com.example.platform

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray as KJsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject as KJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Tiny org.json-compatible wrappers (JSONObject / JSONArray) on top of
 * kotlinx.serialization so the original persistence and parsing code keeps
 * working unchanged on iOS.
 */
class JSONException(message: String) : Exception(message)

private fun Any?.toElement(): JsonElement = when (this) {
    null -> JsonNull
    is JSONObject -> this.toJsonObject()
    is JSONArray -> this.toJsonArray()
    is JsonElement -> this
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Collection<*> -> KJsonArray(this.map { it.toElement() })
    is Map<*, *> -> KJsonObject(this.entries.associate { it.key.toString() to it.value.toElement() })
    else -> JsonPrimitive(this.toString())
}

private fun JsonElement.toAny(): Any? = when (this) {
    is JsonNull -> null
    is KJsonObject -> JSONObject(this)
    is KJsonArray -> JSONArray(this)
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
}

class JSONObject() {
    private val map = LinkedHashMap<String, JsonElement>()

    constructor(json: String) : this() {
        val parsed = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            throw JSONException("Invalid JSON: ${e.message}")
        }
        val obj = parsed as? KJsonObject ?: throw JSONException("Not a JSON object")
        map.putAll(obj)
    }

    internal constructor(obj: KJsonObject) : this() {
        map.putAll(obj)
    }

    internal fun toJsonObject(): KJsonObject = KJsonObject(map)

    fun put(key: String, value: Any?): JSONObject {
        map[key] = value.toElement(); return this
    }

    fun has(key: String): Boolean = map.containsKey(key)
    fun isNull(key: String): Boolean = map[key] == null || map[key] is JsonNull
    fun length(): Int = map.size
    fun keys(): Iterator<String> = map.keys.toList().iterator()
    fun remove(key: String): Any? = map.remove(key)?.toAny()

    fun opt(key: String): Any? = map[key]?.toAny()
    fun get(key: String): Any = opt(key) ?: throw JSONException("No value for $key")

    private fun prim(key: String): JsonPrimitive? = map[key] as? JsonPrimitive

    fun optString(key: String, fallback: String = ""): String {
        val e = map[key] ?: return fallback
        if (e is JsonNull) return fallback
        return (e as? JsonPrimitive)?.contentOrNull ?: e.toString()
    }

    fun getString(key: String): String {
        val e = map[key] ?: throw JSONException("No value for $key")
        return (e as? JsonPrimitive)?.contentOrNull ?: e.toString()
    }

    fun optInt(key: String, fallback: Int = 0): Int =
        prim(key)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() ?: it.contentOrNull?.toIntOrNull() } ?: fallback

    fun getInt(key: String): Int =
        prim(key)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() ?: it.contentOrNull?.toIntOrNull() }
            ?: throw JSONException("No int for $key")

    fun optLong(key: String, fallback: Long = 0L): Long =
        prim(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() ?: it.contentOrNull?.toLongOrNull() } ?: fallback

    fun getLong(key: String): Long = prim(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
        ?: throw JSONException("No long for $key")

    fun optDouble(key: String, fallback: Double = Double.NaN): Double =
        prim(key)?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() } ?: fallback

    fun getDouble(key: String): Double = prim(key)?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
        ?: throw JSONException("No double for $key")

    fun optBoolean(key: String, fallback: Boolean = false): Boolean =
        prim(key)?.let { it.booleanOrNull ?: it.contentOrNull?.toBooleanStrictOrNull() } ?: fallback

    fun getBoolean(key: String): Boolean = prim(key)?.let { it.booleanOrNull ?: it.contentOrNull?.toBooleanStrictOrNull() }
        ?: throw JSONException("No boolean for $key")

    fun optJSONObject(key: String): JSONObject? = (map[key] as? KJsonObject)?.let { JSONObject(it) }
    fun getJSONObject(key: String): JSONObject = optJSONObject(key) ?: throw JSONException("No object for $key")
    fun optJSONArray(key: String): JSONArray? = (map[key] as? KJsonArray)?.let { JSONArray(it) }
    fun getJSONArray(key: String): JSONArray = optJSONArray(key) ?: throw JSONException("No array for $key")

    override fun toString(): String = toJsonObject().toString()
    fun toString(@Suppress("UNUSED_PARAMETER") indent: Int): String = toString()
}

class JSONArray() {
    private val list = ArrayList<JsonElement>()

    constructor(json: String) : this() {
        val parsed = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            throw JSONException("Invalid JSON: ${e.message}")
        }
        val arr = parsed as? KJsonArray ?: throw JSONException("Not a JSON array")
        list.addAll(arr)
    }

    constructor(items: Collection<*>) : this() {
        items.forEach { list.add(it.toElement()) }
    }

    internal constructor(arr: KJsonArray) : this() {
        list.addAll(arr)
    }

    internal fun toJsonArray(): KJsonArray = KJsonArray(list)

    fun length(): Int = list.size
    fun put(value: Any?): JSONArray {
        list.add(value.toElement()); return this
    }

    fun opt(index: Int): Any? = list.getOrNull(index)?.toAny()
    fun get(index: Int): Any = opt(index) ?: throw JSONException("No value at $index")
    fun isNull(index: Int): Boolean = list.getOrNull(index)?.let { it is JsonNull } ?: true

    fun optString(index: Int, fallback: String = ""): String {
        val e = list.getOrNull(index) ?: return fallback
        if (e is JsonNull) return fallback
        return (e as? JsonPrimitive)?.contentOrNull ?: e.toString()
    }

    fun getString(index: Int): String = optString(index).takeIf { index < list.size }
        ?: throw JSONException("No value at $index")

    fun optInt(index: Int, fallback: Int = 0): Int =
        (list.getOrNull(index) as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() } ?: fallback

    fun getInt(index: Int): Int = (list.getOrNull(index) as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
        ?: throw JSONException("No int at $index")

    fun optDouble(index: Int, fallback: Double = Double.NaN): Double =
        (list.getOrNull(index) as? JsonPrimitive)?.doubleOrNull ?: fallback

    fun getDouble(index: Int): Double = (list.getOrNull(index) as? JsonPrimitive)?.doubleOrNull
        ?: throw JSONException("No double at $index")

    fun optBoolean(index: Int, fallback: Boolean = false): Boolean =
        (list.getOrNull(index) as? JsonPrimitive)?.booleanOrNull ?: fallback

    fun getBoolean(index: Int): Boolean = (list.getOrNull(index) as? JsonPrimitive)?.booleanOrNull
        ?: throw JSONException("No boolean at $index")

    fun optJSONObject(index: Int): JSONObject? = (list.getOrNull(index) as? KJsonObject)?.let { JSONObject(it) }
    fun getJSONObject(index: Int): JSONObject = optJSONObject(index) ?: throw JSONException("No object at $index")
    fun optJSONArray(index: Int): JSONArray? = (list.getOrNull(index) as? KJsonArray)?.let { JSONArray(it) }
    fun getJSONArray(index: Int): JSONArray = optJSONArray(index) ?: throw JSONException("No array at $index")

    override fun toString(): String = toJsonArray().toString()
    fun toString(@Suppress("UNUSED_PARAMETER") indent: Int): String = toString()
}
