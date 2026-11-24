package com.mxchip.mxapp.pair.viewmodel

import com.mxchip.mxapp.pair.api.ProductRepository
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import kotlinx.coroutines.flow.Flow


/**
 *产品相关
 */
class PairProductViewModel : BaseViewModel() {

    private val repository by lazy {
        ProductRepository()
    }

    /**
     * 获取云端所有产品
     */
    fun findProducts(): Flow<NetStateResponse> {
        return launchFlow {
            repository.findProduct()
        }
    }
}