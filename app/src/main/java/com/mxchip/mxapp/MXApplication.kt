package com.mxchip.mxapp

import com.mxchip.apicore.HeaderInterceptor
import com.mxchip.http.HttpManager
import com.mxchip.http.internal.CustomResponseFilter
import com.mxchip.http.utils.HttpLogger
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.NightModeManager
import com.mxchip.mxapp.utils.EnvConfigManager.initAppConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/5
 * @desc:
 */
class MXApplication : MXBusinessApplication() {

    override fun onCreate() {
        super.onCreate()
        initAppConfig(this)
        initHttp()
        NightModeManager.init(NightModeManager.getCurrentMode())
    }

    private fun initHttp() {
        //http 日志
        HttpLogger.setLogHandler { priority, tag, message ->
            MXLog.println(priority, tag, message)
        }
        HttpManager.init(
            AppConfigManager.getBaseUrl() ?: "",
            listOf(HeaderInterceptor(AppUtil.getVersionName(), null)),
            object : CustomResponseFilter {
                override fun onResponseFilter(code: Int): Boolean {
                    // INVALID_TOKEN = 10403,INVALID_SIGN = 10401
                    if (code == 10403 || code == 10401) {
                        if (LoginManager.getToken().isNotEmpty()) {
                            LoginManager.removeToken()
                            RouterManager.navigation(RouterConstants.LOGIN_GUIDE_ACTIVITY)
                            finishOtherActivity(RouterConstants.LOGIN_GUIDE_ACTIVITY)
                        }
                        return true
                    }
                    return false
                }
            },
            converterFactories = listOf(
                MoshiConverterFactory.create(
                    Moshi.Builder().add(
                        KotlinJsonAdapterFactory()
                    ).build()
                )
            )
        )
    }
}