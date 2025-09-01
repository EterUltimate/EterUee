package me.rerere.common.http

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

val JsonElement.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

fun JsonObject.getByKey(key: String): JsonElement? {
    val keys = key.split(".")
    var current: JsonElement = this
    
    for (k in keys) {
        current = when (current) {
            is JsonObject -> current[k] ?: return null
            else -> return null
        }
    }
    
    return current
}
