package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import coil.load
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.ClipboardManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.mxapp.device.databinding.DeviceAcSharedDeviceDetailBinding
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import java.text.SimpleDateFormat

/**
 *分享设备详情
 */
@Router(RouterConstants.SHARED_DEVICE_DETAIL_ACTIVITY)
class SharedDeviceDetailActivity : MXBusinessActivity<DeviceAcSharedDeviceDetailBinding>() {


    private val vm by viewModels<DeviceViewModel>()
    private var deviceInfo: DeviceBean? = null

    private val formatter by lazy {
        SimpleDateFormat("yyyy-MM-dd")
    }

    override fun getLayoutBinding(): DeviceAcSharedDeviceDetailBinding {
        return DeviceAcSharedDeviceDetailBinding.inflate(layoutInflater)
    }

    private var iotId: String? = null
    override fun onInit() {
        iotId = intent.getStringExtra(CommonConstants.KEY_IOT_ID)
        if (iotId.isNullOrEmpty()) {
            finish()
            return
        }
        inject()
        initData()
        binding.itemFeedback.visibility = android.view.View.GONE
    }

    private fun initData() {
        showLoading()
        vm.getDeviceInfo(iotId!!)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<DeviceBean>(
                    success = {
                        dismissLoading()
                        it?.apply {
                            parseDeviceData(this)
                        }
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }
                ))
    }

    private fun inject() {
        binding.toolbar.left {
            finish()
        }

        binding.itemDeviceIdentify.setOnLongClickListener {
            ClipboardManager.setTextToClip(this, binding.itemDeviceIdentify.getEndText())
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_copy_clipboard))
            true
        }

        binding.btnDelete.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_del_device))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_del_share_device_des))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                    dialog.dismiss()
                    showLoading()
                    vm.unBindSharedDevice(listOf(iotId!!))
                }.create().show()
        }

        vm.unBindState.observe(
            this, loadObserver<Any>(success = {
                dismissLoading()
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.SHARED_DEVICE)
                RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            })
        )

        binding.itemFeedback.setOnClickListener {
            if (null == deviceInfo) return@setOnClickListener
            RouterManager.navigation(
                RouterConstants.FEEDBACK_EDIT_ACTIVITY, mapOf(
                    ProductConstants.KEY_PRODUCT_NAME to deviceInfo!!.nick_name.ifEmpty { deviceInfo!!.product_name },
                    ProductConstants.KEY_PRODUCT_KEY to deviceInfo!!.product_key,
                    "faq.type" to 1
                )
            )
        }
    }

    private fun parseDeviceData(device: DeviceBean) {
        this.deviceInfo = device
        binding.itemOtaUpdate.setEndText(device.firmware_version).setEndIconGone()
        if (device.mcu_version.isNullOrEmpty()) {
            binding.itemMcu.visibility = View.GONE
        } else {
            binding.itemMcu.setEndText(device.mcu_version).setEndIconGone()
        }
        if (device.bind_time == null)
            binding.tvDeviceAddTime.visibility = View.GONE
        else
            binding.tvDeviceAddTime.text = String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_shared_time),
                formatter.format(device.bind_time!! * 1000)//*1000 服务端返回时间戳 10位
            )
        binding.ivDeviceIcon.load(device.product_image)
        binding.tvNickName.text = device.nick_name.ifEmpty { device.product_name }
        binding.itemDeviceIdentify.setEndText(device.device_name)
        binding.itemProductIdentify.setEndText(device.product_key).setEndIconGone()
        binding.itemDeviceMac.setEndText(device.mac)
    }
}