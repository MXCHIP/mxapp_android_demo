package com.mxchip.mxapp.family.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 *
 */
class HomeRepository : BaseRepository<HomeAPIService>(HomeAPIService::class.java) {

    suspend fun findHome(params: Map<String, Int>): NetStateResponse {
        return stateApiCall {
            apiService.findHomes(params)
        }
    }

    suspend fun getHomeInfo(homeId: Int): NetStateResponse {
        return stateApiCall {
            apiService.getHomesInfo(homeId)
        }
    }

    suspend fun setCurrentHome(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.setCurrentHome(params)
        }
    }

    suspend fun createFamily(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.createHome(params) }
    }

    suspend fun createHomeSetDefaultRoom(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.createHomeSetDefaultRoom(params) }
    }

    suspend fun deleteFamily(params: Map<String, Int>): NetStateResponse {
        return stateApiCall { apiService.deleteFamily(params) }
    }

    suspend fun updateHome(param: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.updateHome(param)
        }
    }

    suspend fun memberList(params: Map<String, Int>): NetStateResponse {
        return stateApiCall { apiService.memberList(params) }
    }

    suspend fun removeFamilyMember(params: Map<String, Int>): NetStateResponse {
        return stateApiCall { apiService.removeFamilyMember(params) }
    }

    suspend fun updateMember(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.updateMember(params) }
    }

    suspend fun bindShareFamilyByCode(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.bindShareFamilyByCode(params) }
    }

    suspend fun fetchLocationByKeyword(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.fetchLocationByKeyword(params) }
    }

    suspend fun geoToLocation(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.geoToLocation(params) }
    }

    suspend fun getWeatherOneHour(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getWeatherOneHour(params) }
    }

    suspend fun handOverCode(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.handOverCode(map) }
    }

    suspend fun handOver(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.handOver(map) }
    }

    suspend fun handOverByAccount(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.handOverByAccount(map) }
    }

    suspend fun shareInProgress(map: Map<String, Any>): NetStateResponse {//<BaseListWrapperData<ShareInProgressBean>> {
        return stateApiCall { apiService.shareInProgress(map) }
    }

    suspend fun shareCancel(param: Map<String, Int>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.shareCancel(param) }//mapOf("share_id" to id)) }
    }


    suspend fun checkAccountExist(params: Map<String, Any>): NetStateResponse {//<CheckAccountExistBean> {
        return stateApiCall { apiService.checkAccountExist(params) }
    }
}