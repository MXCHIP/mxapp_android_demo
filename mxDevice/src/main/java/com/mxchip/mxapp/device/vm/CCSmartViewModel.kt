package com.mxchip.mxapp.device.vm

import com.mxchip.mxapp.device.api.CCSmartRepository
import com.mxchip.mxapp.device.consts.CCSmartEnum
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import kotlinx.coroutines.flow.Flow

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/14
 * @desc:
 */
class CCSmartViewModel : BaseViewModel() {

    private val repository by lazy { CCSmartRepository() }

    fun getDeviceList(
        iotId: String,
        type: Int
    ): Flow<NetStateResponse> {
        return launchFlow {
            val map = mapOf(
                "type" to type,
                "iotid" to iotId,
                "page_size" to 199,
                "page_no" to 1
            )
            repository.getDeviceList(map)
        }
    }

    fun getSceneList(
        iotId: String,
        type: Int
    ): Flow<NetStateResponse> {
        return launchFlow {
            val map = mapOf(
                "type" to type,
                "iotid" to iotId,
                "page_size" to 199,
                "page_no" to 1
            )
            repository.getSceneList(map)
        }
    }

    fun getGroupList(
        iotId: String,
        type: Int
    ): Flow<NetStateResponse> {
        return launchFlow {
            val map = mapOf(
                "type" to type,
                "iotid" to iotId,
                "page_size" to 199,
                "page_no" to 1
            )
            repository.getGroupList(map)
        }
    }

    fun update(
        updateType: CCSmartEnum,
        iotId: String,
        ids: List<Any>
    ): Flow<NetStateResponse> {
        return launchFlow {
            val map = HashMap<String, Any>()
            when (updateType) {
                CCSmartEnum.DEVICE -> map["iotids"] = ids
                CCSmartEnum.SCENE -> map["scene_ids"] = ids
                CCSmartEnum.GROUP -> map["group_ids"] = ids
            }
            map["iotid"] = iotId
            repository.update(updateType, map)
        }
    }
}