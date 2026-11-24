package com.mxchip.mxapp.device.ui

import com.mxchip.router.annotation.Router
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.device.databinding.DeviceAcSyncMatterHelpBinding
/**
 *同步Matter协议，使用帮助
 */
@Router(RouterConstants.SYNC_MATTER_HELP_ACTIVITY)
class SyncMatterHelpActivity : MXBusinessActivity<DeviceAcSyncMatterHelpBinding>() {

    override fun getLayoutBinding(): DeviceAcSyncMatterHelpBinding {
        return DeviceAcSyncMatterHelpBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        inject()
    }


    private fun inject() {
        binding.toolbar.left {
            finish()
        }
    }
}