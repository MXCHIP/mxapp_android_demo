package com.mxchip.mxapp.room.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/1
 * @desc:
 */
class RoomRepository : BaseRepository<RoomApi>(RoomApi::class.java) {

    suspend fun roomListV4(param: Map<String, Any>): NetStateResponse { //RoomListBean> {
        return stateApiCall { apiService.roomListV4(param) }
    }

    suspend fun createRoom(map: Map<String, Any>): NetStateResponse { //RoomCreateBean> {
        return stateApiCall { apiService.createRoom(map) }
    }

    suspend fun updateRoom(map: Map<String, Any>): NetStateResponse { //Any> {
        return stateApiCall { apiService.updateRoom(map) }
    }

    suspend fun deleteRoom(map: Map<String, Int>): NetStateResponse { //Any> {
        return stateApiCall { apiService.deleteRoom(map) }
    }

    suspend fun orderRoom(map: Map<String, Any>): NetStateResponse { //Any> {
        return stateApiCall { apiService.orderRoom(map) }
    }

    suspend fun roomRecommend(): NetStateResponse { //List<RoomRecommendBean>> {
        return stateApiCall { apiService.roomRecommend() }
    }

    suspend fun updateMember(params: Map<String, Any>): NetStateResponse { //Any> {
        return stateApiCall { apiService.updateMember(params) }
    }

    suspend fun getUserInfo(): NetStateResponse { //Map<String, Any>> {
        return stateApiCall { apiService.getUserInfo() }
    }

    suspend fun generateShareFamilyCode(
        param: Map<String, Any>
    ): NetStateResponse { //Map<String, Any>> {
        return stateApiCall { apiService.generateShareFamilyCode(param) }
    }

    suspend fun shareFamilyByAccount(
        param: Map<String, Any>
    ): NetStateResponse { //Any> {
        return stateApiCall { apiService.shareFamilyByAccount(param) }
    }

    suspend fun delGroups(data: Map<String, List<Int>>): NetStateResponse { //Any> {
        return stateApiCall { apiService.delGroups(data) }
    }
}