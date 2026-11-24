package com.mxchip.mxapp.utils

import com.mxchip.utils.LiveDataBus
import com.mxchip.utils.WebSocketMange
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.utils.AppConfigManager

import org.json.JSONObject
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshState

object CloudMessageUtil {

    fun parseMsg(msg: String) {
        val json = JSONObject(msg)
        if (json.has("mxMessageType")) {
            //服务相关消息
            serviceMsg(json)
        } else if (json.has("identifier")) { // 物模型事件上报
            LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_WARN_INFO).postValue(msg)
        } else {
            //设备相关消息
            LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_CLOUD_DATA)
                .postValue(msg)
        }
    }

    private fun serviceMsg(json: JSONObject) {
        val dataJson = json.getJSONObject("data")
        if (dataJson.has("homeId")) {
            val msgHomeId = dataJson.getInt("homeId")
            if (msgHomeId != AppConfigManager.getUseHomeId())
                return
        }
        when (json.getString("mxMessageType")) {
            WebSocketMange.MsgType.DEVICE_UNBIND.code -> {
                if (dataJson.has("UUID")) {
                    val UUID = dataJson.getString("UUID")
                    if (UUID.isNotEmpty()) {
                        MXMesh.getNode(UUID)?.apply {
                            MXMesh.resetNode(this)
                        }
                    }
                }
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.DEVICE)
            }

            WebSocketMange.MsgType.GROUP_ADD.code,
            WebSocketMange.MsgType.GROUP_DELETE.code -> {
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.DEVICE)
            }

            WebSocketMange.MsgType.SHARE_DEVICE_UNBIND.code -> {
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.SHARED_DEVICE)
            }

            WebSocketMange.MsgType.DEVICE_BIND.code -> {
                val nodes = dataJson.getJSONArray("nodes")
                if (nodes.length() > 0) {
                    MXMesh.importMeshNetwork(dataJson.toString()) {
                        if (it == MeshState.SUCCESS)
                            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                                .postValue(HomeDataRefresh.DEVICE)
                    }
                } else
                    LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                        .postValue(HomeDataRefresh.DEVICE)
            }

            WebSocketMange.MsgType.HOME_QUIT.code,
            WebSocketMange.MsgType.HOME_ROLE_CHANGE.code -> {
                LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
            }

            WebSocketMange.MsgType.ROOM_ADD.code,
            WebSocketMange.MsgType.ROOM_DELETE.code -> {
                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                    .postValue(HomeDataRefresh.ROOM_LIST)
            }
        }
    }
}