package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import coil.load
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.ClipboardManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.device.databinding.DeviceAcShortcutDeviceDetailBinding
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 *快捷设备详情
 */
@Router(RouterConstants.SHORTCUT_DEVICE_DETAIL)
class ShortcutDeviceDetailActivity : MXBusinessActivity<DeviceAcShortcutDeviceDetailBinding>() {


    private val vm by viewModels<DeviceViewModel>()

    private val formatter by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    }

    private var shortcutIdentifier: String? = null

    override fun getLayoutBinding(): DeviceAcShortcutDeviceDetailBinding {
        return DeviceAcShortcutDeviceDetailBinding.inflate(layoutInflater)
    }

    private var deviceInfo: DeviceBean? = null

    private val roomLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.apply {
                    val roomId = this.getIntExtra("room_id", -1)
                    if (deviceInfo?.room_id != roomId) {
                        val roomName = this.getStringExtra("room_name")!!
                        updateRoom(roomId, roomName)
                    }
                }
            }
        }
    private var shortcutDeviceNum = 0
    override fun onInit() {
        val iotId = intent.getStringExtra(CommonConstants.KEY_IOT_ID)
        shortcutDeviceNum = intent.getIntExtra("number", 0)
        if (iotId.isNullOrEmpty() || shortcutDeviceNum == 0) {
            finish()
            return
        }
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        binding.toolbar.left {
            backPressed()
        }

        binding.btnEditName.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_device_name))
                .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
                .edittextText(binding.tvNickName.text.toString())
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, input ->
                    val inputText = input?.trim()
                    if (inputText.isNullOrEmpty()) {
                        ToastUtil.showToast(this, getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                        return@rightButton
                    } else if (inputText.length > MXDialog.maxLength) {
                        ToastUtil.showToast(
                            this, getString(com.mxchip.mxapp.base.R.string.mx_name_invalid)
                        )
                        return@rightButton
                    }
                    dialog.dismiss()
                    binding.tvNickName.text = inputText
                    updateDeviceIdentifierName(iotId, inputText)
                }.create().show()
        }
        binding.itemDeviceIdentify.setOnLongClickListener {
            ClipboardManager.setTextToClip(this, binding.itemDeviceIdentify.getEndText())
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_copy_clipboard))
            true
        }
        binding.itemRoomName.setOnClickListener {
            if (null == deviceInfo) return@setOnClickListener
            if (RoleManager.isUser()) return@setOnClickListener
            RouterManager.navigation(RouterConstants.ROOM_SELECT_ACTIVITY,mapOf(
                "room_id" to deviceInfo!!.room_id, "rightText" to getString(com.mxchip.mxapp.base.R.string.mx_save)
            ))
        }
        binding.btnDelete.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_shortcut_delete))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_del_shortcut_des))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                    dialog.dismiss()
                    delShortcutDevice()
                }.create().show()
        }
        initData(iotId, shortcutDeviceNum)
    }

    private fun initData(iotId: String, number: Int) {
        showLoading()
        vm.getShortcutDeviceInfo(iotId, number).launchAndCollectIn(
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
                }))
    }

    private fun parseDeviceData(device: DeviceBean) {
        deviceInfo = device
        if (device.bind_time == null)
            binding.tvDeviceAddTime.visibility = View.GONE
        else
            binding.tvDeviceAddTime.text = String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_created_time),
                formatter.format(device.bind_time!! * 1000)//*1000 服务端返回时间戳 10位
            )
        binding.ivDeviceIcon.load(device.product_image)
        binding.tvNickName.text = device.nick_name.ifEmpty { device.product_name }
        shortcutIdentifier = device.propertys?.firstOrNull()?.identifier
        binding.itemDeviceIdentify.setEndText(device.device_name)
        binding.itemRoomName.setEndText(device.room_name)
    }

    private var homeRefresh: HomeDataRefresh? = null

    private fun updateDeviceIdentifierName(iotId: String, name: String) {
        showLoading()
        if (shortcutIdentifier.isNullOrEmpty()) return
        vm.updateDeviceIdentifierName(iotId, shortcutIdentifier!!, name).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlow<Any>(
                success = {
                    dismissLoading()
                    homeRefresh = HomeDataRefresh.DEVICE
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                })
        )
    }

    private fun updateRoom(roomId: Int, roomName: String) {
        val shortcutDev = deviceInfo
        if (shortcutDev != null) {
            val shortcutProps = shortcutDev.propertys?.map { it.identifier } ?: listOf<String>()
            showLoading()
            vm.updateDeviceShortcutConfig(
                shortcutDev.iotid,
                shortcutDeviceNum,
                shortcutProps,
                roomId,
                binding.tvNickName.text.toString(),
                shortcutDev.show
            ).launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<Any>(
                    success = {
                        dismissLoading()
                        binding.itemRoomName.setEndText(roomName)
                        homeRefresh = HomeDataRefresh.DEVICE
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }
                )
            )
        }
    }

    private fun delShortcutDevice() {
        val delShortcut = deviceInfo
        if (delShortcut != null){
            showLoading()
            vm.updateDeviceShortcutConfig(
                delShortcut.iotid,
                shortcutDeviceNum,
                listOf<String>(),
                delShortcut.room_id,
                delShortcut.nick_name,
                false
            ).launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<Any>(
                    success = {
                        dismissLoading()
                        LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                            .postValue(HomeDataRefresh.DEVICE)
                        RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
                        finish()
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    })
            )
        }

    }

    private fun backPressed() {
        homeRefresh?.apply {
            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH).postValue(this)
        }
        finish()
    }
}