package com.mxchip.mxapp.room.ui

import android.content.Intent
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.mxapp.base.utils.vibrateShot
import com.mxchip.mxapp.room.adapter.RoomManageAdapter
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.room.consts.RoomConstants.KEY_ORDER_ROOM
import com.mxchip.mxapp.room.databinding.RoomAcRoomManageBinding
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import java.util.Collections

/**
 * 房间管理-房间列表
 */
@Router(RouterConstants.ROOM_MANAGE_ACTIVITY)
class RoomManageActivity : MXBusinessActivity<RoomAcRoomManageBinding>() {

    private val viewModel by viewModels<RoomViewModel>()
    private var isEdit = false //列表是否处于编辑状态
    private var refresh: HomeDataRefresh? = null//是否需要通知首页房间数据发现变化
    private var mRoomList: List<RoomBean>? = null
    private var editHomeId = 0
    private var isMoveChanged = false //是否有房间移动

    private val adapter by lazy {
        RoomManageAdapter(this)
    }

    override fun getLayoutBinding() = RoomAcRoomManageBinding.inflate(layoutInflater)

    override fun onInit() {
        editHomeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, 0)
        initView()
        initEvent()
        inject()
    }

    override fun onStart() {
        super.onStart()
        showLoading()
        roomList()
    }

    private fun initView() {
        binding.roomList.layoutManager = LinearLayoutManager(this)
        binding.roomList.adapter = adapter
        getItemTouchHelper().attachToRecyclerView(binding.roomList)
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.ivCreateRoom.setColorFilter(mainColor)
        binding.tvCreateRoom.setTextColor(mainColor)
    }

    private fun roomList() {
        val param = mapOf(
            "home_id" to editHomeId, "page_no" to 1, "page_size" to 199
        )
        viewModel.roomListV4(param)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED,
                loadFlowList<RoomBean>(
                    success = {
                        dismissLoading()
                        it?.apply {
                            mRoomList = this
                            adapter.notifyList(this)
                        }
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        if (mRoomList == null) {//只有在首次进入页面时才进行API异常提醒
                            val netErr = onException(code)
                            if (!netErr) showToast("$msg")
                        }
                    })
            )
    }

    private fun inject() {
        viewModel.deleteRoomState.observe(this, loadObserver<Any>(success = {
            roomList()
        }, error = { code, msg, _ ->
            dismissLoading()
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))
    }

    private val launcherResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            //onStart中已处理
//            if (result.resultCode == RESULT_OK) {
//                roomList()
//            }
        }

    private fun initEvent() {
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        binding.toolbar.left { backPressed() }
            .right(getString(com.mxchip.mxapp.base.R.string.mx_edit)) {
                isEdit = !isEdit
                if (isEdit) {
                    binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_done))
                } else {
                    binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_edit))
                    sortList()//保存排序
                }
                adapter.notifyEdit(isEdit)
            }

        binding.createRoom.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.ROOM_CREATE_ACTIVITY,
                mapOf(HomeConstants.KEY_HOME_ID to editHomeId),
                this,
                launcherResult
            )
        }

        adapter.setRemoveListener {
            MXDialog.Builder(this)
                .title(getString(com.mxchip.mxapp.base.R.string.mx_del_room_title))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_del_room_des))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.cancel()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                    dialog.cancel()
                    removeRoom(it)
                }.create().show()
        }.setItemClickListener {
            toRoomSetting(it)
        }

    }

    private fun getItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.adapterPosition == mRoomList?.size) {
                    return makeMovementFlags(0, 0)
                }
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = 0
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                //得到当拖拽的viewHolder的Position
                val fromPosition = viewHolder.adapterPosition
                //拿到当前拖拽到的item的viewHolder
                var toPosition: Int = target.adapterPosition
                if (fromPosition < toPosition) {
                    if (toPosition >= mRoomList!!.size) {
                        toPosition = mRoomList!!.size - 1
                    }
                    for (i in fromPosition until toPosition) {
                        if (i + 1 < mRoomList!!.size) {
                            Collections.swap(mRoomList!!, i, i + 1)
                        }
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(mRoomList!!, i, i - 1)
                    }
                }
                adapter.notifyItemMoved(fromPosition, toPosition)
                isMoveChanged = true
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }


            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    //震动
                    applicationContext.vibrateShot(70)
                } else {
                    adapter.notifyList(mRoomList)
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return if (mRoomList!!.size < 2) {
                    false
                } else isEdit
            }

        })
    }

    /**
     * 房间列表排序
     */
    private fun sortList() {
        if (!isMoveChanged) return
        val ret = arrayListOf<Int>()
        mRoomList?.forEach {
            ret.add(it.room_id)
        }
        showLoading()
        viewModel.orderRoom(editHomeId, ret)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                dismissLoading()
                isMoveChanged = false
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_update_success))
                LiveDataBus.with<Int>(KEY_ORDER_ROOM).postValue(0)
            }, error = { code, msg, data ->
                dismissLoading()
                isMoveChanged = false
                roomList()
            }))
    }

    private fun removeRoom(room: RoomBean) {
        refresh = HomeDataRefresh.ROOM_LIST
        showLoading()
        viewModel.deleteRoom(room.room_id)
    }

    private fun toRoomSetting(room: RoomBean) {
        RouterManager.navigation(
            RouterConstants.ROOM_SETTING_ACTIVITY, mapOf(
                HomeConstants.KEY_HOME_ID to editHomeId,
                RoomConstants.KEY_ROOM_ID to room.room_id,
                RoomConstants.KEY_ROOM_NAME to room.name,
                RoomConstants.KEY_IS_DEFAULT_ROOM to room.is_default,
                RoomConstants.KEY_ROOM_SCENE_COUNT to room.scene_count,
                RoomConstants.KEY_ROOM_DEVICE_COUNT to room.device_count,
                RoomConstants.KEY_ROOM_GROUP_COUNT to room.group_count,
                RoomConstants.KEY_ROOM_AUTO_COUNT to room.auto_count,
            )
        )
    }

    private fun backPressed() {
        if (isEdit) {
            return
        }
        finish()
    }

    override fun onDestroy() {
        setResult(RESULT_OK, Intent().apply {
            putExtra("roomCount", mRoomList?.size)
        })
        refresh?.apply {
            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH).postValue(this)
        }
        super.onDestroy()
    }
}