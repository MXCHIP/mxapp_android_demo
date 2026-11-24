package com.mxchip.mxapp.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mesh.rule.utils.MeshTranscodeUtil
import com.mxchip.mxapp.adapter.RoomDeviceManagerAdapter
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.GroupBean
import com.mxchip.mxapp.base.bean.PropertyBean
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.DeviceTypeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshAttrUtil
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.DeviceManage
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.utils.loadObserver
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.bean.RoomDeviceManagerTitleBean
import com.mxchip.mxapp.databinding.ActivityRoomDeviceManagerBinding
import com.mxchip.mxapp.device.ui.AbsDeviceListActivity
import com.mxchip.mxapp.device.ui.dialog.DeviceSwitchDialog
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.mxapp.room.R
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.mxapp.utils.DeviceUIStateUtil
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 房间设备管理
 */
@Router(RouterConstants.DEVICE_MANAGER_ACTIVITY)
class RoomDeviceManagerActivity : AbsDeviceListActivity<ActivityRoomDeviceManagerBinding>() {

    companion object {
        private const val TAG = "RoomDeviceManagerActivity"
    }

    private val vm by viewModels<RoomViewModel>()
    private val deviceVM by viewModels<DeviceViewModel>()

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val delayRunMap by lazy { mutableMapOf<String, DelayDeleteMeshRun>() }

    inner class DelayDeleteMeshRun(val pos: Int, val uuid: String) : Runnable {
        override fun run() {
            DeviceUIStateUtil.meshPropertiesMap.remove(uuid)
            adapter.notifyItemChanged(pos)
        }
    }

    private var isEditState = false
    private val selectedIotIds by lazy { ArrayList<String>() }
    private val selectedGroupIds by lazy { mutableListOf<Int>() }
    private val notShareIotIds by lazy { mutableListOf<String>() }
    private val showIds by lazy { mutableListOf<String>() }
    private val hideIds by lazy { mutableListOf<String>() }

    private var isRefreshHomeData = false
    private var deviceType = DeviceTypeConstants.DEVICE

    private var otaIotId: String? = null

    private val adapter by lazy {
        RoomDeviceManagerAdapter(
            selectedIotIds,
            selectedGroupIds,
            itemClick = { pos ->
                itemClick(pos)
            }, detailClick = { pos ->
                detailClick(pos)
            }, selectedAll = { pos ->
                selectedAll(pos)
            })
    }

    override fun getLayoutBinding(): ActivityRoomDeviceManagerBinding {
        return ActivityRoomDeviceManagerBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        deviceType = intent.getIntExtra(DeviceConstants.KEY_DEVICE_TYPE, DeviceTypeConstants.DEVICE)
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        initView()
        checkViewState()
        inject()
        showLoading()
        binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_device_manage))
        binding.emptyView.content(getString(com.mxchip.mxapp.base.R.string.mx_no_device))
        findDevice(1)
    }

    private fun initView() {
        binding.rvDevice.layoutManager = GridLayoutManager(this, 2).apply {
            this.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val data = adapter.getData()[position]
                    return if (data is RoomDeviceManagerTitleBean) 2 else 1
                }
            }
        }
        binding.rvDevice.addItemDecoration(
            RVItemDecorationSpace(
                bottom = 10.dp2px(), right = 10.dp2px()
            )
        )
        binding.rvDevice.adapter = adapter

        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.setIconColor(mainColor)
    }

    private fun checkViewState() {
        if (DeviceManage.isSupportDeviceShare()) {
            binding.layoutShare.visibility = View.VISIBLE
        } else binding.layoutShare.visibility = View.GONE

        if (DeviceManage.isSupportAddDevice()) {
            binding.layoutDelete.visibility = View.VISIBLE
        } else binding.layoutDelete.visibility = View.GONE
    }

    private fun inject() {
        //非普通成员，才允许编辑
        if (RoleManager.isUser().not()) {
            binding.toolbar.right {
                isEditState = false
                binding.toolbar.rightText("")
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnSearch.visibility = View.VISIBLE
                binding.layoutBottom.visibility = View.GONE
                selectedGroupIds.clear()
                selectedIotIds.clear()
                notShareIotIds.clear()
                showIds.clear()
                hideIds.clear()
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_device_manage))
                adapter.updateDataEditState(isEditState)
            }
            binding.btnEdit.setOnClickListener {
                isEditState = true
                binding.btnEdit.visibility = View.GONE
                binding.btnSearch.visibility = View.GONE
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_please_select))
                    .rightText(getString(com.mxchip.mxapp.base.R.string.mx_done))
                adapter.updateDataEditState(isEditState)
            }
        }
        binding.btnSearch.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.DEVICE_MANAGE_SEARCH_ACTIVITY, mapOf(
                    DeviceConstants.KEY_DEVICE_TYPE to deviceType
                )
            )
        }
        binding.toolbar.left { backPressed() }

        binding.layoutShare.setOnClickListener {
            if (selectedIotIds.isEmpty()) {
                return@setOnClickListener
            }
            RouterManager.navigation(
                RouterConstants.DEVICE_SHARE_ACCOUNT_ACTIVITY, mapOf(
                    "iotids" to selectedIotIds
                )
            )
        }
        val deleteDialog = getString(com.mxchip.mxapp.base.R.string.mx_del_device_batch_des)

        binding.layoutDelete.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                .content(deleteDialog)
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                    dialog.dismiss()
                    deleteDevice()
                }.create().show()
        }

        binding.layoutReplace.setOnClickListener {
            val selectedDevice =
                adapter.getData().first { (it is DeviceBean && selectedIotIds.contains(it.iotid)) }
            if (selectedDevice is DeviceBean)
                showReplaceDialog(selectedDevice)
        }
        binding.layoutRename.setOnClickListener {
            var selectedDevice: DeviceBean? = null
            var selectedGroup: GroupBean? = null
            var selectedIndex = -1
            run {
                adapter.getData().forEachIndexed { index, bean ->
                    if ((bean is DeviceBean)) {
                        if (selectedIotIds.contains(bean.iotid)) {
                            selectedIndex = index
                            selectedDevice = bean
                            return@run
                        }
                    } else if (bean is GroupBean) {
                        if (selectedGroupIds.contains(bean.group_id)) {
                            selectedIndex = index
                            selectedGroup = bean
                            return@run
                        }
                    }
                }
            }
            selectedDevice?.apply {
                showRenameDialog(this.nick_name.ifEmpty { this.product_name }) { newName ->
                    showLoading()
                    deviceVM.setDeviceName(this.iotid, newName).launchAndCollectIn(
                        this@RoomDeviceManagerActivity,
                        Lifecycle.State.CREATED,
                        loadFlow<Any>(success = {
                            dismissLoading()
                            isRefreshHomeData = true
                            this.nick_name = newName
                            adapter.notifyItemChanged(selectedIndex)
                        }, error = { code: Int, msg: String?, _ ->
                            dismissLoading()
                            val netErr = onException(code)
                            if (!netErr) showToast("$msg")
                        })
                    )
                }
            }
        }
        binding.layoutShowHide.setOnClickListener {
            val visible = hideIds.isNotEmpty()
            val visibleList = mutableListOf<Map<String, Any>>()
            selectedIotIds.forEach {
                visibleList.add(
                    mapOf(
                        "obj_type" to "device",
                        "obj_id" to it,
                        "show" to visible
                    )
                )
            }
            selectedGroupIds.forEach {
                visibleList.add(
                    mapOf(
                        "obj_type" to "group",
                        "obj_id" to "$it",
                        "show" to visible
                    )
                )
            }
            showLoading()
            deviceVM.setCommonShow(visibleList)
                .launchAndCollectIn(
                    this, Lifecycle.State.CREATED, loadFlow<Any>(
                        success = {
                            dismissLoading()
                            isRefreshHomeData = true
                            if (visible) {
                                showIds.addAll(hideIds)
                                hideIds.clear()
                            } else {
                                hideIds.addAll(showIds)
                                showIds.clear()
                            }
                            adapter.updateDataVisibleState(visible)
                            changeDeviceVisibleState()
                        },
                        error = { code, msg, _ ->
                            dismissLoading()
                            val netErr = onException(code)
                            if (!netErr) showToast("$msg")
                        })
                )
        }
        //设备mesh数据
        LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA).observe(this) {
            meshMessageData(it)
        }
        //云端socket数据
        LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_CLOUD_DATA).observe(this) {
            val json = JSONObject(it)
            ilopMessageData(json)
        }

        deviceVM.unBindState.observe(
            this, loadObserver<Any>(
                success = {
                    adapter.getData().forEach {
                        if (it is DeviceBean) {
                            if (selectedIotIds.contains(it.iotid)) {
                                if (it.uuid.isNotEmpty()) {
                                    if (it.category_id !in 140101..140102 && it.category_id !in 140301..140303) //非网关品类才发reset
                                        MXMesh.getNode(uuid = it.uuid)?.apply {
                                            MXMesh.resetNode(node = this)
                                        }
                                }
                            }
                        }
                    }
                    isRefreshHomeData = true
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_del_success))
                    MeshSDKManage.isConnectProxy = true
                    selectedIotIds.clear()
                    notShareIotIds.clear()
                    showIds.clear()
                    hideIds.clear()
                    findDevice(1)
                },
                error = { code, msg, _ ->
                    dismissLoading()
                    MeshSDKManage.isConnectProxy = true
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                })
        )
    }

    private var switchDialog: DeviceSwitchDialog? = null
    private val deviceList by lazy { mutableListOf<DeviceBean>() }

    private fun findDevice(pageNo: Int) {
        if (pageNo == 1) deviceList.clear()
        deviceVM.findDevices(
            mapOf(
                "home_id" to AppConfigManager.getUseHomeId(),
                "page_size" to 199,
                "page_no" to pageNo,
            )
        ).launchAndCollectIn(
            this,
            Lifecycle.State.CREATED,
            loadFlowList<DeviceBean>(
                success = { devices ->
                    if (devices.isNullOrEmpty()) {
                        adapter.setData(listOf())
                        showEmptyView()
                        dismissLoading()
                    } else {
                        deviceList.addAll(devices)
                        if (devices.size == 199) {
                            val next = pageNo + 1
                            findDevice(next)
                        } else {
                            parseDeviceList(deviceList)
                            dismissLoading()
                        }
                    }
                },
                error = { code, msg, _ ->
                    val netErr = onException(code)
                    if (!netErr) {
                        showToast("$msg")
                    }
                    showEmptyView()
                    dismissLoading()
                }
            )
        )
    }

    /**
     * 解析API返回数据
     */
    private fun parseDeviceList(devices: List<DeviceBean>) {
        val roomMap = HashMap<Int, ArrayList<DeviceBean>>()
        val roomList = mutableListOf<Int>()//方便记录顺序
        devices.forEach {
            if (roomMap.containsKey(it.room_id)) {
                roomMap[it.room_id]!!.add(it)
            } else {
                roomList.add(it.room_id)
                roomMap[it.room_id] = arrayListOf(it)
            }
        }
        val realData = mutableListOf<Any>()
        roomList.forEach { roomId ->
            val roomDevices = roomMap[roomId]!!
            realData.add(
                RoomDeviceManagerTitleBean(
                    roomDevices[0].room_name,
                    roomId,
                    roomDevices.size
                )
            )
            realData.addAll(roomDevices)
        }
        adapter.setData(realData)
    }

    private fun showEmptyView() {
        binding.layoutBottom.visibility = View.GONE
        binding.emptyView.visibility = if (adapter.getData().isEmpty()) View.VISIBLE else View.GONE
    }

    private fun itemClick(pos: Int) {
        when (val bean = adapter.getData()[pos]) {
            is DeviceBean -> {
                when {
                    isEditState -> {//编辑状态
                        if (selectedIotIds.contains(bean.iotid)) {
                            selectedIotIds.remove(bean.iotid)
                            if (bean.share_type == 0)//不能分享的设备
                                notShareIotIds.remove(bean.iotid)
                            if (bean.show) {
                                showIds.remove(bean.iotid)
                            } else {
                                hideIds.remove(bean.iotid)
                            }
                        } else {
                            selectedIotIds.add(bean.iotid)
                            if (bean.share_type == 0) {//不能分享的设备
                                if (notShareIotIds.contains(bean.iotid).not())
                                    notShareIotIds.add(bean.iotid)
                            }
                            if (bean.show) {
                                if (showIds.contains(bean.iotid).not())
                                    showIds.add(bean.iotid)
                            } else if (hideIds.contains(bean.iotid).not()) {
                                hideIds.add(bean.iotid)
                            }
                        }
                        adapter.notifyItemChanged(pos)
                        checkBottomAction()
                    }

                    bean.propertys.isNullOrEmpty() -> {
                        checkOtaVersion(bean)
                    }

                    bean.propertys!!.size == 1 -> {
                        val currentSwitch = DeviceUIStateUtil.parseSwitchState(bean)
                        sendSwitch(currentSwitch, bean, bean.propertys!![0].identifier, pos)
                    }

                    else -> {
                        if (switchDialog?.isShowing() == true) return
                        val deviceNick = bean.nick_name.ifEmpty { bean.product_name }
                        switchDialog = DeviceSwitchDialog(
                            this, bean.iotid, deviceNick, bean.propertys!!
                        ).setSwitchListener { properties ->
                            val currentSwitch = when (val pv = properties.value) {
                                is Int -> pv
                                is Double -> pv.toInt()
                                else -> 0
                            }
                            sendSwitch(currentSwitch, bean, properties.identifier, -1)
                        }.setClickDetailListener {
                            checkOtaVersion(bean)
                        }
                        //mesh是否连接
                        if (MXMesh.isConnected()) {
                            MeshSDKManage.getAllProperties(bean.uuid, null)
                        } else {
                            deviceVM.getDeviceProperties(bean.iotid)
                        }
                        switchDialog?.show()
                    }
                }
            }

            is GroupBean -> {
                when {
                    isEditState -> {
                        if (selectedGroupIds.contains(bean.group_id)) {
                            selectedGroupIds.remove(bean.group_id)
                            if (bean.show) {
                                showIds.remove("${bean.group_id}")
                            } else {
                                hideIds.remove("${bean.group_id}")
                            }
                        } else {
                            selectedGroupIds.add(bean.group_id)
                            if (bean.show) {
                                if (showIds.contains("${bean.group_id}").not())
                                    showIds.add("${bean.group_id}")
                            } else if (hideIds.contains("${bean.group_id}").not()) {
                                hideIds.add("${bean.group_id}")
                            }
                        }
                        adapter.notifyItemChanged(pos)
                        checkBottomAction()
                    }

                    bean.propertys.isNullOrEmpty() -> {
                        if (bean.nodes.isNotEmpty()) {
                            toGroupH5Panel(bean)
                        }
                    }

                    else -> {
                        if (bean.nodes.isNotEmpty()) {
                            toGroupH5Panel(bean)
                        }
                    }
                }
            }
        }
    }

    private fun detailClick(pos: Int) {
        val data = adapter.getData()[pos]
        if (data is DeviceBean) {
            checkOtaVersion(data)
        } else if (data is GroupBean) {
            if (data.nodes.isNotEmpty())
                toGroupH5Panel(data)
        }
    }

    private fun selectedAll(pos: Int) {
        val titleBean = adapter.getData()[pos]
        if (titleBean is RoomDeviceManagerTitleBean) {
            val selected = !titleBean.isSelected
            titleBean.isSelected = selected
            adapter.getData().forEach { bean ->
                if (bean is DeviceBean) {
                    if (titleBean.roomId == bean.room_id) {
                        if (selected) {
                            if (selectedIotIds.contains(bean.iotid).not()) {
                                selectedIotIds.add(bean.iotid)
                            }
                            if (bean.share_type == 0) {//不能分享的设备
                                if (notShareIotIds.contains(bean.iotid).not())
                                    notShareIotIds.add(bean.iotid)
                            }
                            if (bean.show) {
                                if (showIds.contains(bean.iotid).not())
                                    showIds.add(bean.iotid)
                            } else if (hideIds.contains(bean.iotid).not()) {
                                hideIds.add(bean.iotid)
                            }
                        } else {
                            selectedIotIds.remove(bean.iotid)
                            //不能分享的设备
                            notShareIotIds.remove(bean.iotid)
                            if (bean.show) {
                                showIds.remove(bean.iotid)
                            } else {
                                hideIds.remove(bean.iotid)
                            }
                        }
                    }
                } else if (bean is GroupBean) {
                    if (titleBean.roomId == bean.room_id) {
                        if (selected) {
                            if (selectedGroupIds.contains(bean.group_id).not())
                                selectedGroupIds.add(bean.group_id)
                            if (bean.show) {
                                if (showIds.contains("${bean.group_id}").not()) {
                                    showIds.add("${bean.group_id}")
                                }
                            } else if (hideIds.contains("${bean.group_id}").not()) {
                                hideIds.add("${bean.group_id}")
                            }
                        } else {
                            selectedGroupIds.remove(bean.group_id)
                            if (bean.show) {
                                showIds.remove("${bean.group_id}")
                            } else {
                                hideIds.remove("${bean.group_id}")
                            }
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged()
            checkBottomAction()
        }
    }

    private fun deleteDevice() {
        MeshSDKManage.isConnectProxy = false
        showLoading()
        deviceVM.unBindDevice(selectedIotIds)
    }

    private fun checkBottomAction() {
        if (selectedGroupIds.isNotEmpty() || selectedIotIds.isNotEmpty()) {
            binding.layoutBottom.visibility = View.VISIBLE
            //选了群组, 需要隐藏【替换/分享】
            if (selectedGroupIds.isNotEmpty()) {
                binding.layoutShare.visibility = View.GONE
                binding.layoutReplace.visibility = View.GONE
                val selectedTotal =
                    selectedGroupIds.size + selectedIotIds.size
                binding.layoutRename.visibility =
                    if (selectedTotal == 1) View.VISIBLE else View.GONE
            } else if (selectedIotIds.isNotEmpty()) {//仅设备
                if (DeviceManage.isSupportDeviceShare()) {//功能配置允许，才检查
                    //是否包含不允许分享类型的设备
                    binding.layoutShare.visibility =
                        if (notShareIotIds.isNotEmpty()) View.GONE
                        else View.VISIBLE
                }
                val isShow = if (selectedIotIds.size > 1) View.GONE else View.VISIBLE
                binding.layoutReplace.visibility = isShow
                binding.layoutRename.visibility = isShow
            }
            if (showIds.isNotEmpty() && hideIds.isNotEmpty()) {
                //存在显示和隐藏的设备，隐藏操作【显示/隐藏】按钮
                binding.layoutShowHide.visibility = View.GONE
            } else {
                binding.layoutShowHide.visibility = View.VISIBLE
                changeDeviceVisibleState()
            }
        } else {
            binding.layoutBottom.visibility = View.GONE
        }
    }

    private fun changeDeviceVisibleState() {
        if (hideIds.isNotEmpty()) {
            binding.ivShowHide.setImageResource(R.drawable.room_ic_device_show)
            binding.tvShowHide.setText(com.mxchip.mxapp.base.R.string.mx_show)
        } else {
            binding.ivShowHide.setImageResource(R.drawable.room_ic_device_hide)
            binding.tvShowHide.setText(com.mxchip.mxapp.base.R.string.mx_show_hide)
        }
    }

    private fun showReplaceDialog(device: DeviceBean) {
        MXDialog.Builder(this)
            .title(getString(com.mxchip.mxapp.base.R.string.mx_replace_alert_title))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_replace_alert_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_next)) { alertDialog, _ ->
                alertDialog.dismiss()
                RouterManager.navigation(
                    RouterConstants.PAIR_GUIDE_ACTIVITY, mapOf(
                        ProductConstants.KEY_PRODUCT_KEY to device.product_key,
                        ProductConstants.KEY_PRODUCT_KEY_LIST to arrayListOf(device.product_key),
                        "replaced.iotId" to device.iotid,
                        "replaced.name" to device.nick_name.ifEmpty { device.product_name },
                        "replaced.roomName" to device.room_name
                    )
                )
            }.create().show()
    }

    private fun showRenameDialog(editText: String, confirm: (newName: String) -> Unit) {
        val title = getString(com.mxchip.mxapp.base.R.string.mx_device_name)
        MXDialog.Builder(this).title(title)
            .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
            .edittextText(editText)
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, inputText ->
                val newInput = inputText?.trim()
                if (newInput.isNullOrEmpty()) {
                    ToastUtil.showLongToast(
                        this,
                        getString(com.mxchip.mxapp.base.R.string.mx_no_input)
                    )
                    return@rightButton
                }
                dialog.dismiss()
                confirm.invoke(inputText)
            }.create().show()
    }

    private fun sendGroupMessage(groupAddress: Int, hexParam: String) {
        MeshSDKManage.senDeviceGroupMsg(
            groupAddress, hexParam
        ) { params ->
            lifecycleScope.launch(Dispatchers.IO) {
                deviceVM.sendCloudMeshMessage(params).collect {}
            }
        }
    }

    private fun sendSwitch(currentSwitch: Int, data: DeviceBean, identifier: String, pos: Int) {
        val attrConfig = AppConfigManager.getAttrConfig()
        if (attrConfig.isNullOrEmpty()) {
            MXLog.e(TAG, "attrConfigJson is empty")
            sendSwitchCloud(data.iotid, identifier, currentSwitch)
            return
        }
        val attrConfigJson = JSONObject(attrConfig)
        val meshParams =
            MeshTranscodeUtil.propertiesToAttrType(attrConfigJson, mapOf(identifier to 2))
        if (meshParams.isEmpty()) {
            sendSwitchCloud(data.iotid, identifier, currentSwitch)
        } else if (MXMesh.isConnected() && meshParams.isNotEmpty()) {
            MeshSDKManage.setMsg(data.uuid, meshParams, object : MapCallback() {
                override fun onResult(resultMap: Map<String, Any>) {
                    val byteArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    val dataMap = MeshAttrUtil.parseData(byteArray)
                    MeshTranscodeUtil.attrTypeToProperties(attrConfigJson, dataMap).apply {
                        DeviceUIStateUtil.meshPropertiesMap[data.uuid] = this
                        if (switchDialog?.isShowing() == true || pos == -1) notifyDataSwitchDialog(
                            data.iotid, data.propertys!!, this
                        )
                        else {
                            adapter.notifyItemChanged(pos)
                            delayRunMap[data.uuid]?.apply {
                                handler.removeCallbacks(this)
                            }
                            val newDelayRun = DelayDeleteMeshRun(pos, data.uuid)
                            handler.postDelayed(newDelayRun, 5 * 1000)
                            delayRunMap[data.uuid] = newDelayRun
                        }
                    }
                }

                override fun onError(callbackMsg: CallbackMsg) {
                    super.onError(callbackMsg)
                    sendSwitchCloud(data.iotid, identifier, currentSwitch)
                }
            })
        } else {//发云端
            sendSwitchCloud(data.iotid, identifier, currentSwitch)
        }
    }

    /**
     * 发云端
     */
    private fun sendSwitchCloud(iotId: String, identifier: String, currentSwitch: Int) {
        val newSwitch = if (currentSwitch == 1) 0 else 1
        deviceVM.setProperties(iotId, mapOf(identifier to newSwitch)).launchAndCollectIn(
            this, Lifecycle.State.CREATED
        ) {
            if (it.state != NetState.SUCCESS) {
                showToast("${it.state.message}")
            }
        }
    }

    private fun checkOtaVersion(data: DeviceBean) {
        if (data.category_id == 130308) { //追光摄像头
            RouterManager.navigation(
                RouterConstants.DEVICE_CAMERA_PANEL_ACTIVITY, mapOf(
                    DeviceConstants.KEY_DEVICE_NAME to data.nick_name.ifEmpty { data.product_name },
                    CommonConstants.KEY_IOT_ID to data.iotid
                )
            )
        } else {
            toH5Panel(data)
        }
    }

    /**
     * 解析mesh数据在离线/主属性
     */
    private fun meshMessageData(dtMap: Map<String, Any>) {
        if (dtMap.containsKey(DeviceConstants.KEY_MESH_STATE)) {
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        } else if (dtMap.containsKey(MeshConstants.KEY_UUID) && dtMap.containsKey(MeshConstants.KEY_DATA)) {
            val uuid = dtMap[MeshConstants.KEY_UUID] as String
            adapter.getData().forEachIndexed { index, data ->
                if (data is DeviceBean
                    && data.uuid == uuid
                ) {
                    //2022/07/07 不显示开关状态，卡片单控时显示8s后隐藏
                    val byteArray = dtMap[MeshConstants.KEY_DATA] as ByteArray
                    val dataMap = MeshAttrUtil.parseData(byteArray)
                    AppConfigManager.getAttrConfig()?.let {
                        MeshTranscodeUtil.attrTypeToProperties(JSONObject(it), dataMap)
                    }?.apply {
                        notifyDataSwitchDialog(data.iotid, data.propertys!!, this)
                    }
                    return
                }
            }
        }
    }

    private fun notifyDataSwitchDialog(
        notifyIotId: String, propertys: List<PropertyBean>, json: JSONObject
    ) {
        switchDialog?.apply {
            if (isShowing() && iotId == notifyIotId) {
                propertys.forEachIndexed { index, properties ->
                    val identifier = properties.identifier
                    if (json.has(identifier)) {
                        val value = json.getInt(identifier)
                        switchChange(index, value)
                    }
                }
            }
        }
    }

    /**
     * 解析云端socket数据，在离线/主属性
     */
    private fun ilopMessageData(json: JSONObject) {
        run {
            adapter.getData().forEachIndexed { index, data ->
                if (data is DeviceBean) {
                    if (json.has("iotId")) {
                        val iotId = json.getString("iotId")
                        if (iotId == data.iotid) {
                            if (json.has("items")) {
                                data.propertys?.apply {
                                    val itemJson = json.getJSONObject("items")
                                    if (this.size == 1) if (itemJson.has(this[0].identifier)) {
                                        val switchJson = itemJson.getJSONObject(this[0].identifier)
                                        if (switchJson.has("value")) {
                                            val newSwt = switchJson.getDouble("value")
                                            this[0].value = newSwt.toInt()
                                        }
                                    }
                                    notifyDataSwitchDialog(data.iotid, this, itemJson)
                                }
                                data.online = true
                            } else if (json.has("status")) {
                                val online = json.getString("status") == "online"
                                data.online = online
                            }
                            lifecycleScope.launch(Dispatchers.Main) {
                                adapter.notifyItemChanged(index)
                            }
                            return@run
                        }
                    }
                }
            }
        }
    }

    private fun backPressed() {
        if (isRefreshHomeData) LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
            .postValue(HomeDataRefresh.DEVICE)
        finish()
    }

    private fun sendVersion(version: String) {
        otaIotId?.apply {
            deviceVM.updateOTAVersion(this, version).launchAndCollectIn(
                this@RoomDeviceManagerActivity, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                    MXLog.i("OtaMeshActivity", "sendOtaVersion success")
                }, error = { _, msg, _ ->
                    MXLog.i("OtaMeshActivity", "sendOtaVersion failed, msg=$msg")
                })
            )
        }
    }
}