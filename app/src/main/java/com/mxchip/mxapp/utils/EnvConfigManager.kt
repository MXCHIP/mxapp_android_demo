package com.mxchip.mxapp.utils


import android.content.Context
import com.mxchip.apicore.utils.MXApiConfigUtil
import com.mxchip.http.HttpManager
import com.mxchip.mxapp.BuildConfig
import com.mxchip.mxapp.account.bean.CountryBean
import com.mxchip.mxapp.account.util.AccountManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.AssetsUtil
import com.mxchip.mxapp.base.utils.DeviceManage
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.family.HomeManager
import com.mxchip.mxapp.pair.utils.PairManager
import com.tencent.mmkv.MMKV

/**
 * 环境配置管理
 */
object EnvConfigManager {

    const val BUILD_TYPE_DEBUG = "debug"
    const val ENV_DEV = "dev"
    const val ENV_TEST = "test"
    const val ENV_PREV = "preview"
    const val ENV_RELEASE = "release"

    const val SELECT_COUNTRY = true  // 支持选国家
    const val Home_CreateGroup = false  //首页创建群组
    const val Home_CreateScene = false  //首页创建智能
    const val Home_Scan = true  //首页扫一扫
    private val mAppRecord = "沪ICP备05042130号-15A"

    private var mAppId = "40c91412fc48469685e97dab1b50dca4"
    private var mAppSecret = "8efc9ad89eb711eca8bd784f438b7146"

    private const val KEY_ENV_SWITCH = "env_switch"
    private val envSwitchMMKV by lazy { MMKV.mmkvWithID("env_switch") }

    fun switchEnv(buildType: String) {
        envSwitchMMKV.encode(KEY_ENV_SWITCH, buildType)
    }

    fun getEnvSwitch(): String? = envSwitchMMKV.decodeString(KEY_ENV_SWITCH)

    private fun getHostConfig(server: String): Map<String, String> {
        return when (server) {
            "america" ->
                mapOf(
                    "HOST" to "https://api-us.mxchip.com",
                    "FOG_API" to "api-us.fogcloud.io",
                    "FOG_MQTT" to "mqtt-us.fogcloud.io"
                )

            "Singapore" ->
                mapOf(
                    "HOST" to "https://api-sg.mxchip.com:443",
                    "FOG_API" to "api-sg.fogcloud.io",
                    "FOG_MQTT" to "mqtt.sg.gispacehub.com"
                )

            else -> {
                if (BuildConfig.BUILD_TYPE == BUILD_TYPE_DEBUG) {
                    when (getEnvSwitch()) {
                        ENV_DEV -> {
                            mAppId = "40c91412fc48469685e97dab1b50dca4"
                            mAppSecret = "8efc9ad89eb711eca8bd784f438b7146"
                            mapOf(
                                "HOST" to "https://dev-app-api-cloud.dev.mxchip.com.cn",
                                "FOG_API" to "api-dev.fogcloud.io",
                                "FOG_MQTT" to "mqtt-dev.fogcloud.io"
                            )
                        }

                        ENV_PREV -> {
                            mAppId = "40c91412fc48469685e97dab1b50dca4"
                            mAppSecret = "8efc9ad89eb711eca8bd784f438b7146"
                            mapOf(
                                "HOST" to "https://stg-app-api-cloud.dev.mxchip.com.cn",
                                "FOG_API" to "app.api.fogcloud.io",
                                "FOG_MQTT" to "app.mqtt.fogcloud.io"
                            )
                        }

                        ENV_RELEASE -> {
                            mAppId = "40c91412fc48469685e97dab1b50dca4"
                            mAppSecret = "8efc9ad89eb711eca8bd784f438b7146"
                            mapOf(
                                "HOST" to "https://app.api.cloud.mxchip.com:2443",
                                "FOG_API" to "app.api.fogcloud.io",
                                "FOG_MQTT" to "app.mqtt.fogcloud.io"
                            )
                        }

                        else -> { //默认测试环境
                            mAppId = "57dba37764004cc39b4cafe1a7c8fca5"
                            mAppSecret = "9c0bb76790ed438899321d8f3b2486fb"
                            mapOf(
                                "HOST" to "https://sit-app-api-cloud.dev.mxchip.com.cn",
                                "FOG_API" to "api-dev.fogcloud.io",
                                "FOG_MQTT" to "mqtt-dev.fogcloud.io"
                            )
                        }
                    }
                } else { //生产环境
                    mAppId = "40c91412fc48469685e97dab1b50dca4"
                    mAppSecret = "8efc9ad89eb711eca8bd784f438b7146"
                    mapOf(
                        "HOST" to "https://app.api.cloud.mxchip.com:2443",
                        "FOG_API" to "app.api.fogcloud.io",
                        "FOG_MQTT" to "app.mqtt.fogcloud.io"
                    )
                }
            }
        }
    }

    fun initHostConfig(serverStation: String, isUpdate: Boolean = false) {
        val serverMap = getHostConfig(serverStation)
        val host = serverMap["HOST"]
        if (host.isNullOrEmpty()) return
        AppConfigManager.setHttpHost(host).setAppRecord(mAppRecord)
        if (isUpdate) HttpManager.updateBaseUrl(host)
        val fogApi = serverMap["FOG_API"]
        val fogMqtt = serverMap["FOG_MQTT"]
        val mxcloud = host.replace("https://", "")
        val fogConfig = if (fogApi.isNullOrEmpty() || fogMqtt.isNullOrEmpty())
            null else
            Pair(
                fogApi, fogMqtt
            )
        PairManager.setGatewayConfig(cloudHost = mxcloud, fogConfig = fogConfig)
        MXApiConfigUtil.setMxAppInfo(mAppId, mAppSecret)
    }


    /**
     * 配置各模块支持、展示的功能，
     */
    fun initAppConfig(context: Context) {
        //是否已选国家
        val iso3 = LoginManager.getSelectRegionCode()
        if (!iso3.isNullOrEmpty()) {
            if (iso3 == "CHN"){
                initHostConfig("CHN")
            }else{
                val regionData = JsonUtil.fromJsonToArray(
                    AssetsUtil.readJsonFromAssets(context.applicationContext, "countrys.json"),
                    CountryBean::class.java
                )
                regionData?.firstOrNull { it.iso3 == iso3 }?.apply {
                    initHostConfig(this.server_station)
                }
            }
        }
        //国家选择
        AccountManager.setSupportSelectCountry(false)
        //设备
        DeviceManage.setSupportAddDevice(true)//添加/删除家庭设备
            .setSupportSetCommonly(true)//设备详情，允许设为常用
            .setSupportDeviceShare(true)//设备分享
            .setSupportDeviceFeedback(false)//设备反馈依赖已移除的个人中心模块

        //配网
        PairManager.setSupportProductCategory(true)//是否显示手动配网，品类列表
            .setSupportSetCommonly(true)//配网成功页面，允许设为常用


        //家庭
        HomeManager.setSupportHomeShare(
            true,//家庭分享，显示成员邀请
        ).setSupportHomeLocation(true) //展示家庭位置
    }
}
