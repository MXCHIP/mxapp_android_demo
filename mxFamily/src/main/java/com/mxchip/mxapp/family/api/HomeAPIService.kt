package com.mxchip.mxapp.family.api

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
interface HomeAPIService {
    @GET("/app/v3/home/list")
    suspend fun findHomes(@QueryMap param: Map<String, Int>): ResponseBody

    @GET("/app/v3/home/info")
    suspend fun getHomesInfo(@Query("home_id") homeId: Int): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/home/current/update", hasBody = true)
    suspend fun setCurrentHome(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/create")
    suspend fun createHome(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v4/home/create")
    suspend fun createHomeSetDefaultRoom(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @HTTP(method = "DELETE", path = "/app/v3/home/delete", hasBody = true)
    suspend fun deleteFamily(@Body map: Map<String, Int>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/home/update", hasBody = true)
    suspend fun updateHome(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/home/member/list")
    suspend fun memberList(@QueryMap map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/member/cancel")
    suspend fun removeFamilyMember(@Body map: Map<String, Int>): ResponseBody

    @HTTP(method = "PUT", path = "/app/v3/home/member/update", hasBody = true)
    suspend fun updateMember(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/share/code/bind")
    suspend fun bindShareFamilyByCode(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v1/weather/location")
    suspend fun fetchLocationByKeyword(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v1/weather/geoToLocation")
    suspend fun geoToLocation(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v1/weather/oneHour")
    suspend fun getWeatherOneHour(@QueryMap params: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/handover/code")
    suspend fun handOverCode(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/handover")
    suspend fun handOver(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/free/constr/handover")
    suspend fun handOverByAccount(@Body map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v3/home/share/inprogress")
    suspend fun shareInProgress(@QueryMap map: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/home/share/cancel")
    suspend fun shareCancel(@Body map: Map<String, Int>): ResponseBody

    @POST("/app/v3/auth/account/check")
    suspend fun checkAccountExist(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody
}
