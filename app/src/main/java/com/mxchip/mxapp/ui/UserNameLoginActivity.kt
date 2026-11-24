package com.mxchip.mxapp.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import coil.load
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mxapp.R
import com.mxchip.mxapp.account.bean.AutoLoginBean
import com.mxchip.mxapp.account.consts.AccountConstants
import com.mxchip.mxapp.account.util.AccountManager
import com.mxchip.mxapp.account.vm.AccountViewModel
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.activity.MXWebViewActivity
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AgreementManager
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.mxapp.databinding.ActivityAccountLoginBinding
import com.mxchip.router.annotation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs

/**
 * 登录
 */
@Router(RouterConstants.LOGIN_ACTIVITY)
class UserNameLoginActivity : MXBusinessActivity<ActivityAccountLoginBinding>() {

    override fun getLayoutBinding() = ActivityAccountLoginBinding.inflate(layoutInflater)

    private var loginMode = AccountConstants.MODE_REGISTER
    private lateinit var countryIOS3: String

    private val accountVm by viewModels<AccountViewModel>()

    private val policyAnimation = ValueAnimator.ofFloat(0f, 30f, -30f, 30f, -30f, 30f, 0f).apply {
        addUpdateListener {
            val value = it.animatedValue as Float
            binding.llPolicy.translationX = value
        }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {

            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }

        })
    }

    override fun onInit() {
        loginMode = intent.getIntExtra(AccountConstants.KEY_MODE, AccountConstants.MODE_REGISTER)
        val mainColor = ContextCompat.getColor(
            applicationContext, R.color.app_main_color
        )
        binding.next.setBtnBackgroundColor(mainColor)
        countryIOS3 = LoginManager.getSelectRegionCode() ?: "CHN"
        binding.tvThirdLogin.visibility = View.GONE
        binding.loginByWechat.visibility = View.GONE
        binding.loginByAlipay.visibility = View.GONE
        if (countryIOS3 == "CHN") {//中国大陆使用 手机号
            binding.etAccount.hint =
                getString(com.mxchip.mxapp.base.R.string.mx_account_phone_hint)
            binding.etAccount.inputType = InputType.TYPE_CLASS_PHONE
        } else {
            binding.tvSelectedRegion.visibility = View.GONE
            binding.ivEmail.visibility = View.VISIBLE
            binding.etAccount.hint =
                getString(com.mxchip.mxapp.base.R.string.mx_account_email_hint)
            binding.etAccount.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        binding.tvHasReadPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvHasReadPolicy.highlightColor = Color.TRANSPARENT
        binding.tvHasReadPolicy.text = textSpan()

        inject()
        switchLoginMode(loginMode)
        initEvent()
        val account = intent.getStringExtra(AccountConstants.KEY_ACCOUNT)
        binding.etAccount.setText(account)
        nextButtonEnable(!account.isNullOrEmpty())
    }

    private fun textSpan(): SpannableString {
        val protocol = getString(com.mxchip.mxapp.base.R.string.mx_user_agreement)
        val policy = getString(com.mxchip.mxapp.base.R.string.mx_privacy_policy)
        val agreementText =
            getString(com.mxchip.mxapp.base.R.string.mx_read_agree) + " " + protocol + " " + getString(
                com.mxchip.mxapp.base.R.string.mx_and
            ) + " " + policy
        val spanString = SpannableString(agreementText)
        val protocolIndex = spanString.indexOf(protocol)
        val policyIndex = spanString.indexOf(policy)
        val protocolClickSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                accountVm.docInfo(2)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColor(R.color.app_main_color)
            }
        }

        val policyClickSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                accountVm.docInfo(1)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = getColor(R.color.app_main_color)
            }
        }
        spanString.setSpan(
            protocolClickSpan,
            protocolIndex,
            protocolIndex + protocol.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spanString.setSpan(
            policyClickSpan,
            policyIndex,
            policyIndex + policy.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spanString
    }


    private fun initEvent() {
        if (loginMode != AccountConstants.MODE_MODIFY_PWD && loginMode != AccountConstants.MODE_FORGET_PWD) {
            //修改密码逻辑，不用
            val isPhone = countryIOS3 == "CHN"
            binding.etAccount.addTextChangedListener {
                val username = binding.etAccount.text.toString()
                binding.clearText.visibility = if (username.isEmpty()) View.GONE else View.VISIBLE
                val btnEnable = if (isPhone) {
                    RegexUtil.isPhoneNumber(username)
                } else {
                    RegexUtil.isEmail(username)
                }
                nextButtonEnable(btnEnable)
            }
        }

        binding.next.setOnClickListener {
            this.currentFocus?.apply {
                KeyboardUtil.hideKeyboard(this)
            }
            when (loginMode) {
                AccountConstants.MODE_PWD_LOGIN -> {
                    if (!binding.ivPolicy.isSelected) {
                        policyNotSelected()
                    } else {
                        loginAction(true)
                    }
                }

                AccountConstants.MODE_REGISTER -> {
                    if (!binding.ivPolicy.isSelected) {
                        policyNotSelected()
                    } else {
                        loginAction(false)
                    }
                }

                AccountConstants.MODE_MODIFY_PWD, AccountConstants.MODE_FORGET_PWD ->
                    requestVerCode()
            }
        }

        binding.toolbar.left { finish() }
            .right {
                switchLoginMode(abs(loginMode - 1))
            }

        binding.clearText.setOnClickListener {
            binding.etAccount.setText("")
        }

        binding.ivPolicy.setOnClickListener {
            it.isSelected = !it.isSelected
            changeIvSelectedState(binding.ivPolicy, it.isSelected)
        }
    }

    private var registerToken: String? = null//第三方注册所需token

    private fun wechatLogin(autoLoginBean: AutoLoginBean) {
        if (autoLoginBean.status == 0) {//微信未绑定
            loginMode = AccountConstants.MODE_REGISTER //切换到（注册模式）输入手机号与微信绑定
            binding.desc.text = getString(com.mxchip.mxapp.base.R.string.mx_bind_account)
            binding.toolbar.rightText("")
            binding.llPolicy.visibility = View.GONE
            binding.tvThirdLogin.visibility = View.GONE
            binding.loginByWechat.visibility = View.GONE
            binding.loginByAlipay.visibility = View.GONE
            binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_send_code)
            registerToken = autoLoginBean.register_token
        } else {
            LoginManager.saveToken(autoLoginBean.access_token!!)
            RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
            MXBusinessApplication.Companion.mxBusinessApp.finishOtherActivity(RouterConstants.MAIN_ACTIVITY)
        }
    }

    private fun inject() {
        //获取验证码
        accountVm.sendVerState.observe(
            this, loadObserver<Any>(
                {
                    dismissLoading()
                    val map = mutableMapOf<String, Any>()
                    map[AccountConstants.KEY_ACCOUNT] = binding.etAccount.text.toString()
                    map[AccountConstants.KEY_MODE] = loginMode
                    map[AccountConstants.KEY_ACCOUNT_EXIST] = true
                    registerToken?.apply {//第三方登录
                        map["registerToken"] = this
                    }
                    RouterManager.navigation(RouterConstants.VERIFICATION_ACTIVITY, map)
                },
                { code: Int, msg: String?, data: Any? ->
                    dismissLoading()
                    MXLog.i("cxk", "error   ---> $code, $msg, $data")
                    ToastUtil.showToast(this, msg ?: code.toString())
                }
            )
        )

        accountVm.docInfoState.observe(this) {
            if (it.state == NetState.SUCCESS) {
                it.data?.apply {
                    val json = JSONObject(this)
                    RouterManager.navigation(
                        RouterConstants.PROTOCOL_ACTIVITY, mapOf(
                            MXWebViewActivity.Companion.TOP_BAR_TITLE to json.getString("title"),
                            MXWebViewActivity.Companion.LOAD_URL to json.getString("url")
                        )
                    )
                }
            } else {
                it.state.message?.apply {
                    showToast(this)
                }
            }
        }
    }

    private fun policyNotSelected() {
        ToastUtil.showLongToast(
            this,
            getString(com.mxchip.mxapp.base.R.string.mx_read_policy_first)
        )
        disagreePolicyAnimation()
    }

    private fun nextButtonEnable(isEnable: Boolean) {
        binding.next.isEnabled = isEnable
        binding.next.alpha = if (isEnable) 1f else 0.5f
    }

    private fun switchLoginMode(mode: Int) {
        loginMode = mode
        when (mode) {
            AccountConstants.MODE_PWD_LOGIN -> {
                binding.desc.text = getString(com.mxchip.mxapp.base.R.string.mx_login)
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_next)
                binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_code_login))
            }

            AccountConstants.MODE_REGISTER -> {
                binding.desc.text = getString(com.mxchip.mxapp.base.R.string.mx_login)
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_send_code)
                binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_password_login))
            }

            AccountConstants.MODE_MODIFY_PWD -> {
                binding.etAccount.isEnabled = false //禁止输入
                binding.clearText.visibility = View.GONE    //隐藏清空输入内容icon

                binding.desc.text =
                    getString(com.mxchip.mxapp.base.R.string.mx_change_password)
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_send_code)
                binding.toolbar.rightText("")
                binding.tvThirdLogin.visibility = View.GONE
                binding.loginByWechat.visibility = View.GONE
                binding.loginByAlipay.visibility = View.GONE
                binding.llPolicy.visibility = View.GONE
            }

            AccountConstants.MODE_FORGET_PWD -> {
                binding.etAccount.isEnabled = false //禁止输入
                binding.clearText.visibility = View.GONE    //隐藏清空输入内容icon

                binding.desc.text =
                    getString(com.mxchip.mxapp.base.R.string.mx_forget_password)
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_send_code)
                binding.toolbar.rightText("")
                binding.tvThirdLogin.visibility = View.GONE
                binding.loginByWechat.visibility = View.GONE
                binding.loginByAlipay.visibility = View.GONE
                binding.llPolicy.visibility = View.GONE
            }
        }
    }

    /**
     * 密码登录
     */
    private fun loginAction(isPwdLogin: Boolean) {
        AgreementManager.agree()
        val account = binding.etAccount.text.toString()
        val type: Int
        val regionCode: String?
        if (countryIOS3 == "CHN") {
            type = 0
            regionCode = "86"
        } else {
            type = 1
            regionCode = null
        }
        checkAccountExist(type, account, regionCode) {
            if (isPwdLogin) {
                RouterManager.navigation(
                    RouterConstants.PASSWORD_ACTIVITY, mapOf(
                        AccountConstants.KEY_ACCOUNT to account,
                        AccountConstants.KEY_MODE to AccountConstants.MODE_PWD_LOGIN
                    )
                )
            } else {
                accountVm.sendVerifyCode(type, 0, account, regionCode)
            }
        }
    }

    private fun checkAccountExist(
        type: Int,
        account: String,
        regionCode: String?,
        exist: () -> Unit
    ) {
        accountVm.checkAccountExist(type, account, regionCode)
            .launchAndCollectIn(this, Lifecycle.State.CREATED) {
                if (it.state == NetState.SUCCESS) {
                    val resJson = JSONObject(it.data.toString())
                    if (resJson.optBoolean("exist")) {
                        exist.invoke()
                    } else {
                        MXDialog.Builder(this@UserNameLoginActivity)
                            .title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                            .content(getString(com.mxchip.mxapp.base.R.string.mx_account_unregistered))
                            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                                alertDialog.cancel()
                            }
                            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_go_register)) { alertDialog, _ ->
                                alertDialog.cancel()
                                RouterManager.navigation(
                                    RouterConstants.REGISTER_ACTIVITY,
                                    mapOf(AccountConstants.KEY_ACCOUNT to account)
                                )
                                finish()
                            }.create().show()
                    }
                } else {
                    val netErr = onException(it.state.code)
                    if (!netErr)
                        showToast("${it.state.message}")
                }
            }
    }

    private fun requestVerCode() {
        val account = binding.etAccount.text.toString()
        val type: Int
        val regionCode: String?
        if (countryIOS3 == "CHN") {
            type = 0
            regionCode = "86"
        } else {
            type = 1
            regionCode = null
        }
        //SCENE 0账号已存在，1注册
        accountVm.sendVerifyCode(type, 0, account, regionCode)
    }

    private fun changeIvSelectedState(view: ImageView, isSelected: Boolean) {
        if (isSelected) {
            view.load(com.mxchip.mxapp.base.R.drawable.core_ic_round_selected)
        } else {
            view.load(com.mxchip.mxapp.base.R.drawable.core_ic_round_unselected)
            view.clearColorFilter()
        }
    }

    private fun disagreePolicyAnimation(duration: Long = 1200L) {
        if (!policyAnimation.isStarted)
            policyAnimation.duration = duration
        policyAnimation.start()
    }
}