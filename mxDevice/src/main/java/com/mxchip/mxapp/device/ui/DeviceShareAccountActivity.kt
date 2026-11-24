package com.mxchip.mxapp.device.ui

import android.text.InputType
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.device.databinding.DeviceAcDeviceShareAccountBinding
import com.mxchip.mxapp.device.viewmodel.ShareViewModel
import com.mxchip.router.annotation.Router

@Router(RouterConstants.DEVICE_SHARE_ACCOUNT_ACTIVITY)
class DeviceShareAccountActivity : MXBusinessActivity<DeviceAcDeviceShareAccountBinding>() {
    override fun getLayoutBinding() = DeviceAcDeviceShareAccountBinding.inflate(layoutInflater)

    private val viewModel by viewModels<ShareViewModel>()
    private lateinit var iotIds: List<String>

    override fun onInit() {
        val iotIdStr = intent.getStringExtra("iotids")
        val iotList = JsonUtil.fromJsonToArray(iotIdStr, String::class.java)
        if (iotList.isNullOrEmpty()) {
            finish()
            return
        }
        iotIds = iotList
        binding.accountBg.background = generateStoke(2, ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 25
        )
        val countryIOS3 = LoginManager.getSelectRegionCode() ?: "CHN"
        val isPhone = countryIOS3 == "CHN"

        if (isPhone) {//中国大陆使用 手机号
            binding.etAccount.hint = getString(com.mxchip.mxapp.base.R.string.mx_account_phone_hint)
            binding.etAccount.inputType = InputType.TYPE_CLASS_PHONE
        } else {
            binding.tvSelectedRegion.visibility = View.GONE
            binding.ivEmail.visibility = View.VISIBLE
            binding.etAccount.hint = getString(com.mxchip.mxapp.base.R.string.mx_account_email_hint)
            binding.etAccount.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        binding.etAccount.addTextChangedListener {
            val username = binding.etAccount.text.toString()
            binding.clearText.visibility = if (username.isEmpty()) View.GONE else View.VISIBLE
        }
        binding.clearText.setOnClickListener {
            binding.etAccount.setText("")
        }
        binding.toolbar.left { finish() }.rightText(
            getString(com.mxchip.mxapp.base.R.string.mx_submit),
            ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        ).right {
            val account = binding.etAccount.text.toString().trim()
            if (account.isEmpty()) {
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                return@right
            }
            if (countryIOS3 == "CHN") {
                if (!RegexUtil.isPhoneNumber(account)) {
                    ToastUtil.showToast(this, getString(com.mxchip.mxapp.base.R.string.mx_account_phone_invalid))
                    return@right
                }
            } else {
                if (!RegexUtil.isEmail(account)) {
                    ToastUtil.showToast(this, getString(com.mxchip.mxapp.base.R.string.mx_account_email_invalid))
                    return@right
                }
            }
            this.currentFocus?.apply {
                KeyboardUtil.hideKeyboard(this)
            }
            showLoading()
            viewModel.shareDevices(iotIds, account).launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<Any>(
                    success = {
                        dismissLoading()
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_share_success))
                        finish()
                    },
                    error = { code, msg, data ->
                        dismissLoading()
                        showToast(msg.toString())
                    }
                )
            )
        }
    }
}