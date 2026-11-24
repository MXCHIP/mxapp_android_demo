package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import coil.load
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.DeviceOtaV3Bean
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.device.ui.WifiGroupDeviceListDialog
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.ClipboardManager
import com.mxchip.mxapp.base.utils.DeviceManage
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.compareVersion
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.mxapp.device.R
import com.mxchip.mxapp.device.bean.ProductUseManualBean
import com.mxchip.mxapp.device.databinding.DeviceAcDeviceDetailBinding
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.ByteUtil
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

/**
 *
 */
@Router(RouterConstants.DEVICE_DETAIL_ACTIVITY)
class DeviceDetailActivity : MXBusinessActivity<DeviceAcDeviceDetailBinding>() {


    private val vm by viewModels<DeviceViewModel>()
    private var deviceInfo: DeviceBean? = null
    private var currentRoomId = -1
    private var editName: String? = null
    private val formatter by lazy {
        SimpleDateFormat("yyyy-MM-dd")
    }
    private var homeDataRefresh: HomeDataRefresh? = null

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.apply {
                    val roomId = this.getIntExtra("room_id", -1)
                    if (currentRoomId != roomId) {
                        val roomName = this.getStringExtra("room_name")!!
                        if (!deviceInfo?.uuid.isNullOrEmpty()) {
                            updateDeviceRoom(roomId, roomName)
                        } else {
                            val roomAddress = this.getIntExtra("room_address", -1)
                            if (roomAddress > 0) {
                                val productInfo = JsonUtil.fromJson(
                                    ProductInfoManager.getProductInfo(deviceInfo!!.product_key),
                                    ProductInfo::class.java
                                )
                                if (productInfo?.is_scene_preset == true) writePresetRule(
                                    roomId,
                                    roomName,
                                    roomAddress
                                )
                                else updateDeviceRoom(roomId, roomName)
                            } else updateDeviceRoom(roomId, roomName)
                        }
                    }
                }
            }
        }


    private val otaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val version = it.data?.getStringExtra("version")
                if (version.isNullOrEmpty()) {
                    deviceInfo?.apply {
                        vm.getOtaUpgradeInfoV3(this.iotid).launchAndCollectIn(
                            this@DeviceDetailActivity,
                            Lifecycle.State.CREATED,
                            loadFlow<DeviceOtaV3Bean>(success = { otaBean ->
                                showVersion(otaBean)
                            }, error = { _, _, _ ->
                                showVersion(null)
                            })
                        )
                    }
                } else {
                    binding.itemOtaUpdate.setEndText(version).setShowRedPoint(false)
                    noOtaVersion()
                    deviceInfo?.apply {
                        if (!this.uuid.isEmpty()) {//mesh设备需要主动告知API
                            vm.updateOTAVersion(this.iotid, version).launchAndCollectIn(
                                this@DeviceDetailActivity,
                                Lifecycle.State.CREATED,
                                loadFlow<Any>(success = {
                                    MXLog.i("OtaMeshActivity", "sendOtaVersion success")
                                }, error = { _, _, _ -> })
                            )
                        }
                    }
                }
            }
        }

    private val mcuLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val version = it.data?.getStringExtra("version")
                binding.itemMcu.setEndText(version).setShowRedPoint(false)
                noMcuVersion()
            }
        }

    override fun getLayoutBinding(): DeviceAcDeviceDetailBinding {
        return DeviceAcDeviceDetailBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        val iotId = intent.getStringExtra(CommonConstants.KEY_IOT_ID)
        if (iotId.isNullOrEmpty()) {
            finish()
            return
        }
        checkViewState()
        inject()
        initData(iotId)
    }

    private fun initData(iotId: String) {
        showLoading()
        vm.getDeviceInfo(iotId)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<DeviceBean>(
                    success = {
                        dismissLoading()
                        it?.apply {
                            parseDeviceData(this)
                        }
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }
                ))
    }

    private fun checkViewState() {
        binding.itemFeedback.visibility =
            if (DeviceManage.isSupportDeviceFeedback()) View.VISIBLE else View.GONE

        if (RoleManager.isUser()) {//普通成员
            binding.btnEditName.visibility = View.GONE
            binding.itemDeviceReplace.visibility = View.GONE
            binding.itemSceneAuto.visibility = View.GONE
            binding.itemDeviceShare.visibility = View.GONE
            binding.layoutCommonly.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        } else {
            binding.itemRoomName.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
            // groupId >0 表示从群组详情页面过来
            val groupId = intent.getIntExtra(DeviceConstants.KEY_GROUP_ID, 0)
            if (groupId > 0) {
                binding.itemDeviceReplace.visibility = View.GONE
                binding.layoutCommonly.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.layoutCommonly.visibility =
                    if (DeviceManage.isSupportSetCommonly()) View.VISIBLE else View.GONE
                binding.itemDeviceShare.visibility =
                    if (DeviceManage.isSupportDeviceShare()) View.VISIBLE else View.GONE
                binding.btnDelete.visibility =
                    if (DeviceManage.isSupportAddDevice()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun inject() {
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        binding.toolbar.left {
            backPressed()
        }
        binding.btnEditName.setOnClickListener {
            if (null == deviceInfo) return@setOnClickListener
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_device_name))
                .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
                .edittextText(editName ?: "")

                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, inputText ->
                    if (inputText.isNullOrBlank()) {
                        ToastUtil.showToast(
                            this,
                            getString(com.mxchip.mxapp.base.R.string.mx_no_input)
                        )
                        return@rightButton
                    } else if (inputText.length > MXDialog.maxLength) {
                        ToastUtil.showToast(
                            this, getString(com.mxchip.mxapp.base.R.string.mx_name_invalid)
                        )
                        return@rightButton
                    }

                    dialog.dismiss()
                    editName = inputText.trim()
                    if (deviceInfo != null) {
                        showLoading()
                        vm.setDeviceName(deviceInfo!!.iotid, editName!!)
                            .launchAndCollectIn(
                                this,
                                Lifecycle.State.CREATED,
                                loadFlow<Any>(success = {
                                    dismissLoading()
                                    binding.tvNickName.text = editName
                                    homeDataRefresh = HomeDataRefresh.DEVICE
                                }, error = { code, msg, _ ->
                                    dismissLoading()
                                    val netErr = onException(code)
                                    if (!netErr) showToast("$msg")
                                })
                            )
                    }
                }.create().show()
        }
        binding.itemDeviceIdentify.setOnLongClickListener {
            ClipboardManager.setTextToClip(this, binding.itemDeviceIdentify.getEndText())
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_copy_clipboard))
            true
        }
        binding.itemRoomName.setOnClickListener {
            if (null == deviceInfo) return@setOnClickListener
            if (RoleManager.isUser()) return@setOnClickListener
            RouterManager.navigation(
                RouterConstants.ROOM_SELECT_ACTIVITY, mapOf(
                    "room_id" to currentRoomId,
                    "rightText" to getString(com.mxchip.mxapp.base.R.string.mx_save)
                ), this, launcher
            )
        }
        binding.itemDeviceShare.setOnClickListener {
            deviceInfo?.apply {
                RouterManager.navigation(
                    RouterConstants.DEVICE_SHARE_ACCOUNT_ACTIVITY, mapOf(
                        "iotids" to arrayListOf(this.iotid)
                    )
                )
            }
        }

        binding.itemDeviceReplace.setOnClickListener {
            deviceInfo?.apply {
                RouterManager.navigation(
                    RouterConstants.PAIR_GUIDE_ACTIVITY, mapOf(
                        ProductConstants.KEY_PRODUCT_KEY to product_key,
                        ProductConstants.KEY_PRODUCT_KEY_LIST to arrayListOf(product_key),
                        "replaced.iotId" to this.iotid,
                        "replaced.name" to this.nick_name.ifEmpty { this.product_name },
                        "replaced.roomName" to this.room_name,
                        "replaced.roomId" to this.room_id
                    )
                )
            }
        }

        binding.ivFavorite.setOnClickListener {
            deviceInfo?.apply {
                homeDataRefresh = HomeDataRefresh.DEVICE
                val newState = !it.isSelected
                it.isSelected = newState
                deviceFavoriteState(newState)
                vm.setDeviceShow(this.iotid, newState)
            }

        }
        binding.btnDelete.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_del_device))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_del_device_des))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                    dialog.dismiss()
                    deviceInfo?.apply {
                        showLoading()
                        vm.unBindDevice(listOf(iotid))
                    }
                }.create().show()
        }

        vm.unBindState.observe(
            this, loadObserver<Any>(success = {
                dismissLoading()
                deviceInfo?.apply {
                    val productIndo = ProductInfoManager.getProductInfo(this.product_key)
                    val product = JsonUtil.fromJson(productIndo, ProductInfo::class.java)
                    if (product?.isGateWay() != true) //非网关品类才发reset
                        MXMesh.getNode(uuid = this.uuid)?.apply {
                            MXMesh.resetNode(node = this)
                        }
                }
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.DEVICE)
                RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            })
        )

        binding.itemFeedback.setOnClickListener {
            if (null == deviceInfo) return@setOnClickListener
            RouterManager.navigation(
                RouterConstants.FEEDBACK_EDIT_ACTIVITY, mapOf(
                    ProductConstants.KEY_PRODUCT_NAME to deviceInfo!!.nick_name.ifEmpty { deviceInfo!!.product_name },
                    ProductConstants.KEY_PRODUCT_KEY to deviceInfo!!.product_key,
                    "faq.type" to 1
                )
            )
        }

        binding.itemProductManual.setOnClickListener {
            val di = deviceInfo ?: return@setOnClickListener
            RouterManager.navigation(
                RouterConstants.PRODUCT_USE_MANUAL,
                mapOf(
                    ProductConstants.KEY_PRODUCT_KEY to di.product_key,
                    ProductConstants.KEY_PRODUCT_NAME to di.product_name,
                    "link" to "${it.tag}"
                )
            )
        }
    }

    private fun deviceFavoriteState(newState: Boolean) {
        if (newState) {
            binding.ivFavorite.setImageResource(R.drawable.device_ic_password_show)
            binding.ivFavorite.isSelected = true
        } else {
            binding.ivFavorite.setImageResource(R.drawable.device_ic_password_hide)
            binding.ivFavorite.isSelected = false
        }
    }

    private fun parseDeviceData(device: DeviceBean) {
        binding.itemSceneAuto.visibility = View.GONE
        binding.itemWifiGroup.visibility = View.GONE

        this.deviceInfo = device
        binding.itemOtaUpdate.setEndText(device.firmware_version).setEndIconGone()
        if (device.mcu_version.isNullOrEmpty()) {
            binding.itemMcu.visibility = View.GONE
        } else {
            binding.itemMcu.setEndText(device.mcu_version)
        }
        deviceFavoriteState(device.show)
        if (device.bind_time == null)
            binding.tvDeviceAddTime.visibility = View.GONE
        else
            binding.tvDeviceAddTime.text = String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_added_time),
                formatter.format(device.bind_time!! * 1000)//*1000 服务端返回时间戳 10位
            )
        if (0 == device.share_type)//是否可以被分享 1：可以，0：不能
            binding.itemDeviceShare.visibility = View.GONE
        binding.ivDeviceIcon.load(device.product_image)
        editName = device.nick_name.ifEmpty { device.product_name }
        binding.tvNickName.text = editName
        val roomName =
            device.room_name.ifEmpty { getString(com.mxchip.mxapp.base.R.string.mx_no_set) }
        currentRoomId = device.room_id
        binding.itemRoomName.setEndText(roomName)
        binding.itemDeviceIdentify.setEndText(device.device_name)
        binding.itemProductIdentify.setEndText(device.product_key).setEndIconGone()
        binding.itemDeviceMac.setEndText(device.mac)
        getDeviceOtherInfo(device)
    }

    private fun getDeviceOtherInfo(device: DeviceBean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val manual = async {
                vm.getProductUseManual(device.product_key)
            }
            val manualWait = manual.await()
            manualWait.collect {
                if (it.state == NetState.SUCCESS) {
                    val pm = JsonUtil.fromJson(it.data, ProductUseManualBean::class.java)
                    val link = pm?.link
                    if (!link.isNullOrEmpty()) {//配置了使用手册
                        binding.itemProductManual.tag = link
                        withContext(Dispatchers.Main) {
                            binding.itemProductManual.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun showVersion(data: DeviceOtaV3Bean?) {
        if (null == data) {
            noOtaVersion()
        } else {
            if (data.version.isEmpty()) {//无最新版本
                noOtaVersion()
            } else {
                if (data.ota_type == 1) { //fog
                    //是否需要app确认
                    if (data.need_app_confirm) {
                        val versionState = data.current_version.compareVersion(data.version)
                        when (versionState) {
                            1 -> {//提示红点升级
                                binding.itemOtaUpdate.tag = data.version
                                binding.itemOtaUpdate.setShowRedPoint(true)
                                    .setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
                            }

                            -1 -> {//不提示红点升级
                                binding.itemOtaUpdate.tag = data.version
                                binding.itemOtaUpdate.setShowRedPoint(false).setEndIconGone()
                            }

                            else -> {
                                noOtaVersion()
                            }
                        }
                    } else {//false 静默升级
                        noOtaVersion()
                    }
                } else { //mesh
                    if (data.current_version != data.version) {
                        binding.itemOtaUpdate.tag = data.ota_type
                        binding.itemOtaUpdate.setShowRedPoint(true)
                            .setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
                    } else {
                        noOtaVersion()
                    }
                }
            }
        }
    }

    private fun noOtaVersion() {
        binding.itemOtaUpdate.tag = null
        binding.itemOtaUpdate.setShowRedPoint(false).setEndIcon(null)
    }

    private fun showMcuVersion(data: DeviceOtaV3Bean?) {
        if (null == data) {
            noMcuVersion()
        } else {
            if (data.version.isEmpty()) {//无最新版本
                noMcuVersion()
            } else {
                //fog ota need_app_confirm字段为false时，为静默升级，不需要app参与
                if (!data.need_app_confirm) {
                    noMcuVersion()
                    return
                }
                val versionState = data.current_version.compareVersion(data.version)
                when (versionState) {
                    1 -> {//提示红点升级
                        binding.itemMcu.tag = data.version
                        binding.itemMcu.setShowRedPoint(true)
                            .setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
                    }

                    -1 -> {//不提示红点升级
                        binding.itemMcu.tag = data.version
                        binding.itemMcu.setShowRedPoint(false).setEndIconGone()
                    }

                    else -> {
                        noMcuVersion()
                    }
                }
            }
        }
    }

    private fun noMcuVersion() {
        binding.itemMcu.tag = null
        binding.itemMcu.setShowRedPoint(false).setEndIcon(null)
    }

    private fun backPressed() {
        homeDataRefresh?.apply {
            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH).postValue(this)
        }
        finish()
    }


    private fun updateDeviceRoom(roomId: Int, roomName: String) {
        val iotId = deviceInfo?.iotid ?: return
        showLoading()
        vm.setDeviceRoom(iotId, roomId).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlow<Any>(
                success = {
                    dismissLoading()
                    binding.itemRoomName.setEndText(roomName)
                    homeDataRefresh = HomeDataRefresh.DEVICE
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                }
            )
        )
    }

    private fun writePresetRule(roomId: Int, roomName: String, roomAddress: Int) {
        showLoading()
        val roomAddressHex = MeshSDKManage.stringReversedToLittleEndian(
            String.format(
                "%04X", roomAddress
            )
        )
        MeshSDKManage.setMsg(
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SYNC,
            deviceInfo!!.uuid,
            "80$roomAddressHex",
            object : MapCallback() {
                override fun onResult(resultMap: Map<String, Any>) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //attrType = 80,1字节
                    val resultArray = ByteArray(dataArray.size - 1)
                    System.arraycopy(dataArray, 1, resultArray, 0, resultArray.size)
                    val rest = ByteUtil.bytesToInt(resultArray)
                    if (rest == 0) {
                        updateDeviceRoom(roomId, roomName)
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            dismissLoading()
                            showToast(getString(com.mxchip.mxapp.base.R.string.mx_save_failed))
                        }
                    }
                }

                override fun onError(callbackMsg: CallbackMsg) {
                    super.onError(callbackMsg)
                    lifecycleScope.launch {
                        dismissLoading()
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_save_failed))
                    }
                }
            },
            isTimeOut = true,
            isRetry = true
        )
    }

    private fun fetchDeviceList() {
        val deviceInfo = deviceInfo ?: return
        showLoading()
        vm.findDevices(AppConfigManager.getUseHomeId(), null, deviceInfo.product_key)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED,
                loadFlowList<DeviceBean>(
                    success = {
                        dismissLoading()
                        showWifiGroupDialog(deviceInfo, it)
                    },
                    error = { code, msg, _ ->
                        dismissLoading()
                        MXLog.e("DeviceDetail", "fetchDeviceList error: $code, $msg")
                    }
                )
            )
    }

    private val wifiGroupDialog by lazy { WifiGroupDeviceListDialog() }

    private fun showWifiGroupDialog(deviceInfo: DeviceBean, deviceList: List<DeviceBean>?) {
        wifiGroupDialog.setData(0, deviceList, null)
        wifiGroupDialog.show(supportFragmentManager, "wifiGroupDialog")
        wifiGroupDialog.setWifiGroupListener { selectedDevices ->
            if (selectedDevices.isNotEmpty())
                RouterManager.navigation(
                    RouterConstants.CREATE_WIFI_GROUP_ACTIVITY, mapOf(
                        ProductConstants.KEY_PRODUCT_KEY to deviceInfo.product_key,
                        "iotIds" to selectedDevices.map { it.iotid },
                        "categoryId" to deviceInfo.category_id
                    )
                )
        }
    }
}