package com.mxchip.mxapp.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.mxchip.mxapp.device.api.DeviceRepository
import com.mxchip.http.response.NetState
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.base.base.BaseDefaultDataSource
import com.mxchip.mxapp.bean.RecommendSceneBean
import com.mxchip.mxapp.family.api.HomeRepository
import com.mxchip.mxapp.pair.api.ProductRepository
import com.mxchip.mxapp.api.MainRepository
import com.mxchip.sdk.mesh.MXMesh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.ZoneId

/**
 *
 */
class MainViewModel : BaseViewModel() {

    private val repository by lazy { MainRepository() }

    private val productRepository by lazy {
        ProductRepository()
    }

    private val homeRepository by lazy {
        HomeRepository()
    }

    private val deviceRepository by lazy {
        DeviceRepository()
    }

    private val _importMeshState by lazy { MutableStateFlow<Long?>(null) }
    val importMeshState: StateFlow<Long?> = _importMeshState

    fun getStartupAD(): Flow<NetStateResponse> {
        return launchFlow { repository.getStartupAD() }
    }

    fun findHomeAttrConfig(): Flow<NetStateResponse> {
        return launchFlow { repository.findHomeAttrConfig() }
    }

    fun getWebSocketInfo(): Flow<NetStateResponse> {
        return launchFlow { repository.getWebSocket() }
    }

    fun getMQTTInfo(): Flow<NetStateResponse> {
        return launchFlow { repository.getMQTT() }
    }

    fun readPrivacy(docId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val param = mapOf("doc_id" to docId)
            repository.readPrivacy(param)
        }
    }

    fun privacyChange(): Flow<NetStateResponse> {
        return launchFlow { repository.privacyChange() }
    }

    fun fastLogin(
        mobToken: String,
        opToken: String,
        operator: String,
        clientid: String? = null
    ): Flow<NetStateResponse> {
        return launchFlow {
            val param = mutableMapOf(
                "mob_token" to mobToken,
                "op_token" to opToken,
                "operator" to operator
            )
            clientid?.apply {
                param["clientid"] = clientid
            }
            repository.fastLogin(param)
        }
    }

    fun userStat() {
        launchMain {
            repository.userStat()
        }
    }

    fun logout(): Flow<NetStateResponse> {
        return launchFlow { repository.logout() }
    }

    fun checkAppVersionFlow(): Flow<NetStateResponse> {
        return launchFlow { repository.checkAppUpgrade() }
    }

    fun updateTimezone() {
        val zoneId = ZoneId.systemDefault()
        viewModelScope.launch(Dispatchers.IO) {
            launchFlow { repository.updateUserInfo(mapOf("timezone" to zoneId.id)) }.collect { }
        }
    }

    fun setMeshSeq(homeId: Int, currentSeq: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setMeshSeq(mapOf("home_id" to homeId, "sequence_number" to currentSeq))
        }
    }

    fun getMeshInfo(homeId: Int) {
        launchMain {
            val addressRes = async {
                //0 - 设备单播地址；1 - 用户单播地址
                repository.getMeshAddress(mapOf("home_id" to homeId, "type" to 1))
            }
            val seqRes = async {
                repository.getMeshSeq(mapOf("home_id" to homeId))
            }
            val configRes = async {
                repository.getHomeMeshConfig(mapOf("home_id" to homeId))
            }
            //mesh本机地址
            val addressAwait = addressRes.await()
            if (NetState.SUCCESS == addressAwait.state) {
                addressAwait.data?.apply {
                    val json = JSONObject(this)
                    if (json.has("mesh_address")) {
                        val address = json.getInt("mesh_address")
                        //设置本机地址
                        MXMesh.setProvisionerUnicastAddress(address)
                    }
                }
            }
            //mesh当前sequence
            val seqAwait = seqRes.await()
            if (NetState.SUCCESS == seqAwait.state) {
                seqAwait.data?.apply {
                    val json = JSONObject(this)
                    if (json.has("sequence_number")) {
                        val seq = json.getInt("sequence_number") + 200
                        MXMesh.setMeshMessageSequence(seq)
                    }
                }
            }
            //mesh配置
            val configAwait = configRes.await()
            if (NetState.SUCCESS == configAwait.state) {
                configAwait.data?.apply {
                    MXMesh.importMeshNetwork(this) {
                        _importMeshState.value = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    /**
     * 获取云端所有产品
     */
    fun findProducts(): Flow<NetStateResponse> {
        return launchFlow {
            productRepository.findProduct()
        }
    }

    fun updateSmartHomeModel(homeId: Int, isShowSmartHome: Boolean): Flow<NetStateResponse> {
        return launchFlow {
            homeRepository.updateHome(
                mapOf(
                    "home_id" to homeId,
                    "show_whole_home" to isShowSmartHome
                )
            )
        }
    }

    fun findSharedDevices(size: Int): Flow<NetStateResponse> {
        return launchFlow {
            deviceRepository.findSharedDevices(
                mapOf(
                    "page_size" to size,
                    "page_no" to 1,
                    "type" to 1
                )
            )
        }
    }

    fun setGroupProperties(
        groupId: Int,
        ilopParams: Map<String, Any>
    ): Flow<NetStateResponse> {
        return launchFlow {
            //{
            //  "group_id": 1,
            //  "items": {
            //    "LightSwitch1": 0,
            //    "LightSwitch2": 1
            //  }
            //}
            deviceRepository.setProperties(mapOf("group_id" to groupId, "items" to ilopParams))
        }
    }

    fun setProperties(
        iotId: String,
        ilopParams: Map<String, Any>
    ): Flow<NetStateResponse> {
        return launchFlow {
            deviceRepository.setProperties(mapOf("iotid" to iotId, "items" to ilopParams))
        }
    }

    fun getPanelInfo(pk: String): Flow<NetStateResponse> {
        return launchFlow { deviceRepository.getPanelInfo(mapOf("product_key" to pk, "v3" to 1)) }
    }


    fun getOtaUpgradeInfo(iotId: String): Flow<NetStateResponse> {
        return launchFlow { deviceRepository.getOtaUpgradeInfoV3(mapOf("iotid" to iotId)) }
    }

    fun updateOTAVersion(iotId: String, version: String): Flow<NetStateResponse> {
        return launchFlow {
            deviceRepository.updateOTAVersion(
                mapOf(
                    "iotid" to iotId,
                    "version" to version
                )
            )
        }
    }

    fun findSmartHomeRoomList(homeId: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.findSmartHomeRoomList(homeId)
        }
    }

    /**
     * @param type 类型：0-温度；1 - 湿度；2 - pm2.5;  3 - 人存
     */
    fun findRoomSensorList(roomId: Int, type: Int): Flow<NetStateResponse> {
        return launchFlow {
            repository.findRoomSensorList(mapOf("room_id" to roomId, "type" to type))
        }
    }

    fun settingRoomUseSensor(roomId: Int, type: Int, iotId: String): Flow<NetStateResponse> {
        return launchFlow {
            repository.settingRoomUseSensor(
                mapOf(
                    "room_id" to roomId,
                    "iotid" to iotId,
                    "type" to type
                )
            )
        }
    }

    fun setRoomProperties(
        roomId: Int,
        identifiers: List<String>,
        value: Int
    ): Flow<NetStateResponse> {
        return launchFlow {
            val items = mutableMapOf<String, Int>()
            identifiers.forEach {
                items[it] = value
            }
            repository.setRoomProperties(
                mapOf(
                    "room_id" to roomId,
                    "items" to items
                )
            )
        }
    }


    fun getRecommendSceneList(): Flow<PagingData<RecommendSceneBean>> {
        return paging {
            BaseDefaultDataSource(RecommendSceneBean::class.java) {
                repository.getRecommendSceneList(
                    mapOf(
                        "page_size" to 20,
                        "page_no" to it,
                    )
                )
            }
        }
    }
}