package com.mxchip.mxapp.bean

data class DataListTitleBean(
    val type: Int,//[DeviceTypeConstants]
    val title: String? = null,
    val describe: String? = null,
    val roomId: Int? = null,
    var dataCount: Int = 0
)