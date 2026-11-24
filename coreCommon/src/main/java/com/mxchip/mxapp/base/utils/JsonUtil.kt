package com.mxchip.mxapp.base.utils

import com.mxchip.mxapp.base.bean.AnyValueJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/19
 * @desc:
 */
object JsonUtil {

    val moshi by lazy {
        Moshi.Builder()
            .add(AnyValueJsonAdapter.FACTORY)
            .addLast(KotlinJsonAdapterFactory()).build()
    }

    fun <T> fromJson(json: String?, clazz: Class<T>): T? {
        if (json.isNullOrEmpty()) return null
        val jsonAdapter = moshi.adapter(clazz)
        return jsonAdapter.fromJson(json)
    }

    fun <T> fromJsonToArray(json: String?, clazz: Class<T>): List<T>? {
        if (json.isNullOrEmpty()) return null
        val type = Types.newParameterizedType(List::class.java, clazz)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(json)
    }

    fun <T> fromJsonMXList(json: String?, clazz: Class<T>): List<T>? {
        if (json.isNullOrEmpty()) return null
        val dataJson = JSONObject(json)
        val jsonArray = dataJson.getString("list")
        val type = Types.newParameterizedType(List::class.java, clazz)
        val adapter = moshi.adapter<List<T>>(type)
        return adapter.fromJson(jsonArray)
    }

    fun toJson(entity: Any): String {
        return when (entity) {
            is JSONObject -> {
                // 直接使用 AnyValueJsonAdapter 序列化 JSONObject，避免 Moshi 尝试序列化内部的 LinkedHashMap
                val adapter = AnyValueJsonAdapter()
                val buffer = okio.Buffer()
                com.squareup.moshi.JsonWriter.of(buffer).use { writer ->
                    adapter.toJson(writer, entity)
                }
                buffer.readUtf8()
            }
            is JSONArray -> {
                // 直接使用 AnyValueJsonAdapter 序列化 JSONArray
                val adapter = AnyValueJsonAdapter()
                val buffer = okio.Buffer()
                com.squareup.moshi.JsonWriter.of(buffer).use { writer ->
                    adapter.toJson(writer, entity)
                }
                buffer.readUtf8()
            }
            is List<*> -> {
                // 获取列表中第一个非 null 元素的类型，如果列表为空或全是 null，使用 Any
                val elementType = entity.firstOrNull { it != null }?.javaClass ?: Any::class.java
                val type = Types.newParameterizedType(List::class.java, elementType)
                val jsonAdapter = moshi.adapter<List<Any?>>(type)
                jsonAdapter.toJson(entity)
            }
            is Map<*, *> -> {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val adapter = moshi.adapter<Map<String, Any?>>(type)
                adapter.toJson(entity as Map<String, Any?>)
            }
            else -> {
                val jsonAdapter = moshi.adapter(entity.javaClass)
                jsonAdapter.toJson(entity)
            }
        }
    }

    fun toJson(list: List<Map<String,*>>): String {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(
            List::class.java,
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
        val adapter = moshi.adapter<List<Map<String, Any?>>>(type)

        return adapter.toJson(list)
    }
//
//    fun toJson(map: Map<String, Any>): String {
//        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
//        val adapter = moshi.adapter<Map<String, Any>>(type)
//        return adapter.toJson(map)
//    }

    fun toRequestBody(content: String): RequestBody {
        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        return RequestBody.create(JSON, content)
    }

    fun parseJsonObjectToMap(
        json: JSONObject,
        resultMap: HashMap<String, Any>
    ) {
        //{"data":{"HSVColor":{"Hue":86,"Saturation":92,"Value":100}},"messageType":"unack","messageControl":1,"iotId":"26e364bfd97a477lcmwC"}
        for (jsonKey in json.keys()) {
            val value = json[jsonKey]
            when (value) {
                is JSONObject -> {
                    val subMap = HashMap<String, Any>()
                    parseJsonObjectToMap(value, subMap)
                    resultMap[jsonKey] = subMap
                }

//                is JSONArray -> {
//                    for (i in 0 until value.length()) {
//                        parseDataToMap(value.getJSONObject(i), resultMap)
//                    }
//                }

                else -> {
                    resultMap[jsonKey] = value
                }
            }
        }
    }
}