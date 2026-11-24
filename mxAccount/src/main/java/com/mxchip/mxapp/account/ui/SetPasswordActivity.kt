package com.mxchip.mxapp.account.ui

import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.mxchip.mxapp.account.R
import com.mxchip.mxapp.account.bean.AutoLoginBean
import com.mxchip.mxapp.account.bean.TokenBean
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_ACCOUNT
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_CODE
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_TOKEN
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_FAST_LOGIN_SET_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_FORGET_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_MODIFY_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_REGISTER
import com.mxchip.mxapp.account.databinding.AccountAcSetPasswordBinding
import com.mxchip.mxapp.account.util.AccountManager
import com.mxchip.mxapp.account.vm.AccountViewModel
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.router.annotation.Router


/**
 * 设置密码
 */
@Router(path = RouterConstants.SET_PASSWORD_ACTIVITY)
class SetPasswordActivity : MXBusinessActivity<AccountAcSetPasswordBinding>() {

    private val viewModel by viewModels<AccountViewModel>()
    private var currentMode = MODE_REGISTER
    private var account = ""

    override fun getLayoutBinding() = AccountAcSetPasswordBinding.inflate(layoutInflater)

    override fun onInit() {
        account = intent.getStringExtra(KEY_ACCOUNT)!!
        initEvent()
        inject()
    }

    private fun initEvent() {
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.next.setBtnBackgroundColor(mainColor)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pwd = binding.loginPwd.text.toString()
                val pwd2 = binding.loginPwd2.text.toString()
                val enable = pwd.isNotEmpty() && pwd2.isNotEmpty()
                binding.next.isEnabled = enable
                binding.next.alpha = if (enable) 1f else 0.4f
            }

            override fun afterTextChanged(s: Editable?) {}

        }
        binding.loginPwd.addTextChangedListener(watcher)
        binding.loginPwd2.addTextChangedListener(watcher)
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                binding.tvPwdValidDesc.setTextColor(
                    ContextCompat.getColor(
                        this,
                        com.mxchip.mxapp.base.R.color.core_color_text_list_content
                    )
                )
        }
        binding.loginPwd.onFocusChangeListener = focusListener
        binding.loginPwd2.onFocusChangeListener = focusListener
        binding.next.setOnClickListener {
            this.currentFocus?.apply {
                KeyboardUtil.hideKeyboard(this)
            }
            val pwd = binding.loginPwd.text.toString()
            val pwd2 = binding.loginPwd2.text.toString()
            if (pwd != pwd2) {
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_pwd_two_match))
                return@setOnClickListener
            } else if (RegexUtil.isValidPwd(pwd).not()) {
                binding.tvPwdValidDesc.setTextColor(
                    ContextCompat.getColor(
                        this,
                        com.mxchip.mxapp.base.R.color.core_color_error
                    )
                )
                return@setOnClickListener
            }
            when (currentMode) {
                MODE_REGISTER -> register()
                MODE_MODIFY_PWD, MODE_FORGET_PWD -> resetAndLogin()
                MODE_FAST_LOGIN_SET_PWD -> fastLoginSetPwd()
            }
        }

        binding.pwdHide.setOnClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                binding.pwdHide.setImageResource(R.drawable.account_ic_password_show)
                binding.loginPwd.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.pwdHide.setImageResource(R.drawable.account_ic_password_hide)
                binding.loginPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.loginPwd.setSelection(binding.loginPwd.text.length)
        }
        binding.pwdHide2.setOnClickListener {
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                binding.pwdHide2.setImageResource(R.drawable.account_ic_password_show)
                binding.loginPwd2.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.pwdHide2.setImageResource(R.drawable.account_ic_password_hide)
                binding.loginPwd2.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.loginPwd2.setSelection(binding.loginPwd2.text.length)
        }

        binding.toolbar.left { finish() }
    }

    private fun inject() {
        viewModel.registerState.observe(this, loadObserver<TokenBean>(success = {
            dismissLoading()
            it?.apply {
                loginSuccess(this.token)
            }
        }, error = { code, msg, _ ->
            dismissLoading()
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))

        viewModel.loginState.observe(this, loadObserver<TokenBean>(success = {
            dismissLoading()
            it?.apply {
                loginSuccess(this.token)
            }
        }, error = { code, msg, _ ->
            dismissLoading()
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))

        viewModel.resetPwdLoginState.observe(this, loadObserver<TokenBean>(success = {
            dismissLoading()
            it?.apply {
                LoginManager.saveToken(this.token)
                RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
                MXBusinessApplication.mxBusinessApp.finishOtherActivity(RouterConstants.MAIN_ACTIVITY)
            }
        }, error = { code, msg, _ ->
            dismissLoading()
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))
    }

    /**
     * 重置密码
     */
    private fun resetAndLogin() {
        val pwd = binding.loginPwd.text.toString()
        val code = intent.getStringExtra(KEY_CODE) ?: throw NullPointerException("code is null")
        val countryCode = LoginManager.getSelectRegionCode() ?: "CHN"
        val area = if (countryCode == "CHN") "86" else ""
        val isEmail = RegexUtil.isEmail(account)
        showLoading()
        viewModel.resetPwdLogin(
            account,
            pwd,
            code,
            AccountManager.getClientId(),
            area,
            isEmail,
            countryCode
        )
    }

    /**
     * 注册
     */
    private fun register() {
        val pwd = binding.loginPwd.text.toString()
        val code = intent.getStringExtra(KEY_CODE) ?: throw NullPointerException("code is null")
        val countryCode = LoginManager.getSelectRegionCode() ?: "CHN"
        val area = if (countryCode == "CHN") "86" else ""
        val clientID = AccountManager.getClientId()
        //微信等第三方注册所需token
        val registerToken = intent.getStringExtra("registerToken")
        if (registerToken.isNullOrEmpty()) {
            val isEmail = RegexUtil.isEmail(account)
            showLoading()
            viewModel.register(account, pwd, code, clientID, area, isEmail, countryCode)
        } else {
            viewModel.addPhone(registerToken, account, pwd, code, area, clientID)
                .launchAndCollectIn(
                    this,
                    Lifecycle.State.CREATED,
                    loadFlow<AutoLoginBean>(success = {
                        it?.apply {
                            loginSuccess(this.access_token!!)
                        }
                    }, error = { ecode, msg, _ ->
                        val netErr = onException(ecode)
                        if (!netErr) showToast("$msg")
                    })
                )
        }
    }

    private fun fastLoginSetPwd() {
        val pwd = binding.loginPwd.text.toString()
//        if (!RegexUtil.isValidPwd(pwd)) {
//            showToast(getString(com.mxchip.mxapp.base.R.string.invalid_pwd))
//            return
//        }
        val account =
            intent.getStringExtra(KEY_ACCOUNT) ?: throw NullPointerException("account is null")
        val loginToken = intent.getStringExtra(KEY_TOKEN) ?: ""
        showLoading()
        viewModel.fastLoginSetPwd(pwd, account, loginToken, AccountManager.getClientId())
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<TokenBean>(success = {
                dismissLoading()
                it?.apply {
                    loginSuccess(this.token)//this.token 是resetPassword后有效token
//                    aliLogin(this.token)
                }
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            }))
    }

//    private fun aliLogin(token: String) {
//        showLoading()
//        LoginBusiness.authCodeLogin(token, object : ILoginCallback {
//            override fun onLoginSuccess() {
//                dismissLoading()
//                MXLog.w("ali login success")
//                loginSuccess(token)
//            }
//
//            override fun onLoginFailed(code: Int, error: String) {
//                dismissLoading()
//                MXLog.w("ali login failure ---> $code, $error")
//                showToast(error)
//            }
//        })
//    }

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