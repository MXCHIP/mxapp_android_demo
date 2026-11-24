package com.mxchip.mxapp.family.ui

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.mxchip.router.annotation.Router
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.http.response.NetState
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.family.databinding.FamilyAcJoinHomeByInviteCodeBinding
import org.json.JSONObject


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/22
 * @desc:
 */
@Router(RouterConstants.JOIN_HOME_BY_INVITE_CODE_ACTIVITY)
class JoinHomeByInviteCodeActivity : MXBusinessActivity<FamilyAcJoinHomeByInviteCodeBinding>() {

    override fun getLayoutBinding() = FamilyAcJoinHomeByInviteCodeBinding.inflate(layoutInflater)

    private var code: String? = null
    private val viewModel by viewModels<HomeViewModel>()

    override fun onInit() {
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.join.setBtnBackgroundColor(mainColor)
        binding.ivMember.setColorFilter(mainColor)
        initEvent()
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }

        binding.join.setOnClickListener {
            KeyboardUtil.hideKeyboard(binding.etVerificationCode)
            if (code.isNullOrBlank() || code!!.length < 6) {
                return@setOnClickListener
            }
            showLoading()
            viewModel.bindShareFamilyByCode(1, code!!)
                .launchAndCollectIn(this, Lifecycle.State.CREATED) { netState ->
                    dismissLoading()
                    if (netState.state == NetState.SUCCESS) {
                        netState.data?.apply {
                            val json = JSONObject(this)
                            val newHomeId = json.getInt("home_id")
                            val intent = Intent()
                            intent.putExtra("homeId", newHomeId)
                            setResult(RESULT_OK, intent)
                            finish()
                        }
                    } else {
                        netState.state.message?.apply {
                            showToast(this)
                        }
                    }
                }
        }

        binding.etVerificationCode.setOnTextChangedListener {
            if (it.length == 6) {
                code = it
                binding.join.isEnabled = true
                binding.join.alpha = 1f
            } else {
                code = null
                binding.join.isEnabled = false
                binding.join.alpha = 0.4f
            }
        }
    }
}