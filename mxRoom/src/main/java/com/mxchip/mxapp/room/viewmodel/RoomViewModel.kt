package com.mxchip.mxapp.room.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.room.api.RoomRepository
import kotlinx.coroutines.flow.Flow

class RoomViewModel : BaseViewModel() {
    private val repository by lazy {
        RoomRepository()
    }

    val deleteRoomState by lazy { MutableLiveData<NetStateResponse>() }

    fun roomListV4(param: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.roomListV4(param) }
    }

    fun createRoom(homeId: Int, name: String): Flow<NetStateResponse> {
        return launchFlow {
            val param = HashMap<String, Any>()
            param["home_id"] = homeId
            param["name"] = name
//            if (!bgColor.isNullOrBlank()) param["bg_color"] = bgColor
            repository.createRoom(param)
        }
    }

    fun updateRoom(roomId: Int, name: String): Flow<NetStateResponse> {
        return launchFlow {
            val param = HashMap<String, Any>()
            param["room_id"] = roomId
            param["name"] = name
            repository.updateRoom(param)
        }
    }

    fun deleteRoom(roomId: Int) {
        launchMain {
            launchFlowUnit(deleteRoomState) {
                val param = mapOf("room_id" to roomId)
                repository.deleteRoom(param)
            }
        }
    }

    fun orderRoom(homeId: Int, rooms: List<Int>): Flow<NetStateResponse> {
        val param = mapOf("home_id" to homeId, "rooms" to rooms)
        return launchFlow { repository.orderRoom(param) }
    }

    fun roomRecommend(): Flow<NetStateResponse> {
        return launchFlow { repository.roomRecommend() }
    }

    fun getUserInfo(): Flow<NetStateResponse> {
        return launchFlow {
            repository.getUserInfo()
        }
    }

    fun updateMember(
        home_id: Int,
        member_id: Int,
        name: String?,
        role: Int?,
        roomList: List<Int>?
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["home_id"] = home_id
        param["member_id"] = member_id
        if (!name.isNullOrBlank()) {
            param["name"] = name
        }
        role?.apply {
            param["role"] = this
        }
        if (!roomList.isNullOrEmpty()) {
            param["room_ids"] = roomList
        }
        return launchFlow {
            repository.updateMember(param)
        }
    }

    fun generateInviteCode(
        type: Int,
        homeId: Int,
        role: Int = 2,
        roomIds: List<Int>? = null
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["type"] = type
        param["home_id"] = homeId
        param["role"] = role
        roomIds?.apply {
            param["room_ids"] = this
        }
        return launchFlow { repository.generateShareFamilyCode(param) }
    }

    fun shareFamilyByAccount(
        homeId: Int,
        account: String,
        area: String?,
        role: Int = 2,
        roomIds: List<Int>? = null
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["home_id"] = homeId
        param["account"] = account
        if (!area.isNullOrBlank()) param["area"] = area
        param["role"] = role
        roomIds?.apply {
            param["room_ids"] = this
        }
        return launchFlow { repository.shareFamilyByAccount(param) }
    }

    fun deleteGroup(groupIds: List<Int>): Flow<NetStateResponse> {
        return launchFlow {
            repository.delGroups(mapOf("group_ids" to groupIds))
        }
    }
}