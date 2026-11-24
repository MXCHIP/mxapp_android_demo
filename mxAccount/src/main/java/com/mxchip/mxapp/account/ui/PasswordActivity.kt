package com.mxchip.mxapp.account.ui

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.account.R
import com.mxchip.mxapp.account.bean.AutoLoginBean
import com.mxchip.mxapp.account.bean.TokenBean
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_ACCOUNT
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_CODE
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_MODE
import com.mxchip.mxapp.account.consts.AccountConstants.KEY_TOKEN
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_FAST_LOGIN_SET_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_FORGET_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_MODIFY_PWD
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_PWD_LOGIN
import com.mxchip.mxapp.account.consts.AccountConstants.MODE_REGISTER
import com.mxchip.mxapp.account.databinding.AccountAcPasswordBinding
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
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/28
 * @desc:
 */
@Router(path = RouterConstants.PASSWORD_ACTIVITY)
class PasswordActivity : MXBusinessActivity<AccountAcPasswordBinding>() {

    private val viewModel by viewModels<AccountViewModel>()
    private var currentMode = MODE_PWD_LOGIN
    private var account = ""
    private var pwdVisibility = false
    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val enable =
                binding.loginPwd.text.isNotEmpty()//.toString().length >= 6 todo cd：29068 产品要求与iOS保持一致
            binding.next.isEnabled = enable
            binding.next.alpha = if (enable) 1f else 0.4f
        }

        override fun afterTextChanged(s: Editable?) {}

    }

    override fun getLayoutBinding() = AccountAcPasswordBinding.inflate(layoutInflater)

    override fun onInit() {
        account = intent.getStringExtra(KEY_ACCOUNT)!!
        currentMode = intent.getIntExtra(KEY_MODE, MODE_PWD_LOGIN)
        initState()
        initEvent()
        inject()
    }

    private fun initState() {
        if (currentMode == MODE_PWD_LOGIN) {
            binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_forget_password))
            binding.desc.text = getString(com.mxchip.mxapp.base.R.string.mx_password)
        } else {
            binding.toolbar.rightText("")
            binding.desc.text = getString(com.mxchip.mxapp.base.R.string.mx_set_password)
        }
    }

    private fun initEvent() {
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.next.setBtnBackgroundColor(mainColor)
        binding.loginPwd.addTextChangedListener(watcher)

        binding.loginPwd.filters = arrayOf(
            InputFilter.LengthFilter(16),
            InputFilter { source, start, end, dest, dstart, dend -> //不允许 输入空格
                if (source.isNullOrBlank()) {
                    ""
                } else {
                    null
                }
            })

        binding.next.setOnClickListener {
            this.currentFocus?.apply {
                KeyboardUtil.hideKeyboard(this)
            }
            when (currentMode) {
                MODE_REGISTER -> register()
                MODE_MODIFY_PWD, MODE_FORGET_PWD -> resetAndLogin()
                MODE_PWD_LOGIN -> login()
                MODE_FAST_LOGIN_SET_PWD -> fastLoginSetPwd()
            }
        }

        binding.pwdHide.setOnClickListener {
            pwdVisibility = !pwdVisibility
            if (pwdVisibility) {
                binding.pwdHide.setImageResource(R.drawable.account_ic_password_show)
                binding.loginPwd.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.pwdHide.setImageResource(R.drawable.account_ic_password_hide)
                binding.loginPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.loginPwd.setSelection(binding.loginPwd.text.length)
        }

        binding.toolbar.left { finish() }
            .right {
                RouterManager.navigation(
                    RouterConstants.LOGIN_ACTIVITY, mapOf(
                        KEY_ACCOUNT to account,
                        KEY_MODE to MODE_FORGET_PWD
                    )
                )
            }
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
        val isEmail = RegexUtil.isEmail(account)
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

    /**
     * 登录
     */
    private fun login() {
        val pwd = binding.loginPwd.text.toString()
        val countryCode = LoginManager.getSelectRegionCode() ?: "CHN"
        val area = if (countryCode == "CHN") "86" else ""
        val isEmail = RegexUtil.isEmail(account)
        showLoading()
        viewModel.login(account, pwd, AccountManager.getClientId(), area, isEmail, countryCode)
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