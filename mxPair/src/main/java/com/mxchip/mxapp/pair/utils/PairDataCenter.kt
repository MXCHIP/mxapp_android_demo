package com.mxchip.mxapp.pair.utils

import com.mxchip.mxapp.pair.bean.PairDeviceBean
import com.mxchip.mxapp.pair.bean.PairSuccessDevice

object PairDataCenter {

    /** 发现设备时，临时选择的配网设备 */
    val pairSelectedDeviceList by lazy { mutableListOf<PairDeviceBean>() }
    /** 进行配网的设备列表 */
    val pairDeviceList by lazy { mutableListOf<PairDeviceBean>() }

    val pairSuccessfulList by lazy { mutableListOf<PairSuccessDevice>() }

    private var mSSID: String? = null
    private var mPWD: String? = null

    private var mReplaceIotId: String? = null
    private var mReplaceName: String? = null
    private var mReplaceRoomId: Int? = null
    private var mReplaceRoomName: String? = null

    private var mLatitude: String? = null
    private var mLongitude: String? = null
    fun setCacheWifi(ssid: String?, pwd: String?) {
        mSSID = ssid
        mPWD = pwd
    }

    fun getCacheSSID() = mSSID
    fun getCachePwd() = mPWD

    fun clearReplaceInfo() {
        mReplaceIotId = null
        mReplaceName = null
        mReplaceRoomId = null
        mReplaceRoomName = null
    }

    fun setReplacedDeviceInfo(iotId: String, deviceName: String, roomName: String, roomId: Int?) {
        mReplaceIotId = iotId
        mReplaceName = deviceName
        mReplaceRoomName = roomName
        mReplaceRoomId = roomId
    }

    fun getReplaceIotId() = mReplaceIotId
    fun getReplacedName() = mReplaceName
    fun getReplacedRoomName() = mReplaceRoomName
    fun getReplacedRoomId() = mReplaceRoomId

    fun saveLocationCurrent(latitude: Double, longitude: Double) {
        //云端只支持小数点后6位，过长会10500异常。
        val newLats = "$latitude".split(".")
        val latS = newLats[0]
        val latE = if (newLats[1].length > 6) newLats[1].substring(0, 6)
        else newLats[1]
        val newLong = "$longitude".split(".")
        val longS = newLong[0]
        val longE = if (newLong[1].length > 6) newLong[1].substring(0, 6)
        else newLong[1]
        mLatitude = "${latS}.${latE}"
        mLongitude = "${longS}.${longE}"
    }

    fun removeLocationCurrent() {
        mLatitude = null
        mLongitude = null
    }

    fun getLocationLatitude() = mLatitude
    fun getLocationLongitude() = mLongitude
}