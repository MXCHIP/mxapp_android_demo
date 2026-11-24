package com.mxchip.mxapp.family.ui

import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ImageSpan
import android.text.style.UnderlineSpan
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.family.R
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.databinding.FamilyAcCreateHomeGuideBinding
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/15
 * @desc:
 */
@Router(RouterConstants.CREATE_HOME_GUIDE_ACTIVITY)
class CreateHomeGuideActivity : MXBusinessActivity<FamilyAcCreateHomeGuideBinding>() {

    override fun getLayoutBinding() = FamilyAcCreateHomeGuideBinding.inflate(layoutInflater)

    private val viewModel by viewModels<HomeViewModel>()

    override fun onInit() {
        onBackPressedDispatcher.addCallback {
            // 禁止
        }
        initView()
        initDesc()
        initSkip()
        initEvent()
    }

    private fun initView() {
        binding.homeName.background =
            generateStoke(2, getColor(com.mxchip.mxapp.base.R.color.core_color_dividers), 25)
        binding.homeName.text = getString(com.mxchip.mxapp.base.R.string.mx_my_family)
    }

    private fun initDesc() {
        val span = SpannableString(getString(com.mxchip.mxapp.base.R.string.mx_home_guide_desc))
        val insertIndex = span.indexOf("@")
        val drawable = ContextCompat.getDrawable(this, R.drawable.family_ic_edit_underline)
        drawable?.setBounds(0, 0, 14.dp2px(), 14.dp2px())
        val imageSpan = ImageSpan(drawable!!)
        span.setSpan(imageSpan, insertIndex, insertIndex + 1, SPAN_INCLUSIVE_EXCLUSIVE)
        binding.tvDesc.text = span
    }

    private fun initSkip() {
        val content = SpannableString(getString(com.mxchip.mxapp.base.R.string.mx_skip))
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        binding.skip.text = content
    }

    private fun createHomeSetDefaultRoom() {
        showLoading()
        viewModel.createFamilyFlow(getString(com.mxchip.mxapp.base.R.string.mx_my_family), listOf())
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<HomeBean>(success = {
                dismissLoading()
                LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
                RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
            }, error = { code, msg, _ ->
                val netErr = onException(code)
                if (!netErr) {
                    if (10021 == code) {//家庭已创建成功，重复code
                        LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
                        RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
                    } else showToast("$msg")
                }
            }))
    }

    private fun initEvent() {
        binding.ivEdit.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_family_name))
                .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
                .edittextText(binding.homeName.text.toString())
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.cancel()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, inputText ->
                    if (inputText.isNullOrBlank()) {
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                        return@rightButton
                    } else if (inputText.length > MXDialog.maxLength) {
                        ToastUtil.showLongToast(
                            this, getString(com.mxchip.mxapp.base.R.string.mx_name_invalid)
                        )
                        return@rightButton
                    }
                    dialog.cancel()
                    binding.homeName.text = inputText.trim()
                }.create().show()
        }
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.btnNext.setBtnBackgroundColor(mainColor)
        binding.btnNext.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.CREATE_ROOM_GUIDE_ACTIVITY, mapOf(
                    "home_name" to binding.homeName.text.toString()
                )
            )
        }

        binding.skip.setOnClickListener {
            createHomeSetDefaultRoom()
        }
    }
}