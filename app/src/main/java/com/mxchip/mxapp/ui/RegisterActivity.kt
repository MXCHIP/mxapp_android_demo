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
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mxapp.R
import com.mxchip.mxapp.account.consts.AccountConstants
import com.mxchip.mxapp.account.util.AccountManager
import com.mxchip.mxapp.account.vm.AccountViewModel
import com.mxchip.mxapp.base.activity.MXWebViewActivity
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AgreementManager
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.mxapp.databinding.ActivityAccountRegisterBinding
import com.mxchip.router.annotation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 注册
 */
@Router(RouterConstants.REGISTER_ACTIVITY)
class RegisterActivity : MXBusinessActivity<ActivityAccountRegisterBinding>() {

    override fun getLayoutBinding() = ActivityAccountRegisterBinding.inflate(layoutInflater)

    private val loginMode = AccountConstants.MODE_REGISTER
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
        val mainColor = ContextCompat.getColor(
            applicationContext, R.color.app_main_color
        )
        binding.next.setBtnBackgroundColor(mainColor)
        countryIOS3 = LoginManager.getSelectRegionCode() ?: "CHN"
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
        if (AccountManager.isSupportSelectCountry()) {
            binding.btnSwitchRegion.visibility = View.VISIBLE
        }
        binding.tvHasReadPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.tvHasReadPolicy.highlightColor = Color.TRANSPARENT
        binding.tvHasReadPolicy.text = textSpan()

        inject()
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
            if (!binding.ivPolicy.isSelected) {
                policyNotSelected()
            } else {
                AgreementManager.agree()
                register()
            }
        }

        binding.toolbar.left { finish() }
            .right {

            }

        binding.clearText.setOnClickListener {
            binding.etAccount.setText("")
        }

        binding.ivPolicy.setOnClickListener {
            it.isSelected = !it.isSelected
            changeIvSelectedState(binding.ivPolicy, it.isSelected)
        }

        binding.btnSwitchRegion.setOnClickListener {

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
                    map[AccountConstants.KEY_ACCOUNT_EXIST] = false
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
                            MXWebViewActivity.TOP_BAR_TITLE to json.getString("title"),
                            MXWebViewActivity.LOAD_URL to json.getString("url")
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

    /**
     * 注册
     */
    private fun register() {
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
            if (it) {
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_account_already_registered))
            } else {
                requestVerCode()
            }
        }
    }

    private fun checkAccountExist(
        type: Int,
        account: String,
        regionCode: String?,
        checkResult: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                accountVm.checkAccountExist(type, account, regionCode).collect {
                    if (it.state == NetState.SUCCESS) {
                        val resJson = JSONObject(it.data.toString())
                        checkResult.invoke(resJson.optBoolean("exist"))
                    } else {
                        val netErr = onException(it.state.code)
                        withContext(Dispatchers.Main) {
                            if (!netErr)
                                showToast("${it.state.message}")
                        }
                    }
                }
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
        accountVm.sendVerifyCode(type, 1, account, regionCode)
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