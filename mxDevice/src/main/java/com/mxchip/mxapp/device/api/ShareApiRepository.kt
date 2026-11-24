package com.mxchip.mxapp.device.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/16
 * @desc:
 */
class ShareApiRepository : BaseRepository<ShareApiService>(ShareApiService::class.java) {

    suspend fun shareDeviceList(
        map: Map<String, Int>
    ): NetStateResponse {//<BaseListWrapperData<ShareDeviceListBean>> {
//        val param = mapOf("type" to type, "home_id" to homeId, "page_size" to pageSize, "page_no" to pageNo)
        return stateApiCall { apiService.shareDeviceList(map) }
    }

    suspend fun shareUserList(
        param: Map<String, Any>
    ): NetStateResponse {//<BaseListWrapperData<ShareUserListBean>> {
//        val param = mapOf("iotid" to iotid, "page_size" to pageSize, "page_no" to pageNo)
        return stateApiCall { apiService.shareUserList(param) }
    }
//
//    suspend fun generateShareFamilyCode(
//        param: Map<String, Any>
//    ): NetStateResponse {//<GenerateShareCodeBean> {
////        val param = HashMap<String, Any>()
////        param["type"] = type
////        param["home_id"] = homeId
////        param["role"] = role
////        roomIds?.apply {
////            param["room_ids"] = this
////        }
//        return stateApiCall { apiService.generateShareFamilyCode(param) }
//    }

//    suspend fun setCurrentHome(param: Map<String, Int>): NetStateResponse {//<Any> {
//        return stateApiCall {
//            apiService.setCurrentHome(param)//mapOf("home_id" to homeId))
//        }
//    }
//
//    suspend fun shareFamilyByAccount(
//        param: Map<String, Any>
//    ): NetStateResponse {//<Any> {
////        val param = HashMap<String, Any>()
////        param["home_id"] = homeId
////        param["account"] = account
////        if (!area.isNullOrBlank()) param["area"] = area
////        param["role"] = role
////        roomIds?.apply {
////            param["room_ids"] = this
////        }
//        return stateApiCall { apiService.shareFamilyByAccount(param) }
//    }

    suspend fun shareDevice(
        param: Map<String, Any>
    ): NetStateResponse {//<Any> {
//        val param = HashMap<String, Any>()
//        param["iotids"] = iotIds
//        param["account"] = account
//        if (!area.isNullOrBlank()) param["area"] = area
        return stateApiCall { apiService.shareDevice(param) }
    }

    suspend fun cancelDeviceShare(param: Map<String, Any>): NetStateResponse {//<Any> {
//        val param = mapOf("iotid" to iotid, "user_ids" to user_ids)
        return stateApiCall { apiService.cancelDeviceShare(param) }
    }

    suspend fun unbindShareDevice(param: Map<String, Any>): NetStateResponse {//<Any> {
//        val param = mapOf("iotids" to iotids)
        return stateApiCall { apiService.unbindShareDevice(param) }
    }
}
