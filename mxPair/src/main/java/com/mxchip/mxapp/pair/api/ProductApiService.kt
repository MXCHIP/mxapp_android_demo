package com.mxchip.mxapp.pair.api

import okhttp3.ResponseBody
import retrofit2.http.*

interface ProductApiService {

    @GET("/app/v3/category/product/info")
    suspend fun findProduct(): ResponseBody
}