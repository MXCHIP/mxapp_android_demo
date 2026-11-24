package com.mxchip.mxapp.pair.ui

import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVLinearItemDecorationSpace
import com.mxchip.mxapp.pair.adapter.PairSuccessAdapter
import com.mxchip.mxapp.pair.adapter.PairSuccessRoomAdapter
import com.mxchip.mxapp.pair.bean.PairSuccessDevice
import com.mxchip.mxapp.pair.databinding.PairAcPairSuccessBinding
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.mxapp.pair.viewmodel.PairSuccessViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.ByteUtil
import com.mxchip.utils.LiveDataBus

/**
 *配网成功，设置设备信息
 */
@Router(RouterConstants.PAIR_SUCCESS_ACTIVITY)
class PairSuccessActivity : MXBusinessActivity<PairAcPairSuccessBinding>() {

    private val vm by viewModels<PairSuccessViewModel>()

    private val allDeviceRoomAdapter by lazy { PairSuccessRoomAdapter() }
    private val adapter by lazy { PairSuccessAdapter() }

    private var allDeviceRoomId: Int? = null
    private var isExistWritePresetFailed = false //是否存在预置场景写入失败

    override fun getLayoutBinding(): PairAcPairSuccessBinding {
        return PairAcPairSuccessBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        allDeviceRoomId = PairManager.getFromRoomId()
        //配网成功页面，允许连接proxy
        MeshSDKManage.isConnectProxy = true
        initView()
        inject()
        showLoading()
        queryRooms()
    }

    private fun initView() {
        binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_add_success))
        binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_done))
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.addItemDecoration(RVLinearItemDecorationSpace(10.dp2px()))
        binding.rvDevice.adapter = adapter

        binding.roomList.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.roomList.adapter = allDeviceRoomAdapter
        binding.roomList.background =
            generateShape(
                getColor(com.mxchip.mxapp.base.R.color.core_color_background_tab),
                ShapeRadius(0, 0, 16, 16)
            )
    }

    private fun inject() {
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        binding.toolbar.left {
            backPressed()
        }.right {
            save()
        }

        adapter.setIconClick {
            MeshSDKManage.setUnAckMsg(it, listOf(Pair("0100", "02")))
        }
        adapter.setNicknameClickListener {
            updateName(it)
        }
        adapter.setAddRoomClick {
            createRoom()
        }

        allDeviceRoomAdapter.setItemClickListener { roomId ->
            if (roomId == -1) {
                createRoom()
            } else {
                if (allDeviceRoomId != roomId) {
                    allDeviceRoomId = roomId
                    allDeviceRoomAdapter.setSelectedRoomId(roomId)
                    //更新设备选中房间UI
                    adapter.getData().forEach {
                        it.room_id = allDeviceRoomId
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun allDeviceRoom(roomList: List<RoomBean>, selectedRoomId: Int?) {
        allDeviceRoomAdapter.setRoomList(roomList)
        allDeviceRoomAdapter.setSelectedRoomId(selectedRoomId)
    }

    private fun createRoom() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_add_room))
            .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_save)) { dialog, text ->
                if (text.isNullOrBlank()) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                    return@rightButton
                } else if (text.length > MXDialog.maxLength) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
                    return@rightButton
                }
                showLoading()
                vm.createRoom(AppConfigManager.getUseHomeId(), text.trim())
                    .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                        queryRooms()
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }))
                dialog.cancel()
            }.create().show()
    }


    private fun updateName(pos: Int) {
        val oldNikeName = adapter.getData()[pos].nick_name
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_device_name))
            .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name)).edittextText(oldNikeName)
            
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, input ->
                if (input.isNullOrBlank()) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                    return@rightButton
                } else if (input.length > MXDialog.maxLength) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
                    return@rightButton
                }
                dialog.dismiss()
                adapter.getData()[pos].nick_name = input.trim()
                adapter.notifyItemChanged(pos)
            }.create().show()
    }

    private fun save() {
        showLoading()
        writePresetGroup()
    }

    private fun executeSave() {
        showLoading()
        vm.modifyDeviceInfo(AppConfigManager.getUseHomeId(), adapter.getData())
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                dismissLoading()
                if (isExistWritePresetFailed) showWritePresetFailedDialog()
                else successFinish()
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            }))
    }

    private fun showWritePresetFailedDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_device_set_room_fail)).leftButton(null)
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_i_know)) { alertDialog, _ ->
                alertDialog.dismiss()
                successFinish()
            }.create().show()
    }

    private fun writePresetGroup(index: Int = 0) {
        if (adapter.itemCount == index) {
            executeSave()
            return
        }
        val pairSuccessDevice = adapter.getData()[index]
        when {
            pairSuccessDevice.uuid.isNullOrEmpty() -> nextWritePresetGroup(index)
            pairSuccessDevice.isPresetScene -> writeMeshPreset(pairSuccessDevice, index)
            else -> nextWritePresetGroup(index)
        }
    }

    private fun writeMeshPreset(pairSuccessDevice: PairSuccessDevice, index: Int) {
        //获取设备所在房间的群组地址
        val roomBean =
            allDeviceRoomAdapter.getItems().firstOrNull { it.room_id == pairSuccessDevice.room_id }
        if (roomBean == null || roomBean.is_default) {
            nextWritePresetGroup(index)
        } else {
            val groupAddress = roomBean.room_address
            val finalGroupAddress =
                MeshSDKManage.stringReversedToLittleEndian(String.format("%04X", groupAddress))
            MeshSDKManage.setMsg(
                MeshOpcodeConstants.VENDOR_MSG_OPCODE_SYNC,
                pairSuccessDevice.uuid!!,
                "80$finalGroupAddress",
                object : MapCallback() {
                    override fun onResult(resultMap: Map<String, Any>) {
                        val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                        //attrType = 80,1字节
                        val resultArray = ByteArray(dataArray.size - 1)
                        System.arraycopy(dataArray, 1, resultArray, 0, resultArray.size)
                        val rest = ByteUtil.bytesToInt(resultArray)
                        if (rest != 0) {
                            isExistWritePresetFailed = true
                            pairSuccessDevice.room_id = null
                        }
                        nextWritePresetGroup(index)
                    }

                    override fun onError(callbackMsg: CallbackMsg) {
                        super.onError(callbackMsg)
                        isExistWritePresetFailed = true
                        pairSuccessDevice.room_id = null
                        nextWritePresetGroup(index)
                    }
                },
                isTimeOut = true,
                isRetry = true,
            )
        }
    }

    private fun nextWritePresetGroup(curtIndex: Int) {
        val next = curtIndex + 1
        writePresetGroup(next)
    }

    private fun queryRooms() {
        vm.roomListV4(AppConfigManager.getUseHomeId(), 1, 199).launchAndCollectIn(
            this,
            Lifecycle.State.CREATED,
            loadFlowList<RoomBean>(success = {
                dismissLoading()
                it?.apply {
                    //未通过房间进入配网，查找默认房间
                    if (allDeviceRoomId == null) allDeviceRoomId =
                        this.firstOrNull { roomBean -> roomBean.is_default }?.room_id
                    val list = mutableListOf<RoomBean>()
                    list.addAll(this)
                    //追加add icon
                    list.add(RoomBean(-1, "+"))
                    allDeviceRoom(list, allDeviceRoomId)
                    adapter.setRoomList(list)
                    PairDataCenter.pairSuccessfulList.forEach { successDevice ->
                        successDevice.room_id = allDeviceRoomId
                        val productKey = successDevice.productKey!!
                        val productInfo = ProductInfoManager.getProductInfo(productKey)
                        JsonUtil.fromJson(productInfo, ProductInfo::class.java)?.apply {
                            successDevice.icon = this.image
                            successDevice.productName = this.name
                            successDevice.isPresetScene = this.is_scene_preset
                        }
                    }
                    adapter.addAll(PairDataCenter.pairSuccessfulList)
                }
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            })
        )
    }

    private fun backPressed() {
        //返回配网页面，禁止连接proxy
        MeshSDKManage.isConnectProxy = false
        LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
            .postValue(HomeDataRefresh.DEVICE)
        finish()
    }

    private fun successFinish() {
        LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
            .postValue(HomeDataRefresh.DEVICE)
        RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
        finish()
    }

    override fun onDestroy() {
        PairDataCenter.pairSuccessfulList.clear()
        super.onDestroy()
    }
}