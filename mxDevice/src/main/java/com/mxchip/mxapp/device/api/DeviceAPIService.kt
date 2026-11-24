package com.mxchip.mxapp.device.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 *
 */
interface DeviceAPIService {

    @GET("/app/v3/device/list")
    suspend fun findSharedDevices(@QueryMap map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v5/device/list")
    suspend fun findDevices(@QueryMap map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/room/device/set")
    suspend fun updateDeviceLocation(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/properties/set")
    suspend fun setProperties(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/device/properties/get")
    suspend fun getAllProperties(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    /**
     * H5面板信息
     */
    @GET("/app/v1/panel/h5/upgradeInfo")
    suspend fun getPanelInfo(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/unbind")
    suspend fun unBindDevice(@Body map: Map<String, @JvmSuppressWildcards List<String>>): ResponseBody

    /**
     * 用户解绑分享设备
     */
    @POST("/app/v3/device/share/device/cancel")
    suspend fun unBindSharedDevice(@Body map: Map<String, @JvmSuppressWildcards List<String>>): ResponseBody

    @GET("/app/v3/device/info")
    suspend fun getDeviceInfo(@Query("iotid") iotId: String): ResponseBody

    @HTTP(method = "PUT", path = "/app/v1/device/updateNickname", hasBody = true)
    suspend fun setDeviceName(@Body map: Map<String, String>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/device/show", hasBody = true)
    suspend fun setDeviceShow(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/common/show")
    suspend fun setCommonShow(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v1/device/version/update", hasBody = true)
    suspend fun updateOTAVersion(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/ota/newVersion")
    suspend fun getOtaUpgradeInfoV3(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/ota/remind")
    suspend fun hasOtaUpgrade(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/device/gateway/mesh/message")
    suspend fun sendCloudMeshMessage(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/home/recovery/status")
    suspend fun getRecoveryStatus(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/home/recovery/device/list")
    suspend fun findRecoveryDeviceList(@QueryMap params: Map<String, Int>): ResponseBody

    @POST("/app/v3/home/recovery/start")
    suspend fun startHomeRecovery(@Body params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/home/recovery/device/rule")
    suspend fun getRecoveryDeviceRule(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/recovery/device/rule/written")
    suspend fun updateRecoveryDeviceWriteState(@Body params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/recovery/completed")
    suspend fun recoveryCompleted(@Body params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v4/room/list")
    suspend fun roomListV4(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/device/gateway/config", hasBody = true)
    suspend fun updateMasterGateway(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/device/shortcut/list")
    suspend fun findShortcutDevice(@QueryMap param: Map<String, Int>): ResponseBody

    @GET("/app/v3/device/shortcut/info")
    suspend fun getShortcutDeviceInfo(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/device/shortcut/conf")
    suspend fun getDeviceShortcutConfig(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/device/shortcut/conf/update", hasBody = true)
    suspend fun updateDeviceShortcutConfig(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v1/device/prop/name", hasBody = true)
    suspend fun updateDeviceIdentifierName(@Body param: Map<String, String>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v5/room/device/update", hasBody = true)
    suspend fun updateRoomDevice(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v5/whole/home/alarm")
    suspend fun findDeviceAlarms(@Query("home_id") homeId: Int): ResponseBody

    @GET("/app/v1/product/instructions/detail")
    suspend fun getProductUseManual(@Query("product_key") productKey: String): ResponseBody
}