package com.mxchip.mxapp.account.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/6/23
 * @desc:
 */
interface AccountApiService {

    @POST("/app/v1/auth/login")
    suspend fun login(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v1/auth/resetPwdLogin")
    suspend fun resetPwdLogin(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v1/auth/register")
    suspend fun register(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v1/auth/sendVerifyCode")
    suspend fun sendVerifyCode(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v1/auth/checkVerifyCode")
    suspend fun checkVerifyCode(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/auth/account/check")
    suspend fun checkAccountExist(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/auth/code/login")
    suspend fun loginByVerCode(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @GET("/app/v1/help/docInfo")
    suspend fun docInfo(@QueryMap data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v3/auth/reset/pwd")
    suspend fun fastLoginSetPwd(@Body data: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v4/auth/autologin")
    suspend fun autoLogin(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody

    @POST("/app/v4/auth/autologin/addPhone")
    suspend fun addPhone(@Body param: Map<String, @JvmSuppressWildcards Any>): ResponseBody
}