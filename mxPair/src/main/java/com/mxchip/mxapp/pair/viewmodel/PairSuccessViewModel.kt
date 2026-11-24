package com.mxchip.mxapp.pair.viewmodel

import com.mxchip.mxapp.pair.api.AppPairRepository
import com.mxchip.mxapp.pair.bean.PairSuccessDevice
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import kotlinx.coroutines.flow.Flow


/**
 *配网成功
 */
class PairSuccessViewModel : BaseViewModel() {

    private val repository by lazy {
        AppPairRepository()
    }

    fun roomListV4(homeId: Int, pageNo: Int, pageSize: Int = 20): Flow<NetStateResponse> {
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

    fun modifyDeviceInfo(
        homeId: Int,
        successDeviceList: List<PairSuccessDevice>
    ): Flow<NetStateResponse> {
        return launchFlow {
            repository.modifyBatchDeviceInfo(
                mapOf(
                    "home_id" to homeId,
                    "device" to successDeviceList
                )
            )
        }
    }

    fun createRoom(
        homeId: Int,
        name: String,
        bgColor: String? = null
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["home_id"] = homeId
        param["name"] = name
        if (!bgColor.isNullOrBlank()) param["bg_color"] = bgColor
        return launchFlow { repository.createRoom(param) }
    }
}