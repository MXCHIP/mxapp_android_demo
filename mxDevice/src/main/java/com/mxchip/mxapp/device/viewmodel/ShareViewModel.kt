package com.mxchip.mxapp.device.viewmodel

import androidx.paging.PagingData
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.base.base.BaseDefaultDataSource
import com.mxchip.mxapp.device.api.ShareApiRepository
import com.mxchip.mxapp.device.bean.ShareDeviceListBean
import com.mxchip.mxapp.device.bean.ShareUserListBean
import kotlinx.coroutines.flow.Flow


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/16
 * @desc:
 */
class ShareViewModel : BaseViewModel() {
    private val repository by lazy {
        ShareApiRepository()
    }

    fun fetchShareDeviceList(type: Int, homeId: Int): Flow<PagingData<ShareDeviceListBean>> {
        return paging {
            BaseDefaultDataSource(ShareDeviceListBean::class.java) {
                val param =
                    mapOf("type" to type, "home_id" to homeId, "page_no" to it, "page_size" to 20)
                repository.shareDeviceList(param)
            }
        }
    }

    fun shareDevices(
        iotids: List<String>,
        account: String,
    ): Flow<NetStateResponse> {
        return launchFlow {
            val param = HashMap<String, Any>()
            param["iotids"] = iotids
            param["account"] = account
            repository.shareDevice(param)
        }
    }

    fun fetchSharedUserList(iotId: String): Flow<PagingData<ShareUserListBean>> {
        return paging {
            BaseDefaultDataSource(ShareUserListBean::class.java) {
                val param = mapOf("iotid" to iotId, "page_size" to 20, "page_no" to it)
                repository.shareUserList(param)
            }
        }
    }

    fun mineDeviceRemoveSharedUsers(
        iotId: String,
        userIds: List<Int>
    ): Flow<NetStateResponse> {
        val param = mapOf("iotid" to iotId, "user_ids" to userIds)
        return launchFlow { repository.cancelDeviceShare(param) }
    }

    fun unbindShareDevice(iotids: List<String>): Flow<NetStateResponse> {
        val param = mapOf("iotids" to iotids)
        return launchFlow { repository.unbindShareDevice(param) }
    }

//    fun generateInviteCode(
//        type: Int,
//        homeId: Int,
//        role: Int = 2,
//        roomIds: List<Int>? = null
//    ): Flow<NetStateResponse> {
//        val param = HashMap<String, Any>()
//        param["type"] = type
//        param["home_id"] = homeId
//        param["role"] = role
//        roomIds?.apply {
//            param["room_ids"] = this
//        }
//        return launchFlow { repository.generateShareFamilyCode(param) }
//    }

//    fun shareFamilyByAccount(
//        homeId: Int,
//        account: String,
//        area: String?,
//        role: Int = 2,
//        roomIds: List<Int>? = null
//    ): Flow<NetStateResponse> {
//        val param = HashMap<String, Any>()
//        param["home_id"] = homeId
//        param["account"] = account
//        if (!area.isNullOrBlank()) param["area"] = area
//        param["role"] = role
//        roomIds?.apply {
//            param["room_ids"] = this
//        }
//        return launchFlow { repository.shareFamilyByAccount(param) }
//    }

//
//    fun setCurrentHome(homeId: Int): Flow<NetStateResponse> {
//        val param = mapOf("home_id" to homeId)
//        return launchFlow { repository.setCurrentHome(param) }
//    }
}