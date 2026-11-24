package com.mxchip.mxapp.device.ui.bridge

import android.webkit.JavascriptInterface
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.widget.web.CompletionHandler
import org.json.JSONObject

class LightBridgeJsApi(private val categoryId: Int) {
    companion object {
        private const val TAG = "LightBridgeJsApi"
    }

    @JavascriptInterface
    fun getLightConfig(msg: Any?, handler: CompletionHandler<Any>?) {
        val values = JSONObject()
        var isSupportTracing = false //是否支持追光
        var isSupportColoring = false //是否支持拍照取色
        var isSupportRhythm = false //是否支持音乐律动
        if (categoryId == 100107) {
            isSupportTracing = true
        } else if (categoryId == 100104 || categoryId == 100105 || categoryId == 100106) {
            isSupportRhythm = true
        }
        values.put("isSupportRhythm", isSupportRhythm)
        values.put("isSupportTracing", isSupportTracing)
        values.put("isSupportColoring", isSupportColoring)
        MXLog.d(TAG, "getLightConfig->$values")
        handler?.complete(values)
    }
}