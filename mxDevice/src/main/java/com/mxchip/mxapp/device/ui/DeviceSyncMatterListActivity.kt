package com.mxchip.mxapp.device.ui

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.PermissionUtil
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.device.adapter.DeviceSyncMatterAdapter
import com.mxchip.mxapp.device.databinding.DeviceAcDeviceSyncMatterListBinding
import com.mxchip.mxapp.device.ui.dialog.ChooseMatterGatewayDialog
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.mxapp.device.xbt.XbtHelper
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *同步Matter协议，设备列表
 */
@Router(RouterConstants.DEVICE_SYNC_MATTER_LIST_ACTIVITY)
class DeviceSyncMatterListActivity : MXBusinessActivity<DeviceAcDeviceSyncMatterListBinding>() {

    companion object {
        private const val TAG = "DeviceSyncMatterListActivity"
    }

    private val vm by viewModels<DeviceViewModel>()

    private val adapter by lazy { DeviceSyncMatterAdapter() }

    private var currentMatterGatewayUUID: String? = null
    private var currentMatterGatewayMAC: String? = null

    private val gatewayList by lazy { mutableListOf<DeviceBean>() }

    private val chooseMatterGatewayDialog by lazy {
        ChooseMatterGatewayDialog({
            currentMatterGatewayUUID = it.uuid
            val reverseMac = it.uuid.replace("-", "").substring(14, 26)
            val stringBuilder = StringBuilder()
            val array =
                reverseMac.replace("(.{2})".toRegex(), "$1:").split(":".toRegex()).toTypedArray()
            for (i in array.indices.reversed()) {
                if (array[i].isNotEmpty()) stringBuilder.append(array[i]).append(":")
            }
            currentMatterGatewayMAC = stringBuilder.substring(0, stringBuilder.length - 1)
            binding.tvMatterGateway.text = it.nick_name.ifEmpty { it.product_name }
        }, gatewayList)
    }

    override fun getLayoutBinding(): DeviceAcDeviceSyncMatterListBinding {
        return DeviceAcDeviceSyncMatterListBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initView()
        inject()
        initData()
    }

    private fun initView() {
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.adapter = adapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvNext.setBtnBackgroundColor(mainColor)
        binding.emptyView.setIconColor(mainColor)
    }

    private fun inject() {
        binding.toolbar.left {
            finish()
        }
        binding.toolbar.right {
            RouterManager.navigation(RouterConstants.SYNC_MATTER_HELP_ACTIVITY)
        }
        binding.tvMatterGateway.setOnClickListener {
            if (gatewayList.isEmpty()) {
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_matter_unbound_gateway))
                return@setOnClickListener
            }
            chooseMatterGateway()
        }
        binding.tvNext.setOnClickListener {
            PermissionUtil.checkFindBlePermission(applicationContext) { locationPermissionState, locationSwitchState, bluePermissionState, blueSwitchState ->
                when {
                    !locationPermissionState -> openPermissionDialog(isLocationPermission = true)
                    !locationSwitchState -> openLocationDialog()
                    !bluePermissionState -> openPermissionDialog(isLocationPermission = false)
                    !blueSwitchState -> bleDialog()
                    currentMatterGatewayUUID.isNullOrEmpty() -> chooseMatterGateway()
                    else -> {
                        val networkKey = AppConfigManager.getUseHomeNetworkKey()
                        val deviceMac = currentMatterGatewayMAC
                        val content = createMatterFile()
                        if (networkKey.isEmpty() || deviceMac.isNullOrEmpty() || content.isNullOrEmpty()) {
                            MXLog.e(
                                TAG,
                                "networkKey:$networkKey,deviceMac:$deviceMac,content:$content"
                            )
                        } else {
                            XbtHelper.startXbt(this, networkKey, deviceMac, content) {syncResult->
                                if (syncResult){
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        dismissLoading()
                                        ToastUtil.showToast(
                                            this@DeviceSyncMatterListActivity,
                                            getString(com.mxchip.mxapp.base.R.string.mx_sync_success)
                                        )
                                    }
                                    finish()
                                }else{
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        dismissLoading()
                                        ToastUtil.showToast(
                                            this@DeviceSyncMatterListActivity,
                                            getString(com.mxchip.mxapp.base.R.string.mx_save_failed)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initData() {
        showLoading()
        queryGatewayData()
    }

    private fun queryGatewayData() {
        vm.findDevices(
            mapOf(
                "home_id" to AppConfigManager.getUseHomeId(),
                "is_gateway" to 1,
                "page_size" to 50,
                "page_no" to 1
            )
        ).launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<DeviceBean>(success = {
            it?.apply {
                gatewayList.addAll(this)
                if (this.isEmpty()) binding.tvNext.apply {
                    alpha = 0.45f
                    isEnabled = false
                    setText(com.mxchip.mxapp.base.R.string.mx_matter_no_gateway)
                }
                else binding.tvNext.apply {
                    alpha = 1f
                    isEnabled = true
                    setText(com.mxchip.mxapp.base.R.string.mx_sync)
                }
                queryData()
            }
        }, error = { code: Int, msg: String?, data: Any? ->
            dismissLoading()
            binding.tvNext.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))
    }

    private fun queryData(pageNo: Int = 1) {
        vm.findDevices(
            mapOf(
                "type" to 4,
                "home_id" to AppConfigManager.getUseHomeId(),
                "matter" to 1,
                "page_size" to 199,
                "page_no" to pageNo
            )
        ).launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<DeviceBean>(success = {
            dismissLoading()
            if ((it?.size ?: 0) < 199) {
                dismissLoading()
            } else {
                val newPageNo = pageNo + 1
                queryData(newPageNo)
            }
            if (pageNo == 1) {
                it?.apply {
                    adapter.setData(this)
                    if (this.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.tvNext.visibility = View.GONE
                    } else binding.emptyView.visibility = View.GONE
                }
            } else {
                it?.apply {
                    adapter.appendData(this)
                }
            }
        }, error = { code: Int, msg: String?, _ ->
            dismissLoading()
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))
    }

    private fun chooseMatterGateway() {
        chooseMatterGatewayDialog.show(supportFragmentManager, "ChooseMatterGatewayDialog")
    }

    /**
     * 位置权限
     */
    private fun openPermissionDialog(isLocationPermission: Boolean) {
        val permissionTitle: String
        val permissionContent = if (isLocationPermission) {
            permissionTitle = getString(com.mxchip.mxapp.base.R.string.mx_permission_location_denied)
            String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off_des),
                getString(com.mxchip.mxapp.base.R.string.mx_app_name)
            )
        } else {
            permissionTitle = getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_denied)
            String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off_des),
                getString(com.mxchip.mxapp.base.R.string.mx_app_name)
            )
        }
        MXDialog.Builder(this).title(permissionTitle).content(permissionContent)
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_disagree)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_agree)) { alertDialog, _ ->
                alertDialog.dismiss()
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Permission.BLUETOOTH_SCAN,
                        Permission.BLUETOOTH_CONNECT,
                    )
                } else {
                    arrayOf(
                        Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION
                    )
                }
                XXPermissions.with(this).permission(permissions)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>, all: Boolean
                        ) {
                            if (all) PermissionUtil.checkFindBlePermission(applicationContext) { locationPermissionState, locationSwitchState, bluePermissionState, blueSwitchState ->
                                when {
                                    !locationPermissionState -> openPermissionDialog(
                                        isLocationPermission = true
                                    )

                                    !locationSwitchState -> openLocationDialog()
                                    !bluePermissionState -> openPermissionDialog(
                                        isLocationPermission = false
                                    )

                                    !blueSwitchState -> bleDialog()
                                }
                            }
                        }

                        override fun onDenied(
                            permissions: MutableList<String>,
                            never: Boolean
                        ) {
                        }
                    })
            }.create().show()
    }

    /**
     * 对话框开启位置开关
     */
    private fun openLocationDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_disagree)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_go_setting), { alertDialog, _ ->
                alertDialog.dismiss()
                //请求打开位置开关
                PermissionUtil.remindStartLocateService(this)
            }).create().show()
    }

    @SuppressLint("MissingPermission")
    private fun bleDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_disagree)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_agree), { alertDialog, _ ->
                alertDialog.dismiss()
                //请求打开蓝牙开关
                PermissionUtil.remindStartBlueService(this)
            }).create().show()
    }

    private fun createMatterFile(): String? {
        //举个例子，一个三键墙壁开关，Mesh 地址为 0x1234，服务类型为开关 0x010A，则其设备信息如下：
        //34 12 00 0A 01 34 12 01 0A 01 34 12 02 0A 01
        // 服务类型列表
        //0x010A 墙壁开关、插座
        //0x0100 可开关的灯
        //0x0101 可调亮度的灯
        //0x010C 可调色温和亮度的灯
        //0x010D 可调色温、亮度和颜色的灯
        //0x000F 场景面板
        val content = StringBuilder()
        adapter.getDatas().forEach {
            val node = MXMesh.getNode(it.uuid) ?: return@forEach
            val meshAddress = MeshSDKManage.stringReversedToLittleEndian(
                String.format(
                    "%04X", node.unicastAddress
                )
            )
            when (it.category_id) {
                100102 -> {
                    //C
                    content.append(meshAddress).append("00").append("0101")
                }

                100103 -> {
                    //CW
                    content.append(meshAddress).append("00").append("0C01")
                }

                100104 -> {
                    //CWRGB
                    content.append(meshAddress).append("00").append("0D01")
                }

                100310, 100303 -> {
                    //一路开关
                    content.append(meshAddress).append("00").append("0A01")
                }

                100311, 100304 -> {
                    //二路开关
                    content.append(meshAddress).append("00").append("0A01").append(meshAddress)
                        .append("01").append("0A01")
                }

                100312, 100305 -> {
                    //三路开关
                    content.append(meshAddress).append("00").append("0A01").append(meshAddress)
                        .append("01").append("0A01").append(meshAddress).append("02")
                        .append("0A01")
                }

                100316, 100314 -> {
                    //四路开关
                    content.append(meshAddress).append("00").append("0A01").append(meshAddress)
                        .append("01").append("0A01").append(meshAddress).append("02")
                        .append("0A01")
                        .append(meshAddress).append("03").append("0A01")
                }
            }
        }

        return if (content.isEmpty()) null else content.toString()
    }
}