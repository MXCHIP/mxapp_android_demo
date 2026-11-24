package com.mxchip.mxapp.pair.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 *
 */
class AppPairRepository : BaseRepository<AppPairApiService>(AppPairApiService::class.java) {

    suspend fun getMeshAddress(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.getMeshAddress(params)
        }
    }

    suspend fun bindDevice(bindParams: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.bindDevice(bindParams)
        }
    }

    suspend fun roomListV4(map: Map<String, Int>): NetStateResponse {
        return stateApiCall { apiService.roomListV4(map) }
    }

    suspend fun modifyBatchDeviceInfo(map: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.modifyBatchDeviceInfo(map)
        }
    }

    suspend fun createRoom(
        map: Map<String, Any>
    ): NetStateResponse {
        return stateApiCall { apiService.createRoom(map) }
    }

    suspend fun findGroupsAndRulesByIotId(
        homeId: Int, iotId: String
    ): NetStateResponse {
        return stateApiCall {
            val param = mapOf("home_id" to homeId, "iotid" to iotId)
            apiService.findGroupsAndRulesByDevice(param)
        }
    }

    suspend fun getFogRandom(map: Map<String, Any>): NetStateResponse {//<Map<String, String>> {
        return stateApiCall {
            //0 - 设备单播地址；1 - 用户单播地址
            apiService.getFogRandom(map)
        }
    }

    suspend fun getFogBleKey(map: Map<String, Any>): NetStateResponse {//<Map<String, Any>> {
        return stateApiCall {
            //0 - 设备单播地址；1 - 用户单播地址
            apiService.getFogBleKey(map)
        }
    }

    suspend fun getFogConnectState(map: Map<String, Any>): NetStateResponse {//<BleConfigNetWorkBean> {
        return stateApiCall { apiService.getFogConnectState(map) }
    }

    suspend fun getAliCipher(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getAliCipher(param) }
    }


    suspend fun getPairGuideInfo(productKey: String): NetStateResponse {//<PairGuideInfo> {
        return stateApiCall { apiService.getPairGuideInfo(productKey) }
    }

    suspend fun restoreAPGateway(param: Map<String, String>): NetStateResponse {//<PairGuideInfo> {
        return stateApiCall { apiService.restoreAPGateway(param)}
    }

    suspend fun updateOTAVersion(params: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.updateOTAVersion(params) }
    }
}