package com.mxchip.mxapp.device.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.QueryMap

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/16
 * @desc:
 */
interface ShareApiService {

    @GET("/app/v3/device/share/list")
    suspend fun shareDeviceList(@QueryMap map: Map<String, Int>): ResponseBody

    @GET("/app/v3/device/share/user/list")
    suspend fun shareUserList(@QueryMap map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

//    @POST("/app/v3/home/share/code")
//    suspend fun generateShareFamilyCode(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/home/current/update", hasBody = true)
    suspend fun setCurrentHome(@Body map: Map<String, Int>): ResponseBody

//    @POST("/app/v3/home/share/account")
//    suspend fun shareFamilyByAccount(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/share")
    suspend fun shareDevice(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/share/user/cancel")
    suspend fun cancelDeviceShare(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/share/device/cancel")
    suspend fun unbindShareDevice(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody
}