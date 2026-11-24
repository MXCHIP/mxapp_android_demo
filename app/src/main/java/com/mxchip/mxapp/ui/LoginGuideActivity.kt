package com.mxchip.mxapp.ui

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import com.mxchip.http.response.NetState
import com.mxchip.mxapp.BuildConfig
import com.mxchip.mxapp.account.bean.CountryBean
import com.mxchip.mxapp.account.ui.AgreementDialog
import com.mxchip.mxapp.account.util.AccountManager
import com.mxchip.mxapp.account.vm.AccountViewModel
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.activity.MXWebViewActivity
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.dialog.BottomActionDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AgreementManager
import com.mxchip.mxapp.base.utils.AssetsUtil
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.LanguageUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.databinding.ActivityLoginGuideBinding
import com.mxchip.mxapp.utils.EnvConfigManager
import com.mxchip.router.annotation.Router
import org.json.JSONObject
import kotlin.system.exitProcess

@Router(path = RouterConstants.LOGIN_GUIDE_ACTIVITY)
class LoginGuideActivity : MXBusinessActivity<ActivityLoginGuideBinding>() {
    companion object {
        private const val TAG = "LoginGuideActivity"
    }

    override fun getLayoutBinding() = ActivityLoginGuideBinding.inflate(layoutInflater)

    private val accountVm by viewModels<AccountViewModel>()

    override fun onInit() {
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
            }
        }

        if (AccountManager.isSupportSelectCountry()) {
            binding.tvRegion.setOnClickListener {
                RouterManager.navigation(RouterConstants.SELECT_REGION_ACTIVITY)
            }
        } else {
            binding.tvRegion.visibility = View.GONE
            LoginManager.saveSelectRegionCode("CHN")//不支持，默认中国大陆
            EnvConfigManager.initHostConfig("CHN", true)
        }

        binding.tvLogin.setOnClickListener {
            if (AccountManager.isSupportSelectCountry()) {
                val iso3 = LoginManager.getSelectRegionCode()
                if (iso3.isNullOrEmpty()) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_select_country_code_hint))
                    RouterManager.navigation(RouterConstants.SELECT_REGION_ACTIVITY)
                } else
                    RouterManager.navigation(RouterConstants.LOGIN_ACTIVITY)
            } else {
                RouterManager.navigation(RouterConstants.LOGIN_ACTIVITY)
            }
        }
        binding.tvRegister.setOnClickListener {
            RouterManager.navigation(RouterConstants.REGISTER_ACTIVITY)
        }
        val iso3 = LoginManager.getSelectRegionCode()
        if (!iso3.isNullOrEmpty()) {//已选国家/地区
            checkAgreement()//检查是否同意协议
            regionData?.firstOrNull { it.iso3 == iso3 }?.apply {
                binding.tvRegion.text = if (LanguageUtil.isZh()) this.zh_name else this.en_name
            }
        }
        if (BuildConfig.BUILD_TYPE == EnvConfigManager.BUILD_TYPE_DEBUG) {
            binding.envState.visibility = View.VISIBLE
            //debug 包
            changeEnvText()
            binding.envState.setOnClickListener {
                envSwitchDialog.create().show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val iso3 = LoginManager.getSelectRegionCode()
        if (!iso3.isNullOrEmpty()) {
            //检查是否同意协议
            checkAgreement()
            regionData?.firstOrNull { it.iso3 == iso3 }?.apply {
                binding.tvRegion.text = if (LanguageUtil.isZh()) this.zh_name else this.en_name
                EnvConfigManager.initHostConfig(this.server_station, true)
            }
        }
    }

    private val regionData by lazy {
        JsonUtil.fromJsonToArray(
            AssetsUtil.readJsonFromAssets(applicationContext, "countrys.json"),
            CountryBean::class.java
        )
    }

    private val dialog by lazy {
        AgreementDialog().apply {
            setPolicyClick { type ->
                getPolicy(type)
            }
            setAgreementClick {
                //同意协议
            }
            setDisagreeClick {
                MXBusinessApplication.mxBusinessApp.finishAllActivity()
                exitProcess(0)
            }
        }
    }

    /**
     * 检查用户隐私协议是否同意
     */
    private fun checkAgreement() {
        when {
            AgreementManager.isAgree().not() -> {
                dialog.show(supportFragmentManager, "AgreementDialog")
            }

            AccountManager.isSupportSelectCountry().not() -> {
                //中国大陆不支持选国家
            }
        }
    }

    private fun getPolicy(type: Int) {
        accountVm.docInfo(type)
    }

    private fun changeEnvText() {
        binding.envState.text = when (EnvConfigManager.getEnvSwitch()) {
            EnvConfigManager.ENV_DEV -> "当前开发环境"
            EnvConfigManager.ENV_PREV -> "当前预发环境"
            EnvConfigManager.ENV_RELEASE -> "当前生产环境"
            else -> "当前测试环境"
        }
    }

    private fun switchEnv(env: String) {
        EnvConfigManager.switchEnv(env)
        EnvConfigManager.initHostConfig("CHN", true)
    }

    private val envSwitchDialog by lazy {
        BottomActionDialog(this)
            .title("切换环境")
            .addItem("开发环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_DEV)
                changeEnvText()
            }.addItem("测试环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_TEST)
                changeEnvText()
            }.addItem("预发环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_PREV)
                changeEnvText()
            }.addItem("生产环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_RELEASE)
                changeEnvText()
            }
    }
}