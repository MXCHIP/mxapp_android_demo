package com.mxchip.mxapp.device.bean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/15
 * @desc:
 */
data class BaseBean(
    val iotid: String?,
    val room_name: String?,
    val room_id: Int?,
    val product_name: String?,
    val product_image: String?,
    val nick_name: String?,

    val group_id: Int?,
    val category_group_image: String?,
    val name: String?,

    val scene_id: Int?,
    val icon_image: String?,
    val icon_color: String?
)
