package com.mxchip.mxapp.account.ui

import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.mxchip.mxapp.account.R
import com.mxchip.mxapp.account.bean.AutoLoginBean
import com.mxchip.mxapp.account.bean.TokenBean
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_ACCOUNT
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_ACCOUNT_EXIST
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_CODE
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_MODE
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_FORGET_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_MODIFY_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_REGISTER
import com.mxchip.mxapp.account.databinding.AccountAcVerificationCodeBinding
import com.mxchip.mxapp.account.util.AccountManager
import com.mxchip.mxapp.account.vm.AccountViewModel
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.router.annotation.Router

@Router(path = RouterConstants.VERIFICATION_ACTIVITY)
class VerificationCodeActivity : MXBusinessActivity<AccountAcVerificationCodeBinding>() {

    private val viewModel by viewModels<AccountViewModel>()
    private lateinit var account: String
    private var countryCode = "CHN"
    private var mode = MODE_REGISTER

    //第三方注册所需token
    private var registerToken: String? = null
    private var accountExist = false

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

    override fun getLayoutBinding() = AccountAcVerificationCodeBinding.inflate(layoutInflater)

    override fun onInit() {
        account = intent.getStringExtra(KEY_ACCOUNT)!!
        mode = intent.getIntExtra(KEY_MODE, MODE_REGISTER)
        accountExist = intent.getBooleanExtra(KEY_ACCOUNT_EXIST, false)
        registerToken = intent.getStringExtra("registerToken")
        LoginManager.getSelectRegionCode()?.apply {
            countryCode = this
        }
        binding.desc.text =
            String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_verification_code_hint),
                account
            )
        binding.etVerificationCode.requestFocus()
        binding.etVerificationCode.showKeyBoard(this)
        initTimer()
        inject()
        initEvent()
    }

    private fun initEvent() {
        val type: Int
        val areaCode: String?
        if (countryCode == "CHN") {
            type = 0
            areaCode = "86"
        } else {
            type = 1
            areaCode = null
        }
        binding.sendVerCodeAgain.setOnClickListener {
            binding.sendVerCodeAgain.isEnabled = false
            changeAgainSendTextColor(false)
            viewModel.sendVerifyCode(
                type,
                if (accountExist) 0 else 1,
                account,
                areaCode
            )
            sendVerCodeAgainTimer.cancel()
            sendVerCodeAgainTimer.start()
        }

        binding.etVerificationCode.setOnInputCompletedListener {
            KeyboardUtil.hideKeyboard(binding.etVerificationCode)
            val otherLoginToken = registerToken
            when {
                !otherLoginToken.isNullOrEmpty() -> {//第三方登录绑定手机号
                    if (accountExist)//第三方登录关联的手机号已注册
                        otherLogin(otherLoginToken, it)
                    else //关联的手机号未注册，需要录入密码
                        verifyVerCode(it)
                }

                accountExist.not() //账号不存在
                        || mode == MODE_MODIFY_PWD //修改密码
                        || mode == MODE_FORGET_PWD //找回密码
                    -> {
                    verifyVerCode(it)
                }

                else -> {
                    login(it)
                }
            }
        }

        binding.toolbar.left {
            sendVerCodeAgainTimer.cancel()
            finish()
        }
    }

    private fun inject() {
        viewModel.sendVerState.observe(
            this, loadObserver<Any>(
                success = {
                    ToastUtil.showToast(
                        this,
                        getString(com.mxchip.mxapp.base.R.string.mx_phone_code_success)
                    )
                },
                error = { code, msg, data ->
                    ToastUtil.showToast(this, msg ?: code.toString())
                }
            ))
    }

    private fun initTimer() {
        binding.sendVerCodeAgain.isEnabled = false
        changeAgainSendTextColor(false)
        sendVerCodeAgainTimer.cancel()
        sendVerCodeAgainTimer.start()
    }

    private fun changeAgainSendTextColor(enable: Boolean) {
        if (enable)
            binding.sendVerCodeAgain.setTextColor(
                ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
            )
        else
            binding.sendVerCodeAgain.setTextColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.account_color_65_101010
                )
            )
    }

    private fun verifyVerCode(code: String) {
        binding.etVerificationCode.hideKeyBoard(this)
        showLoading()
        viewModel.checkVerifyCode(code, account)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<Any>(
                    success = {
                        dismissLoading()
                        val params = mutableMapOf<String, Any>(
                            KEY_ACCOUNT to account,
                            KEY_CODE to code,
                            KEY_MODE to mode
                        )
                        registerToken?.apply {
                            params["registerToken"] = this
                        }
                        if (mode == MODE_REGISTER)
                            RouterManager.navigation(RouterConstants.SET_PASSWORD_ACTIVITY, params)
                        else
                            RouterManager.navigation(RouterConstants.PASSWORD_ACTIVITY, params)
                    },
                    error = { code, msg, data ->
                        dismissLoading()
                        ToastUtil.showToast(this, msg ?: code.toString())
                    }
                ))
    }

    private fun login(code: String) {
        val type: Int
        val areaCode: String?
        if (countryCode == "CHN") {
            type = 0
            areaCode = "86"
        } else {
            type = 1
            areaCode = null
        }
        showLoading()
        viewModel.loginByVerCode(
            type,
            account,
            code,
            areaCode,
            countryCode,
            AccountManager.getClientId()
        ).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlow<TokenBean>(
                success = {
                    dismissLoading()
                    it?.apply {
                        loginSuccess(token)
                    }
                },
                error = { code, msg, data ->
                    dismissLoading()
                    showToast(msg.toString())
                }
            )
        )
    }

    private fun otherLogin(registerToken: String, code: String) {
        val clientID = AccountManager.getClientId()
        showLoading()
        viewModel.addPhone(registerToken, account, pwd = null, code, "86", clientID)
            .launchAndCollectIn(
                this,
                Lifecycle.State.CREATED,
                loadFlow<AutoLoginBean>(success = {
                    dismissLoading()
                    it?.apply {
                        loginSuccess(this.access_token!!)
                    }
                }, error = { ecode, msg, _ ->
                    dismissLoading()
                    val netErr = onException(ecode)
                    if (!netErr) showToast("$msg")
                })
            )
    }

    private fun loginSuccess(token: String) {
        LoginManager.saveToken(token)
        RouterManager.navigation(
            RouterConstants.MAIN_ACTIVITY, mapOf(
                "isLogin" to true
            )
        )
        MXBusinessApplication.mxBusinessApp.finishOtherActivity(RouterConstants.MAIN_ACTIVITY)
    }
}