package com.mxchip.mxapp.device.ui

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.GroupBean
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.device.vm.DeviceViewModel

/**
 * 设备OTA检查、进入H5
 */
abstract class AbsDeviceListActivity<Binding : ViewBinding> : MXBusinessActivity<Binding>() {

    private val deviceVM by viewModels<DeviceViewModel>()

    private var otaIotId: String? = null
    private val otaBleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.getStringExtra("version")?.apply {
                    sendVersion(this)
                }
            }
        }

    private fun sendVersion(version: String) {
        otaIotId?.apply {
            deviceVM.updateOTAVersion(this, version).launchAndCollectIn(
                this@AbsDeviceListActivity, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                    MXLog.i("OtaMeshActivity", "sendOtaVersion success")
                }, error = { code, msg, data ->
                    MXLog.e("OtaMeshActivity", "sendOtaVersion failed, msg=$msg")
                })
            )
        }
    }


    protected fun toH5Panel(data: DeviceBean) {
        RouterManager.navigation(
            RouterConstants.DEVICE_PANEL_ACTIVITY, mapOf(
                DeviceConstants.KEY_DEVICE_BEAN to data
            )
        )
    }

    protected fun toGroupH5Panel(data: GroupBean) {
        val firstNode = data.nodes[0]
        val pk = firstNode.product_key!!
        val params = mutableMapOf<String, Any>()
        params[DeviceConstants.KEY_GROUP_ID] = data.group_id
        params[DeviceConstants.KEY_PRODUCT_KEY] = pk
        params[DeviceConstants.KEY_CATEGORY_ID] = data.category_id
        params[CommonConstants.KEY_IOT_ID] = firstNode.iotid!!
        if (data.group_type == 1) {//wifi组
            RouterManager.navigation(RouterConstants.WIFI_GROUP_PANEL_ACTIVITY, params)
        } else {//mesh组
            params[DeviceConstants.KEY_GROUP_ADDRESS] = data.group_address
            firstNode.uuid?.apply {
                params[CommonConstants.KEY_UUID] = this
            }
            RouterManager.navigation(RouterConstants.GROUP_PANEL_ACTIVITY, params)
        }
    }
}