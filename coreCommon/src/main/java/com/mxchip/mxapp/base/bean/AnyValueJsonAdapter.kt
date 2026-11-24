package com.mxchip.mxapp.base.bean

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.json.JSONArray
import org.json.JSONObject

/**
 * 专门用于处理 PropertyBean.value 字段的 JsonAdapter
 * 将 JSON 对象直接读取为 JSONObject，而不是 Map
 * 将 JSON 数组直接读取为 JSONArray，而不是 List
 */
class AnyValueJsonAdapter : JsonAdapter<Any?>() {

    override fun fromJson(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonReader.Token.BEGIN_OBJECT -> readJsonObject(reader)
            JsonReader.Token.BEGIN_ARRAY -> readJsonArray(reader)
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                // 读取为 Double，然后根据值判断是否应该转换为整数类型
                val number = reader.nextDouble()
                when {
                    // 如果是整数值（没有小数部分）
                    number % 1 == 0.0 -> {
                        // 根据范围选择 Int 或 Long
                        when {
                            number >= Int.MIN_VALUE && number <= Int.MAX_VALUE -> number.toInt()
                            else -> number.toLong()
                        }
                    }
                    // 否则保持为 Double
                    else -> number
                }
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NULL -> reader.nextNull<Any?>()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: Any?) {
        when (value) {
            null -> writer.nullValue()
            is String -> writer.value(value)
            is Number -> writer.value(value)
            is Boolean -> writer.value(value)
            is JSONObject -> writeJsonObject(writer, value)
            is JSONArray -> writeJsonArray(writer, value)
            // 兼容 Map 和 List（以防某些地方直接赋值）
            is Map<*, *> -> {
                writer.beginObject()
                @Suppress("UNCHECKED_CAST")
                (value as Map<String, Any?>).forEach { (k, v) ->
                    writer.name(k)
                    toJson(writer, v)
                }
                writer.endObject()
            }
            is List<*> -> {
                writer.beginArray()
                value.forEach { toJson(writer, it) }
                writer.endArray()
            }
            else -> writer.value(value.toString())
        }
    }

    /**
     * 读取 JSON Object，直接返回 JSONObject
     */
    private fun readJsonObject(reader: JsonReader): JSONObject {
        val jsonObject = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = fromJson(reader)
            jsonObject.put(key, value)
        }
        reader.endObject()
        return jsonObject
    }

    /**
     * 读取 JSON Array，直接返回 JSONArray
     */
    private fun readJsonArray(reader: JsonReader): JSONArray {
        val jsonArray = JSONArray()
        reader.beginArray()
        while (reader.hasNext()) {
            jsonArray.put(fromJson(reader))
        }
        reader.endArray()
        return jsonArray
    }

    /**
     * 写入 JSONObject
     * 注意：由于我们使用 AnyValueJsonAdapter 读取，所有嵌套对象都应该是 JSONObject，不会有 Map
     */
    private fun writeJsonObject(writer: JsonWriter, jsonObject: JSONObject) {
        writer.beginObject()
        jsonObject.keys().forEach { key ->
            writer.name(key)
            when (val value = jsonObject.get(key)) {
                null -> writer.nullValue()
                is JSONObject -> writeJsonObject(writer, value)
                is JSONArray -> writeJsonArray(writer, value)
                // 基本类型直接写入
                is String, is Number, is Boolean -> toJson(writer, value)
                // 其他类型（理论上不应该有 Map，但为了防御性编程保留）
                else -> toJson(writer, value)
            }
        }
        writer.endObject()
    }

    /**
     * 写入 JSONArray
     */
    private fun writeJsonArray(writer: JsonWriter, jsonArray: JSONArray) {
        writer.beginArray()
        for (i in 0 until jsonArray.length()) {
            toJson(writer, jsonArray.get(i))
        }
        writer.endArray()
    }

    /**
     * Factory 用于注册到 Moshi
     */
    companion object {
        val FACTORY = Factory { type, annotations, moshi ->
            // 检查是否是 Any 类型（包括 Any? 和 Object）
            val rawType = com.squareup.moshi.Types.getRawType(type)
            if (rawType == Any::class.java || rawType == Object::class.java) {
                AnyValueJsonAdapter()
            } else {
                null
            }
        }
    }
}
