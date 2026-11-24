package com.mxchip.mxapp.device.api

import com.mxchip.mxapp.device.consts.CCSmartEnum
import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/14
 * @desc:
 */
class CCSmartRepository : BaseRepository<CCSmartApi>(CCSmartApi::class.java) {

    suspend fun getDeviceList(param: Map<String, Any>): NetStateResponse {//<BaseListWrapperData<BaseBean>> {
//        val map = mapOf(
//            "type" to type,
//            "iotid" to iotId,
//            "page_size" to 199,
//            "page_no" to pageNo
//        )
        return stateApiCall { apiService.getList(CCSmartEnum.DEVICE.value, param) }
    }

    suspend fun getSceneList(param: Map<String, Any>): NetStateResponse {//<BaseListWrapperData<BaseBean>> {
//        val map = mapOf(
//            "type" to type,
//            "iotid" to iotId,
//            "page_size" to 199,
//            "page_no" to pageNo
//        )
        return stateApiCall { apiService.getList(CCSmartEnum.SCENE.value, param) }
    }

    suspend fun getGroupList(param: Map<String, Any>): NetStateResponse {//<BaseListWrapperData<BaseBean>> {
//        val map = mapOf(
//            "type" to type,
//            "iotid" to iotId,
//            "page_size" to 199,
//            "page_no" to pageNo
//        )
        return stateApiCall { apiService.getList(CCSmartEnum.GROUP.value, param) }
    }

    suspend fun update(updateType: CCSmartEnum, param: Map<String, Any>): NetStateResponse {//<Any> {
//        val map = HashMap<String, Any>()
//        when(updateType) {
//            CCSmartEnum.DEVICE -> map["iotids"] = ids
//            CCSmartEnum.SCENE -> map["scene_ids"] = ids
//            CCSmartEnum.GROUP -> map["group_ids"] = ids
//        }
//        map["iotid"] = iotId
        return stateApiCall { apiService.update(updateType.value, param) }
    }
}