package com.mxchip.mxapp.base.mesh

import com.mxchip.log.MXLog

abstract class MapCallback : BaseCallback {
    companion object {
        private const val TAG = "MapCallback"
    }

    override fun onError(callbackMsg: CallbackMsg) {
        MXLog.e(TAG, "code: ${callbackMsg.code},msg: ${callbackMsg.msg}")
    }

    abstract fun onResult(resultMap: Map<String, Any>)
}