package com.mxchip.mxapp.room.ui

import android.content.Intent
import android.text.InputFilter
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.room.adapter.RoomRecommendAdapter
import com.mxchip.mxapp.room.bean.RoomCreateBean
import com.mxchip.mxapp.room.bean.RoomRecommendBean
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.room.databinding.RoomAcRoomCreateBinding
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/22
 * @desc:
 */
@Router(RouterConstants.ROOM_CREATE_ACTIVITY)
class RoomCreateActivity : MXBusinessActivity<RoomAcRoomCreateBinding>() {

    override fun getLayoutBinding() = RoomAcRoomCreateBinding.inflate(layoutInflater)

    private val viewModel by viewModels<RoomViewModel>()

    //创建房间时，所属的家庭id
    private var homeId = 0

    private val adapter by lazy {
        RoomRecommendAdapter()
    }

    override fun onInit() {
        binding.roomList.setMaxLines(4)
        initEvent()
        initData()
        inject()
    }


    private fun initEvent() {
        binding.toolbar.left { finish() }
            .right {
                createRoom()
            }

        adapter.setItemClickListener { bean, i ->
            binding.etRoomName.setText(bean.name)
        }
    }

    private fun initData() {
        homeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, 0)
        getRecommendRoom()
    }

    private fun inject() {
        val inputFilter = InputFilter { source, _, _, _, _, _ ->
            if (RegexUtil.containsEmoji(source)) "" else null
        }
        binding.etRoomName.filters = arrayOf(inputFilter)
    }

    private fun getRecommendRoom() {
        viewModel.roomRecommend().launchAndCollectIn(this, Lifecycle.State.CREATED) {
            it.data?.apply {
                val roomRecommends =
                    JsonUtil.fromJsonToArray(this, RoomRecommendBean::class.java)
                adapter.notifyList(roomRecommends)
                binding.roomList.setAdapter(adapter)
            }
        }
    }

    private fun createRoom() {
        val roomName = binding.etRoomName.text.toString().trim()
        if (roomName.isEmpty()) {
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
            return
        } else if (roomName.length > MXDialog.maxLength) {
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
            return
        }
        val realHome = if (homeId > 0) homeId else AppConfigManager.getUseHomeId()
        showLoading()
        viewModel.createRoom(realHome, roomName).launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlow<RoomCreateBean>(
                success = {
                    dismissLoading()
                    LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                        .postValue(HomeDataRefresh.ROOM_LIST)
                    val resultIntent = Intent()
                    it?.apply {
                        resultIntent.putExtra(RoomConstants.KEY_ROOM_ID, this.room_id)
                        val roomName = binding.etRoomName.text.toString().trim()
                        resultIntent.putExtra(RoomConstants.KEY_ROOM_NAME, roomName)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                }

            )
        )
    }
}