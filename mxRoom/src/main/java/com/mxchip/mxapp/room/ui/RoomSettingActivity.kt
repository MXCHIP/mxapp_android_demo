package com.mxchip.mxapp.room.ui

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.room.databinding.RoomAcRoomSettingBinding
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus

@Router(RouterConstants.ROOM_SETTING_ACTIVITY)
class RoomSettingActivity : MXBusinessActivity<RoomAcRoomSettingBinding>() {

    private val viewModel by viewModels<RoomViewModel>()

    private var refresh: HomeDataRefresh? = null//是否需要通知首页房间数据发现变化
    private var roomId: Int = 0
    private var editHomeId = -1
    private var isDefaultRoom = false

    override fun getLayoutBinding() = RoomAcRoomSettingBinding.inflate(layoutInflater)

    override fun onInit() {
        editHomeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, -1)
        if (editHomeId == -1) {
            finish()
            return
        }
        roomId = intent.getIntExtra(RoomConstants.KEY_ROOM_ID, 0)
        isDefaultRoom = intent.getBooleanExtra(RoomConstants.KEY_IS_DEFAULT_ROOM, false)
        val roomName = intent.getStringExtra(RoomConstants.KEY_ROOM_NAME)!!
        binding.roomName.setEndText(roomName)
        val deviceCount = intent.getIntExtra(RoomConstants.KEY_ROOM_DEVICE_COUNT, 0)
        binding.itemDeviceManager.setEndText("$deviceCount")
        binding.itemSceneManager.visibility = View.GONE
        binding.itemGroupManager.visibility = View.GONE
        binding.itemAutoManager.visibility = View.GONE
        initEvent()
    }

    private val activityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val type = it.data?.getStringExtra("type")
                when (type) {
                    "smart" -> {
                        //"scene" else "auto"
                        val smartType = it.data?.getStringExtra("smart_type")
                        val count = it.data?.getIntExtra("count", 0)
                        if (smartType == "scene") {
                            binding.itemSceneManager.setEndText("$count")
                        } else {
                            binding.itemAutoManager.setEndText("$count")
                        }
                    }

                    "device" -> {
                        val count = it.data?.getIntExtra("count", 0)
                        binding.itemDeviceManager.setEndText("$count")
                    }

                    "group" -> {
                        val count = it.data?.getIntExtra("count", 0)
                        binding.itemGroupManager.setEndText("$count")
                    }
                }

            }
        }

    private fun initEvent() {
        binding.toolbar.left { finish() }

        binding.roomName.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_set_name_title))
                .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_room_name))
                
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.cancel()
                }.edittextText(binding.roomName.getEndText())
                .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_save)) { dialog, inputText ->
                    if (inputText.isNullOrBlank()) {
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                        return@rightButton
                    } else if (inputText.length > MXDialog.maxLength) {
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
                        return@rightButton
                    }
                    dialog.dismiss()
                    val roomName = inputText.trim()
                    updateRoomInfo(roomName)
                }.create().show()
        }
        binding.itemDeviceManager.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.ROOM_SETTING_DEVICE_ACTIVITY, mapOf(
                    HomeConstants.KEY_HOME_ID to editHomeId,
                    RoomConstants.KEY_ROOM_ID to roomId,
                    RoomConstants.KEY_ROOM_NAME to binding.roomName.getEndText(),
                    RoomConstants.KEY_IS_DEFAULT_ROOM to isDefaultRoom
                ), this, activityResult
            )
        }
    }

    private fun updateRoomInfo(roomName: String) {
        showLoading()
        viewModel.updateRoom(roomId, roomName)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED,
                loadFlow<Any>(
                    success = {
                        dismissLoading()
                        binding.roomName.setEndText(roomName)
                        refresh = HomeDataRefresh.ROOM_LIST
                    },
                    error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) {
                            showToast("$msg")
                        }
                    }
                )
            )
    }

    override fun onDestroy() {
        refresh?.apply {
            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH).postValue(this)
        }
        super.onDestroy()
    }
}