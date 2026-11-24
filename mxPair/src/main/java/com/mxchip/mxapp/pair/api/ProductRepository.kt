package com.mxchip.mxapp.pair.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 *
 */
class ProductRepository : BaseRepository<ProductApiService>(ProductApiService::class.java) {

    suspend fun findProduct(): NetStateResponse {//<BaseListWrapperData<FirstCategory>> {
        return stateApiCall {
            apiService.findProduct()
        }
    }
}