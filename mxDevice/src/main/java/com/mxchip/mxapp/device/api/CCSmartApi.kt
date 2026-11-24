package com.mxchip.mxapp.device.api

import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/14
 * @desc:
 */
interface CCSmartApi {

    @GET("/app/v3/centralControl/{listType}/list")
    suspend fun getList(
        @Path("listType") listType: String,
        @QueryMap map: Map<String, @JvmSuppressWildcards Any>
    ): ResponseBody


    @POST("/app/v3/centralControl/{updateType}/update")
    suspend fun update(
        @Path("updateType") updateType: String,
        @Body map: Map<String, @JvmSuppressWildcards Any>
    ): ResponseBody
}