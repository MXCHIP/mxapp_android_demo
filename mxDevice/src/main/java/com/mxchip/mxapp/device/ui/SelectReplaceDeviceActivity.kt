package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.device.adapter.SelectReplaceDeviceAdapter
import com.mxchip.mxapp.device.databinding.DeviceAcReplaceDeviceListBinding
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router

/**
 * 选择替换设备
 */
@Router(RouterConstants.SELECT_REPLACE_DEVICE_ACTIVITY)
class SelectReplaceDeviceActivity : MXBusinessActivity<DeviceAcReplaceDeviceListBinding>() {
    override fun getLayoutBinding() = DeviceAcReplaceDeviceListBinding.inflate(layoutInflater)

    private val viewModel by viewModels<DeviceViewModel>()

    private val adapter by lazy {
        SelectReplaceDeviceAdapter(this).apply {
            setItemClickListener {
                showReplaceDialog(it)
            }
        }
    }

    override fun onInit() {
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = adapter
        binding.toolbar.left { finish() }
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.setIconColor(mainColor)
        getDeviceList()
    }

    private fun showReplaceDialog(device: DeviceBean) {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_replace_alert_title))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_replace_alert_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_next)) { alertDialog, _ ->
                alertDialog.dismiss()
                RouterManager.navigation(
                    RouterConstants.PAIR_GUIDE_ACTIVITY, mapOf(
                        ProductConstants.KEY_PRODUCT_KEY to device.product_key,
                        ProductConstants.KEY_PRODUCT_KEY_LIST to arrayListOf(device.product_key),
                        "replaced.iotId" to device.iotid,
                        "replaced.name" to device.nick_name.ifEmpty { device.product_name },
                        "replaced.roomName" to device.room_name,
                        "replaced.roomId" to device.room_id
                    )
                )
            }.create().show()
    }

    private fun getDeviceList() {
        viewModel.findDevices(
            mapOf(
                "home_id" to AppConfigManager.getUseHomeId(),
                "page_size" to 199
            )
        )
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlowList<DeviceBean>(
                    success = { deviceList ->
                        if (deviceList.isNullOrEmpty()) {
                            binding.emptyView.visibility = View.VISIBLE
                            adapter.setData(listOf())
                        } else {
                            binding.emptyView.visibility = View.GONE
                            val newList = deviceList.sortedBy { it.room_id }
                            adapter.setData(newList)
                        }
                    }, error = { code, msg, data ->
                        binding.emptyView.visibility = View.VISIBLE
                        adapter.setData(listOf())
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    })
            )
    }
}