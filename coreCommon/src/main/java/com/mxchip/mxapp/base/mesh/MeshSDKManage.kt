package com.mxchip.mxapp.base.mesh

import android.text.TextUtils
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.bean.MeshState
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.sdk.mesh.util.MeshMessageUtil
import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *
 */
object MeshSDKManage {
    const val ATTR_TYPE_GROUP_ADD = "000D"//将服务添加到群组
    const val ATTR_TYPE_GROUP_DEL = "000E"//将服务从群组移除
    const val ATTR_TYPE_GROUP_RESET = "000F"//重置设备配置（清除一个设备中相关群组信息）
    const val ATTR_TYPE_RULE = "0004" // 联动规则
    const val ATTR_TYPE_RULE_NEW = "0017"
    const val ATTR_TYPE_CEVT_CONFIG = "0024"
    const val ATTR_TYPE_WRITE_RESULT_LENGTH = 4

    const val ATTR_TYPE_ILOP_GET_QUADRUPLES = "0003" // 获取设备五元祖-飞燕平台
    const val ATTR_TYPE_ILOP_GET_QUADRUPLES_V2 = "0019" // 获取设备五元祖-飞燕平台,v2
    const val ATTR_TYPE_FOG_GET_QUADRUPLES = "0030" // 获取设备五元祖-fog平台

    const val ATTR_TYPE_GET_VERSION = "0005" // 获取设备固件版本号
    const val ATTR_TYPE_GET_VERSION_LENGTH = 3

    const val ATTR_TYPE_REBOOT_GATEWAY = "0006" // 重启设备
    const val ATTR_TYPE_PAIR_SUCCESSFUL = "0018" // 配网成功通知

    // 一键执行（虚拟按钮），APP 在发送虚拟按钮等消息来触发本地场景时，其 ATT type 的最高 bit 需要设置为 1。
    const val ATTR_TYPE_ONE_KEY = "0780"

    const val ATTR_TYPE_WIFI_LINK_STATUS = "0013"//模组上报自己的 Wi-Fi 连接状态
    const val ATTR_TYPE_WIFI_LINK_STATUS_LENGTH = 1

    //默认扫描到proxy设备直接连接
    var isConnectProxy = true

    /**
     * mesh 连接成功后，订阅组播地址
     */
    fun subscribeGroups(filterResult: (Boolean) -> Unit) {
        addProxyFilterCount = 0
        addProxyFilter(filterResult)
    }

    private var addProxyFilterCount = 0
    private fun addProxyFilter(filterResult: (Boolean) -> Unit) {
        MXMesh.addProxyFilter(intArrayOf(0xD000, 0xD003)) { meshState, _ ->
            if (isConnectProxy) { //只有当前处于连接代理模式
                if (meshState == MeshState.SUCCESS) {
                    addProxyFilterCount = -1
                    filterResult.invoke(true)
                } else if (addProxyFilterCount < 5) {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(200)
                        addProxyFilterCount++
                        addProxyFilter(filterResult)
                    }
                } else {
                    filterResult.invoke(false)
                }
            }
        }
    }

    /**
     * 通过uuid 获取mac
     * @param uuid 设备uuid
     * @return 设备mac
     */
    fun getAddressIdByUUID(uuid: String): String {
        if (TextUtils.isEmpty(uuid)) return ""
        val reverseMac = uuid.replace("-", "").substring(14, 26)
        //每格2字符插入","用于split截取
        val array = reverseMac.replace(Regex("(.{2})"), "$1,").split(",").filter { it.isNotEmpty() }
        return array.reversed().joinToString(":")
    }

    /**
     * 将Type和value结合起来
     */
    fun combineAttTypeAndAttValue(params: List<Pair<String, String?>>?): String {
        val res = StringBuilder()
        params?.forEach { pair ->
            //attrType是否使用小端模式
            res.append(stringReversedToLittleEndian(pair.first))
            res.append(pair.second ?: "")
        }
        return res.toString()
    }

    fun stringReversedToLittleEndian(str: String): String {
        return if (str.length > 2 && str.length % 2 == 0) {
            val reversed = StringBuilder()
            val array = str.replace("(.{2})".toRegex(), "$1-").split("-".toRegex()).toTypedArray()
            for (i in array.indices.reversed()) {
                reversed.append(array[i])
            }
            reversed.toString()
        } else str
    }

    /**
     * 通过attrType获取到value后使用，
     * return 返回格式 2.1.1
     */
    fun generateVersionName(attrValue: String): String {
        return "" + attrValue.substring(0, 2).toInt() + "." + attrValue.substring(2, 4)
            .toInt() + "." + attrValue.substring(4, 6).toInt()
    }

    fun setWiFi(
        uuid: String, hex: String, msgId: String, callback: MapCallback
    ) {
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET, hex, { meshState, resultMap ->
                if (meshState == MeshState.SUCCESS) {
                    callback.onResult(resultMap!!)
                } else callback.onError(CallbackMsg(meshState.code, meshState.msg))
            }, timeout = true, retry = false, timeoutMillis = 15 * 1000, msgId
        )
    }

    fun setMsg(
        uuid: String,
        params: List<Pair<String, String>>?,
        callback: MapCallback?,
        isTimeOut: Boolean = false,
        isRetry: Boolean = false,
        timeoutMillis: Long = 1500
    ) {
        val timeout = isTimeOut || callback != null
        setMsg(uuid, combineAttTypeAndAttValue(params), callback, timeout, isRetry, timeoutMillis)
    }

    fun setMsg(
        opcode: String,
        uuid: String,
        hex: String,
        callback: MapCallback?,
        isTimeOut: Boolean,
        isRetry: Boolean,
        timeoutMillis: Long = 1500L
    ) {
        MXMesh.sendDeviceMessage(
            uuid, 0, opcode, hex, { meshState, resultMap ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (meshState == MeshState.SUCCESS) {
                        callback?.onResult(resultMap!!)
                    } else callback?.onError(CallbackMsg(meshState.code, meshState.msg))
                }
            }, isTimeOut, isRetry, timeoutMillis
        )
    }

    fun setMsg(
        uuid: String,
        hex: String,
        callback: MapCallback?,
        timeout: Boolean = false,
        isRetry: Boolean = false,
        timeoutMillis: Long = 1500
    ) {
        setMsg(
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET,
            uuid,
            hex,
            callback,
            timeout,
            isRetry,
            timeoutMillis
        )
    }

    fun setMsg(uuid: String, hex: String, callback: MapCallback?) {
        setMsg(uuid, hex, callback, timeout = true, isRetry = true)
    }

    fun setMsg(
        uuid: String,
        hex: String,
        timeout: Boolean = true,
        retry: Boolean = true,
        timeoutMillis: Long = 1500L,
        meshState: ((MeshState, Map<String, Any>?) -> Unit),
    ) {
        MXMesh.sendDeviceMessage(
            uuid,
            0,
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET,
            hex,
            meshState,
            timeout,
            retry,
            timeoutMillis
        )
    }

    fun setUnAckMsg(uuid: String, params: List<Pair<String, String>>?) {
        setUnAckMsg(uuid, combineAttTypeAndAttValue(params))
    }

    fun setUnAckMsg(uuid: String, hex: String) {
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET_UNACK, hex
        )
    }

    fun getMsg(uuid: String, params: List<Pair<String, String>>?, callback: MapCallback) {
        getMsg(uuid, combineAttTypeAndAttValue(params), callback)
    }

    fun getMsg(
        uuid: String,
        hex: String,
        callback: MapCallback?,
        timeout: Boolean = true,
        retry: Boolean = false,
        timeoutMillis: Long = 1_500
    ) {
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_GET, hex, { meshState, resultMap ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (meshState == MeshState.SUCCESS) {
                        callback?.onResult(resultMap!!)
                    } else callback?.onError(CallbackMsg(meshState.code, meshState.msg))
                }
            }, timeout = timeout, retry = retry, timeoutMillis = timeoutMillis
        )
    }

    fun getAllProperties(uuid: String, callback: MapCallback?) {
        getMsg(uuid, "", callback, timeout = true, retry = true, timeoutMillis = 5000)
    }

    /**
     * 发送设备群组消息
     */
    fun senDeviceGroupMsg(
        groupAddress: Int,
        hexParam: String,
        repeatNum: Int = 2,
        requireNetworkExecute: Boolean = false,
        opcode: String = MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET_UNACK,
        cloudMesh: ((Map<String, Any>) -> Unit)?
    ) {
        if (MXMesh.isConnected() && !requireNetworkExecute) {
            MXMesh.sendGroupMessage(
                groupAddress, opcode, hexParam, repeatNum
            )
        } else if (null != cloudMesh) {
            val realOpCodeBytes = MeshParserUtils.createVendorOpCode(
                opcode.toInt(16), 0x0922
            )
            val realOpcode = ByteUtil.bytesToHexString(realOpCodeBytes)//D32209
            parseDataToCloud(realOpcode, groupAddress, hexParam, repeatNum, cloudMesh)
        }
    }

    fun parseDataToCloud(
        opcode: String,
        meshAddress: Int,
        hexParam: String,
        repeatSendNum: Int,
        cloudMesh: (Map<String, Any>) -> Unit
    ) {
        val address = String.format("%04X", meshAddress)
        val tid = MeshMessageUtil.getNextMeshMessageTid()
        val version = "00"
        val repeatNum = String.format("%02X", repeatSendNum)
        val repeatTime = "C800"  //16进制，单位毫秒
        val message =
            version + repeatNum + repeatTime + stringReversedToLittleEndian(address) + opcode + tid + hexParam
        val map = mapOf("home_id" to AppConfigManager.getUseHomeId(), "data" to message)
        cloudMesh.invoke(map)
    }

    fun addDeviceGroup(uuid: String, groupAddress: Int, addResult: (Boolean) -> Unit) {
        //service 序号+address+master
        val value = "00" + stringReversedToLittleEndian(String.format("%04X", groupAddress))
        val paramHex = stringReversedToLittleEndian(ATTR_TYPE_GROUP_ADD) + value
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET, paramHex, { meshState, resultMap ->
                var rest = -1
                if (resultMap?.containsKey(MeshConstants.KEY_DATA) == true) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //attrType 2字节
                    val valueByteArray = ByteArray(dataArray.size - 2)
                    System.arraycopy(dataArray, 2, valueByteArray, 0, valueByteArray.size)
                    rest = ByteUtil.bytesToInt(valueByteArray)
                }
                addResult.invoke(rest == 0)
            }, timeout = true, retry = true
        )
    }

    fun delDeviceGroup(uuid: String, groupAddress: Int, delResult: (Boolean) -> Unit) {
        //serviceNum = "00"
        val value = "00" + stringReversedToLittleEndian(String.format("%04X", groupAddress))
        val paramHex = stringReversedToLittleEndian(ATTR_TYPE_GROUP_DEL) + value
        MXMesh.sendDeviceMessage(
            uuid, 0, MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET, paramHex, { meshState, resultMap ->
                var rest = -1
                if (resultMap?.containsKey(MeshConstants.KEY_DATA) == true) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //attrType 2字节
                    val valueByteArray = ByteArray(dataArray.size - 2)
                    System.arraycopy(dataArray, 2, valueByteArray, 0, valueByteArray.size)
                    rest = ByteUtil.bytesToInt(valueByteArray)
                }
                delResult.invoke(rest == 0)
            }, timeout = true, retry = true
        )
    }

    /**
     * 发送消息通知设备配网成功（退出配网模式
     */
    fun sendPairedSuccessful(uuid: String, result: (Boolean) -> Unit) {
        MXMesh.sendDeviceMessage(
            uuid,
            0,
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET,
            "1800", //0018
            { meshState, _ ->
                result.invoke(meshState == MeshState.SUCCESS)
            },
            timeout = true,
            retry = true,
        )
    }


    /**
     * 发送消息从设备端获取version
     */
    fun getVersion(uuid: String, version: (String?) -> Unit) {
        MXMesh.sendDeviceMessage(
            uuid,
            0,
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_GET,
            "0500", //0005
            { _, resultMap ->
                if (resultMap?.containsKey(MeshConstants.KEY_DATA) == true) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //attrType 2字节
                    val versionByteArray = ByteArray(dataArray.size - 2)
                    System.arraycopy(dataArray, 2, versionByteArray, 0, versionByteArray.size)
                    val versionData = ByteUtil.bytesToHexString(versionByteArray)
                    val versionStr = "${versionData.take(2).toInt(16)}.${
                        versionData.substring(2, 4).toInt(16)
                    }.${versionData.substring(4, 6).toInt(16)}"
                    version.invoke(versionStr)
                } else {
                    version.invoke(null)
                }
            },
            timeout = true,
            retry = true,
        )
    }

    /**
     * 获取网络设备的ip和mac
     * @param type WiFi	1 ,以太网	2
     */
    fun getIpWithMac(type: Int, uuid: String, netResult: (ip: String?, mac: String?) -> Unit) {
        MXMesh.sendDeviceMessage(
            uuid,
            0,
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_GET,
            "1B0002${String.format("%02X", type)}",
            { _, resultMap ->
                if (resultMap?.containsKey(MeshConstants.KEY_DATA) == true) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    var ip: String? = null
                    if (dataArray.size > 6 && dataArray[3].toInt() != 0) {
                        val ipBytes = ByteArray(4)
                        System.arraycopy(dataArray, 3, ipBytes, 0, ipBytes.size)
                        ip = ipBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                    }
                    val netMac = if (dataArray.size > 11) {
                        val wifiMacBytes = ByteArray(6)
                        System.arraycopy(dataArray, 7, wifiMacBytes, 0, wifiMacBytes.size)
                        wifiMacBytes.joinToString(separator = ":") { ByteUtil.byteToHexString(it) }
                    } else {
                        null
                    }
                    MXLog.i("MeshSDKManage", "getIpWithMac() ip: $ip, mac: $netMac")
                    netResult.invoke(ip, netMac)
                } else {
                    netResult.invoke(null, null)
                }
            },
            timeout = true,
            retry = true,
            timeoutMillis = 10_000
        )
    }
}
