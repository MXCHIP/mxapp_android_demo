package com.mxchip.mxapp.utils

import com.tencent.mmkv.MMKV

object AppDataCenter {

    private const val KEY_SHOW_SMART_HOME = "KEY_SHOW_SMART_HOME"
    private const val KEY_STARTUP_AD_LIST = "KEY_STARTUP_AD_LIST"

    private val mmkv by lazy {
        MMKV.mmkvWithID("smarthome")
    }

    /**
     * 已读
     */
    fun readSmartHomeGuide() {
        mmkv.encode(KEY_SHOW_SMART_HOME, true)
    }

    fun isReadSmartHomeGuide(): Boolean = mmkv.decodeBool(KEY_SHOW_SMART_HOME)

    fun saveStartupAdList(listJson: String) {
        mmkv.encode(KEY_STARTUP_AD_LIST, listJson)
    }

    fun getStartupAdList() = mmkv.decodeString(KEY_STARTUP_AD_LIST)
}