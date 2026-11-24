package com.mxchip.mxapp.base.h5bridge

import android.app.Activity
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.core.net.toUri
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.DeviceTypeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.widget.web.CompletionHandler
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PageBridgeJsApi(
    private val activity: Activity,
    private val productKey: String? = null,
    private val uuid: String? = null,
    private val groupId: Int? = null,
    private val groupType: Int? = null, //0 mesh组 1 wifi组
    private val shortcutNumber: Int = 0,
    private val deviceType: Int = DeviceTypeConstants.DEVICE
) {

    companion object {
        const val TAG = "RouterBridgeJsApi"
    }

    @JavascriptInterface
    fun has(msg: Any, handler: CompletionHandler<Any>) {
        val ret = msg.toString()
        MXLog.d(TAG, "has $ret")
        handler.complete("1")
    }

    /**
     * 页面跳转
     * 参数: # path 1. external不存在或者为false , path表示app外页面 2. external为true, path表示app内页面
     * # external 是否跳转外部页面
     */
    @JavascriptInterface
    fun go(msg: Any, handler: CompletionHandler<*>) {
        MXLog.d(TAG, msg.toString())
        if (activity.isFinishing)
            return
        try {
            val jsonObject = JSONObject(msg.toString())
            val path = jsonObject.getString("path")
            val external = jsonObject.optBoolean("external", false)
            if (external) {
                val browserIntent = Intent(Intent.ACTION_VIEW, path.toUri())
                activity.startActivity(browserIntent)
                return
            }
            //旧版本h5路由写法不带前面的"/"
            val newRouterPath = if (path.startsWith("/")) path else "/$path"
            if (newRouterPath == RouterConstants.SMART_CONDITION_ACTIVITY) {
                //跳转到本地自动化规则，需要检查是否具备权限
                if (RoleManager.isUser()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        RoleManager.showDialog(activity, RoleManager.RoleType.TYPE_USER)
                    }
                    return
                }
            }
            val paramMap = mutableMapOf<String, Any>()
            if (jsonObject.has("query")) {
                val queryObject = jsonObject.getJSONObject("query")
                val it = queryObject.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    val value = queryObject.get(key)
                    if (value is JSONArray) {
                        val array = queryObject.getJSONArray(key)
                        if (array.length() > 0) {
                            if (array.get(0) is Int) {
                                val list = arrayListOf<Int>()
                                for (i in 0 until array.length()) {
                                    list.add(array[i].toString().toInt())
                                }
                                paramMap[key] = list
                            } else {
                                val list = arrayListOf<String>()
                                for (i in 0 until array.length()) {
                                    list.add(array[i].toString())
                                }
                                paramMap[key] = list
                            }
                        }
                    } else {
                        paramMap[key] = value
                    }
                }
            }
            productKey?.apply {
                paramMap[DeviceConstants.KEY_PRODUCT_KEY] = this
            }
            groupId?.apply {
                paramMap[DeviceConstants.KEY_GROUP_ID] = this
                paramMap[DeviceConstants.KEY_GROUP_TYPE] = groupType ?: 0//默认mesh组
            }
            if (!uuid.isNullOrEmpty())
                paramMap[CommonConstants.KEY_UUID] = uuid
            paramMap[CommonConstants.KEY_FROM_PAGE] = "h5"
            // 中控屏原生页面
            when (newRouterPath) {
                DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE, DeviceConstants.ROUTER_CENTER_CONTROL_GROUP, DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> {
                    paramMap["listType"] = newRouterPath
                    RouterManager.navigation(
                        RouterConstants.CENTER_CONTROL_SMART_ACTIVITY,
                        paramMap
                    )
                    handler.complete()
                    return
                }

                RouterConstants.DEVICE_PHOTO_COLOR_ACTIVITY -> {
                    //拍照取色需要相机权限
                    XXPermissions.with(activity)
                        .permission(Permission.CAMERA)
                        .request(object : OnPermissionCallback {
                            override fun onGranted(
                                permissions: MutableList<String>,
                                all: Boolean
                            ) {
                                if (all) {
                                    RouterManager.navigation(newRouterPath, paramMap)
                                    handler.complete()
                                }
                            }

                            override fun onDenied(
                                permissions: MutableList<String>,
                                never: Boolean
                            ) {
                                if (never) {
                                    XXPermissions.startPermissionActivity(activity, permissions)
                                }
                            }

                        })
                    return
                }

                RouterConstants.DEVICE_DETAIL_ACTIVITY -> {
                    //设备详情，根据设备类型重新跳转
                    when (deviceType) {
                        DeviceTypeConstants.SHARED_DEVICE -> {//分享设备详情
                            RouterManager.navigation(
                                RouterConstants.SHARED_DEVICE_DETAIL_ACTIVITY,
                                paramMap
                            )
                        }

                        DeviceTypeConstants.SHORTCUT_DEVICE -> {//快捷设备详情
                            paramMap["number"] = shortcutNumber
                            RouterManager.navigation(
                                RouterConstants.SHORTCUT_DEVICE_DETAIL,
                                paramMap
                            )
                        }

                        else -> {
                            RouterManager.navigation(
                                RouterConstants.DEVICE_DETAIL_ACTIVITY,
                                paramMap
                            )
                        }
                    }
                }

                else -> {
                    RouterManager.navigation(newRouterPath, paramMap)
                }
            }
            handler.complete()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * 退出webview页面
     */
    @JavascriptInterface
    fun closeWebView(msg: Any?, handler: CompletionHandler<*>) {
        activity.finish()
        handler.complete()
    }

    @JavascriptInterface
    fun getPlatformInfo(msg: Any?, handler: CompletionHandler<Any>) {
        val info = JSONObject()
        try {
            info.put("platform", "android")
            info.put("version", AppUtil.getVersionName())
            handler.complete(info)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun getBarHeight(msg: Any?, handler: CompletionHandler<Any>) {
        val barinfo = JSONObject()
        try {
            //cssPx=dp*(devicePxWidth/deviceDpWidth/devicePixelRatio)

//            int topHeight = BarUtils.getStatusBarHeight();
            MXLog.d("===StatusBarHeight====", "" + AppUtil.getStatusBarHeight())
            barinfo.put("top", AppUtil.getStatusBarHeight() / 3)
            barinfo.put("bottom", 0)
            handler.complete(barinfo)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onReflushDevices(msg: Any?, handler: CompletionHandler<Any>?) {
        LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH).postValue(
            HomeDataRefresh.DEVICE
        )
    }
}