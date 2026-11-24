package com.mxchip.mxapp.base.utils

import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import com.tencent.mmkv.MMKV

/**
 *
 */
object DeviceManage {

    private var _isAddDevice = false

    /**
     * 设置是否允许操作（增加/删除）家庭中设备；
     * 注：共享单设备不受此限制
     */
    fun setSupportAddDevice(show: Boolean) = apply {
        _isAddDevice = show
    }

    /**
     * 是否允许操作（增加/删除）家庭中设备；
     */
    fun isSupportAddDevice() = _isAddDevice

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

    private var _deviceShare = false

    /**
     * 设置是否支持单设备分享
     */
    fun setSupportDeviceShare(show: Boolean) = apply {
        _deviceShare = show
    }

    /**
     * 是否支持单设备分享
     */
    fun isSupportDeviceShare() = _deviceShare

    private var _deviceFeedback = false

    /**
     * 设置是否支持设备反馈
     */
    fun setSupportDeviceFeedback(show: Boolean) = apply {
        _deviceFeedback = show
    }

    /**
     * 是否支持设备反馈
     */
    fun isSupportDeviceFeedback() = _deviceFeedback


    private val h5MMKV by lazy {
        MMKV.mmkvWithID("device_h5")
    }

    fun savePanelVersion(pk: String, panelVersion: String) {
        h5MMKV.encode(pk, panelVersion)
    }

    fun getPanelVersion(pk: String): String = h5MMKV.decodeString(pk, "")!!

    /**
     * 从UUID中解析出productId
     */
    fun getFeiYanPidByUUID(uuid: String): Int {
        val uuidHex = uuid.replace("-", "")
        val uuidBytes = ByteUtil.hexStringToBytes(uuidHex)
        if (uuidBytes.size >= 11) {
            val cid = MeshParserUtils.unsignedBytesToInt(uuidBytes[0], uuidBytes[1])
            if (cid == 0x0922) {//公司ID，VBS9010是0x0922，天猫精灵是0x01A8，APP 以此作为过滤条件
                val mxData = ByteArray(4)
                System.arraycopy(uuidBytes, 3, mxData, 0, mxData.size)
                return ByteUtil.bytesToInt(mxData)
            }
        }
        return -1
    }

    fun getPkByUUID(uuid: String): String {
        val uuidHex = uuid.replace("-", "")
        val uuidBytes = ByteUtil.hexStringToBytes(uuidHex)
        val cid = MeshParserUtils.unsignedBytesToInt(uuidBytes[0], uuidBytes[1])
        if (cid == 0x0922) {//公司ID，VBS9010是0x0922，天猫精灵是0x01A8，APP 以此作为过滤条件
            return String.format(
                "%02x%02x%02x%02x", uuidBytes[6],
                uuidBytes[5],
                uuidBytes[4],
                uuidBytes[3]
            )
        }
        return ""
    }
}