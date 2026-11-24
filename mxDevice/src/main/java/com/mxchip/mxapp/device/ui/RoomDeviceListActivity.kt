package com.mxchip.mxapp.device.ui

import android.content.Intent
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.dialog.BottomActionDialog
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.base.widget.SimpleItemTouchHelperCallback
import com.mxchip.mxapp.device.adapter.RoomEditDevicesAdapter
import com.mxchip.mxapp.device.adapter.RoomEditDevicesOutsideAdapter
import com.mxchip.mxapp.device.bean.RoomEditWritePresetBean
import com.mxchip.mxapp.device.databinding.DeviceAcRoomSettingDeviceBinding
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.ByteUtil
import com.mxchip.utils.LiveDataBus

/**
 * 房间内，设备 管理
 */
@Router(RouterConstants.ROOM_SETTING_DEVICE_ACTIVITY)
class RoomDeviceListActivity : MXBusinessActivity<DeviceAcRoomSettingDeviceBinding>() {

    private val viewModel by viewModels<DeviceViewModel>()

    private var editHomeId = -1
    private var editRoomId = -1

    private var isExistWritePresetFailed = false //是否存在预置场景写入失败
    private var isDefaultRoom = false
    private var roomGroupAddress = -1

    private val oldRoomDevices by lazy { mutableListOf<DeviceBean>() }
    private val roomWritePresetDevices by lazy { mutableListOf<RoomEditWritePresetBean>() }
    private val roomNoWritePresetDevices by lazy { mutableListOf<DeviceBean>() }

    override fun getLayoutBinding(): DeviceAcRoomSettingDeviceBinding {
        return DeviceAcRoomSettingDeviceBinding.inflate(layoutInflater)
    }

    private val roomEditDevicesAdapter by lazy { RoomEditDevicesAdapter() }

    private val roomEditDevicesOutsideAdapter by lazy { RoomEditDevicesOutsideAdapter() }

    private var edit = false

    private val bottomActionDialog by lazy {
        BottomActionDialog(this).title(getString(com.mxchip.mxapp.base.R.string.mx_exit_not_saved))
            .addItem(getString(com.mxchip.mxapp.base.R.string.mx_saved_exit)) { _, _ ->
                save()
            }.addItem(getString(com.mxchip.mxapp.base.R.string.mx_exit)) { _, _ ->
                finish()
            }.create()
    }

    override fun onInit() {
        editHomeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, -1)
        if (editHomeId == -1) {
            finish()
            return
        }
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        val roomName = intent.getStringExtra(DeviceConstants.KEY_ROOM_NAME) ?: ""
        binding.toolbar.title(roomName).left { backPressed() }
            .right(getString(com.mxchip.mxapp.base.R.string.mx_save)) {
                save()
            }
        editRoomId = intent.getIntExtra(DeviceConstants.KEY_ROOM_ID, -1)
        isDefaultRoom = intent.getBooleanExtra("is_default_room", false)
        ItemTouchHelper(SimpleItemTouchHelperCallback(roomEditDevicesAdapter)).attachToRecyclerView(
            binding.roomDeviceList
        )
        binding.roomDeviceList.addItemDecoration(
            RVItemDecorationSpace(
                top = 10.dp2px(),
                left = 10.dp2px(),
                right = 10.dp2px()
            )
        )
        binding.roomDeviceList.layoutManager = LinearLayoutManager(this)
        binding.roomDeviceList.adapter = roomEditDevicesAdapter

        binding.deviceOutsideList.addItemDecoration(
            RVItemDecorationSpace(
                top = 10.dp2px(),
                left = 10.dp2px(),
                right = 10.dp2px()
            )
        )
        binding.deviceOutsideList.layoutManager = LinearLayoutManager(this)
        binding.deviceOutsideList.adapter = roomEditDevicesOutsideAdapter

        //已在当前房间
        roomEditDevicesAdapter.setRemoveListener {
            edit = true
            roomEditDevicesOutsideAdapter.deleteFromRoom(it)
        }
        roomEditDevicesAdapter.setItemStateChangedListener {
            edit = true
        }

        //不在当前房间，添加操作
        roomEditDevicesOutsideAdapter.setAddListener {
            edit = true
            addOutsideDevice(it)
        }
        showLoading()
        findDevices()
    }

    private fun findDevices(pageNo: Int = 1) {
        //查找家庭下所有设备，pageSize =199
        viewModel.findDevices(
            mapOf(
                "home_id" to editHomeId,
                "page_size" to 199,
                "page_no" to pageNo,
            )
        ).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlowList<DeviceBean>(
                success = {
                    val roomList = mutableListOf<DeviceBean>()
                    val outList = mutableListOf<DeviceBean>()
                    it?.forEach { deviceBean ->
                        if (deviceBean.room_id == editRoomId) roomList.add(deviceBean)
                        else outList.add(deviceBean)
                    }

                    oldRoomDevices.addAll(roomList)
                    if (pageNo == 1) {
                        roomEditDevicesAdapter.notifyList(isDefaultRoom, roomList)
                        roomEditDevicesOutsideAdapter.notifyList(outList)
                    } else {
                        roomEditDevicesAdapter.appendNotifyList(roomList)
                        roomEditDevicesOutsideAdapter.appendNotifyList(outList)
                    }
                    dismissLoading()
                    if ((it?.size ?: 0) >= 199) {
                        val newPageNo = pageNo + 1
                        findDevices(newPageNo)
                    } else {
                        if (roomEditDevicesAdapter.itemCount == 0 && roomEditDevicesOutsideAdapter.itemCount == 0) {
                            binding.emptyView.visibility = View.VISIBLE
                            binding.tvTitleAdded.visibility = View.GONE
                            binding.tvTitleOut.visibility = View.GONE
                            binding.emptyView.content(getString(com.mxchip.mxapp.base.R.string.mx_no_device))
                        } else if (roomEditDevicesAdapter.itemCount == 0) {
                            binding.tvTitleAdded.visibility = View.GONE
                        } else if (roomEditDevicesOutsideAdapter.itemCount == 0) {
                            binding.tvTitleOut.visibility = View.GONE
                        }
                    }
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val netErr = onException(code)
                    if (!netErr) {
                        showToast("$msg")
                    }
                })
        )
    }

    private fun addOutsideDevice(bean: DeviceBean) {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_room_move_to_room))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_room_move_device_des),
                    bean.room_name
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                dialog.cancel()
                bean.show = true//新加入的数据，默认显示
                roomEditDevicesOutsideAdapter.addToRoom(bean)
                roomEditDevicesAdapter.addDevice(bean)
            }.create().show()
    }

    private fun save() {
        showLoading()
        //设备需要对预置场景进行检查写入
        roomEditDevicesAdapter.getDeviceList()?.forEach { new ->
            val old = oldRoomDevices.firstOrNull { it.device_id == new.device_id }
            if (old == null) {
                roomWritePresetDevices.add(
                    RoomEditWritePresetBean(
                        new.product_key,
                        new.device_id,
                        new.iotid,
                        new.uuid,
                        isAdd = true,
                        new.show,
                    )
                )
            } else roomNoWritePresetDevices.add(old)
        }
        //默认房间无法进行移除操作，不需要检查
        if (!isDefaultRoom) {
            //检查从当前房间移除的设备
            oldRoomDevices.forEach { old ->
                val new = roomEditDevicesAdapter.getDeviceList()
                    ?.firstOrNull { it.device_id == old.device_id }
                if (new == null) {
                    roomWritePresetDevices.add(
                        RoomEditWritePresetBean(
                            old.product_key,
                            old.device_id,
                            old.iotid,
                            old.uuid,
                            isAdd = false,
                            old.show
                        )
                    )
                }
            }
        }
        if (roomWritePresetDevices.isEmpty()) {
            //无预置写入，直接使用房间列表数据保存
            val deviceIds = mutableListOf<Map<String, Any>>()
            roomEditDevicesAdapter.getDeviceList()?.forEach {
                deviceIds.add(mapOf("iotid" to it.iotid, "show" to it.show))
            }
            saveDevice(deviceIds)
        } else {
            isExistWritePresetFailed = false//重置
            writePresetGroup()
        }
    }

    private fun saveDevice(deviceIds: List<Map<String, Any>>) {
        showLoading()
        viewModel.updateRoomDevice(editRoomId, deviceIds)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                dismissLoading()
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.ROOM_LIST)
                if (isExistWritePresetFailed) {
                    showWritePresetFailedDialog(deviceIds.size)
                } else {
                    finishResult(deviceIds.size)
                }
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            }))
    }

    private fun checkWritePresetGroup(
        writePresetBean: RoomEditWritePresetBean, uuid: String, index: Int
    ) {
        showLoading()
        val productKey = writePresetBean.productKey
        val productInfo = JsonUtil.fromJson(
            ProductInfoManager.getProductInfo(productKey),
            ProductInfo::class.java
        )
        if (productInfo?.is_scene_preset == true) writeMeshPreset(writePresetBean, uuid, index)
        else nextWritePresetGroup(index)
    }

    private fun writePresetGroup(index: Int = 0) {
        if (roomWritePresetDevices.size == index) {
            val deviceIds = mutableListOf<Map<String, Any>>()
            roomWritePresetDevices.forEach {
                if (it.writeResult == false) isExistWritePresetFailed = true
                if (it.isAdd) { //新增到房间，预置场景 未失败
                    if (it.writeResult != false) deviceIds.add(
                        mapOf("iotid" to it.iotId, "show" to it.show)
                    )
                } else {//移出房间，预置 失败
                    if (it.writeResult == false) deviceIds.add(
                        mapOf("iotid" to it.iotId, "show" to it.show)
                    )
                }
            }
            //未变动的旧设备
            roomNoWritePresetDevices.forEach {
                deviceIds.add(mapOf("iotid" to it.iotid, "show" to it.show))
            }
            saveDevice(deviceIds)
            return
        }
        val writePresetBean = roomWritePresetDevices[index]
        val uuid = writePresetBean.uuid
        if (uuid.isNullOrEmpty()) {
            nextWritePresetGroup(index)
        } else {
            checkWritePresetGroup(writePresetBean, uuid, index)
        }
    }

    private fun writeMeshPreset(
        writePresetBean: RoomEditWritePresetBean, uuid: String, index: Int
    ) {
        //默认房间 预设地址：0xE000
        val groupAddress = if (writePresetBean.isAdd) roomGroupAddress else 0xE000
        if (groupAddress <= 0) {
            dismissLoading()
            return
        }
        val finalGroupAddress =
            MeshSDKManage.stringReversedToLittleEndian(String.format("%04X", groupAddress))
        MeshSDKManage.setMsg(
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SYNC,
            uuid,
            "80$finalGroupAddress",
            object : MapCallback() {
                override fun onResult(resultMap: Map<String, Any>) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    //attrType = 80,1字节
                    val resultArray = ByteArray(dataArray.size - 1)
                    System.arraycopy(dataArray, 1, resultArray, 0, resultArray.size)
                    val rest = ByteUtil.bytesToInt(resultArray)
                    writePresetBean.writeResult = rest == 0
                    nextWritePresetGroup(index)
                }

                override fun onError(callbackMsg: CallbackMsg) {
                    super.onError(callbackMsg)
                    writePresetBean.writeResult = false
                    nextWritePresetGroup(index)
                }
            },
            true,
            isRetry = true
        )
    }

    private fun nextWritePresetGroup(curtIndex: Int) {
        val next = curtIndex + 1
        writePresetGroup(next)
    }

    private fun showWritePresetFailedDialog(roomDeviceCount: Int) {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_device_set_room_fail))
            .leftButton(null)
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_i_know)) { alertDialog, _ ->
                alertDialog.dismiss()
                finishResult(roomDeviceCount)
            }.create().show()
    }

    private fun finishResult(count: Int) {
        setResult(RESULT_OK, Intent().apply {
            putExtra("type", "device")
            putExtra("count", count)
        })
        finish()
    }

    private fun backPressed() {
        if (edit && !bottomActionDialog.isShowing) bottomActionDialog.show()
        else finish()
    }
}