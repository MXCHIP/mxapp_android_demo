package com.mxchip.mxapp.device.bean

data class RoomEditWritePresetBean(
    val productKey: String,
    val deviceId: Int,
    val iotId: String,
    val uuid: String?,
    val isAdd: Boolean,
    val show: Boolean ,
    var writeResult: Boolean? = null
)