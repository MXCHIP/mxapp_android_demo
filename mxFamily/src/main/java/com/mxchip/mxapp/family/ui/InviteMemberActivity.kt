package com.mxchip.mxapp.family.ui

import android.text.InputFilter
import android.text.InputType
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.http.response.NetState
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_ROOM_IDS
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.family.adapter.ShareInProgressAdapter
import com.mxchip.mxapp.family.bean.ShareInProgressBean
import com.mxchip.mxapp.family.databinding.FamilyAcInviteMemberBinding
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/18
 * @desc:
 */
@Router(RouterConstants.INVITEE_MEMBER_ACTIVITY)
class InviteMemberActivity : MXBusinessActivity<FamilyAcInviteMemberBinding>() {
    override fun getLayoutBinding() = FamilyAcInviteMemberBinding.inflate(layoutInflater)

    private val shareInProgressAdapter by lazy { ShareInProgressAdapter() }
    private var memberInCurrentHomeRole = 2
    private var memberInCurrentHomeRoomPermission = ""
    private var homeId = -1
    private lateinit var homeName: String
    private val viewModel by viewModels<HomeViewModel>()

    override fun onInit() {
        homeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, -1)
        homeName = intent.getStringExtra("home.name") ?: ""
        memberInCurrentHomeRole = intent.getIntExtra("memberInCurrentHomeRole", 2)
        memberInCurrentHomeRoomPermission = intent.getStringExtra(KEY_ROOM_IDS) ?: ""

        binding.ivFaceToFaceInviteIcon.background = generateShape("#1a29a3ff".toColorInt(), 12)
        binding.ivCodeInviteIcon.background = generateShape("#1a976ffb".toColorInt(), 12)
        binding.ivAccountIcon.background = generateShape("#1afec60c".toColorInt(), 12)
        binding.llWaitAgree.visibility = View.GONE
        binding.rvShareInProgress.background =
            generateShape(getColor(com.mxchip.mxapp.base.R.color.core_color_background_cancel), 8)
        binding.rvShareInProgress.layoutManager = LinearLayoutManager(this)
        binding.rvShareInProgress.adapter = shareInProgressAdapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.btnInvite.setBtnBackgroundColor(mainColor)

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
        initEvent(isPhone)
        shareInProgress()
    }

    private fun initEvent(isPhone: Boolean) {
        binding.toolbar.left { finish() }
        binding.etAccount.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (source == " ") "" else null
        })
        binding.etAccount.addTextChangedListener {
            val username = binding.etAccount.text.toString()
            binding.clearText.visibility = if (username.isEmpty()) View.GONE else View.VISIBLE
            val btnEnable = if (isPhone) {
                RegexUtil.isPhoneNumber(username)
            } else {
                RegexUtil.isEmail(username)
            }
            binding.btnInvite.isEnabled = btnEnable
            binding.btnInvite.alpha = if (btnEnable) 1f else 0.4f
        }
        binding.clearText.setOnClickListener {
            binding.etAccount.setText("")
        }
        binding.faceToFaceInvite.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.ROOM_PERMISSION_ACTIVITY, mapOf(
                    HomeConstants.KEY_HOME_ID to homeId,
                    HomeConstants.KEY_HOME_NAME to homeName,
                    HomeConstants.KEY_ROOM_PERMISSION_TYPE to 0,
                    "memberInCurrentHomeRole" to memberInCurrentHomeRole,
                    "memberInCurrentHomeRoomPermission" to memberInCurrentHomeRoomPermission,
                )
            )
        }

        binding.codeInvite.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.ROOM_PERMISSION_ACTIVITY, mapOf(
                    HomeConstants.KEY_HOME_ID to homeId,
                    HomeConstants.KEY_HOME_NAME to homeName,
                    HomeConstants.KEY_ROOM_PERMISSION_TYPE to 1,
                    "memberInCurrentHomeRole" to memberInCurrentHomeRole,
                    "memberInCurrentHomeRoomPermission" to memberInCurrentHomeRoomPermission,
                    "home.name" to homeName,
                )
            )
        }

        binding.btnInvite.setOnClickListener {
            val account = binding.etAccount.text.toString().trim()
            this.currentFocus?.apply {
                KeyboardUtil.hideKeyboard(this)
            }
            checkAccountExist(isPhone, account)
        }

        shareInProgressAdapter.setRecallCallback { sharingBean ->
            showLoading()
            viewModel.shareCancel(sharingBean.id).launchAndCollectIn(
                this@InviteMemberActivity, Lifecycle.State.CREATED, loadFlow<Any>(
                    success = {
                        dismissLoading()
                        shareInProgress()
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }
                )
            )
        }
        LiveDataBus.with<Int>(LiveDataBusKey.KEY_SHARE_PROGRESS_REFRESH).observe(this) {
            shareInProgress()
        }
    }

    private fun checkAccountExist(isPhone: Boolean, account: String) {
        val type: Int
        val regionCode: String?
        if (isPhone) {
            type = 0
            regionCode = "86"
        } else {
            type = 1
            regionCode = null
        }
        showLoading()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.checkAccountExist(type, account, regionCode).collect {
                    dismissLoading()
                    if (it.state == NetState.SUCCESS) {
                        val resJson = JSONObject(it.data.toString())
                        if (resJson.optBoolean("exist")) {
                            RouterManager.navigation(
                                RouterConstants.ROOM_PERMISSION_ACTIVITY, mapOf(
                                    HomeConstants.KEY_HOME_ID to homeId,
                                    HomeConstants.KEY_HOME_NAME to homeName,
                                    HomeConstants.KEY_ROOM_PERMISSION_TYPE to 2,
                                    "account" to account,
                                    "memberInCurrentHomeRole" to memberInCurrentHomeRole,
                                    "memberInCurrentHomeRoomPermission" to memberInCurrentHomeRoomPermission,
                                )
                            )
                        } else {
                            showToast(getString(com.mxchip.mxapp.base.R.string.mx_account_not_exist))
                        }
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

    private fun shareInProgress() {
        viewModel.shareInProgress(homeId)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlowList<ShareInProgressBean>(
                    success = {
                        it?.apply {
                            if (this.isNotEmpty()) {
                                binding.llWaitAgree.visibility = View.VISIBLE
                                shareInProgressAdapter.notifyData(this)
                            } else {
                                binding.llWaitAgree.visibility = View.GONE
                            }
                        }
                    }, error = { code, msg, _ ->
                        binding.llWaitAgree.visibility = View.GONE
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }
                ))
    }

    override fun finish() {
        setResult(RESULT_OK)
        super.finish()
    }
}