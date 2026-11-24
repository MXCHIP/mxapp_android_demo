package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mesh.rule.IRuleWrite
import com.mxchip.mesh.rule.WriteCEVT
import com.mxchip.mesh.rule.WriteExtend
import com.mxchip.mesh.rule.WriteGroup
import com.mxchip.mesh.rule.WritePresetGroup
import com.mxchip.mesh.rule.WriteRule
import com.mxchip.mesh.rule.WriteVirtualController
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceRuleBean
import com.mxchip.mxapp.base.bean.DeviceWriteStateBean
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.adapter.WriteRuleProcessDialogAdapter
import com.mxchip.mxapp.base.device.ui.WriteRuleProcessDialog
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.MeshCommand
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.DeviceWriteConfigManage
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.device.adapter.DeviceRecoveryAdapter
import com.mxchip.mxapp.device.bean.HomeRecoveryStateBean
import com.mxchip.mxapp.device.databinding.DeviceAcDeviceRecoveryListBinding
import com.mxchip.mxapp.device.ui.dialog.ChooseSyncRoomDialog
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 *一键恢复，设备列表
 */
@Router(RouterConstants.DEVICE_RECOVERY_LIST_ACTIVITY)
class DeviceRecoveryListActivity : MXBusinessActivity<DeviceAcDeviceRecoveryListBinding>() {

    companion object {
        private const val TAG = "DeviceRecoveryListActivity"
    }

    private val vm by viewModels<DeviceViewModel>()

    //    private val recoveryRoomList by lazy { mutableListOf<RoomBean>() }
    private val recoveryRoomMap by lazy { HashMap<Int, RoomBean>() }
    private val chooseRoomDialog by lazy {
        ChooseSyncRoomDialog(created = {
            setChooseRoomData()
        }, sync = {
            writeRuleDialog.show(supportFragmentManager, "ruleDialog")
        })
    }

    private val writeRuleDialog by lazy {
        WriteRuleProcessDialog(dialogCreated = {
            setDataWriting()
        }, okClick = {
            checkRecoveryCompleted()
        }, retryClick = {
            retryDataWrite()
        })
    }

    private val adapter by lazy { DeviceRecoveryAdapter() }

    private val meshCommand by lazy { MeshCommand() }

    override fun getLayoutBinding(): DeviceAcDeviceRecoveryListBinding {
        return DeviceAcDeviceRecoveryListBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initView()
        inject()
        checkHomeRecoveryStatus()
    }

    private fun initView() {
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.adapter = adapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvRestart.setBtnBackgroundColor(mainColor)
        binding.tvContinue.setBtnBackgroundColor(mainColor)
        binding.emptyView.setIconColor(mainColor)
    }

    private fun inject() {
        binding.toolbar.left {
            finish()
        }
        binding.tvRestart.setOnClickListener {
            if (MXMesh.isConnected()) {
                isRestart = true
                showRecoveryTips()
            } else {
                ToastUtil.showLongToast(
                    this, getString(com.mxchip.mxapp.base.R.string.mx_sync_unconnect_title)
                )
            }
        }
        binding.tvContinue.setOnClickListener {
            if (MXMesh.isConnected()) {
                isRestart = false
                showRecoveryTips()
            } else {
                ToastUtil.showLongToast(
                    this, getString(com.mxchip.mxapp.base.R.string.mx_sync_unconnect_title)
                )
            }
        }
    }

    private fun checkHomeRecoveryStatus() {
        showLoading()
        vm.getRecoveryStatus(mapOf("home_id" to AppConfigManager.getUseHomeId()))
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<HomeRecoveryStateBean>(success = {
                    dismissLoading()
                    //一键恢复状态：0 - 没有备份数据；1 - 未开始；2 - 进行中
                    if (it == null || it.recovery_status == 0) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        queryRecoveryDevice(it.recovery_status, 1)
                    }
                }, error = { code, msg, _ ->
                    dismissLoading()
                    binding.emptyView.visibility = View.VISIBLE
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                })
            )
    }

//    private fun queryData(apiRecoveryStatus: Int) {
//        vm.roomListV4(AppConfigManager.getUseHomeId(), 1, 199).launchAndCollectIn(
//            this, Lifecycle.State.CREATED, loadFlowList<RoomBean>(success = {
//                it?.apply {
//                    recoveryRoomList.addAll(this)
//                    queryRecoveryDevice(apiRecoveryStatus, 1)
//                }
//            }, error = { _, msg, _ ->
//                binding.emptyView.visibility = View.VISIBLE
//                showToast("$msg")
//            }, loading = { loading(it) })
//        )
//    }

    private fun queryRecoveryDevice(apiRecoveryStatus: Int, pageNo: Int) {
        lifecycleScope.launch {
            vm.findRecoveryDeviceList(
                mapOf(
                    "home_id" to AppConfigManager.getUseHomeId(),
                    "page_size" to 199,
                    "page_no" to pageNo,
                )
            ).collect {
                val data = JsonUtil.fromJsonMXList(it.data, DeviceWriteStateBean::class.java)
                val newDeviceList = mutableListOf<DeviceWriteStateBean>()
                data?.forEach { writeStateDevice ->
                    if (writeStateDevice.isGateWay()) {
                        return@forEach
                    }
                    newDeviceList.add(writeStateDevice)
                    writeStateDevice.room_id?.apply {
                        if (recoveryRoomMap.containsKey(this)) {
                            recoveryRoomMap[this]!!.device_count += 1
                        } else {
                            recoveryRoomMap[this] =
                                RoomBean(
                                    this,
                                    writeStateDevice.room_name ?: "null",
                                    device_count = 1
                                )
                        }
                    }
                }
                if (pageNo == 1) adapter.setData(newDeviceList)
                else adapter.appendData(newDeviceList)
                if ((data?.size ?: 0) < 199) {
                    dismissLoading()
                    if (adapter.getDatas().isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        adapter.notifyDataSetChanged()
                        binding.emptyView.visibility = View.GONE
                        binding.tvDesc.visibility = View.VISIBLE
                        binding.rvDevice.visibility = View.VISIBLE
                        binding.tvRestart.visibility = View.VISIBLE
                        //一键恢复状态：1 - 未开始；2 - 进行中
                        if (apiRecoveryStatus == 1) {
                            binding.tvRestart.setText(com.mxchip.mxapp.base.R.string.mx_recovery)
                        } else {//已经进行中了
                            //设备 一键恢复写入状态： 0 - 未写入；1 - 写入
                            val existNot = adapter.getDatas()
                                .firstOrNull { recoverDevice -> recoverDevice.recovery_status == 0 } != null
                            if (existNot) {
                                // 显示继续恢复按钮
                                binding.tvContinue.visibility = View.VISIBLE
                                binding.tvRestart.setText(com.mxchip.mxapp.base.R.string.mx_recover_restart)
                            } else {
                                binding.tvRestart.setText(com.mxchip.mxapp.base.R.string.mx_recovery)
                                recoveryCompleted()
                            }
                        }
                    }
                } else {
                    val newPageNo = pageNo + 1
                    queryRecoveryDevice(apiRecoveryStatus, newPageNo)
                }
            }
        }
    }

    private fun showRecoveryTips() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_recovery_tips_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                alertDialog.dismiss()
                startRecovery()
            }.create().show()
    }

    private fun setChooseRoomData() {
        //计算各房间未同步数据
        val unSyncList = mutableListOf<Int>()
        adapter.getDatas().forEach {
            if (it.room_id != 0 && it.room_id!! > 0) {
                recoveryRoomMap[it.room_id]?.apply { this.unWrittenDeviceCount = 0 }
                if (isRestart || (it.recovery_status == 0 && it.writeState != WriteRuleProcessDialogAdapter.STATE_SUCCESS)) {
                    if (unSyncList.contains(it.room_id)) {
                        recoveryRoomMap[it.room_id]?.apply { this.unWrittenDeviceCount += 1 }
                    } else {
                        recoveryRoomMap[it.room_id]?.apply { this.unWrittenDeviceCount = 1 }
                        unSyncList.add(it.room_id!!)
                    }
                }
            }
        }
        chooseRoomDialog.setRoomData(recoveryRoomMap.values.toList())
    }

    private val needWriteList by lazy { ArrayList<DeviceWriteStateBean>() }
    private val writeRuleDeviceMap by lazy { HashMap<String, DeviceRuleBean>() }
    private var recoveryClick = false
    private var isRestart = true

    private fun startRecovery() {
        if (recoveryClick) return
        recoveryClick = true
        if (isRestart) {
            //执行开始恢复
            showLoading()
            vm.startHomeRecovery(AppConfigManager.getUseHomeId()).launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                    dismissLoading()
                    LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                        .postValue(HomeDataRefresh.HOME)
                    //设置为进行中了，显示继续恢复按钮
                    binding.tvContinue.visibility = View.VISIBLE
                    binding.tvRestart.setText(com.mxchip.mxapp.base.R.string.mx_recover_restart)
                    showRoomDialog()
                    recoveryClick = false
                }, error = { code, msg, _ ->
                    dismissLoading()
                    recoveryClick = false
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                })
            )
        } else {
            recoveryClick = false
            showRoomDialog()
        }
    }

    private fun showRoomDialog() {
        chooseRoomDialog.show(supportFragmentManager, "roomDialog")
    }

    private fun retryDataWrite() {
        writeRuleDialog.reStartState()
        setDataWriting()
    }

    private fun setDataWriting() {
        needWriteList.clear()
        adapter.getDatas().forEach {
            if (isRestart) {
                //执行了新的开始，非继续恢复，重置非选中房间的 写入过滤状态
                it.writeState = -1 //未执行
            }
            //在所选房间设备
            if (chooseRoomDialog.getSelectedRoomIds().contains(it.room_id)) {
                //检查一键恢复设备 ： 0 - 未写入 ui执行状态 非成功w
                if (it.recovery_status == 0 || it.writeState != WriteRuleProcessDialogAdapter.STATE_SUCCESS) {
                    it.writeState = -1 //未执行
                    needWriteList.add(it)
                }
            }
        }
        writeRuleDialog.setDatas(needWriteList)
        requestRecoveryDevice()
    }

    private fun notifyCloudSuccessful(deviceIndex: Int) {
        val iotId = needWriteList[deviceIndex].iotid
        CoroutineScope(Dispatchers.IO).launch {
            vm.updateRecoveryDeviceWriteState(
                mapOf(
                    "home_id" to AppConfigManager.getUseHomeId(), "iotid" to iotId
                )
            ).collect {}
        }
    }

    private fun checkRecoveryCompleted() {
        val needs = adapter.getDatas()
            .firstOrNull { it.writeState != WriteRuleProcessDialogAdapter.STATE_SUCCESS }
        binding.tvRestart.visibility = View.VISIBLE
        if (needs == null) {
            recoveryCompleted()
            binding.tvRestart.setText(com.mxchip.mxapp.base.R.string.mx_recovery)
            binding.tvContinue.visibility = View.GONE
        } else {
            binding.tvContinue.visibility = View.VISIBLE
        }
    }

    /**
     * 所有数据恢复成功
     */
    private fun recoveryCompleted() {
        CoroutineScope(Dispatchers.IO).launch {
            vm.recoveryCompleted(mapOf("home_id" to AppConfigManager.getUseHomeId())).collect {}
        }
    }

    private fun requestRecoveryDevice(deviceIndex: Int = 0) {
        if (deviceIndex >= needWriteList.size) {
            writeRuleDialog.complete()
            return
        }
        writeRuleDialog.writeRule(deviceIndex)
        val bean = needWriteList[deviceIndex]
        getRecoveryRule(bean, deviceIndex, bean.room_id)
    }

    private fun getRecoveryRule(
        deviceWriteStateBean: DeviceWriteStateBean,
        deviceIndex: Int = 0,
        roomId: Int? = null
    ) {
        val oldRuleDevice = writeRuleDeviceMap[deviceWriteStateBean.uuid]
        if (oldRuleDevice != null) {
            executeDeviceWrite(deviceIndex, oldRuleDevice, false)
        } else {
            vm.getRecoveryDeviceRule(
                mapOf(
                    "home_id" to AppConfigManager.getUseHomeId(),
                    "iotid" to deviceWriteStateBean.iotid
                )
            ).launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<DeviceRuleBean>(
                    success = { deviceRuleBean ->
                        if (deviceRuleBean == null) {
                            itemWriteFailed(deviceIndex)
                        } else {
                            if (roomId != null && roomId > 0) {
                                val roomBean = recoveryRoomMap[roomId]
                                if (roomBean != null && roomBean.room_address > 0) {
                                    deviceRuleBean.room_address = roomBean.room_address
                                }
                            }
                            writeRuleDeviceMap[deviceWriteStateBean.uuid] = deviceRuleBean
                            executeDeviceWrite(deviceIndex, deviceRuleBean, true)
                        }
                    }, error = { _, _, _ ->
                        itemWriteFailed(deviceIndex)
                    })
            )
        }
    }

    private val attrConfigJson by lazy { JSONObject(AppConfigManager.getAttrConfig() ?: "{}") }

    private fun executeDeviceWrite(deviceIndex: Int, ruleBean: DeviceRuleBean, isFirst: Boolean) {
        val writeStateBean = needWriteList[deviceIndex]
        val deviceUUID = writeStateBean.uuid
        if (deviceUUID.isEmpty()) {
            requestNext(listOf(), deviceIndex, 0)
            return
        }
        val writeRuleList =
            DeviceWriteConfigManage.parseMeshWriteRule(
                deviceUUID,
                ruleBean,
                attrConfigJson,
                meshCommand,
            ) { null }
        if (isFirst) {
            //先进行所有规则clear
            val isClearAll = ruleBean.writeRuleSuccessfulRuleIds.isNullOrEmpty()
            val clearTimeStart = System.currentTimeMillis()
            DeviceWriteConfigManage.clearAllConfig(writeRuleList, isClearAll, 0) { clearResult ->
                if (clearResult) {
                    val cut = System.currentTimeMillis()
                    val clearTime = cut - clearTimeStart
                    MXLog.d(TAG, "clear all rule time: $clearTime")
                    if (clearTime < 3000) {
                        Thread.sleep(3000 - clearTime)
                    }
                    writeDevice(writeRuleList, deviceIndex, 0)
                } else {
                    itemWriteFailed(deviceIndex)
                }
            }
        }
    }

    private fun writeDevice(
        writeRuleList: List<IRuleWrite>,
        deviceIndex: Int,
        writeRuleIndex: Int,
    ) {
        val writeStateBean = needWriteList[deviceIndex]
        val ruleWrite = writeRuleList[writeRuleIndex]
        DeviceWriteConfigManage.writeRule(ruleWrite) { writeState ->
            if (ruleWrite is WriteRule) {
                val newSuccessful = mutableListOf<Int>()
                newSuccessful.addAll(ruleWrite.getWriteSuccessfulRuleIds())
                writeRuleDeviceMap[writeStateBean.uuid]?.writeRuleSuccessfulRuleIds?.apply {
                    newSuccessful.addAll(this)
                }
                writeRuleDeviceMap[writeStateBean.uuid]?.writeRuleSuccessfulRuleIds =
                    newSuccessful
            }
            if (writeState) {
                when (ruleWrite) {
                    is WriteVirtualController -> {
                        writeRuleDeviceMap[writeStateBean.uuid]?.writeRemoteControllerVirtual = true
                    }

                    is WriteGroup -> {
                        writeRuleDeviceMap[writeStateBean.uuid]?.writeGroup = true
                    }

                    is WriteRule -> {
                        writeRuleDeviceMap[writeStateBean.uuid]?.writeRule = true
                    }

                    is WriteExtend -> {
                        writeRuleDeviceMap[writeStateBean.uuid]?.writeExtend = true
                    }

                    is WriteCEVT -> {
                        writeRuleDeviceMap[writeStateBean.uuid]?.writeCEVT = true
                    }

                    is WritePresetGroup -> {
                        writeRuleDeviceMap[writeStateBean.uuid]?.writePresetSceneAddress = true
                    }
                }
                requestNext(writeRuleList, deviceIndex, writeRuleIndex)
            } else {
                itemWriteFailed(deviceIndex)
            }
        }
    }

    private fun requestNext(
        writeDatas: List<IRuleWrite>,
        deviceIndex: Int,
        writeRuleIndex: Int,
    ) {
        val next = writeRuleIndex + 1
        //没有下一个规则
        if (next >= writeDatas.size) {
            writeRuleDialog.success(deviceIndex)
            //通知云端同步成功
            notifyCloudSuccessful(deviceIndex)
            val nextDeviceIndex = deviceIndex + 1
            //下一个设备
            requestRecoveryDevice(nextDeviceIndex)
        } else {
            val nextWriteRuleIndex = writeRuleIndex + 1
            writeDevice(writeDatas, deviceIndex, nextWriteRuleIndex)
        }
    }

    private fun itemWriteFailed(deviceIndex: Int) {
        writeRuleDialog.failed(deviceIndex)
        val nextIndex = deviceIndex + 1
        requestRecoveryDevice(nextIndex)
    }
}