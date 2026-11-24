package com.mxchip.mxapp.base.utils

import com.tencent.mmkv.MMKV


/**
 * app配置管理
 */
object AppConfigManager {

    private const val ATTR_CONFIG = "attr_config"

    private val mmkv by lazy {
        MMKV.mmkvWithID("home_attr_config")
    }

    fun saveAttrConfig(jsonAttr: String) {
        mmkv.encode(ATTR_CONFIG, jsonAttr)
    }

    fun getAttrConfig(): String? = mmkv.decodeString(ATTR_CONFIG)


    private var _appRecord: String? = null

    /**
     * app的备案号
     */
    fun setAppRecord(appRecord: String) = apply {
        _appRecord = appRecord
    }

    /**
     * app备案号
     */
    fun getAppRecord() = _appRecord

    private var _httpHost: String? = null

    /**
     * 设置app请求的host
     */
    fun setHttpHost(httpHost: String) = apply {
        _httpHost = httpHost
    }

    fun getBaseUrl() = _httpHost

    private var _useHomeId = 0
    private var _useHomeName = ""
    private var _useHomeNetworkKey = ""
    private var _useHomeLocationKey: String? = null
    private var _useHomeLocationCity: String? = null

    fun setUseHomeData(
        homeId: Int,
        homeName: String,
        homeNetworkKey: String,
        locationKey: String?,
        locationCity: String?
    ) {
        _useHomeId = homeId
        _useHomeName = homeName
        _useHomeNetworkKey = homeNetworkKey
        _useHomeLocationKey = locationKey
        _useHomeLocationCity = locationCity
    }

    fun updateUseHomeLocation(locationKey: String?, locationCity: String?) {
        _useHomeLocationKey = locationKey
        _useHomeLocationCity = locationCity
    }

    fun getUseHomeId() = _useHomeId
    fun getUseHomeName() = _useHomeName
    fun getUseHomeNetworkKey() = _useHomeNetworkKey
    fun getUseHomeLocationKey() = _useHomeLocationKey
    fun getUseHomeLocationCity() = _useHomeLocationCity

    fun clearUseHomeData() {
        _useHomeId = 0
        _useHomeName = ""
        _useHomeNetworkKey = ""
        _useHomeLocationKey = null
        _useHomeLocationCity = null
    }
}