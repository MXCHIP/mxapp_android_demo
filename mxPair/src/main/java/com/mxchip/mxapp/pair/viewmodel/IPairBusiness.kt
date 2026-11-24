package com.mxchip.mxapp.pair.viewmodel

import com.mxchip.mxapp.base.bean.DeviceRuleBean

interface IPairBusiness {

    fun getFogRandom(paramMap: Map<String, Any>, result: (String?) -> Unit)

    fun getFogBleKey(paramMap: Map<String, Any>, result: (String?) -> Unit)

    fun getFogConnectState(paramMap: Map<String, Any>, result: (connected: Boolean) -> Unit)

    fun getAliCipher(paramMap: Map<String, Any>, result: (String?) -> Unit)

    fun getPairGuideInfo(productKey: String, result: (String?) -> Unit)

    /**
     * 配网设备成功执行下一步，跳转成功页面
     */
    fun getPairSuccessfulRoute(): String

    fun setDeviceIdentifier(identifier: String, pk: String, compositeProductKey: String?)

    fun getProductKeyPairByIdentifier(identifier: String): Pair<String, String?>?

    fun getDeviceMeshAddress(
        uuid: String, replaceIotId: String?, meshAddressResult: (meshAddress: Int?) -> Unit
    )

    /**
     * 在网关设备发送Wi-Fi前，执行逻辑
     */
    fun checkGatewayConfig(uuid: String, linkTypeId: Int, result: (Boolean) -> Unit)

    fun executeBind(
        identifier: String,
        bindParams: MutableMap<String, Any>,
        roomAddress: Int?,
        result: (iotId:String?) -> Unit
    )

    fun getReplaceDeviceRule(
        uuid: String,
        replaceIotId: String,
        roomAddress: Int? = null,
        result: (DeviceRuleBean?) -> Unit
    )

    fun replaceDeviceCheckSyncGatewayData(bindParams: MutableMap<String, Any>, result: (Boolean) -> Unit)
    fun executeReplaceBind(
        uuid: String?, bindParams: MutableMap<String, Any>, result: (Boolean) -> Unit
    )

    /**
     * 获取替换成功后跳转路由地址
     */
    fun getReplaceSuccessfulRoute(): String

    fun restoreAPGateway(
        productKey: String,
        deviceName: String,
        result: (Boolean, Int, String?) -> Unit
    )
}