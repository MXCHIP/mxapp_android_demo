package com.mxchip.mxapp.base.router

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.router.MxRouter

object RouterManager {

    fun navigation(path: String, params: Map<String, Any>? = null) {
        val router = MxRouter.build(path)
        params?.forEach { (key, value) ->
            when (value) {
                is String -> router.withString(key, value)
                is Boolean -> router.withBoolean(key, value)
                is Int -> router.withInt(key, value)
                is Long -> router.withLong(key, value)
                is Float -> router.withFloat(key, value)
                is Double -> router.withDouble(key, value)
                is IntArray -> router.withIntArray(key, value)
                else -> {
                    val anyStr = JsonUtil.toJson(value)
                    router.withString(key, anyStr)
                }
            }
        }
        router.navigation()
    }

    fun navigation(
        path: String,
        params: Map<String, Any>? = null,
        context: Context,
        activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        val router = MxRouter.build(path)
        params?.forEach { (key, value) ->
            when (value) {
                is String -> router.withString(key, value)
                is Boolean -> router.withBoolean(key, value)
                is Int -> router.withInt(key, value)
                is Long -> router.withLong(key, value)
                is Float -> router.withFloat(key, value)
                is Double -> router.withDouble(key, value)
                is IntArray -> router.withIntArray(key, value)
                else -> {
                    val anyStr = JsonUtil.toJson(value)
                    router.withString(key, anyStr)
                }
            }
        }
        router.navigation(context, activityResultLauncher)
    }
}