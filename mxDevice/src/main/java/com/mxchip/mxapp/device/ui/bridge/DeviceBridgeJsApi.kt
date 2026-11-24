package com.mxchip.mxapp.device.ui.bridge

import android.app.Activity
import android.text.TextUtils
import android.webkit.JavascriptInterface
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mesh.rule.utils.MeshTranscodeUtil
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshAttrUtil
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.DeviceWriteConfigManage.isMeshRuleKv
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.vibrateShot
import com.mxchip.mxapp.base.widget.web.CompletionHandler
import com.mxchip.mxapp.device.api.DeviceRepository
import com.mxchip.router.MxRouter
import com.mxchip.scanner.MxScan
import com.mxchip.scanner.bean.MxBluetoothDevice
import com.mxchip.scanner.callback.MxScanListener
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.bean.MeshState
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class DeviceBridgeJsApi(
    private val iotId: String,
    private var uuid: String? = null,
    private val isShareDevice: Boolean,
    private var remoteStatus: Int, //云端在离线
    private var localStatus: Int, //蓝牙mesh在离线
    private val context: Activity,
    private val productAttrConfig: String,
) {
    private val featureProvider by lazy { MxRouter.get<IDeviceFeature>() }

    companion object {
        private const val TAG = "DeviceBridgeJsApi"
    }

    private val deviceRepository by lazy {
        DeviceRepository()
    }

    private var subscribeHandler: CompletionHandler<Any>? = null
    private var handlerTime = 0L //记录最近的操作时间，用于消息过滤
    private var messageControl = 1 //[0-不延迟，1延迟]
    private var isHexTextMsg = false//[true-hex消息传输，false-app解析转换值]
    private val delayMessageQueue = mutableMapOf<String, JSONObject>()

    fun onDestroy() {
        MXLog.i(TAG, "onDestroy()")
        timerTask?.cancel()
        subscribeHandler = null
        MxScan.removeScanDataListener(scanListener)
        // 清理可选功能资源
        featureProvider?.destroyRhythm()
    }

    private val scanListener by lazy {
        object : MxScanListener {
            override fun onScanResultReceived(mxBluetoothDevice: MxBluetoothDevice) {
                MXLog.d(TAG, "onScanResultReceived: ${mxBluetoothDevice.getAddress()}")
                if (mxBluetoothDevice.getAddress().equals(connectMac, true)) {
                    val deviceUUID = uuid
                    if (!deviceUUID.isNullOrEmpty())
                        MXMesh.connect(mxBluetoothDevice.getDevice(), deviceUUID) {
                            if (it == MeshState.SUCCESS) {
                                MxScan.stopScan()
                                updateDeviceStatus(localOnline = 1)
                            }
                        }
                }
            }
        }
    }
    var connectMac: String? = null

    @JavascriptInterface
    fun connectDevice(msg: Any, handler: CompletionHandler<JSONObject>?) {
        MXLog.i(TAG, "connectDevice() $msg")
        val deviceUUID = uuid
        if (!deviceUUID.isNullOrEmpty()) {
            MXMesh.disConnect()
            MeshSDKManage.isConnectProxy = false
            if (connectMac == null) {
                connectMac = MeshSDKManage.getAddressIdByUUID(deviceUUID)
                MXLog.d(TAG, "connectMac: $connectMac")
                MxScan.addScanDataListener(scanListener)
            }
            MxScan.startScan(listOf(MxScan.SERVICE_MX))
        }
    }

    @JavascriptInterface
    fun disconnectDevice(msg: Any, handler: CompletionHandler<JSONObject>?) {
        MXLog.i(TAG, "disconnectDevice() $msg")
        MxScan.stopScan()
        MXMesh.disConnect()
        MeshSDKManage.isConnectProxy = true
        MxScan.startScanMeshProxy()
    }

    @JavascriptInterface
    fun setFeedbackGenerator(msg: Any, handler: CompletionHandler<JSONObject>?) {
        //播放震动
        context.applicationContext.vibrateShot(70)
    }

    /**
     * 体脂秤算法计算
     */
    @JavascriptInterface
    fun icBodyFatAlgorithmsRequest(msg: Any, handler: CompletionHandler<JSONObject>) {
        featureProvider?.calculateBodyFat(msg, handler) ?: run {
            MXLog.w(TAG, "体脂秤功能不可用")
            val errorJson = JSONObject()
            errorJson.put("code", -1)
            errorJson.put("message", "体脂秤功能不可用")
            handler.complete(errorJson)
        }
    }

    /**
     * 获取设备状态
     * 格式如下：
     * {"status":1}
     * 说明：status表示设备生命周期，目前有以下几个状态，
     * 0:离线；1：上线；
     */
    @JavascriptInterface
    fun getStatus(msg: Any?, handler: CompletionHandler<JSONObject>) {
        val values = JSONObject()
        values.put("remoteStatus", remoteStatus)
        values.put("localStatus", localStatus)
        if (uuid.isNullOrEmpty() || isShareDevice) {
            values.put("status", remoteStatus)
        } else {
            values.put("status", localStatus or remoteStatus)
        }
        MXLog.d(TAG, "complete: $values")
        handler.complete(values)
    }

    /**
     * 注册iotId监听
     * 注意，此处拿到的 properties，events，services 是物的模型中的物的三要素。
     *
     * @param msg
     * @param handler
     */
    @JavascriptInterface
    fun registThing(msg: Any, handler: CompletionHandler<Any>?) {
        val ret = msg.toString()
        MXLog.d(TAG, "registThing: $ret")
        val obj = JSONObject(ret)
        if (!obj.isNull("messageControl")) {
            messageControl = obj.getInt("messageControl")
        }
        if (!obj.isNull("hex")) {
            isHexTextMsg = obj.getBoolean("hex")
        }
        subscribeHandler = handler
        if (null == timerTask) {
            timerTask = object : TimerTask() {
                override fun run() {
                    val ct = System.currentTimeMillis()
                    if (ct - handlerTime >= 3000) {
                        val jsonObject = delayMessageQueue["mesh"]
                        if (null != jsonObject) {
                            MXLog.d(TAG, "delay setProgressData: $jsonObject")
                            subscribeHandler!!.setProgressData(jsonObject)
                            delayMessageQueue.remove("mesh")
                        }
                    }
                    if (ct - handlerTime >= 3000) {
                        val jsonObject = delayMessageQueue["remote"]
                        if (null != jsonObject) {
                            MXLog.d(TAG, "delay setProgressData: $jsonObject")
                            subscribeHandler!!.setProgressData(jsonObject)
                            delayMessageQueue.remove("remote")
                        }
                    }
                }
            }
            Timer().schedule(timerTask, Date(), 1000)
        }
    }

    private var timerTask: TimerTask? = null

    @JavascriptInterface
    fun hexToProperties(msg: Any, handler: CompletionHandler<Any>?) {
        val msgObject = JSONObject(msg.toString())
        if (msgObject.has("hex")) {
            val hexData = msgObject.getString("hex")
            var dataJson = meshByteArrayToIlops(ByteUtil.hexStringToBytes(hexData))
            val finalJson = JSONObject()
            dataJson?.keys()?.forEach {
                finalJson.put(it, JSONObject().apply { put("value", dataJson.get(it)) })
            }
            handler?.complete(finalJson)
        } else {
            MXLog.e(TAG, "hexToProperties() hex is null")
            handler?.complete(JSONObject())
        }
    }

    private var firstGetPropertiesFull = true

    /**
     * @param msg
     * @param handler 获取设备属性
     * 格式如下：
     * {
     * "_sys_device_mid": {
     * "time": 1516356290173,
     * "value": "example.demo.module-id"
     * },
     * "WorkMode": {
     * "time": 1516347450295,
     * "value": 0
     * },
     * "_sys_device_pid": {
     * "time": 1516356290173,
     * "value": "example.demo.partner-id"
     * }
     * }
     */
    @JavascriptInterface
    fun getPropertiesFull(msg: Any, handler: CompletionHandler<Any>?) {
        MXLog.d(TAG, "getPropertiesFull: $msg")
        val msgObject = JSONObject(msg.toString())
        val isFromNetwork =
            if (msgObject.has("isFromNetwork")) msgObject.getBoolean("isFromNetwork")
            else false
        if (!isFromNetwork && MXMesh.isConnected() && !uuid.isNullOrEmpty() && !isShareDevice) {
            if (firstGetPropertiesFull) {
                //默认先从云端只获取一次，避免H5重复调用
                firstGetPropertiesFull = false
                //云端在线，先从云端获取
                if (remoteStatus == 1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val map = mapOf("iotid" to iotId)
                        MXLog.d(TAG, "first getRemoteProperties")
                        val response = deviceRepository.getAllProperties(map)
                        if (NetState.SUCCESS == response.state) {
                            MXLog.d(TAG, "complete: ${response.data}")
                            //将云端结果给h5
                            if (response.data.isNullOrEmpty()) handler?.complete(JSONObject())
                            else handler?.complete(JSONObject(response.data!!))
                        } else {
                            val errJson = JSONObject()
                            errJson.put("code", response.state.code)
                            errJson.put("message", "${response.state.message}")
                            MXLog.d(TAG, "complete: $errJson")
                            handler?.complete(errJson)
                        }
                        //判断是否需要再次从mesh获取
                        if (MXMesh.isConnected()) {
                            getMeshProperties(msg, null)
                        }
                    }
                } else {
                    getMeshProperties(msg, handler)
                }
            } else {
                getMeshProperties(msg, handler)
            }
        } else {
            getRemoteProperties(handler)
        }
    }

    /**
     * 从云端获取设备属性
     */
    private fun getRemoteProperties(handler: CompletionHandler<Any>?) {
        MXLog.d(TAG, "getRemoteProperties")
        CoroutineScope(Dispatchers.IO).launch {
            val map = mapOf("iotid" to iotId)
            val response = deviceRepository.getAllProperties(map)
            if (NetState.SUCCESS == response.state) {
                MXLog.d(TAG, "complete: ${response.data}")
                if (response.data.isNullOrEmpty()) handler?.complete(JSONObject())
                else handler?.complete(JSONObject(response.data!!))
            } else {
                val errJson = JSONObject()
                errJson.put("code", response.state.code)
                errJson.put("message", "${response.state.message}")
                MXLog.d(TAG, "complete: $errJson")
                handler?.complete(errJson)
            }
        }
    }

    /**
     * 设置设备属性
     */
    @JavascriptInterface
    fun setProperties(msg: Any, handler: CompletionHandler<Any>?) {
        MXLog.d(TAG, "setProperties: $msg")
        val msgObject = JSONObject(msg.toString())
        handlerTime = System.currentTimeMillis() //增加操作下全局callback消息延迟
        val isFromNetwork =
            if (msgObject.has("isFromNetwork")) msgObject.getBoolean("isFromNetwork")
            else false
        if (isFromNetwork || !MXMesh.isConnected() || uuid.isNullOrEmpty() || isShareDevice) {
            setRemoteProperties(msgObject, handler)
        } else {
            setMeshProperties(msgObject, handler)
        }
    }

    /**
     * 音乐律动
     */
    @JavascriptInterface
    fun setPropertiesPre(msg: Any, handler: CompletionHandler<Any>?) {
        MXLog.d(TAG, "setPropertiesPre: $msg")
        try {
            val msgObject = JSONObject(msg.toString())
            val obj = msgObject.getJSONObject("data") ?: return
            val isFromNetwork =
                if (msgObject.has("isFromNetwork")) msgObject.getBoolean("isFromNetwork")
                else false
            if (isFromNetwork || !MXMesh.isConnected() || uuid.isNullOrEmpty() || isShareDevice) {
                //本地无uuid或是共享设备，直接走远程
                MXLog.w(TAG, "暂不支持或设备本地离线")
            } else {
                val enable = obj.getJSONObject("LightScene").getInt("Enable")
                if (XXPermissions.isGranted(context, Permission.RECORD_AUDIO)) {
                    startRhythm(enable)
                } else {
                    XXPermissions.with(context)
                        .permission(Permission.RECORD_AUDIO)
                        .request(object : OnPermissionCallback {
                            override fun onGranted(
                                permissions: MutableList<String>,
                                all: Boolean
                            ) {
                                if (all) {
                                    startRhythm(enable)
                                }
                            }

                            override fun onDenied(
                                permissions: MutableList<String>,
                                never: Boolean
                            ) {
                                if (never) {
                                    XXPermissions.startPermissionActivity(context, permissions)
                                }
                            }
                        })
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun startRhythm(enable: Int) {
        // 使用组合模式调用音乐律动功能
        featureProvider?.startRhythm(enable, uuid) ?: MXLog.w(TAG, "音乐律动功能不可用")
    }


    @JavascriptInterface
    fun actionLinkage(msg: Any?, handler: CompletionHandler<Any>?) {
        if (msg != null) {
            var specKey = -1
            var opCode: String? = null
            val jsonObject = JSONObject(msg.toString())
            val isFromNetwork =
                if (jsonObject.has("isFromNetwork")) jsonObject.getBoolean("isFromNetwork")
                else false
            try {
                val data = jsonObject.getJSONObject("data")
                opCode = data.getString("opCode") //大端
                specKey = data.getInt("specKey")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (specKey == -1) {
                MXLog.e(TAG, "specKey 为空")
                return
            }
            if (TextUtils.isEmpty(opCode)) {
                MXLog.e(TAG, "attrType 为空")
                return
            }
//            val bytes: ByteArray = ByteUtil.hexStringToBytes(opCode)
//            //将byte1 高位置1
//            val byt: Byte = ByteUtil.setSpecifiedBitTo1(bytes[0], 7)
//            val attrType: String = ByteUtil.bytesToHexString(byteArrayOf(byt, bytes[1]))
//            val paramHex = MeshSDKManage.smallToBig(attrType) + "0$specKey"
//            MXMesh.sendGroupMessage(
//                0xD003,
//                MeshOpcodeConstants.VENDOR_MSG_OPCODE_RECEIVE,
//                paramHex,
//                pretendSrcDeviceIdentify = uuid
//            )
            uuid?.apply {
                val node = MXMesh.getNode(this)
                if (node != null) {
                    val address = String.format("%04X", node.unicastAddress)
//                    var address = node.unicastAddress.toString(16)
//                    while (address.length != 4) {
//                        address = "0$address"
//                    }
                    val paramHex =
                        "1000" + MeshSDKManage.stringReversedToLittleEndian(address) + MeshSDKManage.stringReversedToLittleEndian(
                            opCode!!
                        ) + "0$specKey"
                    val requireNetwork = isFromNetwork || uuid.isNullOrEmpty()
                    MeshSDKManage.senDeviceGroupMsg(
                        0xD003, paramHex, requireNetworkExecute = requireNetwork
                    ) { map ->
                        CoroutineScope(Dispatchers.IO).launch {
                            deviceRepository.sendCloudMeshMessage(map)
                        }
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun sendMeshMessage(msg: Any?, handler: CompletionHandler<Any>?) {
        MXLog.d(TAG, "sendMeshMessage: $msg")
        if (null == msg) return
        val msgObject = JSONObject(msg.toString())
        if (!msgObject.has("msgBody")) {
            return
        }
        val hexParam = msgObject.getString("msgBody")

        val opcode = if (msgObject.has("opCode")) msgObject.getString("opCode")
        else MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET_UNACK

        if (msgObject.has("groupAddress")) {
            val meshAddress = msgObject.getString("groupAddress")
            val repeatNum = if (msgObject.has("repeatNum")) msgObject.getInt("repeatNum")
            else 2
            val isFromNetwork =
                if (msgObject.has("isFromNetwork")) msgObject.getBoolean("isFromNetwork")
                else false
            MeshSDKManage.senDeviceGroupMsg(
                meshAddress.toInt(16), hexParam, repeatNum, isFromNetwork, opcode
            ) { map ->
                CoroutineScope(Dispatchers.IO).launch {
                    deviceRepository.sendCloudMeshMessage(map)
                }
            }
            setPropertiesSuccess(handler)
        } else {
            val finalUuID = if (msgObject.has("dstUUID")) msgObject.getString("dstUUID")
            else uuid
            val isFromNetwork =
                if (msgObject.has("isFromNetwork")) msgObject.getBoolean("isFromNetwork")
                else false
            if (isFromNetwork || !MXMesh.isConnected()) {
                //api执行
                MXMesh.getNode(finalUuID!!)?.unicastAddress?.apply {
                    val realOpCodeBytes = MeshParserUtils.createVendorOpCode(
                        opcode.toInt(16), 0x0922
                    )
                    val realOpcode = ByteUtil.bytesToHexString(realOpCodeBytes)//D32209
                    MeshSDKManage.parseDataToCloud(realOpcode, this, hexParam, 1) { map ->
                        CoroutineScope(Dispatchers.IO).launch {
                            deviceRepository.sendCloudMeshMessage(map)
                        }
                    }
                }
            } else {
                val timeoutMills = if (msgObject.has("timeout")) msgObject.getLong("timeout")
                else 0L
                MeshSDKManage.setMsg(
                    opcode, finalUuID!!, hexParam, object : MapCallback() {
                        override fun onResult(resultMap: Map<String, Any>) {
                            setPropertiesSuccess(handler, resultMap)
                        }

                        override fun onError(callbackMsg: CallbackMsg) {
                            super.onError(callbackMsg)
                            resultFailed(handler, callbackMsg.code, callbackMsg.msg)
                        }
                    }, isTimeOut = timeoutMills > 0, isRetry = false, timeoutMillis = timeoutMills
                )
            }
        }
    }

    /**
     * 接收到更新设备状态消息
     */
    fun updateDeviceStatus(remoteOnline: Int? = null, localOnline: Int? = null) {
        MXLog.d(TAG, "updateDeviceStatus")
        subscribeHandler?.apply {
            val jsonObject = JSONObject()
            remoteOnline?.apply {
                remoteStatus = this
            }
            localOnline?.apply {
                localStatus = this
            }
            jsonObject.put("type", "status")
            val values = JSONObject()
            values.put("remoteStatus", remoteStatus)
            values.put("localStatus", localStatus)
            if (uuid.isNullOrEmpty() || isShareDevice) {
                values.put("value", remoteStatus)
            } else {
                values.put("value", localStatus or remoteStatus)
            }
            jsonObject.put("data", values)
            MXLog.d(TAG, "setProgressData: $jsonObject")
            this.setProgressData(jsonObject)
        }
    }

    /**
     * 接收到mesh属性消息
     */
    fun updateMeshProperties(dtMap: Map<String, Any>) {
        MXLog.d(TAG, "updateMeshProperties")
        localStatus = 1 //收到mesh数据，设备在线
        subscribeHandler?.apply {
            val jsonObject = JSONObject()
            if (isHexTextMsg) {
                jsonObject.put("type", "hex")
                if (dtMap.containsKey(MeshConstants.KEY_DATA)) {
                    val data = dtMap[MeshConstants.KEY_DATA] as ByteArray
                    jsonObject.put(
                        "data", ByteUtil.bytesToHexString(data)
                    )
                } else jsonObject.put("data", "")
            } else {
                jsonObject.put("type", "property")
                //卡片单控时显示8s后隐藏
                val byteArray = dtMap[MeshConstants.KEY_DATA] as ByteArray
                val finalJson = JSONObject()
                val dataJson = meshByteArrayToIlops(byteArray)
                dataJson?.keys()?.forEach {
                    finalJson.put(it, JSONObject().apply { put("value", dataJson.get(it)) })
                }
                jsonObject.put("data", finalJson)
            }
            if (messageControl == 1) {
                if (System.currentTimeMillis() - handlerTime >= 3000) { //过滤操作3s内的云端推送消息
                    MXLog.d(TAG, "setProgressData: $jsonObject")
                    this.setProgressData(jsonObject)
                } else {
                    MXLog.d(TAG, "push to delay queue->$jsonObject")
                    delayMessageQueue["mesh"] = jsonObject
                }
            } else {
                MXLog.d(TAG, "setProgressData: $jsonObject")
                this.setProgressData(jsonObject)
            }
        }
    }

    /**
     * 接收到云端属性消息
     */
    fun updateRemoteProperties(itemJson: JSONObject) {
        MXLog.d(TAG, "updateRemoteProperties")
        remoteStatus = 1//收到设备的长连接数据，认为设备在线
        val jsonObject = JSONObject()
        jsonObject.put("type", "property")
        jsonObject.put("data", itemJson)
        if (messageControl == 1) { //需要消息延迟
            if (System.currentTimeMillis() - handlerTime >= 3000) { //过滤操作10s内的云端推送消息
                MXLog.d(TAG, "setProgressData: $jsonObject")
                subscribeHandler?.setProgressData(jsonObject)
            } else {
                MXLog.d(TAG, "push to delay queue->$jsonObject")
                delayMessageQueue["remote"] = jsonObject
            }
        } else {
            MXLog.d(TAG, "setProgressData: $jsonObject")
            subscribeHandler?.setProgressData(jsonObject)
        }
    }

    private fun meshByteArrayToIlops(byteArray: ByteArray): JSONObject? {
        return if (productAttrConfig.isEmpty()) {
            val homeAttrConfig = AppConfigManager.getAttrConfig()
            if (homeAttrConfig.isNullOrEmpty()) {
                MXLog.w(TAG, "home attrConfig is null")
                null
            } else {
                MXLog.i(TAG, "use home attrConfig")
                val attrJson = JSONObject(homeAttrConfig)
                //未配置产品独立的mesh转码，使用大json配置
                val dataMap = MeshAttrUtil.parseData(byteArray)
                MeshTranscodeUtil.attrTypeToProperties(attrJson, dataMap, true)
            }
        } else {
            MXLog.i(TAG, "use product attrConfig")
            val attrJson = JSONObject(productAttrConfig)
            //从产品的mesh转码配置中查找
            val dataMap = MeshAttrUtil.parseDataByProductAttr(attrJson, byteArray)
            MeshTranscodeUtil.attrTypeToProperties(attrJson, dataMap, true)
        }
    }

    /**
     * 主动获取设备属性
     */
    private fun getMeshProperties(msg: Any, handler: CompletionHandler<Any>?) {
        val ret = msg.toString()
        val obj = JSONObject(ret)
        if (obj.isNull("hex")) {
            //获取指定data中的数据
            val paramHex = if (obj.has("data")) {
                val attrMap = productAttrConfig.ifEmpty { AppConfigManager.getAttrConfig() }
                if (attrMap.isNullOrEmpty()) {
                    ""
                } else {
                    val attrTypeList = mutableListOf<String>()
                    when (val data = obj.get("data")) {
                        is JSONArray -> {
                            for (ind in 0 until data.length()) {
                                MeshTranscodeUtil.getAttrTypeByIdentifier(
                                    JSONObject(attrMap), data.getString(ind)
                                )?.apply {
                                    attrTypeList.add(this)
                                }
                            }
                        }

                        is String -> {
                            MeshTranscodeUtil.getAttrTypeByIdentifier(
                                JSONObject(attrMap), data
                            )?.apply {
                                attrTypeList.add(this)
                            }
                        }

                        else -> {}
                    }
                    val sb = StringBuilder()
                    attrTypeList.forEach {
                        sb.append(MeshSDKManage.stringReversedToLittleEndian(it))
                    }
                    sb.toString()
                }
            } else ""
            MeshSDKManage.getMsg(uuid!!, paramHex, object : MapCallback() {
                override fun onResult(resultMap: Map<String, Any>) {
                    val finalJson = JSONObject()
                    if (resultMap.containsKey(MeshConstants.KEY_DATA)) {
                        val byteArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                        meshByteArrayToIlops(byteArray)?.apply {
                            keys().forEach {
                                val vale = JSONObject()
                                vale.put("value", this.get(it))
                                finalJson.put(it, vale)
                            }
                        }
                    }
                    if (handler == null) {
                        //走监听，需要额外包装一层
                        val jsonObject = JSONObject()
                        jsonObject.put("type", "property")
                        jsonObject.put("data", finalJson)
                        MXLog.d(TAG, "setProgressData: $jsonObject")
                        subscribeHandler?.setProgressData(jsonObject)
                    } else {
                        MXLog.d(TAG, "complete: $finalJson")
                        handler.complete(finalJson)
                    }
                }

                override fun onError(callbackMsg: CallbackMsg) {
                    super.onError(callbackMsg)
                    resultFailed(handler, callbackMsg.code, callbackMsg.msg)
                }
            }, timeout = true, retry = true, 5000)
        } else {
            val hex = obj.getString("hex")
            MeshSDKManage.getMsg(uuid!!, hex, object : MapCallback() {
                override fun onResult(resultMap: Map<String, Any>) {
                    if (resultMap.containsKey(MeshConstants.KEY_DATA)) {
                        val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                        if (isHexTextMsg) {
                            val resultHex = ByteUtil.bytesToHexString(dataArray)
                            if (handler == null) {
                                val jsonObject = JSONObject()
                                jsonObject.put("type", "property")
                                jsonObject.put("hex", resultHex)
                                MXLog.d(TAG, "setProgressData: $jsonObject")
                                subscribeHandler?.setProgressData(jsonObject)
                            } else {
                                MXLog.d(TAG, "complete: $resultHex")
                                handler.complete(resultHex)
                            }
                        } else {
                            val finalJson = JSONObject()
                            meshByteArrayToIlops(dataArray)?.apply {
                                keys().forEach {
                                    val vale = JSONObject()
                                    vale.put("value", this.get(it))
                                    finalJson.put(it, vale)
                                }
                            }
                            if (handler == null) {
                                val jsonObject = JSONObject()
                                jsonObject.put("type", "property")
                                jsonObject.put("data", finalJson)
                                MXLog.d(TAG, "setProgressData: $jsonObject")
                                subscribeHandler?.setProgressData(jsonObject)
                            } else {
                                MXLog.d(TAG, "complete: $finalJson")
                                handler.complete(finalJson)
                            }
                        }
                    }
                }

                override fun onError(callbackMsg: CallbackMsg) {
                    super.onError(callbackMsg)
                    resultFailed(handler, callbackMsg.code, callbackMsg.msg)
                }
            }, timeout = true, retry = true, timeoutMillis = 5_000)
        }
    }

    /**
     * 设置属性成功回调H5
     */
    private fun setPropertiesSuccess(
        handler: CompletionHandler<Any>?, dtMap: Map<String, Any>? = null
    ) {
        val jsonObject = JSONObject()
        jsonObject.put("code", 0)
        jsonObject.put("success", true)
        if (dtMap?.containsKey(MeshConstants.KEY_DATA) == true) {
            val byteArray = dtMap[MeshConstants.KEY_DATA] as ByteArray
            jsonObject.put("message", ByteUtil.bytesToHexString(byteArray))
        }
        MXLog.d(TAG, "complete: $jsonObject")
        handler?.complete(jsonObject)
    }

    private fun resultFailed(handler: CompletionHandler<Any>?, code: Int, msg: String) {
        val errJson = JSONObject()
        errJson.put("code", code)
        errJson.put("message", msg)
        MXLog.d(TAG, "complete: $errJson")
        handler?.complete(errJson)
    }

    /**
     * 通过mesh设置属性
     */
    private fun setMeshProperties(msgObject: JSONObject, handler: CompletionHandler<Any>?) {
        var hexParam: String? = null
        when {
            msgObject.has("hex") -> {
                hexParam = msgObject.getString("hex")
            }

            msgObject.has("data") -> {
                val paramMap = HashMap<String, Any>()
                JsonUtil.parseJsonObjectToMap(msgObject.getJSONObject("data"), paramMap)
                val kvIdentifier = paramMap.filter { isMeshRuleKv(it.key) }
                if (kvIdentifier.isNotEmpty()) {
                    kvIdentifier.forEach { (key, value) ->
                        //比如 "KV61FB"："01"。 那就写入 2B 00 FB 61 01， 2B 00表示KV读写读写命令
                        if (key.length == 6) {
                            val meshValue = when (value) {
                                is Number -> String.format("%02X", value.toInt())
                                is String -> value
                                else -> ""
                            }
                            val kvHex = "2B00" + key.substring(4) + key.substring(2, 4) + meshValue
                            hexParam += kvHex
                        } else {
                            MXLog.w(TAG, "set KV config，key length is not 6")
                            hexParam = null
                        }
                    }
                } else {
                    val attrMap = productAttrConfig.ifEmpty { AppConfigManager.getAttrConfig() }
                    if (attrMap.isNullOrEmpty()) {
                        MXLog.w(TAG, "attrConfigJson is empty")
                    } else {
                        val attrConfigJson = JSONObject(attrMap)
                        val list = mutableListOf<Pair<String, String>>()
                        list.addAll(
                            MeshTranscodeUtil.propertiesToAttrType(
                                attrConfigJson,
                                paramMap
                            )
                        )
                        hexParam =
                            if (list.isEmpty()) {
                                null
                            } else {
                                MeshSDKManage.combineAttTypeAndAttValue(list)
                            }
                    }
                }
            }

            else -> {
                hexParam = null
            }
        }
        MXLog.i(TAG, "setMeshProperties hexParam : $hexParam")
        hexParam?.apply {
            val ackType = msgObject.optString("messageType")
            if ("unack" == ackType) {
                MeshSDKManage.setUnAckMsg(uuid!!, hexParam)
            } else {
                MeshSDKManage.setMsg(uuid!!, hexParam, object : MapCallback() {
                    override fun onResult(resultMap: Map<String, Any>) {
                        setPropertiesSuccess(handler)
                    }

                    override fun onError(callbackMsg: CallbackMsg) {
                        super.onError(callbackMsg)
                        resultFailed(handler, callbackMsg.code, callbackMsg.msg)
                    }
                })
            }
        }
    }

    /**
     * 通过云端api设置属性
     */
    private fun setRemoteProperties(
        msgObject: JSONObject, handler: CompletionHandler<Any>?
    ) {
        if (msgObject.has("hex")) {
            val address = MXMesh.getNode(uuid!!)?.unicastAddress ?: return
            val params = msgObject.getString("hex")
            val ackType = msgObject.optString("messageType")
            val opcode = if ("unack" == ackType) {
                MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET_UNACK
            } else MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET
            val realOpCodeBytes = MeshParserUtils.createVendorOpCode(
                opcode.toInt(16), 0x0922
            )
            val realOpcode = ByteUtil.bytesToHexString(realOpCodeBytes)//D32209
            MeshSDKManage.parseDataToCloud(realOpcode, address, params, 1) { map ->
                CoroutineScope(Dispatchers.IO).launch {
                    val response = deviceRepository.sendCloudMeshMessage(map)
                    if (NetState.SUCCESS == response.state) {
                        setPropertiesSuccess(handler)
                    }
                }
            }
        } else {
            if (msgObject.has("data")) {
                val paramMap = HashMap<String, Any>()
                JsonUtil.parseJsonObjectToMap(msgObject.getJSONObject("data"), paramMap)
                CoroutineScope(Dispatchers.IO).launch {
                    val param = mapOf("iotid" to iotId, "items" to paramMap)
                    val response = deviceRepository.setProperties(param)
                    if (NetState.SUCCESS == response.state) {
                        setPropertiesSuccess(handler)
                    }
                }
            }
        }
    }

    fun setDeviceWarnInfo(data: String) {
        val dataObject = JSONObject(data)
        if (dataObject.has("iotId")) {
            val iotId = dataObject.getString("iotId")
            MXLog.i(TAG, "setDeviceInfo--> data: $data,  this.iotId: ${this.iotId}")
            if (iotId == this.iotId) {
                val jsonObject = JSONObject()
                jsonObject.put("type", "event")
                jsonObject.put("data", dataObject)
                subscribeHandler?.apply {
                    MXLog.d(TAG, "setProgressData: $jsonObject")
                    this.setProgressData(jsonObject)
                }
            }
        }
    }
}