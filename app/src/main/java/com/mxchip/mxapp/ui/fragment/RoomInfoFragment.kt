package com.mxchip.mxapp.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mesh.rule.utils.MeshTranscodeUtil
import com.mxchip.mxapp.R
import com.mxchip.mxapp.adapter.RoomDataAdapter
import com.mxchip.mxapp.base.base.MXBusinessFragment
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.PropertyBean
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.DeviceTypeConstants
import com.mxchip.mxapp.base.consts.LiveDataBusKey
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
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.bean.DataListTitleBean
import com.mxchip.mxapp.databinding.FragmentRoomInfoBinding
import com.mxchip.mxapp.device.ui.dialog.DeviceSwitchDialog
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.utils.DeviceUIStateUtil
import com.mxchip.mxapp.viewmodel.MainViewModel
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 房间信息、房间下设备列表
 */
open class RoomInfoFragment : MXBusinessFragment<FragmentRoomInfoBinding>(), IRoomRefresh {

    //-1 共享房间
    private var _roomId: Int? = null
    private var _roomAddress: Int? = null

    private val _allDataMap by lazy { mutableMapOf<Int, List<Any>>() }

    private val vm by viewModels<MainViewModel>()
    private val deviceVM by viewModels<DeviceViewModel>()

    private var switchDialog: DeviceSwitchDialog? = null

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val delayRunMap by lazy { mutableMapOf<String, DelayDeleteMeshRun>() }

    inner class DelayDeleteMeshRun(val pos: Int, val uuid: String) : Runnable {
        override fun run() {
            DeviceUIStateUtil.meshPropertiesMap.remove(uuid)
            adapter.notifyItemChanged(pos)
        }
    }

    private val adapter by lazy {
        RoomDataAdapter(itemClick = { pos ->
            itemClick(pos)
        }, detailClick = { pos ->
            detailClick(pos)
        }, hideClick = { deviceType ->
            hideClick(deviceType)
        })
    }

    override fun getLayoutBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentRoomInfoBinding {
        return FragmentRoomInfoBinding.inflate(inflater)
    }

    override fun onInit(view: View, savedInstanceState: Bundle?) {
        _roomId = arguments?.getInt(RoomConstants.KEY_ROOM_ID)
        initView()
        inject()
        onRefresh(null)
    }

    private fun initView() {
        binding.rvDevice.layoutManager = GridLayoutManager(requireContext(), 2).apply {
            this.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val data = adapter.getData()[position]
                    return if (data is DataListTitleBean) 2 else 1
                }
            }
        }
        binding.rvDevice.addItemDecoration(
            RVItemDecorationSpace(
                bottom = 10.dp2px(), right = 10.dp2px()
            )
        )
        binding.rvDevice.adapter = adapter
        binding.tvAdd.setBtnBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.app_main_color
            )
        )
    }

    private fun inject() {
        if (DeviceManage.isSupportAddDevice()) {
            binding.tvAdd.setOnClickListener {
                if (RoleManager.isUser()) {
                    RoleManager.showDialog(requireContext())
                    return@setOnClickListener
                }
                PairManager.setFromRoomInfo(_roomId, _roomAddress)
                if (PairManager.isSupportProductCategory()) {
                    RouterManager.navigation(RouterConstants.PRODUCT_ACTIVITY)
                } else {
                    RouterManager.navigation(RouterConstants.PAIR_SCAN_ACTIVITY)
                }
            }
        } else {
            binding.tvAdd.text = getString(com.mxchip.mxapp.base.R.string.mx_no_device)
        }
    }

    //    private var refreshInvoke: (() -> Unit)? = null
    override fun onRefresh(refreshCompleteInvoke: (() -> Unit)?) {
        if (isAdded.not()) return
        //完成
//        refreshInvoke = refreshCompleteInvoke
        if (null != _roomId) {
            when (_roomId) {
                -1 -> { //共享房间
                    findSharedData()
                }

                else -> {
                    if (AppConfigManager.getUseHomeId() == 0) {//未选择家庭,先选择家庭
                        LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
//                        refreshInvoke?.invoke()
//                        refreshInvoke = null
                    } else
                        findRoomData()
                }
            }
        }
//        else {
//            refreshInvoke?.invoke()
//        }
    }

    private fun findSharedData() {
        vm.findSharedDevices(199).launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlowList<DeviceBean>(
                success = {
//                    refreshInvoke?.invoke()
                    it?.apply {
                        adapter.setData(this, true)
                        adapter.notifyDataSetChanged()
                    }
//                    refreshInvoke = null
                }, error = { code, msg, _ ->
//                    refreshInvoke?.invoke()
                    val netErr = onException(code)
                    if (!netErr) {
                        showToast("$msg")
                    }
//                    refreshInvoke = null
                })
        )
    }

    private fun findRoomData() {
        lifecycleScope.launch {

            val shortcutDevice = async {
                deviceVM.findShortcutDevice(AppConfigManager.getUseHomeId(), _roomId!!)
            }

            val device = async {
                deviceVM.findDevices(
                    mapOf<String, Any>(
                        "home_id" to AppConfigManager.getUseHomeId(),
                        "show" to true,
                        "room_id" to _roomId!!,
                        "page_size" to 199,
                        "page_no" to 1,
                    )
                )
            }
            val shortcutResp = shortcutDevice.await()
            val deviceResp = device.await()
            _allDataMap.clear()
            val adapterDataList = mutableListOf<Any>()
            shortcutResp.collect {
                JsonUtil.fromJsonMXList(it.data, DeviceBean::class.java)?.apply {
                    if (this.isNotEmpty()) {
                        adapterDataList.add(
                            DataListTitleBean(
                                DeviceTypeConstants.SHORTCUT_DEVICE, dataCount = this.size
                            )
                        )
                        _allDataMap.put(DeviceTypeConstants.SHORTCUT_DEVICE, this)
                        if (adapter.getHideDataTitle().contains(DeviceTypeConstants.SHORTCUT_DEVICE)
                                .not()
                        ) {
                            adapterDataList.addAll(this)
                        }
                    }
                }
            }
            deviceResp.collect {
                JsonUtil.fromJsonMXList(it.data, DeviceBean::class.java)?.apply {
                    if (this.isNotEmpty()) {
                        adapterDataList.add(
                            DataListTitleBean(
                                DeviceTypeConstants.DEVICE, dataCount = this.size
                            )
                        )
                        _allDataMap.put(DeviceTypeConstants.DEVICE, this)
                        if (adapter.getHideDataTitle().contains(DeviceTypeConstants.DEVICE).not()) {
                            adapterDataList.addAll(this)
                        }
                    }
                }
            }
//            refreshInvoke?.invoke()
            adapter.setData(adapterDataList, false)
            adapter.notifyDataSetChanged()
            if (adapter.getData().isEmpty()) {
                binding.layoutAdd.visibility = View.VISIBLE
            } else {
                binding.layoutAdd.visibility = View.GONE
            }
//            refreshInvoke = null
        }
    }

    private fun hideClick(deviceType: Int) {
        val titlePos =
            adapter.getData().indexOfFirst { it is DataListTitleBean && it.type == deviceType }
        if (adapter.getHideDataTitle().contains(deviceType)) {
            _allDataMap[deviceType]?.apply {
                adapter.hideData(titlePos, this.size)
            }
        } else {
            _allDataMap[deviceType]?.apply {
                adapter.showData(titlePos, this)
            }
        }
    }

    private fun itemClick(pos: Int) {
        when (val bean = adapter.getData()[pos]) {
            is DeviceBean -> {
                deviceClick(bean, pos)
            }
        }
    }

    private fun detailClick(pos: Int) {
        val data = adapter.getData()[pos]
        if (data is DeviceBean) {
            checkOtaVersion(data)
        }
    }

    /**
     * 解析mesh数据在离线/主属性
     */
    override fun meshMessageData(dtMap: Map<String, Any>) {
        if (dtMap.containsKey(DeviceConstants.KEY_MESH_STATE)) {
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        } else if (dtMap.containsKey(MeshConstants.KEY_UUID) && dtMap.containsKey(MeshConstants.KEY_DATA)) {
            val uuid = dtMap[MeshConstants.KEY_UUID] as String
            if (adapter.getHideDataTitle().contains(DeviceTypeConstants.DEVICE)) {
                //设备隐藏了，无需解析
                return
            }
            _allDataMap[DeviceTypeConstants.DEVICE]?.forEach { data ->
                if (data is DeviceBean
                    && data.uuid == uuid
                ) {
                    //只有主属性>=2才出现弹框
                    if ((data.propertys?.size ?: 0) >= 2) {
                        val byteArray = dtMap[MeshConstants.KEY_DATA] as ByteArray
                        val dataMap = MeshAttrUtil.parseData(byteArray)
                        AppConfigManager.getAttrConfig()?.let {
                            MeshTranscodeUtil.attrTypeToProperties(JSONObject(it), dataMap)
                        }?.apply {
                            notifyDataSwitchDialog(
                                data.iotid,
                                data.propertys!!,
                                this
                            ) { index, value ->
                                switchDialog?.switchChange(index, value)
                            }
                        }
                    }
                    return
                }
            }
        }
    }

    /**
     * 解析云端socket数据，在离线/主属性
     */
    override fun ilopMessageData(ilopData: JSONObject) {
        if (ilopData.has("iotId")) {
            val iotId = ilopData.getString("iotId")
            if (adapter.getHideDataTitle().contains(DeviceTypeConstants.DEVICE)) {
                //设备隐藏了
                _allDataMap[DeviceTypeConstants.DEVICE]?.forEach { data ->
                    if (data is DeviceBean) {
                        if (iotId == data.iotid) {
                            if (ilopData.has("items")) {
                                data.propertys?.apply {
                                    val itemJson = ilopData.getJSONObject("items")
                                    if (this.size == 1) if (itemJson.has(this[0].identifier)) {
                                        val switchJson = itemJson.getJSONObject(this[0].identifier)
                                        if (switchJson.has("value")) {
                                            val newSwt = switchJson.getDouble("value")
                                            this[0].value = newSwt.toInt()
                                        }
                                    }
                                }
                                data.online = true
                            } else if (ilopData.has("status")) {
                                val online = ilopData.getString("status") == "online"
                                data.online = online
                            }
                            return
                        }
                    }
                }
            } else {
                adapter.getData().forEachIndexed { index, data ->
                    if (data is DeviceBean) {
                        if (iotId == data.iotid) {
                            if (ilopData.has("items")) {
                                data.propertys?.apply {
                                    val itemJson = ilopData.getJSONObject("items")
                                    if (this.size == 1) if (itemJson.has(this[0].identifier)) {
                                        val switchJson = itemJson.getJSONObject(this[0].identifier)
                                        if (switchJson.has("value")) {
                                            val newSwt = switchJson.getDouble("value")
                                            this[0].value = newSwt.toInt()
                                        }
                                    }
                                    notifyDataSwitchDialog(
                                        data.iotid,
                                        this,
                                        itemJson
                                    ) { index, value ->
                                        switchDialog?.switchChange(index, value)
                                    }
                                }
                                data.online = true
                            } else if (ilopData.has("status")) {
                                val online = ilopData.getString("status") == "online"
                                data.online = online
                            }
                            lifecycleScope.launch(Dispatchers.Main) {
                                adapter.notifyItemChanged(index)
                            }
                            return
                        }
                    }
                }
            }
        }
    }

    private fun deviceClick(bean: DeviceBean, pos: Int) {
        when {
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
                    requireContext(), bean.iotid, deviceNick, bean.propertys!!
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

    private fun sendSwitch(currentSwitch: Int, data: DeviceBean, identifier: String, pos: Int) {
        if (_roomId == -1) {//共享房间 ,共享设备走云端
            sendSwitchCloud(data.iotid, identifier, currentSwitch)
        } else {
            val attrConfig = AppConfigManager.getAttrConfig()
            if (attrConfig.isNullOrEmpty()) {
                MXLog.w("sendSwitch()", "attrConfigJson is empty")
                sendSwitchCloud(data.iotid, identifier, currentSwitch)
                return
            }
            val attrConfigJson = JSONObject(attrConfig)
            val meshParams =
                MeshTranscodeUtil.propertiesToAttrType(attrConfigJson, mapOf(identifier to 2))
            if (meshParams.isEmpty()) {
                sendSwitchCloud(data.iotid, identifier, currentSwitch)
            } else {
                val hexParam = MeshSDKManage.combineAttTypeAndAttValue(meshParams)
                val opcode = MeshOpcodeConstants.VENDOR_MSG_OPCODE_SET
                MXMesh.getNode(data.uuid)?.unicastAddress?.let { address ->
                    val realOpCodeBytes = MeshParserUtils.createVendorOpCode(
                        opcode.toInt(16), 0x0922
                    )
                    val realOpcode = ByteUtil.bytesToHexString(realOpCodeBytes)//D32209

                    MeshSDKManage.setMsg(data.uuid, hexParam, object : MapCallback() {
                        override fun onResult(resultMap: Map<String, Any>) {
                            val byteArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                            val dataMap = MeshAttrUtil.parseData(byteArray)
                            AppConfigManager.getAttrConfig()?.let {
                                MeshTranscodeUtil.attrTypeToProperties(JSONObject(it), dataMap)
                            }?.apply {
                                DeviceUIStateUtil.meshPropertiesMap[data.uuid] = this
                                val dialog = switchDialog
                                if (dialog != null && dialog.isShowing() && pos == -1) {
                                    notifyDataSwitchDialog(
                                        data.iotid,
                                        data.propertys!!,
                                        this
                                    ) { index, value ->
                                        switchDialog?.switchChange(index, value)
                                    }
                                } else {
                                    delayRunMap[data.uuid]?.apply {
                                        handler.removeCallbacks(this)
                                    }
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        adapter.notifyItemChanged(pos)
                                    }
                                    val newDelayRun = DelayDeleteMeshRun(pos, data.uuid)
                                    handler.postDelayed(newDelayRun, 5 * 1000)
                                    delayRunMap[data.uuid] = newDelayRun
                                }
                            }
                        }

                        override fun onError(callbackMsg: CallbackMsg) {
                            super.onError(callbackMsg)
                            MeshSDKManage.parseDataToCloud(
                                realOpcode, address, hexParam, 1
                            ) { map ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    deviceVM.sendCloudMeshMessage(map).collect {}
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    /**
     * 发云端
     */
    private fun sendSwitchCloud(
        iotId: String, identifier: String, currentSwitch: Int
    ) {
        val newSwitch = if (currentSwitch == 1) 0 else 1
        vm.setProperties(iotId, mapOf(identifier to newSwitch)).launchAndCollectIn(
            this,
            Lifecycle.State.CREATED,
            loadFlow<Any>(success = {}, error = { code: Int, msg: String?, _: Any? ->
                val netErr = onException(code, false)
                if (!netErr) showToast("$msg")
            })
        )
    }

    private fun checkOtaVersion(data: DeviceBean) {
        when {
            DeviceUIStateUtil.isDebugH5 -> {
                val url = DeviceUIStateUtil.debugH5URL
                MXDialog.Builder(requireContext()).title("是否进行调试模式")
                    .edittextText(url.ifEmpty { "http://" })
                    .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                        alertDialog.cancel()
                        toH5Panel(data)
                    }
                    .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, s ->
                        alertDialog.cancel()
                        val input = s!!.trim()
                        if (input.length > 20) {
                            DeviceUIStateUtil.debugH5URL = input
                            RouterManager.navigation(
                                RouterConstants.DEVICE_PANEL_ACTIVITY, mapOf(
                                    DeviceConstants.KEY_WEB_VIEW_PATH to input,
                                    DeviceConstants.KEY_DEVICE_BEAN to data,
                                    "isDebugH5" to true
                                )
                            )
                        }
                    }.create().show()
            }

            else -> {
                toH5Panel(data)
            }
        }
    }

    private fun toH5Panel(data: DeviceBean) {
        val params = mutableMapOf<String, Any>(DeviceConstants.KEY_DEVICE_BEAN to data)
        when {
            _roomId == -1 -> {//共享设备
                params[DeviceConstants.KEY_DEVICE_TYPE] = DeviceTypeConstants.SHARED_DEVICE
            }

            data.number > 0 -> {//快捷设备序号 > 0 时为快捷设备
                params[DeviceConstants.KEY_DEVICE_TYPE] = DeviceTypeConstants.SHORTCUT_DEVICE
            }
        }
        RouterManager.navigation(RouterConstants.DEVICE_PANEL_ACTIVITY, params)
    }

    private fun notifyDataSwitchDialog(
        notifyIotId: String,
        propertys: List<PropertyBean>,
        json: JSONObject,
        matchIdentifier: (Int, Int) -> Unit
    ) {
        val dialog = switchDialog
        if (dialog != null && dialog.isShowing() && dialog.iotId == notifyIotId) {
            propertys.forEachIndexed { index, properties ->
                val identifier = properties.identifier

                if (json.has(identifier)) {
                    val switchValue = json.get(identifier)
                    if (switchValue is JSONObject) {
                        if (switchValue.has("value")) {
                            val newSwt = switchValue.getDouble("value").toInt()
                            matchIdentifier.invoke(index, newSwt)
                        }
                    } else if (switchValue is Number) {
                        matchIdentifier.invoke(index, switchValue.toDouble().toInt())
                    }

                }
            }
        }
    }
}