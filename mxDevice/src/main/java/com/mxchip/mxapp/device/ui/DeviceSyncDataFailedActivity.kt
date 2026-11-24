package com.mxchip.mxapp.device.ui

import coil.load
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.device.databinding.DeviceAcDeviceSyncDataFailedBinding
import com.mxchip.router.annotation.Router

/**
 *同步失败信息
 */
@Router(RouterConstants.DEVICE_SYNC_DATA_FAILED)
class DeviceSyncDataFailedActivity : MXBusinessActivity<DeviceAcDeviceSyncDataFailedBinding>() {

    override fun getLayoutBinding(): DeviceAcDeviceSyncDataFailedBinding {
        return DeviceAcDeviceSyncDataFailedBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        binding.toolbar.left {
            finish()
        }
        initView()
    }

    private fun initView() {
        val icon = intent.getStringExtra("icon")
        binding.icon.load(icon)
        val name = intent.getStringExtra("name")
        binding.name.text = name
        val isGateway = intent.getBooleanExtra("isGateway", false)
        binding.tvFailedDetail.setText(if (isGateway) com.mxchip.mxapp.base.R.string.mx_device_sync_data_failed_step_gateway else com.mxchip.mxapp.base.R.string.mx_device_sync_data_failed_step)
    }
}