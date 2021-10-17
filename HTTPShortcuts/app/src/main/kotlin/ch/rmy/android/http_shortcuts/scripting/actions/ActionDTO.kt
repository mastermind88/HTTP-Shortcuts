package ch.rmy.android.http_shortcuts.scripting.actions

import org.json.JSONException
import org.json.JSONObject
import org.liquidplayer.javascript.JSValue

class ActionDTO(
    val type: String,
    val data: Map<String, JSValue?> = emptyMap(),
) {

    fun getString(key: String): String? {
        val value = data[key]
        return when {
            value == null || value.isNull || value.isUndefined -> null
            value.isObject || value.isTypedArray || value.isArray -> value.toJSON()
            else -> value.toString()
        }
    }

    fun getInt(key: String): Int? =
        getString(key)?.toIntOrNull()

    fun getBoolean(key: String): Boolean? =
        getString(key)?.toBoolean()

    fun getObject(key: String): Map<String, Any?>? {
        val value = data[key]
        return when {
            value == null || value.isNull || value.isUndefined -> null
            value.isObject && !value.isArray -> try {
                JSONObject(value.toJSON())
                    .let { `object` ->
                        mutableMapOf<String, Any?>()
                            .apply {
                                `object`.keys().forEach { key ->
                                    put(key, `object`[key])
                                }
                            }
                    }
            } catch (e: JSONException) {
                null
            }
            else -> null
        }
    }

    fun getList(key: String): List<Any?>? {
        val value = data[key]
        return when {
            value == null || value.isNull || value.isUndefined -> null
            value.isTypedArray || value.isArray -> value.toJSArray()
            else -> null
        }
    }

}
