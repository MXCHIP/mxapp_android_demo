package com.mxchip.mxapp.bean

data class RoomDeviceManagerTitleBean(
    val title: String,
    val roomId: Int,
    var dataCount: Int,
    var isSelected: Boolean = false
)