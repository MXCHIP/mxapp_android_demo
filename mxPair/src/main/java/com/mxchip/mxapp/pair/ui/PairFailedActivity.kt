package com.mxchip.mxapp.pair.ui

import androidx.core.content.ContextCompat
import coil.load
import com.mxchip.router.annotation.Router
import com.mxchip.mxapp.pair.consts.PairState
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.databinding.PairAcPairFailedBinding

/**
 *配网失败信息
 */
@Router(RouterConstants.PAIR_FAILED_ACTIVITY)
class PairFailedActivity : MXBusinessActivity<PairAcPairFailedBinding>() {

    override fun getLayoutBinding() = PairAcPairFailedBinding.inflate(layoutInflater)

    override fun onInit() {
        binding.toolbar.left {
            finish()
        }
        binding.tvConfirm.setBtnBackgroundColor(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        binding.tvConfirm.setOnClickListener {
            finish()
        }
        initView()
    }

    private fun initView() {
        val icon = intent.getStringExtra("icon")
        binding.icon.load(icon)
        val name = intent.getStringExtra("name")
        binding.name.text = name
        when (intent.getIntExtra("state.ordinal", -1)) {
            //配网开始
            PairState.PAIR_STATE_START.ordinal -> {
                binding.tvFailedStep.text = getString(com.mxchip.mxapp.base.R.string.mx_ble_connect_failed)
                binding.tvFailedDetail.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_connect_fail)
            }

            //获取4元组
            PairState.PAIR_STEP_GET_QUADRUPLES.ordinal -> {
                binding.tvFailedStep.text = getString(com.mxchip.mxapp.base.R.string.mx_mesh_config_failed)
                binding.tvFailedDetail.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_config_fail)
            }

            //配网中步骤-设置Wi-Fi信息
            PairState.PAIR_STEP_SET_WIFI.ordinal -> {
                binding.tvFailedStep.text = getString(com.mxchip.mxapp.base.R.string.mx_setting_wifi_failed)
                binding.tvFailedDetail.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_wifi_fail)
            }

            //同步
            PairState.SYNCING.ordinal -> {
                binding.tvFailedStep.text = getString(com.mxchip.mxapp.base.R.string.mx_sync_fail)
                binding.tvFailedDetail.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_config_fail)
            }
            //绑定
            PairState.BINDING.ordinal -> {
                binding.tvFailedStep.text = getString(com.mxchip.mxapp.base.R.string.mx_account_bind_fail)
                binding.tvFailedDetail.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_bind_fail)
            }

            else -> {
                binding.tvFailedStep.text = getString(com.mxchip.mxapp.base.R.string.mx_ble_connect_failed)
                binding.tvFailedDetail.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_connect_fail)
            }
        }
    }
}