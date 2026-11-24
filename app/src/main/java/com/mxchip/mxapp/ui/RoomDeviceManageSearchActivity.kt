package com.mxchip.mxapp.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
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
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshAttrUtil
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.databinding.ActivityRoomDeviceManagerSearchBinding
import com.mxchip.mxapp.device.ui.AbsDeviceListActivity
import com.mxchip.mxapp.device.ui.dialog.DeviceSwitchDialog
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.mxapp.utils.DeviceUIStateUtil
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 房间设备管理,搜索
 */
@Router(RouterConstants.DEVICE_MANAGE_SEARCH_ACTIVITY)
class RoomDeviceManageSearchActivity :
    AbsDeviceListActivity<ActivityRoomDeviceManagerSearchBinding>() {

    companion object {
        private const val TAG = "RoomDeviceManageSearchActivity"
    }

    private val deviceVM by viewModels<DeviceViewModel>()

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val delayRunMap by lazy { mutableMapOf<String, DelayDeleteMeshRun>() }

    inner class DelayDeleteMeshRun(val pos: Int, val uuid: String) : Runnable {
        override fun run() {
            DeviceUIStateUtil.meshPropertiesMap.remove(uuid)
            adapter.notifyItemChanged(pos)
        }
    }

    private val adapter by lazy {
        RoomDeviceManagerAdapter(
            arrayListOf(),
            mutableListOf(),
            itemClick = { pos ->
                itemClick(pos)
            }, detailClick = { pos ->
                detailClick(pos)
            }, selectedAll = {})
    }

    override fun getLayoutBinding() = ActivityRoomDeviceManagerSearchBinding.inflate(layoutInflater)

    private var deviceType = DeviceTypeConstants.DEVICE
    override fun onInit() {
        deviceType = intent.getIntExtra(DeviceConstants.KEY_DEVICE_TYPE, DeviceTypeConstants.DEVICE)
        initView()
        initEvent()
        inject()
        binding.etSearch.setHint(com.mxchip.mxapp.base.R.string.mx_devices_search_hint)
        findDevice(1)
    }

    private fun initView() {
        binding.etSearch.background =
            generateShape(
                color = getColor(com.mxchip.mxapp.base.R.color.core_color_background_cancel),
                radius = 20
            )
        binding.deviceList.layoutManager = GridLayoutManager(this, 2)
        binding.deviceList.addItemDecoration(
            RVItemDecorationSpace(
                bottom = 10.dp2px(), right = 10.dp2px()
            )
        )
        binding.deviceList.adapter = adapter
        binding.etSearch.requestFocus()
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.setIconColor(mainColor)
    }

    private fun initEvent() {
        binding.etSearch.addTextChangedListener {
            val keyword = it.toString()
            binding.clearText.visibility = if (keyword.isEmpty()) View.GONE else View.VISIBLE
            searchResult(keyword)
        }

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.clearText.setOnClickListener {
            binding.etSearch.setText("")
        }
    }

    private fun inject() {
        //设备mesh数据
        LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA).observe(this)
        {
            meshMessageData(it)
        }
        //云端socket数据
        LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_CLOUD_DATA).observe(this)
        {
            val json = JSONObject(it)
            ilopMessageData(json)
        }
    }

    private var switchDialog: DeviceSwitchDialog? = null
    private val deviceList by lazy { mutableListOf<DeviceBean>() }

    private fun findDevice(pageNo: Int) {
        if (pageNo == 1) {
            showLoading()
            deviceList.clear()
        }
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
                    devices?.apply {
                        deviceList.addAll(this)
                    }
                    if (devices.isNullOrEmpty() || devices.size < 199) {
                        dismissLoading()
                    } else {
                        val next = pageNo + 1
                        findDevice(next)
                    }
                },
                error = { code, msg, _ ->
                    val netErr = onException(code)
                    if (!netErr) {
                        showToast("$msg")
                    }
                    dismissLoading()
                }
            )
        )
    }

    private fun itemClick(pos: Int) {
        when (val bean = adapter.getData()[pos]) {
            is DeviceBean -> {
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
                if (bean.nodes.isNotEmpty()) {
                    toGroupH5Panel(bean)
                }
            }
        }
    }

    private fun detailClick(pos: Int) {
        val data = adapter.getData()[pos]
        if (data is DeviceBean) {
            checkOtaVersion(data)
        } else if (data is GroupBean) {
            if (data.nodes.isNotEmpty()) {
                toGroupH5Panel(data)
            }
        }
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
                if (data is DeviceBean && data.uuid == uuid) {
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

    private fun searchResult(keyword: String) {
        val filter =
            if (keyword.isNotEmpty()) {
                deviceList.filter { device ->
                    device.nick_name.ifEmpty { device.product_name }.contains(keyword, true)
                }
            } else {
                listOf()
            }

        if (filter.isEmpty()) {
            binding.tvSearchResult.visibility = View.GONE
            binding.deviceList.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.tvSearchResult.visibility = View.VISIBLE
            binding.deviceList.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            adapter.setData(filter)
        }

    }
}