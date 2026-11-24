package com.mxchip.mxapp.pair.viewmodel

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.viewModelScope
import com.mxchip.link.bean.DeviceCertificate
import com.mxchip.link.bean.FogEnvConfig
import com.mxchip.link.bean.LinkFYParams
import com.mxchip.link.bean.LinkFogParams
import com.mxchip.link.bean.LinkMeshParams
import com.mxchip.link.bean.LinkTMParams
import com.mxchip.link.bean.LinkWiFiInfo
import com.mxchip.link.consts.LinkPlatform
import com.mxchip.link.consts.LinkState
import com.mxchip.link.pair.FYPair
import com.mxchip.link.pair.FogPair
import com.mxchip.link.pair.IALPairFun
import com.mxchip.link.pair.IFogPairFun
import com.mxchip.link.pair.IMeshPairFun
import com.mxchip.link.pair.MeshPair
import com.mxchip.link.pair.TMPair
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.base.bean.DeviceRuleBean
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.mesh.MeshCommand
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.DeviceManage
import com.mxchip.mxapp.base.utils.DeviceWriteConfigManage
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.pair.bean.PairDeviceBean
import com.mxchip.mxapp.pair.consts.MxLinkType
import com.mxchip.mxapp.pair.consts.PairConstants
import com.mxchip.mxapp.pair.consts.PairState
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.mxapp.pair.utils.PairUtils
import com.mxchip.mxapp.pair.utils.PairWiFiSPManage
import com.mxchip.scanner.MxScan
import com.mxchip.scanner.bean.MxBluetoothDevice
import com.mxchip.scanner.callback.MxScanListener
import com.mxchip.scanner.callback.MxScanType
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.bean.MeshState
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.sdk.mesh.util.MeshMessageUtil
import com.mxchip.utils.ByteUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 *配网设备
 */
class PairViewModel : BaseViewModel(), IMeshPairFun, IFogPairFun, IALPairFun {
    companion object {
        private const val TAG = "PairViewModel"
    }

    private val _pairBusiness by lazy {
        //未设置配网，默认mxapp配置
        val pariBusiness = PairManager.getPairBusiness()
        if (pariBusiness == null) PairManager.setPairBusiness(AppPairBusiness())
        PairManager.getPairBusiness()!!
    }

    //扫描设备过滤已发现设备（ble/ble-mesh/Wi-Fi）
    private val foundDataIdentifyFilter by lazy { mutableListOf<String>() }

    private val meshCommand by lazy { MeshCommand() }

    //自动发现设备
    private val _autoFindDevices by lazy { MutableStateFlow<PairDeviceBean?>(null) }
    val autoFindDevices: StateFlow<PairDeviceBean?> = _autoFindDevices

    private var _filter: ((ProductInfo, String?) -> Boolean)? = null

    private var _meshPair: MeshPair? = null
    private var _fogPair: FogPair? = null
    private var _fyPair: FYPair? = null
    private var _tmPair: TMPair? = null

    //配网状态
    private val _pairState by lazy { MutableStateFlow<PairState?>(null) }
    val pairState: StateFlow<PairState?> = _pairState

    private val mxScanListener by lazy {
        object : MxScanListener {

            override fun onScanResultReceived(mxBluetoothDevice: MxBluetoothDevice) {
                when (mxBluetoothDevice.mxScanType) {
                    MxScanType.PROVISIONING -> {
                        val uuid = mxBluetoothDevice.uuid
                        if (uuid.isNullOrEmpty()) return
                        PairManager.getScanRssiThreshold()?.apply {
                            val rssi = mxBluetoothDevice.getRSSI()
                            if (rssi < this) {
                                MXLog.w(TAG, "dump device rssi: $rssi ,set threshold rssi: $this")
                                return
                            }
                        }
                        if (!foundDataIdentifyFilter.contains(uuid)) {
                            val pk = DeviceManage.getPkByUUID(uuid)
                            MXLog.d(TAG, "find product pk: $pk")
                            //先检查pk（fog以pk为key）
                            var pairInfoBean = ProductInfoManager.getProductInfo(pk)
                            if (pairInfoBean.isNullOrEmpty()) {
                                val pid = DeviceManage.getFeiYanPidByUUID(uuid).toString()
                                MXLog.d(TAG, "find product pid: $pid")
                                //pid换pk
                                ProductInfoManager.getProductPkByPid(pid)?.apply {
                                    pairInfoBean = ProductInfoManager.getProductInfo(this)
                                }
                            }
                            if (pairInfoBean.isNullOrEmpty())
                                MXLog.w(TAG, "product does not exist")
                            else {
                                val product =
                                    JsonUtil.fromJson(pairInfoBean, ProductInfo::class.java)
                                if (product == null) {
                                    MXLog.e(TAG, "product is null")
                                } else {
                                    var compositeProductKey: String? = null
                                    //复合产品
                                    if (product.is_composite == 1) {
                                        val index =
                                            PairUtils.getCompositeProductIndexByUUIDFeatureFlag1(
                                                uuid
                                            )
                                        MXLog.d(TAG, "复合产品 index:$index")
                                        compositeProductKey =
                                            product.composite_products.firstOrNull { compositeProductInfo -> compositeProductInfo.index == index }?.product_key
                                    }
                                    if (_filter == null || _filter!!.invoke(
                                            product,
                                            compositeProductKey
                                        )
                                    ) {
                                        postFoundDeviceProduct(
                                            uuid,
                                            mxBluetoothDevice,
                                            product,
                                            compositeProductKey
                                        )
                                    }
                                }
                            }
                        }
                    }

                    MxScanType.MX -> {
                        val mac = mxBluetoothDevice.getAddress()
                        if (!foundDataIdentifyFilter.contains(mac)) {
                            foundDataIdentifyFilter.add(mac)
                            val businessMap = mxBluetoothDevice.resultDataMap
                            MXLog.d(TAG, "parseBusinessData: $businessMap")
                            if (null != businessMap) {
                                val notPairMode = businessMap["control"] != 0
                                if (notPairMode) {
                                    MXLog.w(TAG, "not pair mode")
                                    return
                                }
                                val pk = businessMap["pk"].toString()
                                val deviceName = businessMap["dn"].toString()
                                val pairInfoBean = ProductInfoManager.getProductInfo(pk)
                                if (pairInfoBean.isNullOrEmpty())
                                    MXLog.w(TAG, "product does not exist")
                                else {
                                    val product =
                                        JsonUtil.fromJson(pairInfoBean, ProductInfo::class.java)
                                    if (product == null) {
                                        MXLog.e(TAG, "product is null")
                                    } else {
                                        val linkType = product.link_type_id
                                        val autoBean = PairDeviceBean(
                                            product.image,
                                            product.name,
                                            mac,
                                            product.product_key,
                                            product.cloud_platform,
                                            linkType
                                        )
                                        autoBean.extBluetoothDevice = mxBluetoothDevice
                                        autoBean.deviceName = deviceName
                                        _autoFindDevices.value = autoBean
                                    }
                                }
                            }
                        }
                    }

                    MxScanType.OTHER -> {
                        val scanRecordBytes =
                            mxBluetoothDevice.scanResult.scanRecord?.bytes ?: return
                        val aliAd =
                            PairUtils.getADTypeIndexByScanRecordBytes(scanRecordBytes, 0x01A8)
                        if (aliAd == -1) return
                        val mac = mxBluetoothDevice.getAddress()
                        if (!foundDataIdentifyFilter.contains(mac)) {
                            foundDataIdentifyFilter.add(mac)
                            val businessMap = PairUtils.parseAliPID(scanRecordBytes)
                            MXLog.d(TAG, "parseBusinessData: $businessMap")
                            if (null != businessMap) {
                                val productId = businessMap["productId"].toString()
                                val pk = ProductInfoManager.getProductPkByPid(productId)
                                if (pk.isNullOrEmpty()) {
                                    MXLog.e(TAG, "ali pk is null,pid: $productId")
                                    return
                                }
                                val pairInfoBean = ProductInfoManager.getProductInfo(pk)
                                if (pairInfoBean.isNullOrEmpty())
                                    MXLog.w(TAG, "product does not exist")
                                else {
                                    val product =
                                        JsonUtil.fromJson(pairInfoBean, ProductInfo::class.java)
                                    if (product == null) {
                                        MXLog.e(TAG, "product is null")
                                    } else {
                                        val linkType = product.link_type_id
                                        if (MxLinkType.ALI_BLE.code == linkType || MxLinkType.ALI_TMALL.code == linkType) {
                                            val wifiMac = businessMap["mac"].toString()
                                            val autoBean = PairDeviceBean(
                                                product.image,
                                                product.name,
                                                wifiMac,
                                                product.product_key,
                                                product.cloud_platform,
                                                linkType
                                            )
                                            autoBean.extBluetoothDevice = mxBluetoothDevice
                                            autoBean.productId = productId.toInt()
                                            _autoFindDevices.value = autoBean
                                        } else MXLog.e(
                                            TAG, "暂未实现的配网类型，link_type($linkType)"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * 复合产品配置关联查找
     */
    private fun postFoundDeviceProduct(
        uuid: String,
        extendedBluetoothDevice: MxBluetoothDevice,
        product: ProductInfo,
        compositeProductKey: String? = null
    ) {
        foundDataIdentifyFilter.add(uuid)
        val autoBean = if (compositeProductKey.isNullOrEmpty()) {
            PairDeviceBean(
                product.image,
                product.name,
                extendedBluetoothDevice.getAddress(),
                product.product_key,
                product.cloud_platform,
                product.link_type_id,
                null
            )
        } else {
            val compositeProduct = JsonUtil.fromJson(
                ProductInfoManager.getProductInfo(compositeProductKey),
                ProductInfo::class.java
            )
            if (compositeProduct == null) {
                PairDeviceBean(
                    product.image,
                    product.name,
                    extendedBluetoothDevice.getAddress(),
                    product.product_key,
                    product.cloud_platform,
                    product.link_type_id,
                    compositeProductKey
                )
            } else {
                PairDeviceBean(
                    compositeProduct.image,
                    compositeProduct.name,
                    extendedBluetoothDevice.getAddress(),
                    compositeProductKey,
                    compositeProduct.cloud_platform,
                    compositeProduct.link_type_id,
                    product.product_key
                )
            }
        }
        autoBean.extBluetoothDevice = extendedBluetoothDevice
        _autoFindDevices.value = autoBean
    }


    fun setFilter(filter: (ProductInfo, compositeProductKey: String?) -> Boolean) {
        _filter = filter
    }

    fun registerScanProvisioningData() {
        MxScan.addScanDataListener(mxScanListener)
    }

    /**
     * 清除已自动发现设备过滤
     */
    fun clearFoundDeviceFilterData() {
        _autoFindDevices.value = null
        foundDataIdentifyFilter.clear()
    }

    /**
     * 停止发现设备
     */
    fun stopDiscoveryDevice() {
        MxScan.removeScanDataListener(mxScanListener)
        clearFoundDeviceFilterData()
    }

    private val _linkStateFun: (String, LinkState) -> Unit = { identifier, linkState ->
        when (linkState) {
            LinkState.START -> {
                _pairState.value = PairState.PAIR_STATE_START
            }

            LinkState.GET_QUADRUPLES -> {
                _pairState.value = PairState.PAIR_STEP_GET_QUADRUPLES
            }

            LinkState.SET_WIFI -> {
                _pairState.value = PairState.PAIR_STEP_SET_WIFI
            }

            LinkState.FAIL -> {
                _pairState.value = PairState.PAIR_STATE_FAIL
                MXLog.w(TAG, "linkState: failed")
                stopPair()
            }
        }
    }

    fun stopPair() {
        _meshPair?.stopPair()
        _fogPair?.stopPair()
        _fyPair?.stopPair()
        _tmPair?.stopPair()
        _meshPair = null
        _fogPair = null
        _fyPair = null
        _tmPair = null
        fogRandomCache = null
    }

    /**
     * 发起配网流程
     */
    fun starPair(pairDeviceBean: PairDeviceBean) {
        fogRandomCache = null
        stopPair()//当前只允许单连，所以检查连接并断开
        when (pairDeviceBean.linkTypeId) {
            MxLinkType.ALI_BLE.code -> {//飞燕蓝牙辅助
                val ssid = PairDataCenter.getCacheSSID()
                val password = PairDataCenter.getCachePwd()
                if (ssid.isNullOrEmpty() || password.isNullOrEmpty()) {
                    MXLog.e(TAG, "飞燕蓝牙辅助 Wi-Fi信息未配置")
                    _pairState.value = PairState.PAIR_STATE_FAIL
                    return
                }
                _pairBusiness.setDeviceIdentifier(
                    pairDeviceBean.mac,
                    pairDeviceBean.pk,
                    pairDeviceBean.compositeProductKey
                )
                val fyPair =
                    object : FYPair(MXBusinessApplication.mxBusinessApp, this@PairViewModel) {
                        override fun initParams(): LinkFYParams {
                            return LinkFYParams(
                                pairDeviceBean.pk,
                                pairDeviceBean.mac,
                                pairDeviceBean.extBluetoothDevice!!.getDevice(),
                                LinkWiFiInfo(ssid, password)
                            )
                        }
                    }
                fyPair.startPair(_linkStateFun)
                _fyPair = fyPair
            }

            MxLinkType.ALI_TMALL.code -> {//天猫精灵，蓝牙辅助
                val ssid = PairDataCenter.getCacheSSID()
                val password = PairDataCenter.getCachePwd()
                if (ssid.isNullOrEmpty() || password.isNullOrEmpty()) {
                    MXLog.e(TAG, "天猫精灵蓝牙辅助 Wi-Fi信息未配置")
                    _pairState.value = PairState.PAIR_STATE_FAIL
                    return
                }
                _pairBusiness.setDeviceIdentifier(
                    pairDeviceBean.mac,
                    pairDeviceBean.pk,
                    pairDeviceBean.compositeProductKey
                )
                val tmPair =
                    object : TMPair(MXBusinessApplication.mxBusinessApp, this@PairViewModel) {
                        override fun initParams(): LinkTMParams {
                            return LinkTMParams(
                                pairDeviceBean.pk,
                                pairDeviceBean.productId!!,
                                pairDeviceBean.mac,
                                pairDeviceBean.extBluetoothDevice!!.getDevice(),
                                LinkWiFiInfo(ssid, password)
                            )
                        }
                    }
                tmPair.startPair(_linkStateFun)
                _tmPair = tmPair
            }

            MxLinkType.MXCHIP_MESH.code, MxLinkType.MX_BLE_MESH_SCAN.code -> {
                val platform =
                    if (pairDeviceBean.cloudPlatform == PairConstants.CLOUD_PLATFORM_FOG) LinkPlatform.PLATFORM_FOG
                    else LinkPlatform.PLATFORM_ALI
                val uuid = pairDeviceBean.extBluetoothDevice!!.uuid!!
                _pairBusiness.setDeviceIdentifier(
                    uuid,
                    pairDeviceBean.pk,
                    pairDeviceBean.compositeProductKey
                )
                val meshPair = object : MeshPair(this@PairViewModel) {
                    override fun initParams(): LinkMeshParams {
                        return LinkMeshParams(
                            platform,
                            uuid,
                            pairDeviceBean.pk,
                            pairDeviceBean.extBluetoothDevice!!.getDevice(),
                        )
                    }
                }
                checkMeshConnected(meshPair)
                _meshPair = meshPair
            }

            MxLinkType.MXCHIP_MESH_WIFI.code -> {
                val ssid = PairDataCenter.getCacheSSID()
                val password = PairDataCenter.getCachePwd() ?: ""
                if (ssid.isNullOrEmpty()) {
                    MXLog.e(TAG, "Mesh Wi-Fi设备，SSID is null")
                    _pairState.value = PairState.PAIR_STATE_FAIL
                    return
                }
                val platform =
                    if (pairDeviceBean.cloudPlatform == PairConstants.CLOUD_PLATFORM_FOG) LinkPlatform.PLATFORM_FOG
                    else LinkPlatform.PLATFORM_ALI
                val uuid = pairDeviceBean.extBluetoothDevice!!.uuid!!
                _pairBusiness.setDeviceIdentifier(
                    uuid,
                    pairDeviceBean.pk,
                    pairDeviceBean.compositeProductKey
                )
                val meshPair = object : MeshPair(this@PairViewModel) {
                    override fun initParams(): LinkMeshParams {
                        return LinkMeshParams(
                            platform,
                            uuid,
                            pairDeviceBean.pk,
                            pairDeviceBean.extBluetoothDevice!!.getDevice(),
                            LinkWiFiInfo(ssid, password)
                        )
                    }
                }
                checkMeshConnected(meshPair)
                _meshPair = meshPair
            }

            MxLinkType.MXCHIP_MESH_WIFI_2.code -> { //Wi-Fi可选
                val platform =
                    if (pairDeviceBean.cloudPlatform == PairConstants.CLOUD_PLATFORM_FOG) LinkPlatform.PLATFORM_FOG
                    else LinkPlatform.PLATFORM_ALI
                val ssid = PairDataCenter.getCacheSSID() ?: ""
                val password = PairDataCenter.getCachePwd() ?: ""
                val uuid = pairDeviceBean.extBluetoothDevice!!.uuid!!
                _pairBusiness.setDeviceIdentifier(
                    uuid,
                    pairDeviceBean.pk,
                    pairDeviceBean.compositeProductKey
                )
                val meshPair = object : MeshPair(this@PairViewModel) {
                    override fun initParams(): LinkMeshParams {
                        return LinkMeshParams(
                            platform,
                            uuid,
                            pairDeviceBean.pk,
                            pairDeviceBean.extBluetoothDevice!!.getDevice(),
                            LinkWiFiInfo(ssid, password)
                        )
                    }
                }
                checkMeshConnected(meshPair)
                _meshPair = meshPair
            }

            MxLinkType.FOG_BLE_WIFI.code -> {
                val ssid = PairDataCenter.getCacheSSID()
                val password = PairDataCenter.getCachePwd() ?: ""
                if (ssid.isNullOrEmpty()) {
                    MXLog.e(TAG, "Fog Wi-Fi设备，SSID is null")
                    _pairState.value = PairState.PAIR_STATE_FAIL
                    return
                }
                _pairBusiness.setDeviceIdentifier(
                    pairDeviceBean.mac,
                    pairDeviceBean.pk,
                    pairDeviceBean.compositeProductKey
                )
                val fogPair =
                    object : FogPair(MXBusinessApplication.mxBusinessApp, this@PairViewModel) {
                        override fun initParams(): LinkFogParams {
                            val fogConfig = PairManager.getGatewayFogConfig()
                            val fogEnv = if (fogConfig == null) null else FogEnvConfig(
                                fogConfig.first,
                                fogConfig.second
                            )
                            return LinkFogParams(
                                pairDeviceBean.pk,
                                pairDeviceBean.deviceName!!,
                                pairDeviceBean.mac,
                                pairDeviceBean.extBluetoothDevice!!.getDevice(),
                                LinkWiFiInfo(ssid, password),
                                fogEnv
                            )
                        }
                    }
                fogPair.startPair(_linkStateFun)
                _fogPair = fogPair
            }

            else -> {
                MXLog.e(TAG, "暂未实现的配网类型，link_type->${pairDeviceBean.linkTypeId}")
                _pairState.value = PairState.PAIR_STATE_FAIL
            }
        }
    }

    private fun checkMeshConnected(meshPair: MeshPair) {
        MXMesh.destroyProvisioningData()
        if (MXMesh.isConnected()) {
            MXMesh.disConnect()
            viewModelScope.launch(Dispatchers.IO) {
                delay(500)
                meshPair.startPair(_linkStateFun)
            }
        } else {
            meshPair.startPair(_linkStateFun)
        }
    }

    private var fogRandomCache: String? = null

    override fun getFogRandom(pk: String, dn: String, random: (String) -> Unit) {
        launchMain {
            val map = mapOf("product_key" to pk, "device_name" to dn)
            _pairBusiness.getFogRandom(map) {
                if (it.isNullOrEmpty()) {
                    _pairState.value = PairState.PAIR_STATE_FAIL
                } else {
                    val json = JSONObject(it)
                    val randomStr = json.optString("random")
                    if (randomStr.isNullOrEmpty()) {
                        _pairState.value = PairState.PAIR_STATE_FAIL
                    } else {
                        fogRandomCache = randomStr
                        random.invoke(randomStr)
                    }
                }
            }
        }
    }

    /**
     * @param result 回调bleKye，如执行配网时没有设置Wi-Fi信息，可以配网过程中设置
     */
    override fun getFogBleKey(
        pk: String,
        dn: String,
        random: String,
        cipher: String,
        result: (String, LinkWiFiInfo?) -> Unit
    ) {
        launchMain {
            val map = mapOf(
                "product_key" to pk, "device_name" to dn, "random" to random, "cipher" to cipher
            )
            _pairBusiness.getFogBleKey(map) {
                if (it.isNullOrEmpty()) {
                    _pairState.value = PairState.PAIR_STATE_FAIL
                } else {
                    val json = JSONObject(it)
                    val verify = json.optBoolean("Verify")
                    if (verify) {
                        val bleKeyStr = json.optString("ble_key")
                        if (bleKeyStr.isNullOrEmpty()) {
                            _pairState.value = PairState.PAIR_STATE_FAIL
                        } else {
                            result.invoke(bleKeyStr, null)
                        }
                    } else {
                        _pairState.value = PairState.PAIR_STATE_FAIL
                    }
                }
            }
        }
    }

    /**
     * 获取阿里的密钥
     */
    override fun getAliCipher(param: Map<String, Any>, aliCipher: (String) -> Unit) {
        launchMain {
            _pairBusiness.getAliCipher(param) {
                if (it.isNullOrEmpty()) {
                    _pairState.value = PairState.PAIR_STATE_FAIL
                } else {
                    aliCipher.invoke(it)
                }
            }
        }
    }

    /**
     * 痛过API获取设备证书（当设备无证书时使用）
     */
    override fun getDeviceCertificate(
        uuid: String,
        certificateResult: (DeviceCertificate?) -> Unit
    ) {
        certificateResult.invoke(null)
    }

    /**
     * 绑定设备到云
     */
    override fun pairCompleted(param: Map<String, Any>) {
        val productKey = param["product_key"]!!.toString()
        val deviceName = param["device_name"]!!.toString()
        val bindParams: MutableMap<String, Any> = mutableMapOf(
            "product_key" to productKey, "device_name" to deviceName
        )
        fogRandomCache?.apply {
            bindParams["random"] = this
        }

        param["mac"]?.apply {
            bindParams["mac"] = this
        }
        val identifier = param["identifier"] as String
        var uuid: String? = null
        if (param.containsKey("uuid")) {
            uuid = param["uuid"] as String
            val meshUUid = uuid.replace("-", "")
            bindParams["uuid"] = uuid
            bindParams["mesh_uuid"] = meshUUid
            val node = MXMesh.getNode(uuid)
            if (node != null) {
                val deviceKey = ByteUtil.bytesToHexString(node.deviceKey)
                bindParams["mesh_address"] = node.unicastAddress
                bindParams["device_key"] = deviceKey
            }
        }
        if (param.containsKey("sign_type")) {
            val signTypeHex = param["sign_type"] as String
            bindParams["sign_type"] = signTypeHex.toInt(16)
            bindParams["sign"] = param["sign"]!!
        }

        val replaceIotId = PairDataCenter.getReplaceIotId()
        if (replaceIotId.isNullOrEmpty()) {
            val lat = PairDataCenter.getLocationLatitude()
            if (!lat.isNullOrEmpty()) {
                bindParams["lat"] = lat
                bindParams["lon"] = PairDataCenter.getLocationLongitude()!!
            }
            bindParams["show"] = true
            _pairState.value = PairState.BINDING
            val roomAddress = PairManager.getFromRoomAddress()
            _pairBusiness.executeBind(identifier, bindParams, roomAddress) { iotId ->
                if (iotId == null) {
                    _pairState.value = PairState.PAIR_STATE_FAIL
                } else {
                    if (uuid.isNullOrEmpty() || iotId.isEmpty()) {//非mesh设备,本地配网模式
                        _pairState.value = PairState.PAIR_STATE_SUCCESS
                    } else {
//                        bindParams["replaced_iotid"] = iotId //配网设备iotId，获取规则使用
                        _pairBusiness.getReplaceDeviceRule(uuid, iotId) { replaceRule ->
                            if (replaceRule == null) { //配网设备，没有规则需要写入，直接成功
                                _pairState.value = PairState.PAIR_STATE_SUCCESS
                            } else {
                                sendReplaceInfo(uuid, replaceRule) { writeResult ->
                                    _pairState.value =
                                        if (writeResult) PairState.PAIR_STATE_SUCCESS else PairState.PAIR_STATE_FAIL
                                }
                            }
                        }
                    }
                }
            }
        } else { //替换设备
            bindParams["replaced_iotid"] = replaceIotId
            if (uuid.isNullOrEmpty()) {
                //非mesh设备无规则，直接走替换绑定
                _pairState.value = PairState.BINDING
                _pairBusiness.executeReplaceBind(null, bindParams) { bindResult ->
                    _pairState.value =
                        if (bindResult) PairState.PAIR_STATE_SUCCESS else PairState.PAIR_STATE_FAIL
                }
            } else {//mesh设备
                _pairState.value = PairState.SYNCING
                val productString = ProductInfoManager.getProductInfo(productKey)
                val product = JsonUtil.fromJson(productString, ProductInfo::class.java)
                //网关 2032/无线（140101），2052/有线（140102）
                //中控屏 灵动高科（140301），声必可Linux（140302），声必可安卓（140303）
                if (product != null && (product.category_id in 140101..140102 || product.category_id in 140301..140303)) {
                    MeshSDKManage.getVersion(uuid) { version ->
                        version?.apply {
                            bindParams["firmware_version"] = this
                        }
                        //发送配网成功
                        MeshSDKManage.sendPairedSuccessful(uuid) { //不检查结果
                            //网关会重启
                            _pairBusiness.replaceDeviceCheckSyncGatewayData(bindParams) { syncResult ->
                                if (syncResult) {
                                    _pairState.value = PairState.BINDING
                                    _pairBusiness.executeReplaceBind(
                                        uuid,
                                        bindParams
                                    ) { bindResult ->
                                        _pairState.value =
                                            if (bindResult) PairState.PAIR_STATE_SUCCESS else PairState.PAIR_STATE_FAIL
                                    }
                                } else {
                                    _pairState.value = PairState.PAIR_STATE_FAIL
                                }
                            }
                        }
                    }
                } else {
                    //发送配网成功
                    MeshSDKManage.sendPairedSuccessful(uuid) { //不检查结果
                        _pairBusiness.getReplaceDeviceRule(uuid, replaceIotId) { ruleBean ->
                            if (ruleBean == null) { //替换设备规则获取失败
                                _pairState.value = PairState.PAIR_STATE_FAIL
                            } else {
                                sendReplaceInfo(uuid, ruleBean) { writeResult ->
                                    if (writeResult) {
                                        _pairState.value = PairState.BINDING
                                        _pairBusiness.executeReplaceBind(
                                            uuid,
                                            bindParams
                                        ) { bindResult ->
                                            _pairState.value =
                                                if (bindResult) PairState.PAIR_STATE_SUCCESS else PairState.PAIR_STATE_FAIL
                                        }
                                    } else {
                                        _pairState.value = PairState.PAIR_STATE_FAIL
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val attrConfigJson by lazy { JSONObject(AppConfigManager.getAttrConfig() ?: "{}") }

    private fun sendReplaceInfo(
        uuid: String,
        replaceDevice: DeviceRuleBean,
        writeResult: (Boolean) -> Unit
    ) {
        val writeRuleList =
            DeviceWriteConfigManage.parseMeshWriteRule(
                uuid,
                replaceDevice,
                attrConfigJson,
                meshCommand
            ) { null }
        if (writeRuleList.isEmpty()) { //所有规则空，直接成功
            writeResult.invoke(true)
        } else {
            DeviceWriteConfigManage.writeDeviceRule(writeRuleList, 0, writeResult)
        }
    }

    override fun getFogConnectState(map: Map<String, Any>, result: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val params = mutableMapOf<String, Any>()
            params["home_id"] = AppConfigManager.getUseHomeId()
            params.putAll(map)
            repeatCheckFogConnectState(params, 10) {
                result.invoke(it)
                if (!it) {
                    _pairState.value = PairState.PAIR_STATE_FAIL
                }
            }
        }
    }

    private fun repeatCheckFogConnectState(
        paramMap: Map<String, Any>, repeatCount: Int, result: (connected: Boolean) -> Unit
    ) {
        _pairBusiness.getFogConnectState(paramMap) {
            if (it) {
                result.invoke(true)
            } else if (repeatCount > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    delay(3000)
                    repeatCheckFogConnectState(paramMap, repeatCount - 1, result)
                }
            } else result.invoke(false)
        }
    }

    override fun setUnprovisionedNodeUnicastAddress(
        uuid: String, meshResult: (resultState: Boolean) -> Unit
    ) {
        val replaceIotId = PairDataCenter.getReplaceIotId()
        _pairBusiness.getDeviceMeshAddress(uuid, replaceIotId) { meshAddress ->
            if (meshAddress == null) meshResult.invoke(false)
            else {
                if (!replaceIotId.isNullOrEmpty())//一键替换设备，先根据单播地址删除设备
                    MXMesh.getNode(meshAddress)?.apply {
                        MXMesh.deleteNode(this)
                    }
                MXMesh.setUnprovisionedNodeUnicastAddress(uuid, meshAddress) {
                    meshResult.invoke(it == MeshState.SUCCESS)
                }
            }
        }
    }


    private val handler = Handler(Looper.getMainLooper())

    private val provisioningTimeoutRunnable = Runnable {
        _pairState.value = PairState.PAIR_STATE_FAIL
    }

    private var provisioningRetryCount = 1

    override fun provisioning(
        uuid: String, meshResult: (resultState: Boolean) -> Unit
    ) {
        val pairDeviceBean =
            PairDataCenter.pairDeviceList.firstOrNull { it.extBluetoothDevice?.uuid == uuid }
        val extBlue = pairDeviceBean?.extBluetoothDevice
        if (extBlue == null) {
            meshResult.invoke(false)
        } else {
            provisioningRetryCount = 1
            meshProvisioning(extBlue.getDevice(), uuid, meshResult)
        }
    }

    private fun meshProvisioning(
        bluetoothDevice: BluetoothDevice,
        uuid: String,
        meshResult: (resultState: Boolean) -> Unit
    ) {
        handler.postDelayed(provisioningTimeoutRunnable, 30000)
        val startTime = System.currentTimeMillis()
        MXMesh.startPairByNoOOB(bluetoothDevice, uuid) { meshState ->
            when (meshState) {
                MeshState.SUCCESS -> {//Mesh provisioning成功
                    //防止mesh底层可能会重连设备后继续执行配网流程，业务却触发了30s超时
                    if (System.currentTimeMillis() - startTime < 30000) {
                        //移除超时
                        handler.removeCallbacks(provisioningTimeoutRunnable)
                        meshResult.invoke(true)
                    }
                }

                MeshState.FAIL -> {
                    handler.removeCallbacks(provisioningTimeoutRunnable)
                    _pairState.value = PairState.PAIR_STATE_FAIL
                }

                MeshState.DISCONNECTED -> {
                    handler.removeCallbacks(provisioningTimeoutRunnable)
                    if (provisioningRetryCount > 0) {
                        provisioningRetryCount--
                        CoroutineScope(Dispatchers.Main).launch {
                            MXMesh.disConnect()
                            delay(500)
                            meshProvisioning(bluetoothDevice, uuid, meshResult)
                        }
                    } else _pairState.value = PairState.PAIR_STATE_FAIL
                }

                else -> {}
            }
        }
    }

    override fun setConfigBeforeWiFi(uuid: String, configResult: (Boolean) -> Unit) {
        val pkPair = _pairBusiness.getProductKeyPairByIdentifier(uuid)
        if (pkPair == null) {
            MXLog.e(TAG, "checkBindBeforeConfig, pk is null, uuid: $uuid")
            configResult.invoke(false)
        } else {
            val product =
                JsonUtil.fromJson(
                    ProductInfoManager.getProductInfo(pkPair.first),
                    ProductInfo::class.java
                )
            if (null == product) {
                MXLog.e(TAG, "checkBindBeforeConfig, product is null, uuid: $uuid")
                configResult.invoke(false)
            } else {
                //网关 2032/无线（140101），2052/有线（140102）
                //中控屏 灵动高科（140301），声必可Linux（140302），声必可安卓（140303）
                if (product.category_id in 140101..140102 || product.category_id in 140301..140303) {

                    _pairBusiness.checkGatewayConfig(uuid, product.link_type_id, configResult)
                } else {
                    configResult.invoke(true)
                }
            }
        }
    }

    override fun setMeshWiFi(
        uuid: String, hex: String, meshResult: (resultState: Boolean) -> Unit
    ) {
        //外部指定消息TID，并暂存，用于自行移除callback
        //msgId的生成必须使用sdk提供的getNextMeshMessageTid()
        val msgId = MeshMessageUtil.getNextMeshMessageTid()
        MXMesh.sendDeviceMessage(
            uuid,
            0,
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET,
            hex,
            { meshState, resultMap ->
                if (meshState == MeshState.SUCCESS) {
                    if (resultMap.isNullOrEmpty()) return@sendDeviceMessage
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //减去前面2字节 attrType
                    val wifiStateArray = ByteArray(dataArray.size - 2)
                    System.arraycopy(dataArray, 2, wifiStateArray, 0, wifiStateArray.size)
                    //uint8	BIT[0]-BIT[1]：云服务连接状态{0x00 = Connecting，0x01 = Connected，0x02 = Disconnected}
                    if (1 == ByteUtil.bytesToInt(wifiStateArray)) {
                        //wifi成功,手动移除messageCallback
                        MeshMessageUtil.removeMessageCallback(msgId, uuid)
                        viewModelScope.launch(Dispatchers.IO) {
                            val ssid = PairDataCenter.getCacheSSID()
                            if (!ssid.isNullOrEmpty()) {
                                PairDataCenter.getCachePwd()?.apply {
                                    PairWiFiSPManage.saveWifi(ssid, this)
                                }
                            }
                        }
                        meshResult.invoke(true)
                    }
                } else {
                    MeshMessageUtil.removeMessageCallback(msgId, uuid)
                    meshResult.invoke(false)
                }
            },
            timeout = true,
            retry = false,
            timeoutMillis = 30000,//与iOS同步，30s
            msgId
        )
    }

    override fun setLinkMeshMsg(
        uuid: String,
        hex: String,
        timeout: Boolean,
        retry: Boolean,
        timeoutMillis: Long,
        meshResult: (resultState: Boolean, resultMap: Map<String, Any>?) -> Unit
    ) {
        meshCommand.setMsg(uuid, hex, timeout, retry, timeoutMillis, meshResult)
    }

    override fun getLinkMeshMsg(
        uuid: String,
        hex: String,
        timeout: Boolean,
        retry: Boolean,
        timeoutMillis: Long,
        meshResult: (resultState: Boolean, resultMap: Map<String, Any>?) -> Unit
    ) {
        meshCommand.getMsg(uuid, hex, timeout, retry, timeoutMillis, meshResult)
    }
}