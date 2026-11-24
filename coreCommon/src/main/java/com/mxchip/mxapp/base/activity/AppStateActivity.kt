package com.mxchip.mxapp.base.activity

import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.databinding.CoreAcAppBleStateBinding
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.router.annotation.Router

/**
 * app各类状态说明
 */
@Router(RouterConstants.APP_STATE_ACTIVITY)
class AppStateActivity : MXBusinessActivity<CoreAcAppBleStateBinding>() {

    override fun getLayoutBinding(): CoreAcAppBleStateBinding {
        return CoreAcAppBleStateBinding.inflate(layoutInflater)
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