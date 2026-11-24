package com.mxchip.mxapp.device.vm

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import com.mxchip.http.response.NetState
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.base.base.BaseDefaultDataSource
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.device.api.DeviceRepository
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class DeviceViewModel : BaseViewModel() {

    private val repository by lazy {
        DeviceRepository()
    }

    val unBindState by lazy { MutableLiveData<NetStateResponse>() }

    fun findDevices(homeId: Int): Flow<PagingData<DeviceBean>> {
        return paging {
            BaseDefaultDataSource(DeviceBean::class.java) {
                repository.findDevices(
                    mapOf(
                        "home_id" to homeId,
                        "page_no" to it,
                        "page_size" to 20
                    )
                )
            }
        }
    }

    fun findDevices(
        homeId: Int,
        roomId: Int?,
        productKey: String? = null
    ): Flow<NetStateResponse> {
        val params = mutableMapOf<String, Any>()
        params["home_id"] = homeId
        params["page_size"] = 199
        params["page_no"] = 1
        roomId?.apply {
            params["room_id"] = roomId
        }
        productKey?.apply {
            params["product_key"] = productKey
        }
        return launchFlow {
            repository.findDevices(params)
        }
    }

    fun findDevices(
        params: Map<String, Any>
    ): Flow<NetStateResponse> {
        return launchFlow {
            repository.findDevices(params)
        }
    }

    fun findShortcutDevice(homeId: Int, roomId: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.findShortcutDevice(
                mapOf(
                    "home_id" to homeId,
                    "room_id" to roomId,
                    "page_size" to 199
                )
            )
        }
    }

    fun setProperties(
        iotId: String,
        ilopParams: Map<String, Any>
    ): Flow<NetStateResponse> {
        return launchFlow {
            repository.setProperties(mapOf("iotid" to iotId, "items" to ilopParams))
        }
    }

    fun getDeviceProperties(iotId: String) {
        launchMain {
            val response = repository.getAllProperties(mapOf("iotid" to iotId))
            if (NetState.Companion.SUCCESS == response.state) {
                val map = mutableMapOf<String, Any>()
                map["iotId"] = iotId
                map["items"] = JSONObject(response.data.toString())
                LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_CLOUD_DATA)
                    .postValue(map.toString())
            }
        }
    }

    fun getPanelInfoFlow(pk: String): Flow<NetStateResponse> {
        return launchFlow { repository.getPanelInfo(mapOf("product_key" to pk, "v3" to 1)) }
    }

    fun unBindDevice(iotIds: List<String>) {
        launchMain {
            launchFlowUnit(unBindState) {
                repository.unBindDevice(mapOf("iotids" to iotIds))
            }
        }
    }

    fun unBindSharedDevice(iotIds: List<String>) {
        launchMain {
            launchFlowUnit(unBindState) {
                repository.unBindSharedDevice(mapOf("iotids" to iotIds))
            }
        }
    }

    fun getDeviceInfo(iotId: String): Flow<NetStateResponse> {
        return launchFlow { repository.getDeviceInfo(iotId) }
    }

    fun setDeviceRoom(iotId: String, roomId: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.updateDeviceLocation(
                mapOf(
                    "room_id" to roomId,
                    "iotid" to iotId
                )
            )
        }
    }

    fun setDeviceName(iotId: String, nickname: String): Flow<NetStateResponse> {
        return launchFlow {
            repository.setDeviceName(
                mapOf(
                    "iotid" to iotId,
                    "nickname" to nickname
                )
            )
        }
    }

    /**
     * 设置设备是否主页显示
     */
    fun setDeviceShow(iotId: String, show: Boolean) {
        launchMain {
            repository.setDeviceShow(mapOf("iotid" to iotId, "show" to show))
        }
    }

    fun setCommonShow(list: List<Map<String, Any>>): Flow<NetStateResponse> {
        return launchFlow {
            //"show_items": [
            //    {
            //      "obj_type": "scene",
            //      "obj_id": "17141",
            //      "show": false
            //    },
            //    {
            //      "obj_type": "device",
            //      "obj_id": "BmrjRYn841h4B8bHPXfR000100",
            //      "show": false
            //    },
            //    {
            //      "obj_type": "group",
            //      "obj_id": "2261",
            //      "show": true
            //    },
            //  ]
            repository.setCommonShow(mapOf("show_items" to list))
        }
    }

    fun updateRoomDevice(
        roomId: Int,
        deviceIds: List<Map<String, Any>>,
    ): Flow<NetStateResponse> {
        val map = mapOf(
            "room_id" to roomId,
            "devices" to deviceIds,
        )
        return launchFlow { repository.updateRoomDevice(map) }
    }

    fun updateOTAVersion(iotid: String, version: String): Flow<NetStateResponse> {
        return launchFlow {
            repository.updateOTAVersion(
                mapOf(
                    "iotid" to iotid,
                    "version" to version
                )
            )
        }
    }

    fun getOtaUpgradeInfoV3(iotId: String, mcuModule: String? = null): Flow<NetStateResponse> {
        return launchFlow {
            val params = mutableMapOf("iotid" to iotId)
            mcuModule?.apply {
                params["module"] = mcuModule
            }
            repository.getOtaUpgradeInfoV3(params)
        }
    }

    fun hasOtaUpgrade(homeId: Int): Flow<NetStateResponse> {
        return launchFlow { repository.hasOtaUpgrade(mapOf("home_id" to homeId)) }
    }

    fun sendCloudMeshMessage(params: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.sendCloudMeshMessage(params) }
    }

    fun getRecoveryStatus(params: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.getRecoveryStatus(params) }
    }

    fun findRecoveryDeviceList(params: Map<String, Int>): Flow<NetStateResponse> {
        return launchFlow { repository.findRecoveryDeviceList(params) }
    }

    fun startHomeRecovery(homeId: Int): Flow<NetStateResponse> {
        return launchFlow { repository.startHomeRecovery(mapOf("home_id" to homeId)) }
    }

    fun getRecoveryDeviceRule(params: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.getRecoveryDeviceRule(params) }
    }

    fun updateRecoveryDeviceWriteState(params: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.updateRecoveryDeviceWriteState(params) }
    }

    fun recoveryCompleted(params: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.recoveryCompleted(params) }
    }

    fun roomListV4(
        homeId: Int,
        pageNo: Int,
        pageSize: Int = 20
    ): Flow<NetStateResponse> {
        return launchFlow {
            repository.roomListV4(
                mapOf(
                    "home_id" to homeId,
                    "page_no" to pageNo,
                    "page_size" to pageSize
                )
            )
        }
    }

    fun updateMasterGateway(homeId: Int, iotId: String): Flow<NetStateResponse> {
        //home_id	是	Body	int	家庭id
        //iotid	是	Body	string	网关 iotid
        //is_main	否	Body	int	是否是主网关：0 - 从网关; 1 - 主网关；
        //network_mode	否	Body	int	联网方式：0- 使用以太网络；1- 使用WiFi网络
        return launchFlow {
            repository.updateMasterGateway(
                mapOf(
                    "home_id" to homeId,
                    "iotid" to iotId,
                    "is_main" to 1
                )
            )
        }
    }

    fun getShortcutDeviceInfo(iotId: String, number: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.getShortcutDeviceInfo(mapOf("iotid" to iotId, "number" to number))
        }
    }

    fun getDeviceShortcutConfig(iotId: String, number: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.getDeviceShortcutConfig(mapOf("iotid" to iotId, "number" to number))
        }
    }

    fun updateDeviceShortcutConfig(
        iotId: String,
        number: Int,
        shortcutIdentifier: List<String>,
        roomId: Int,
        nickname: String,
        enable: Boolean
    ): Flow<NetStateResponse> {
        return launchFlow {
            repository.updateDeviceShortcutConfig(
                mapOf(
                    "iotid" to iotId,
                    "number" to number,
                    "shortcut_identifier" to shortcutIdentifier,
                    "room_id" to roomId,
                    "nickname_identifier" to nickname,
                    "enable" to if (enable) 1 else 0
                )
            )
        }
    }

    fun updateDeviceIdentifierName(
        iotId: String,
        identifier: String,
        nickname: String,
    ): Flow<NetStateResponse> {
        return launchFlow {
            repository.updateDeviceIdentifierName(
                mapOf(
                    "iotid" to iotId,
                    "identifier" to identifier,
                    "name" to nickname,//自定义的名称 长度：1-32
                )
            )
        }
    }

    fun findDeviceAlarms(homeId: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.findDeviceAlarms(homeId)
        }
    }

    fun getProductUseManual(productKey: String): Flow<NetStateResponse> {
        return launchFlow {
            repository.getProductUseManual(productKey)
        }
    }
}