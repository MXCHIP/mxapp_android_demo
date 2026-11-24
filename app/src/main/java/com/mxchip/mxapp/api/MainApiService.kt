package com.mxchip.mxapp.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/13
 * @desc:
 */
interface MainApiService {

    /**
     * 获取app版本更新
     */
    @GET("/app/v2/help/upgradeInfo")
    suspend fun checkUpdateApp(@Query("version") version: String): ResponseBody

    @GET("/app/v5/asset/startup")
    suspend fun getStartupAD(): ResponseBody

    /**
     * 获取websocket 信息
     */
    @GET("/app/v3/websocket/info")
    suspend fun getWebSocket(): ResponseBody

    @GET("/app/v3/mqtt/info")
    suspend fun getMQTT(): ResponseBody

    /**
     * 获取全屋attr json配置
     */
    @GET("/app/v3/whole/home/attr/map")
    suspend fun findHomeAttrConfig(): ResponseBody

    /**
     * 隐私协议变更
     */
    @GET("/app/v1/privacy/change")
    suspend fun privacyChange(): ResponseBody

    /**
     * 隐私协议已读
     */
    @POST("/app/v1/privacy/read")
    suspend fun readPrivacy(@Body param: Map<String, Int>): ResponseBody

    @POST("/app/v3/auth/auto/login")
    suspend fun fastLogin(@Body param: Map<String, String?>): ResponseBody

    @GET("/app/v1/user/stat")
    suspend fun userStat(): ResponseBody

    @GET("/app/v1/user/logout")
    suspend fun logout(): ResponseBody

    @GET("/app/v1/help/upgradeInfo")
    suspend fun checkAppUpgrade(): ResponseBody

    @PUT("/app/v1/user/updateInfo")
    suspend fun updateUserInfo(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/mesh/sequence/update", hasBody = true)
    suspend fun setMeshSeq(@Body map: Map<String, Int>): ResponseBody

    @GET("/app/v3/mesh/config")
    suspend fun getHomeMeshConfig(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/mesh/sequence/info")
    suspend fun getMeshSeq(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/mesh/address")
    suspend fun getMeshAddress(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v5/whole/home/room/list")
    suspend fun findSmartHomeRoomList(@Query("home_id") homeId: Int): ResponseBody

    @GET("/app/v5/whole/home/room/properties/device/list")
    suspend fun findRoomSensorList(@QueryMap param: Map<String, Int>): ResponseBody

    @POST("/app/v5/whole/home/room/properties/device/set")
    suspend fun settingRoomUseSensor(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v5/whole/home/room/properties/set")
    suspend fun setRoomProperties(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/scene/recommend/list")
    suspend fun getRecommendSceneList(@QueryMap param: Map<String, @JvmSuppressWildcards Any>): ResponseBody//BaseResponse<BaseListWrapperData<RecommendSceneBean>>
}