package com.mxchip.mxapp.device.bean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/16
 * @desc:
 */
data class ShareDeviceListBean(
    val image_url: String,
    val iotid: String,
    val name: String,
    val master_username: String?,
    val share_type: Int?
)