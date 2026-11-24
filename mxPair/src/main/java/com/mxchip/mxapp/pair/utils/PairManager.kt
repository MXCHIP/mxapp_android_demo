package com.mxchip.mxapp.pair.utils

import androidx.annotation.IntRange
import com.mxchip.mxapp.pair.viewmodel.IPairBusiness

object PairManager {

    private var _showProductList = false

    /**
     * 设置是否显示手动配网，品类列表
     */
    fun setSupportProductCategory(show: Boolean) = apply {
        _showProductList = show
    }

    /**
     * 是否显示手动配网，品类列表
     */
    fun isSupportProductCategory() = _showProductList

    private var _setCommonly = false

    /**
     * 设置是否允许设为常用设备
     */
    fun setSupportSetCommonly(show: Boolean) = apply {
        _setCommonly = show
    }

    /**
     * 是否允许设为常用设备
     */
    fun isSupportSetCommonly() = _setCommonly

    private var _mxCloudHost: String? = null
    private var _fogConfig: Pair<String, String>? = null

    /**
     * 配置网关设备，在配网时发送的连云信息
     * @param fogConfig<FogApi,FogMqtt>
     */
    fun setGatewayConfig(cloudHost: String?, fogConfig: Pair<String, String>?) = apply {
        _mxCloudHost = cloudHost
        _fogConfig = fogConfig
    }

    /**
     * 获取配网时需要发送给网关的mxCloud配置
     */
    fun getGatewayMxCloudHost() = _mxCloudHost

    /**
     * 获取配网时需要发送给网关的mxCloud配置
     */
    fun getGatewayFogConfig() = _fogConfig

    private var _fromRoomId: Int? = null
    private var _fromRoomAddress: Int? = null

    private var _pairBusiness: IPairBusiness? = null

    fun setPairBusiness(pairBusiness: IPairBusiness?) {
        _pairBusiness = pairBusiness
    }

    fun getPairBusiness() = _pairBusiness

    fun setFromRoomInfo(roomId: Int?, roomAddress: Int?) {
        _fromRoomId = roomId
        _fromRoomAddress = roomAddress
    }

    fun getFromRoomId() = _fromRoomId
    fun getFromRoomAddress() = _fromRoomAddress

    private var maxRSSI: Int? = null

    /**
     * 设定蓝牙扫描信号强度的极限值，在此值内设备有效
     */
    fun setScanRssiThreshold(@IntRange(from = -100, to = 0) rssi: Int) {
        maxRSSI = rssi
    }

    fun getScanRssiThreshold() = maxRSSI
}