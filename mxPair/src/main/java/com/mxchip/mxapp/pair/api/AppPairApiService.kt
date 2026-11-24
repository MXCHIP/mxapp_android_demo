package com.mxchip.mxapp.pair.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AppPairApiService {
    @POST("/app/v3/mesh/address")
    suspend fun getMeshAddress(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/bind")
    suspend fun bindDevice(@Body bindParams: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    /**
     * 房间列表
     */
    @GET("/app/v4/room/list")
    suspend fun roomListV4(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/batchSet")
    suspend fun modifyBatchDeviceInfo(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    /**
     * 新建房间
     */
    @POST("/app/v3/room/create")
    suspend fun createRoom(@Body param: Map<String, @JvmSuppressWildcards Any>):ResponseBody

    @GET("/app/v3/device/all/rule")
    suspend fun findGroupsAndRulesByDevice(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/fog/device/setNet/getRandom")
    suspend fun getFogRandom(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/fog/device/setNet/getBleKey")
    suspend fun getFogBleKey(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/fog/device/bleConfigNetworking")
    suspend fun getFogConnectState(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/awss/cipher/get")
    suspend fun getAliCipher(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v1/product/bindGuide")
    suspend fun getPairGuideInfo(@Query("product_key") productKey: String): ResponseBody

    @POST("/app/v3/device/restore")
    suspend fun restoreAPGateway(@Body param: Map<String, String>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v1/device/version/update", hasBody = true)
    suspend fun updateOTAVersion(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody
}