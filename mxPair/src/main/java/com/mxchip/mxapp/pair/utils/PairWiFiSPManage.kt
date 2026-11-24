package com.mxchip.mxapp.pair.utils

import com.tencent.mmkv.MMKV

/**
 *保存app的配网Wi-Fi记录
 */
object PairWiFiSPManage {
    private val mmkv by lazy {
        MMKV.mmkvWithID("pair.wifi")
    }

    fun saveWifi(ssid: String, pwd: String) {
        mmkv.encode(ssid, pwd)
    }

    fun getWifiSSIDArray(): Array<String>? = mmkv.allKeys()

    fun getWifiPwd(ssid: String): String = mmkv.decodeString(ssid, "")!!
}