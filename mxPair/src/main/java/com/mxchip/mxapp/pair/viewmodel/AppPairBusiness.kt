package com.mxchip.mxapp.pair.viewmodel

import com.mxchip.http.response.NetState
import com.mxchip.link.utils.LinkByteUtil
import com.mxchip.mxapp.base.bean.DeviceRuleBean
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.pair.api.AppPairRepository
import com.mxchip.mxapp.pair.bean.BindDeviceResponse
import com.mxchip.mxapp.pair.bean.PairSuccessDevice
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject


/**
 *配网绑定真实逻辑
 */
open class AppPairBusiness : IPairBusiness {

    private val apiRepository by lazy {
        AppPairRepository()
    }

    /**
     * {identifier:{pk,compositeProductKey}}pk和复合pk对
     */
    private val pairingIdentifierMap by lazy { mutableMapOf<String, Pair<String, String?>>() }

    override fun getFogRandom(paramMap: Map<String, Any>, result: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.getFogRandom(paramMap)
            result.invoke(response.data)
        }
    }

    override fun getFogBleKey(paramMap: Map<String, Any>, result: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.getFogBleKey(paramMap)
            result.invoke(response.data)
        }
    }

    override fun getFogConnectState(
        paramMap: Map<String, Any>,
        result: (connected: Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.getFogConnectState(paramMap)
            if (response.data.isNullOrEmpty()) {
                result.invoke(false)
            } else {
                val dataJson = JSONObject(response.data!!)
                result.invoke(dataJson.optBoolean("connected"))
            }
        }
    }

    override fun getAliCipher(paramMap: Map<String, Any>, result: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.getAliCipher(paramMap)
            result.invoke(response.data)
        }
    }

    override fun getPairGuideInfo(productKey: String, result: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.getPairGuideInfo(productKey)
            result.invoke(response.data)
        }
    }

    override fun getPairSuccessfulRoute(): String {
        pairingIdentifierMap.clear()
        //配网成功，选择房间页面
        return RouterConstants.PAIR_SUCCESS_ACTIVITY
    }

    override fun setDeviceIdentifier(identifier: String, pk: String, compositeProductKey: String?) {
        pairingIdentifierMap[identifier] = Pair(pk, compositeProductKey)
    }

    override fun getProductKeyPairByIdentifier(identifier: String): Pair<String, String?>? =
        pairingIdentifierMap[identifier]

    override fun getDeviceMeshAddress(
        uuid: String, replaceIotId: String?, meshAddressResult: (meshAddress: Int?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            //0 - 设备单播地址；1 - 用户单播地址
            val params = mutableMapOf(
                "home_id" to AppConfigManager.getUseHomeId(), "type" to 0, "uuid" to uuid
            )
            if (!replaceIotId.isNullOrEmpty()) {
                params["iotid"] = replaceIotId
            }
            val response = apiRepository.getMeshAddress(params)
            if (NetState.SUCCESS == response.state) {
                if (response.data.isNullOrEmpty()) {
                    meshAddressResult.invoke(null)
                } else {
                    val address = JSONObject(response.data!!).getInt("mesh_address")
                    meshAddressResult.invoke(address)
                }
            } else meshAddressResult.invoke(null)
        }
    }

    private fun sendGatewayConfigure(uuid: String, config: String, sendResult: (Boolean) -> Unit) {
        //attrType ==0015
        val hex = "1500" + LinkByteUtil.bytesToHexString(config.toByteArray())
        MeshSDKManage.setMsg(uuid, hex, object : MapCallback() {
            override fun onResult(resultMap: Map<String, Any>) {
                var result = -1
                if (resultMap.containsKey(MeshConstants.KEY_DATA)) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //减去前面2字节 attrType
                    val wifiStateArray = ByteArray(dataArray.size - 2)
                    System.arraycopy(dataArray, 2, wifiStateArray, 0, wifiStateArray.size)
                    result = LinkByteUtil.bytesToInt(wifiStateArray)
                }
                sendResult.invoke(result == 0)
            }

            override fun onError(callbackMsg: CallbackMsg) {
                super.onError(callbackMsg)
                sendResult.invoke(false)
            }
        })
    }

    override fun checkGatewayConfig(uuid: String, linkTypeId: Int, result: (Boolean) -> Unit) {
        val mxcloud = PairManager.getGatewayMxCloudHost()
        val fogConfig = PairManager.getGatewayFogConfig()
        when {
            mxcloud.isNullOrEmpty() && fogConfig == null -> {
                result.invoke(true)
            }

            !mxcloud.isNullOrEmpty() && fogConfig != null -> { //均配置，需要都成功
                val fogApi = fogConfig.first
                val fogMqtt = fogConfig.second
                sendGatewayConfigure(
                    uuid,
                    "{\"arg\":\"mxcloud\",\"val\":\"$mxcloud\"}"
                ) { mxCloudResult ->
                    if (mxCloudResult) {
                        sendGatewayConfigure(
                            uuid,
                            "{\"arg\":\"fog\",\"val\":{\"mqtt\":\"$fogMqtt\",\"http\":\"$fogApi\"}}",
                            result
                        )
                    } else {
                        result.invoke(false)
                    }
                }
            }

            !mxcloud.isNullOrEmpty() -> {
                sendGatewayConfigure(
                    uuid,
                    "{\"arg\":\"mxcloud\",\"val\":\"$mxcloud\"}", result
                )
            }

            fogConfig != null -> {
                val fogApi = fogConfig.first
                val fogMqtt = fogConfig.second
                sendGatewayConfigure(
                    uuid,
                    "{\"arg\":\"fog\",\"val\":{\"mqtt\":\"$fogMqtt\",\"http\":\"$fogApi\"}}",
                    result
                )
            }
        }
    }

    override fun executeBind(
        identifier: String,
        bindParams: MutableMap<String, Any>,
        roomAddress: Int?,
        result: (String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val bindResp = async {
                bindParams["home_id"] = AppConfigManager.getUseHomeId()
                pairingIdentifierMap[identifier]?.second?.apply {
                    bindParams["composite_product_key"] = this
                }
                apiRepository.bindDevice(bindParams)
            }
            val bind = bindResp.await()
            if (NetState.SUCCESS == bind.state) {
                if (null != bind.data) {
                    val data = JsonUtil.fromJson(bind.data, BindDeviceResponse::class.java)
                    val iotId = data!!.iotid
                    if (bindParams.containsKey("uuid")) {//mesh设备
                        val uuid = bindParams["uuid"] as String
                        MeshSDKManage.sendPairedSuccessful(uuid) {
                            val successDevice = PairSuccessDevice(iotId).apply {
                                this.productKey = bindParams["product_key"].toString()
                                this.uuid = uuid
                            }
                            if (PairDataCenter.pairSuccessfulList.contains(
                                    successDevice
                                )
                            ) {
                                PairDataCenter.pairSuccessfulList.remove(successDevice)
                            }
                            PairDataCenter.pairSuccessfulList.add(successDevice)
                            getVersionToCloud(uuid, iotId) {
                                result.invoke(iotId)
                            }
                        }
                    } else {//非mesh设备
                        val successDevice = PairSuccessDevice(iotId).apply {
                            this.productKey = bindParams["product_key"].toString()
                        }
                        if (PairDataCenter.pairSuccessfulList.contains(
                                successDevice
                            )
                        ) {
                            PairDataCenter.pairSuccessfulList.remove(successDevice)
                        }
                        PairDataCenter.pairSuccessfulList.add(successDevice)
                        result.invoke(iotId)
                    }
                } else result.invoke(null)
            } else result.invoke(null)
        }
    }

    /**
     * 发送消息从设备端获取version 发送到云平台
     */
    private fun getVersionToCloud(
        uuid: String, iotId: String, result: (Boolean) -> Unit
    ) {
        MeshSDKManage.getVersion(uuid) { version ->
            result.invoke(true)//只获取version，失败不影响配网结果
            if (!version.isNullOrEmpty())
                CoroutineScope(Dispatchers.IO).launch {
                    apiRepository.updateOTAVersion(mapOf("iotid" to iotId, "version" to version))
                }
        }
    }

    override fun getReplaceDeviceRule(
        uuid: String, replaceIotId: String, roomAddress: Int?, result: (DeviceRuleBean?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            //一键替换流程
            val replaceResp = apiRepository.findGroupsAndRulesByIotId(
                AppConfigManager.getUseHomeId(), replaceIotId
            )
            if (replaceResp.state == NetState.SUCCESS && !replaceResp.data.isNullOrEmpty()) {
                MeshSDKManage.sendPairedSuccessful(uuid) {
                    val data =
                        JsonUtil.fromJson(replaceResp.data, DeviceRuleBean::class.java)
                    data!!.room_address = roomAddress
                    result.invoke(data)
                }
            } else result.invoke(null)
        }
    }

    override fun replaceDeviceCheckSyncGatewayData(
        bindParams: MutableMap<String, Any>,
        result: (Boolean) -> Unit
    ) {
        result.invoke(true)
    }

    override fun executeReplaceBind(
        uuid: String?, bindParams: MutableMap<String, Any>, result: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            bindParams["home_id"] = AppConfigManager.getUseHomeId()
            val bindResp = async {
                apiRepository.bindDevice(bindParams)
            }
            val bind = bindResp.await()
            if (NetState.SUCCESS == bind.state) {
                if (bind.data.isNullOrEmpty()) {
                    result.invoke(false)
                } else {
                    if (uuid.isNullOrEmpty()) {
                        result.invoke(true)
                    } else {
                        val data = JsonUtil.fromJson(bind.data, BindDeviceResponse::class.java)
                        val iotId = data!!.iotid
                        getVersionToCloud(uuid, iotId, result)
                    }
                    //一键替换成功，通知刷新设备列表
                    LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                        .postValue(HomeDataRefresh.DEVICE)
                }
            } else {
                result.invoke(false)
            }
        }
    }

    override fun getReplaceSuccessfulRoute(): String {
        //替换成功，跳转到智家精灵主页
        return RouterConstants.MAIN_ACTIVITY
    }

    override fun restoreAPGateway(
        productKey: String,
        deviceName: String,
        result: (Boolean, Int, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.restoreAPGateway(
                mapOf(
                    "product_key" to productKey,
                    "device_name" to deviceName
                )
            )
            //成功或者设备离线状态
            if (response.state == NetState.SUCCESS) {
                result.invoke(true, response.state.code, null)
            } else {
                result.invoke(false, response.state.code, response.state.message)
            }
        }
    }
}