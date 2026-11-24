package com.mxchip.mxapp.pair.utils

import com.mxchip.mxapp.pair.consts.MxLinkType

import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PairUtils {

    fun includeWIFI(code: Int): Boolean {
        return MxLinkType.ALI_BROADCAST.code == code ||
                MxLinkType.ALI_WIFI_BATCH.code == code ||
                MxLinkType.ALI_BLE.code == code ||
                MxLinkType.ALI_TMALL.code == code ||
                MxLinkType.MXCHIP_MESH_WIFI.code == code ||
                MxLinkType.MXCHIP_MESH_WIFI_2.code == code ||
                MxLinkType.FOG_BLE_WIFI.code == code
    }

    /**
     * 包含Wi-Fi且必须设置
     */
    fun includeWIFIRequired(code: Int): Boolean {
        return MxLinkType.ALI_BROADCAST.code == code ||
                MxLinkType.ALI_WIFI_BATCH.code == code ||
                MxLinkType.ALI_BLE.code == code ||
                MxLinkType.ALI_TMALL.code == code ||
                MxLinkType.MXCHIP_MESH_WIFI.code == code ||
                MxLinkType.FOG_BLE_WIFI.code == code

    }

    /**
     * 获取广播uuid中标识1的复合产品标识的index【0表示不需要】
     */
    fun getCompositeProductIndexByUUIDFeatureFlag1(uuid: String): Int {
        val uuidHex = uuid.replace("-", "")
        val uuidBytes = ByteUtil.hexStringToBytes(uuidHex)
        val cid = MeshParserUtils.unsignedBytesToInt(uuidBytes[0], uuidBytes[1])
        if (cid == 0x0922) {//公司ID，VBS9010是0x0922，APP 以此作为过滤条件
            //取4～7个bit
            val flag1Byte = uuidBytes[14].toInt() shr 4
            return flag1Byte and 0x0F
        }
        return -1
    }

    fun getADTypeIndexByScanRecordBytes(scanRecordBytes: ByteArray, matchCID: Int): Int {
        try {
            var i = 0
            while (i + 4 < scanRecordBytes.size) {
                val ad = MeshParserUtils.unsignedByteToInt(scanRecordBytes[i])
                val cid = MeshParserUtils.unsignedBytesToInt(
                    scanRecordBytes[i + 1], scanRecordBytes[i + 2]
                )
                if (ad == 0xFF && cid == matchCID) {
                    return i
                }
                i++
            }
        } catch (e: Exception) {
            // ignore
        }
        return -1
    }

    fun parseAliPID(scanRecordBytes: ByteArray): Map<String, Any>? {
        val index = getADTypeIndexByScanRecordBytes(scanRecordBytes, 0x01A8)
        if (index != -1) {
            val buffer = ByteBuffer.wrap(scanRecordBytes)
            val pidBts = ByteArray(4)//pid 4字节
            buffer.position(index + 5)
            buffer[pidBts, 0, pidBts.size]

            val macBts = ByteArray(6)//pid 4字节
            buffer.position(index + 9)
            buffer[macBts, 0, macBts.size]
            macBts.reverse()
            val wifiMac = ByteUtil.bytesToHexString(macBts).replace("(.{2})".toRegex(), "$1:")
            //Ble Mac = Wi-Fi Mac + 1 ，如果 Wi-Fi MAC 最后是 FF, 那么 BLE 的 MAC 是 00, 不进位
            return mapOf(
                "productId" to "${
                    MeshParserUtils.bytesToInt(
                        pidBts, ByteOrder.LITTLE_ENDIAN
                    )
                }", "mac" to wifiMac.substring(0, wifiMac.length - 1)
            )
        }
        return null
    }
}