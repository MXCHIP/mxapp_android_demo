package com.mxchip.mxapp.base.h5bridge

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.utils.JsonUtil

/**
 *
 */
class PanelBridgeRepository :
    BaseRepository<PanelBridgeAPIService>(PanelBridgeAPIService::class.java) {

    suspend fun fetchOwnServerGET(url: String, map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.fetchOwnServerGET(url, map) }
    }

    suspend fun fetchOwnServerPOST(url: String, data: String): NetStateResponse {
        return stateApiCall { apiService.fetchOwnServerPOST(url, JsonUtil.toRequestBody(data)) }
    }

    suspend fun fetchOwnServerPUT(url: String, data: String): NetStateResponse {
        return stateApiCall { apiService.fetchOwnServerPUT(url, JsonUtil.toRequestBody(data)) }
    }

    suspend fun fetchOwnServerDELETE(
        url: String,
        data: String
    ): NetStateResponse{
        return stateApiCall { apiService.fetchOwnServerDELETE(url, JsonUtil.toRequestBody(data)) }
    }
}