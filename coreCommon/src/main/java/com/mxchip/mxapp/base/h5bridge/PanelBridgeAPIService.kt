package com.mxchip.mxapp.base.h5bridge

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 *
 */
interface PanelBridgeAPIService {

    @GET
    suspend fun fetchOwnServerGET(
        @Url url: String,
        @QueryMap data: Map<String, @JvmSuppressWildcards Any>
    ): ResponseBody

    @POST
    suspend fun fetchOwnServerPOST(
        @Url url: String,
        @Body data: RequestBody
    ): ResponseBody

    @PUT
    suspend fun fetchOwnServerPUT(
        @Url url: String,
        @Body data: RequestBody
    ): ResponseBody

    @HTTP(method = "DELETE", hasBody = true)
    suspend fun fetchOwnServerDELETE(
        @Url url: String,
        @Body data: RequestBody
    ): ResponseBody
}