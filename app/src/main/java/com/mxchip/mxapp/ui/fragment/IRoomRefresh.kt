package com.mxchip.mxapp.ui.fragment

import org.json.JSONObject

interface IRoomRefresh {

    fun onRefresh(refreshCompleteInvoke: (() -> Unit)?)

    fun meshMessageData(dtMap: Map<String, Any>)

    fun ilopMessageData(ilopData: JSONObject)
}