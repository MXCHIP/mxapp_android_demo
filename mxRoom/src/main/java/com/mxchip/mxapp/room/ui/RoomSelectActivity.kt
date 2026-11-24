package com.mxchip.mxapp.room.ui

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.room.adapter.RoomSelectAdapter
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.room.databinding.RoomAcRoomSelectBinding
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.router.annotation.Router

/**
 * 房间选择
 */
@Router(RouterConstants.ROOM_SELECT_ACTIVITY)
class RoomSelectActivity : MXBusinessActivity<RoomAcRoomSelectBinding>() {

    private var selectedRoom: RoomBean? = null
    private var oldRoomId = -1
    private val vm by viewModels<RoomViewModel>()
    private val adapter by lazy { RoomSelectAdapter(this) }

    override fun getLayoutBinding() = RoomAcRoomSelectBinding.inflate(layoutInflater)

    override fun onInit() {
        oldRoomId = intent.getIntExtra(RoomConstants.KEY_ROOM_ID, -1)
        initView()
        queryData()
    }

    private fun initView() {
        val rightText =
            intent.getStringExtra("rightText") ?: getString(com.mxchip.mxapp.base.R.string.mx_done)
        binding.toolbar.left {
            finish()
        }.right(rightText) {
            save()
        }
        binding.roomList.layoutManager = LinearLayoutManager(this)
        binding.roomList.addItemDecoration(RVItemDecorationSpace(bottom = 10.dp2px()))
        binding.roomList.adapter = adapter
        adapter.setItemClick { pos ->
            clickRoom(pos)
        }
        adapter.setSelect(oldRoomId)
    }

    private val launcherResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val roomId = result.data?.getIntExtra(RoomConstants.KEY_ROOM_ID, -1)
                val roomName = result.data?.getStringExtra(RoomConstants.KEY_ROOM_NAME)
                if (roomId != null && roomId > 0 && !roomName.isNullOrEmpty()) {
                    adapter.appendData(RoomBean(roomId, roomName))
                } else {
                    queryData()
                }
            }
        }

    private fun clickRoom(pos: Int) {
        val clickRoom = adapter.getRoom(pos)
        if (clickRoom.room_id == -1) {
            RouterManager.navigation(
                RouterConstants.ROOM_CREATE_ACTIVITY,
                mapOf(HomeConstants.KEY_HOME_ID to AppConfigManager.getUseHomeId()),
                this,
                launcherResult
            )
        } else {
            selectedRoom = clickRoom
            adapter.setSelect(clickRoom.room_id)
            adapter.notifyDataSetChanged()
        }
    }

    private fun queryData() {
        val homeId = AppConfigManager.getUseHomeId()
        val param = mapOf("home_id" to homeId, "page_no" to 1, "page_size" to 199)
        vm.roomListV4(param)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlowList<RoomBean>(
                    success = {
                        val finalList = mutableListOf<RoomBean>()
                        it?.apply {
                            finalList.addAll(this)
                        }
                        //添加 添加房间
                        finalList.add(RoomBean(-1, ""))
                        adapter.setData(finalList)
                    }, error = { code, msg, _ ->
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }
                ))
    }

    private fun save() {
        if (selectedRoom == null) {
            finish()
        } else {
            if (selectedRoom!!.room_id == oldRoomId) finish()
            else {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(RoomConstants.KEY_ROOM_NAME, selectedRoom!!.name)
                    putExtra(RoomConstants.KEY_ROOM_ID, selectedRoom!!.room_id)
                    putExtra("room_address", selectedRoom!!.room_address)
                })
                finish()
            }
        }
    }
}