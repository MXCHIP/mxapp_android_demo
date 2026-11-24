package com.mxchip.mxapp.room.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.QueryMap

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/1
 * @desc:
 */
interface RoomApi {

    /**
     * 房间列表
     */
    @GET("/app/v4/room/list")
    suspend fun roomListV4(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    /**
     * 新建房间
     */
    @POST("/app/v3/room/create")
    suspend fun createRoom(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    /**
     * 更新房间
     */
    @PUT("/app/v3/room/update")
    suspend fun updateRoom(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    /**
     * 删除房间
     */
    @HTTP(method = "DELETE", path = "/app/v3/room/delete", hasBody = true)
    suspend fun deleteRoom(@Body param: Map<String, Int>): ResponseBody

    /**
     * 房间排序
     */
    @PUT("/app/v3/room/order")
    suspend fun orderRoom(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/room/roomNameRecommend")
    suspend fun roomRecommend(): ResponseBody

    @GET("/app/v1/user/info")
    suspend fun getUserInfo(): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/home/member/update", hasBody = true)
    suspend fun updateMember(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/share/code")
    suspend fun generateShareFamilyCode(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/share/account")
    suspend fun shareFamilyByAccount(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "DELETE", path = "/app/v3/device/group/delete", hasBody = true)
    suspend fun delGroups(@Body data: Map<String, @JvmSuppressWildcards List<Int>>): ResponseBody
}