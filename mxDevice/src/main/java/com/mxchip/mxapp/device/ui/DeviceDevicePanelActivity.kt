package com.mxchip.mxapp.device.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.mxchip.http.download.DownloadManager
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.DeviceTypeConstants
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.h5bridge.PageBridgeJsApi
import com.mxchip.mxapp.base.h5bridge.RequestBridgeJsApi
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.DeviceManage
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.LanguageUtil
import com.mxchip.mxapp.base.utils.NightModeManager
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.ZipUtils
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.widget.web.DWebView
import com.mxchip.mxapp.device.bean.DevicePanelInfo
import com.mxchip.mxapp.device.databinding.DeviceAcDevicePanelBinding
import com.mxchip.mxapp.device.ui.bridge.DeviceBridgeJsApi
import com.mxchip.mxapp.device.ui.bridge.LightBridgeJsApi
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.TimeZone

@Router(RouterConstants.DEVICE_PANEL_ACTIVITY)
class DeviceDevicePanelActivity : MXBusinessActivity<DeviceAcDevicePanelBinding>() {

    override fun getLayoutBinding(): DeviceAcDevicePanelBinding {
        return DeviceAcDevicePanelBinding.inflate(layoutInflater)
    }

    companion object {
        private const val TAG = "DevicePanelActivity"
    }

    private val deviceVM by lazy { DeviceViewModel() }

    private var deviceBridgeJsApi: DeviceBridgeJsApi? = null

    private var iotId: String = ""
    private var uuid: String = ""
    private val h5Dir by lazy {
        filesDir.path + File.separator + "H5"
    }
    private val timeoutRun by lazy {
        Runnable {
            dismissLoading()
        }
    }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private fun startTimeoutTimer() {
        val isDebugH5 = intent.getBooleanExtra("isDebugH5", false)
        if (isDebugH5) {
            //调试H5模式不启动定时器，直接关闭loading框
            dismissLoading()
        } else {
            handler.postDelayed(timeoutRun, 20 * 1000)
        }
    }

    override fun onInit() {
        val deviceStr = intent.getStringExtra(DeviceConstants.KEY_DEVICE_BEAN)
        setSetting(binding.panelWebview)
        binding.panelWebview.webChromeClient = MyWebChromeClient()
        if (deviceStr.isNullOrEmpty()) {
            val iot = intent.getStringExtra(CommonConstants.KEY_IOT_ID)
            if (iot.isNullOrEmpty()) {
                MXLog.e(TAG, "deviceBean & iotId 不存在，关闭页面")
                finish()
                return
            }
            getDeviceInfo(iot)
        } else {
            val deviceBean = JsonUtil.fromJson(deviceStr, DeviceBean::class.java)
            if (null == deviceBean) {
                MXLog.e(TAG, "设备为空，关闭页面")
                finish()
                return
            }
            val intentUrl = intent.getStringExtra(DeviceConstants.KEY_WEB_VIEW_PATH)
            showLoading()
            initDevicePanel(intentUrl, deviceBean)
        }
    }

    private fun setSetting(webView: DWebView) {
        val mWebSettings = webView.settings
        val isDark = NightModeManager.isDarkMode(this)
        val tzID = TimeZone.getDefault().id
        mWebSettings.userAgentString += " mxchip app/${AppUtil.getPackageName()} theme/${if (isDark) "dark" else "light"} productType/0 lang/${LanguageUtil.getCurrentLanguage()} tz/$tzID"
        mWebSettings.javaScriptEnabled = true
        mWebSettings.setSupportZoom(true)
        mWebSettings.builtInZoomControls = false
        mWebSettings.savePassword = false
        mWebSettings.mixedContentMode = 0
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mWebSettings.textZoom = 100
        mWebSettings.databaseEnabled = true
        if (AppUtil.isNetworkAvailable()) {
            mWebSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        } else {
            mWebSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        mWebSettings.setSupportMultipleWindows(false)
        // 是否阻塞加载网络图片  协议http or https
        mWebSettings.blockNetworkImage = false
        // 允许加载本地文件html  file协议（面板 H5 在 filesDir 私有目录）
        mWebSettings.allowFileAccess = true
        // File 域 JS 禁止读取其它本地文件 / 跨域访问 http(s)
        mWebSettings.allowFileAccessFromFileURLs = false
        mWebSettings.allowUniversalAccessFromFileURLs = false
        mWebSettings.javaScriptCanOpenWindowsAutomatically = true
        mWebSettings.loadsImagesAutomatically = true
        mWebSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        mWebSettings.loadWithOverviewMode = false
        mWebSettings.useWideViewPort = false
        mWebSettings.domStorageEnabled = true
        mWebSettings.setNeedInitialFocus(true)
        mWebSettings.defaultTextEncodingName = "utf-8" //设置编码格式
        mWebSettings.defaultFontSize = 16
        mWebSettings.minimumFontSize = 12 //设置 WebView 支持的最小字体大小，默认为 8
        mWebSettings.setGeolocationEnabled(true)
    }

    private inner class MyWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress == 100) {
                dismissLoading()
                handler.removeCallbacks(timeoutRun)
                inject()//整个页面加载完毕才允许交互更新data
                binding.progressWebpayer.visibility = View.GONE
            } else {
                if (binding.progressWebpayer.isGone)
                    binding.progressWebpayer.visibility = View.VISIBLE
                binding.progressWebpayer.progress = newProgress
            }
        }

        override fun onJsAlert(p0: WebView?, p1: String?, p2: String?, p3: JsResult?): Boolean {
            return true
        }
    }


    private fun inject() {
        //设备mesh数据
        LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA)
            .observe(this) { dtMap ->
                if (dtMap.containsKey(DeviceConstants.KEY_MESH_STATE)) {
                    val online = dtMap[DeviceConstants.KEY_MESH_STATE] as Boolean
                    if (online) {
                        deviceBridgeJsApi?.updateDeviceStatus(localOnline = 1)
                    } else {
                        deviceBridgeJsApi?.updateDeviceStatus(localOnline = 0)
                    }
                } else {
                    val dtUUID = dtMap[MeshConstants.KEY_UUID] as String
                    if (dtUUID == uuid) {
                        deviceBridgeJsApi?.updateDeviceStatus(localOnline = 1)
                        deviceBridgeJsApi?.updateMeshProperties(dtMap)
                    }
                }
            }
        //云端socket数据
        LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_CLOUD_DATA).observe(this) { ilopData ->
            val json = JSONObject(ilopData)
            if (json.has("iotId")) {
                val dtIotId = json.getString("iotId")
                if (dtIotId == iotId) {
                    if (json.has("items")) {
                        //收到设备数据，更新在线（避免出现上线状态丢失）
                        deviceBridgeJsApi?.updateDeviceStatus(1)
                        deviceBridgeJsApi?.updateRemoteProperties(json.getJSONObject("items"))
                    } else if (json.has("status")) {
                        val online = json.getString("status") == "online"
                        val remoteOnline = if (online) 1 else 0
                        deviceBridgeJsApi?.updateDeviceStatus(remoteOnline)
                    }
                }
            }
        }

        LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_WARN_INFO).observe(this) {
            deviceBridgeJsApi?.setDeviceWarnInfo(it)
        }
    }

    private fun initDevicePanel(intentUrl: String?, deviceBean: DeviceBean) {
        iotId = deviceBean.iotid
        uuid = deviceBean.uuid
        val deviceType =
            intent.getIntExtra(DeviceConstants.KEY_DEVICE_TYPE, DeviceTypeConstants.DEVICE)

        getPanelInfo(deviceBean.product_key) {
            lifecycleScope.launch(Dispatchers.Main) {
                dismissLoading()
            }
            val productH5 = h5Dir + File.separator + deviceBean.product_key + "/dist/index.html"
            when {
                File(productH5).exists() -> {
                    initDeviceJSApi(deviceType, deviceBean)
                    loadWebUrl(intentUrl, productH5, deviceBean)
                }

                deviceType == DeviceTypeConstants.SHARED_DEVICE -> { //共享房间设备,无H5
                    MXLog.w(TAG, "product H5 not exists")
                    RouterManager.navigation(
                        RouterConstants.SHARED_DEVICE_DETAIL_ACTIVITY, mapOf(
                            CommonConstants.KEY_IOT_ID to iotId
                        )
                    )
                    finish()
                }

                else -> {
                    MXLog.w(TAG, "product H5 not exists")
                    RouterManager.navigation(
                        RouterConstants.DEVICE_DETAIL_ACTIVITY, mapOf(
                            CommonConstants.KEY_IOT_ID to iotId
                        )
                    )
                    finish()
                }
            }

        }
    }

    private fun initDeviceJSApi(deviceType: Int, deviceBean: DeviceBean) {
        val initLocalStatus = when {
            deviceBean.isHideOnlineStatus() -> 1
            uuid.isEmpty() -> 0
            MXMesh.isConnected() -> 1
            else -> 0
        }
        val productInfo = JsonUtil.fromJson(
            ProductInfoManager.getProductInfo(deviceBean.product_key),
            ProductInfo::class.java
        )

        val pageJs = PageBridgeJsApi(
            this,
            deviceBean.product_key,
            uuid,
            null,
            null,
            deviceBean.number,
            deviceType
        )
        binding.panelWebview.addJavascriptObject(pageJs, "page")
        binding.panelWebview.addJavascriptObject(RequestBridgeJsApi(), "request")
        val productAttrConfig =
            if (productInfo == null || productInfo.attr_map.length < 4) {
                ""
            } else {
                productInfo.attr_map
            }
        MXLog.i("DevicePanelActivity", "productAttrConfig: $productAttrConfig")
        // 创建功能提供者（如果需要特定功能）
        deviceBridgeJsApi = DeviceBridgeJsApi(
            iotId,
            uuid,
            isShareDevice = deviceType == DeviceTypeConstants.SHARED_DEVICE,
            remoteStatus = if (deviceBean.online) 1 else 0,
            initLocalStatus,
            this,
            productAttrConfig
        )
        binding.panelWebview.addJavascriptObject(deviceBridgeJsApi!!, "device")
        binding.panelWebview.addJavascriptObject(LightBridgeJsApi(deviceBean.category_id), "light")
    }

    private fun loadWebUrl(intentUrl: String?, productH5: String, data: DeviceBean) {
        startTimeoutTimer()
        val patch = resolvePanelUrl(intentUrl, productH5)
        val homeId = AppConfigManager.getUseHomeId()
        //快捷设备,H5地址需要到子路由
        val fast = if (data.number > 0) "&fastIndex=${data.number}#/fast" else ""
        val url =
            "$patch?iotId=$iotId&category_id=${data.category_id}&productKey=${data.product_key}&homeId=$homeId$fast"
        MXLog.i("cxk", "devicePanelUrl: $url")
        binding.panelWebview.loadUrl(url)
    }

    /**
     * 限制 file:// 仅能落在 h5Dir 白名单内（canonicalPath 防 ../../ 绕过）；
     * http(s) 仅用于调试面板，其它非法地址回落到本地面板。
     */
    private fun resolvePanelUrl(intentUrl: String?, productH5: String): String {
        val fallback = "file://$productH5"
        if (intentUrl.isNullOrEmpty()) return fallback
        if (intentUrl.startsWith("http://", true) || intentUrl.startsWith("https://", true)) {
            return intentUrl
        }
        val isFileUrl = intentUrl.startsWith("file://", true)
        val filePath = if (isFileUrl) {
            intentUrl.substring(7).substringBefore('?').substringBefore('#')
        } else {
            intentUrl.substringBefore('?').substringBefore('#')
        }
        return if (isPathUnderH5Dir(filePath)) {
            if (isFileUrl) intentUrl else "file://$intentUrl"
        } else {
            MXLog.w(TAG, "reject unsafe panel url: $intentUrl")
            fallback
        }
    }

    private fun isPathUnderH5Dir(path: String): Boolean {
        return try {
            val target = File(path).canonicalFile
            val root = File(h5Dir).canonicalFile
            target.path == root.path || target.path.startsWith(root.path + File.separator)
        } catch (_: Exception) {
            false
        }
    }

//    override fun onBackPressed() {
//        val locaUrl = "file:///android_asset/request_failed/reLoad.html"
//        if (binding.panelWebview.canGoBack() && binding.panelWebview.url != locaUrl) {
//            val mWebBackForwardList = binding.panelWebview.copyBackForwardList()
//            if (mWebBackForwardList.currentIndex > 0) {
//                val historyUrl = mWebBackForwardList.getItemAtIndex(
//                    mWebBackForwardList.currentIndex - 1
//                ).url
//                if (historyUrl == locaUrl) {
//                    finish()
//                    return
//                }
//                binding.panelWebview.goBack()
//            }
//        } else {
//            finish()
//        }
//    }

    private fun getDeviceInfo(iotId: String) {
        showLoading()
        deviceVM.getDeviceInfo(iotId).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlow<DeviceBean>(
                success = { deviceBean ->
                    if (deviceBean == null) {
                        dismissLoading()
                    } else {
                        initDevicePanel(null, deviceBean)

                    }
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                })
        )
    }

    private fun getPanelInfo(productKey: String, h5PanelComplete: () -> Unit) {
        deviceVM.getPanelInfoFlow(productKey)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<DevicePanelInfo>(success = {
                if (null == it?.url) {
                    h5PanelComplete.invoke()
                } else {
                    val destFilePatch = h5Dir + File.separator + productKey
                    val pkH5Dir = File(destFilePatch)
                    //检查H5版本
                    if (pkH5Dir.exists()
                        && DeviceManage.getPanelVersion(productKey)
                            .equals(it.version, ignoreCase = true)
                    ) {
                        h5PanelComplete.invoke()
                    } else {
                        downloadH5(it, productKey, destFilePatch, h5PanelComplete)
                    }
                }
            }, error = { _, _, _ ->
                h5PanelComplete.invoke()
            }))
    }

    private fun downloadH5(
        devicePanelInfo: DevicePanelInfo,
        productKey: String,
        productH5Dir: String,
        complete: (() -> Unit)
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            DownloadManager.url(devicePanelInfo.url)
                .file(cacheDir, File.separator + productKey + ".zip").onStart {
                    MXLog.d("downloadH5", "onStart")
                }.onSuccess { zipFilePatch ->
                    MXLog.d("downloadH5", "onSuccess")
                    zipFilePatch?.apply {
                        try {
                            ZipUtils.unzipFile(this, productH5Dir)
                            //缓存产品的面包版本号
                            DeviceManage.savePanelVersion(productKey, devicePanelInfo.version)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            //重置版本
                            DeviceManage.savePanelVersion(productKey, "")
                        }
                        //删除zip文件
                        File(zipFilePatch).delete()
                    }
                    complete.invoke()
                }.onError { code, msg ->
                    MXLog.d("downloadH5", "onError->$code,$msg")
                    //重置版本
                    DeviceManage.savePanelVersion(productKey, "")
                    complete.invoke()
                }.download()
        }
    }

    override fun onDestroy() {
        dismissLoading()
        binding.panelWebview.clearHistory()
        binding.panelWebview.clearFormData()
        deviceBridgeJsApi?.onDestroy()
        super.onDestroy()
    }
}