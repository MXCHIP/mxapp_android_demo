package com.mxchip.mxapp.base.bean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/8/19
 * @desc:
 */
data class RoomBean(
    val room_id: Int,
    val name: String,
    val is_default: Boolean = false,
    val group_address: Int = 0,
    val room_address: Int = 0,
    val light_group_address: Int = 0,
    val switch_group_address: Int = 0,
    var device_count: Int = 0,
    val scene_count: Int = 0,
    val group_count: Int = 0,
    val auto_count: Int = 0,
) {
    @Transient
    var unWrittenDeviceCount = 0 //ui状态
}