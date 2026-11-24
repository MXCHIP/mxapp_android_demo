package com.mxchip.mxapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.mxchip.mxapp.adapter.SmartHomeAdapter
import com.mxchip.mxapp.base.base.MXBusinessFragment
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.ISmartService
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.bean.SmartHomeRoomBean
import com.mxchip.mxapp.consts.SmartHomeConstants
import com.mxchip.mxapp.databinding.FragmentSmarthomeBinding
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.utils.AppDataCenter
import com.mxchip.mxapp.viewmodel.MainViewModel
import com.mxchip.router.MxRouter
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 全屋2.0
 */
class SmartHomeFragment : MXBusinessFragment<FragmentSmarthomeBinding>(), IRoomRefresh {

    private val mainVM by viewModels<MainViewModel>()

    private val cacheRoomIdList by lazy { mutableListOf<Int>() }

    private val homeDataAdapter by lazy {
        SmartHomeAdapter(
            clickRoom = { roomId ->
                gotoRoomTabPosition(roomId)
            },
            clickSensor = { roomId, iotId, identifier ->
                //普通成员无法进入设置
                if (RoleManager.isUser()) return@SmartHomeAdapter
                gotoSettingRoomSensorPage(roomId, iotId, identifier)
            }, clickScene = { sceneId, _ ->
                executeScene(sceneId)
            }, clickDevice = { roomId, identifiers, value ->
                setDeviceProperties(roomId, identifiers, value)
            }
        )
    }

    override fun getLayoutBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentSmarthomeBinding {
        return FragmentSmarthomeBinding.inflate(inflater)
    }

    override fun onInit(view: View, savedInstanceState: Bundle?) {
        initView()
        inject()
        //是否读取过全屋引导页面，已读过再刷新房间数据
        if (AppDataCenter.isReadSmartHomeGuide()) {
            onRefresh(null)
        }
    }

    private fun initView() {
        binding.rvDevice.layoutManager = GridLayoutManager(context, 2).apply {
            this.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val data = homeDataAdapter.getData()[position]
                    return if (data.properties.size > 1
                        || data.scenes.isNotEmpty()
                        ||
                        (
                                (data.light.isNotEmpty() && data.air_conditoner.isNotEmpty()) ||
                                        (data.light.isNotEmpty() && data.curtain.isNotEmpty()) ||
                                        (data.air_conditoner.isNotEmpty() && data.curtain.isNotEmpty())
                                )
                    ) {
                        2
                    } else {
                        1
                    }
                }
            }
        }
        binding.rvDevice.addItemDecoration(
            RVItemDecorationSpace(bottom = 10.dp2px(), right = 10.dp2px())
        )
        binding.rvDevice.adapter = homeDataAdapter
    }

    private fun inject() {
        LiveDataBus.with<Int>(SmartHomeConstants.KEY_REFRESH_SMART_HOME).observe(this) {
            onRefresh(null)
        }
    }

    override fun onRefresh(refreshCompleteInvoke: (() -> Unit)?) {
        mainVM.findSmartHomeRoomList(AppConfigManager.getUseHomeId()).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlowList<SmartHomeRoomBean>(
                success = {
                    refreshCompleteInvoke?.invoke()
                    it?.filter { hd ->
                        cacheRoomIdList.add(hd.room_id)
                        hd.properties.isNotEmpty() || hd.scenes.isNotEmpty() || hd.light.isNotEmpty() || hd.air_conditoner.isNotEmpty() || hd.curtain.isNotEmpty()
                    }?.apply {
                        homeDataAdapter.setData(this)
                    }
                    binding.emptyView.visibility =
                        if (homeDataAdapter.getData().isEmpty()) View.VISIBLE else View.GONE
                },
                error = { code, msg, _ ->
                    val netErr = onException(code)
                    if (!netErr) {
                        showToast("$msg")
                    }
                    refreshCompleteInvoke?.invoke()
                }
            )
        )
    }

    private fun gotoRoomTabPosition(roomId: Int) {
        val position = cacheRoomIdList.indexOf(roomId)
        //全屋 index 0，房间TAB的真实位置+1
        LiveDataBus.with<Int>(SmartHomeConstants.KEY_SWITCH_ROOM_TAB).postValue(position + 1)
    }

    private fun gotoSettingRoomSensorPage(roomId: Int, iotId: String?, identifier: String) {
        RouterManager.navigation(
            RouterConstants.SMART_HOME_SENSOR_SETTING_ACTIVITY, mapOf(
                RoomConstants.KEY_ROOM_ID to roomId,
                CommonConstants.KEY_IOT_ID to (iotId ?: ""),
                "identifier" to identifier
            )
        )
    }

    private fun executeScene(sceneId: Int) {
        if (sceneId != -1) {
            MxRouter.get<ISmartService>()?.executeScene(sceneId)
        }
    }

    private fun setDeviceProperties(roomId: Int, identifiers: List<String>, value: Int) {
        mainVM.setRoomProperties(roomId, identifiers, value).launchAndCollectIn(
            this,
            Lifecycle.State.CREATED,
            loadFlow<Any>(
                success = {},
                error = { code: Int, msg: String?, _: Any? ->
                    val netErr = onException(code, false)
                    if (!netErr) showToast("$msg")
                })
        )
    }

    override fun meshMessageData(dtMap: Map<String, Any>) {

    }

    override fun ilopMessageData(ilopData: JSONObject) {
        if (ilopData.has("iotId") && ilopData.has("items")) {
            val iotId = ilopData.getString("iotId")
            val itemJson = ilopData.getJSONObject("items")
            homeDataAdapter.getData().forEachIndexed { index, data ->
                val dataProps = data.properties.filter { it.iotid == iotId }
                var notify = false
                dataProps.forEach { prop ->
                    //"items":{"CurrentHumidity":{"value":28,"time":1739005054145},"CurrentTemperature":{"value":23.2,"time":1739005054145}}
                    itemJson.keys().forEach { itemJsonKey ->
                        if (itemJsonKey == prop.identifier) {
                            val switchJson = itemJson.getJSONObject(itemJsonKey)
                            if (switchJson.has("value")) {
                                notify = true
                                val newSwt = switchJson.get("value")
                                prop.value = newSwt
                                prop.online = true
                            }
                        }
                    }
                }
                if (notify) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        homeDataAdapter.notifyItemChanged(index)
                    }
                    return
                }
            }
        }
    }
}