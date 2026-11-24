package com.mxchip.mxapp.family.ui

import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_HOME_ID
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_ID
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_NAME
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.family.R
import com.mxchip.mxapp.family.databinding.FamilyAcHandOverVerificationBinding
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/3/11
 * @desc:
 */
@Router(RouterConstants.HAND_OVER_VERIFICATION_ACTIVITY)
class HandOverVerificationActivity : MXBusinessActivity<FamilyAcHandOverVerificationBinding>() {
    override fun getLayoutBinding() = FamilyAcHandOverVerificationBinding.inflate(layoutInflater)

    private val viewMode by viewModels<HomeViewModel>()

    private var homeId = -1
    private var memberId = -1
    private var memberName = ""

    private val sendVerCodeAgainTimer = object : CountDownTimer(60 * 1000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            binding.sendVerCodeAgain.text =
                "${getString(com.mxchip.mxapp.base.R.string.mx_resend_code)}(${millisUntilFinished / 1000})"
        }

        override fun onFinish() {
            binding.sendVerCodeAgain.isEnabled = true
            binding.sendVerCodeAgain.text = getString(com.mxchip.mxapp.base.R.string.mx_resend_code)
            changeAgainSendTextColor(true)
        }
    }

    override fun onInit() {
        binding.desc.text = String.format(
            getString(com.mxchip.mxapp.base.R.string.mx_verification_code_hint),
            LoginManager.getAccount()
        )
        binding.etVerificationCode.requestFocus()
        KeyboardUtil.showKeyboard(binding.etVerificationCode)
        initTimer()
        initData()
        initEvent()
    }

    private fun initTimer() {
        binding.sendVerCodeAgain.isEnabled = false
        changeAgainSendTextColor(false)
        sendVerCodeAgainTimer.cancel()
        sendVerCodeAgainTimer.start()
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }

        binding.sendVerCodeAgain.setOnClickListener {
            binding.sendVerCodeAgain.isEnabled = false
            changeAgainSendTextColor(false)
            showLoading()
            viewMode.handOverCode(homeId, memberId)
                .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                    dismissLoading()
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_send_success))
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                }))
            sendVerCodeAgainTimer.cancel()
            sendVerCodeAgainTimer.start()
        }

        binding.etVerificationCode.setOnInputCompletedListener {
            KeyboardUtil.hideKeyboard(binding.etVerificationCode)
            showHandOverConfirmDialog(it)
        }
    }

    private fun initData() {
        homeId = intent.getIntExtra(KEY_HOME_ID, 0)
        memberId = intent.getIntExtra(KEY_MEMBER_ID, 0)
        memberName = intent.getStringExtra(KEY_MEMBER_NAME) ?: ""
    }

    private fun showHandOverConfirmDialog(code: String) {
        MXDialog.Builder(this)
            .title(getString(com.mxchip.mxapp.base.R.string.mx_role_hand_over_title))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_role_hand_over_des),
                    memberName
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                alertDialog.cancel()
                showLoading()
                viewMode.handOver(homeId, memberId, code)
                    .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                        dismissLoading()
                        if (homeId == AppConfigManager.getUseHomeId()) {
                            //更新当前用户在此家庭角色
                            RoleManager.CURRENT_HOME_ROLE = RoleConstants.MANAGE
                        }
                        RouterManager.navigation(RouterConstants.FAMILY_MANAGE_ACTIVITY)
                        finish()
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }))
            }.create().show()
    }

    private fun changeAgainSendTextColor(enable: Boolean) {
        if (enable)
            binding.sendVerCodeAgain.setTextColor(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        else
            binding.sendVerCodeAgain.setTextColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.color_65_101010
                )
            )
    }
}