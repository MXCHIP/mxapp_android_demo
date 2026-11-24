package com.mxchip.mxapp.account.util

import com.tencent.mmkv.MMKV


object AccountManager {

    private var _isSelectCountry = false

    /**
     * 设置是否允许选择国家
     */
    fun setSupportSelectCountry(show: Boolean) = apply {
        _isSelectCountry = show
    }

    /**
     * 是否支持选择国家
     */
    fun isSupportSelectCountry() = _isSelectCountry

    private const val CLIENT_ID = "mob.client.id"


    private val mmkv by lazy {
        MMKV.mmkvWithID("account")
    }

    fun saveClientId(clientId: String) {
        mmkv.encode(CLIENT_ID, clientId)
    }

    fun getClientId(): String {
        return mmkv.decodeString(CLIENT_ID, "")!!
    }
}