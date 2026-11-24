package com.mxchip.mxapp.base.utils

import android.content.Context

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/19
 * @desc:
 */
object AssetsUtil {

    fun readJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}