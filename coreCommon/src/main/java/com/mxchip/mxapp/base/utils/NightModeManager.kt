package com.mxchip.mxapp.base.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.mxchip.mxapp.base.consts.CommonConstants
import com.tencent.mmkv.MMKV


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/27
 * @desc:
 */
object NightModeManager {

    private val mmkv by lazy {
        MMKV.mmkvWithID("night_mode")
    }

    fun changeMode(mode: Int) {
        val oldMode = getCurrentMode()
        if (oldMode == mode) return
        mmkv.encode(CommonConstants.KEY_NIGHT_MODE, mode)
    }

    fun getCurrentMode(): Int {
        return mmkv.decodeInt(
            CommonConstants.KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
    }

    fun init(mode: Int? = null) {
        val finalMode = mode ?: getCurrentMode()
        AppCompatDelegate.setDefaultNightMode(finalMode)
    }

    fun isDarkMode(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) 及以上版本
            val currentNightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES
        } else {
            // 在 Android 10 以下没有直接的 API 判断暗黑模式
            return false // 低于 Android 10 默认不支持暗黑模式
        }
    }
}