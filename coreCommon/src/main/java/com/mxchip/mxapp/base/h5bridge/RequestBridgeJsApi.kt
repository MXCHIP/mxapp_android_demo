package com.mxchip.mxapp.base.h5bridge

import android.webkit.JavascriptInterface
import com.mxchip.http.response.NetState
import com.mxchip.http.response.NetStateResponse
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.widget.web.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class RequestBridgeJsApi {
    companion object {
        const val TAG = "RequestBridgeJsApi"
    }

    private val apiRepository by lazy {
        PanelBridgeRepository()
    }

    /**
     * @param msg
     * @param handler
     */
    @JavascriptInterface
    fun fetchOwnServer(msg: Any, handler: CompletionHandler<Any>) {
        MXLog.d(TAG, "fetchOwnServer()->$msg")
        val msgObj = JSONObject(msg.toString())
        val method = msgObj.getString("method")
        val url = AppConfigManager.getBaseUrl() + "/" + msgObj.getString("url")
        when (method) {
            "GET" -> {
                onGetRequest(parseDataToMap(msgObj.getJSONObject("data")), url, handler)
            }

            "POST" -> {
                onPOSTRequest(msgObj.getString("data"), url, handler)
            }

            "PUT" -> {
                onPUTRequest(msgObj.getString("data"), url, handler)
            }

            "DELETE" -> {
                onDELETERequest(msgObj.getString("data"), url, handler)
            }
        }
    }

    private fun parseDataToMap(json: JSONObject): Map<String, Any> {
        val resultMap = mutableMapOf<String, Any>()
        if (AppConfigManager.getUseHomeNetworkKey().isNotEmpty())
            resultMap["network_key"] = AppConfigManager.getUseHomeNetworkKey()
        for (jsonKey in json.keys()) {
            val value = json[jsonKey]
            resultMap[jsonKey] = value
        }
        return resultMap
    }

    private fun onDELETERequest(
        data: String,
        url: String,
        handler: CompletionHandler<Any>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.fetchOwnServerDELETE(url, data)
            if (NetState.SUCCESS == response.state) {
                onSuccess(response.data, handler)
            } else {
                onFailed(response, handler)
            }
        }
    }

    private fun onPUTRequest(data: String, url: String, handler: CompletionHandler<Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.fetchOwnServerPUT(url, data)
            if (NetState.SUCCESS == response.state) {
                onSuccess(response.data, handler)
            } else {
                onFailed(response, handler)
            }
        }
    }

    private fun onPOSTRequest(data: String, url: String, handler: CompletionHandler<Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.fetchOwnServerPOST(url, data)
            if (NetState.SUCCESS == response.state) {
                onSuccess(response.data, handler)
            } else {
                onFailed(response, handler)
            }
        }
    }

    private fun onGetRequest(map: Map<String, Any>, url: String, handler: CompletionHandler<Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiRepository.fetchOwnServerGET(url, map)
            if (NetState.SUCCESS == response.state) {
                onSuccess(response.data, handler)
            } else {
                onFailed(response, handler)
            }
        }
    }

    private fun onSuccess(dataJson: String?, handler: CompletionHandler<Any>) {
        val jsonData = JSONObject()
        jsonData.put("code", 0)
        jsonData.put("data", JSONObject(dataJson ?: "{}"))
        MXLog.d(TAG, "jsonData->$jsonData")
        handler.complete(jsonData)
    }

    private fun onFailed(response: NetStateResponse, handler: CompletionHandler<Any>) {
        val jsonData = JSONObject()
        jsonData.put("code", response.state.code)
        jsonData.put("message", "${response.state.message}")
        MXLog.d(TAG, "jsonData->$jsonData")
        handler.complete(jsonData)
    }
}