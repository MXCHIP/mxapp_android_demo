package com.mxchip.mxapp.base.consts

/**
 *
 */
object LiveDataBusKey {

    //当前家庭以及信息刷新
    const val KEY_HOME_REFRESH = "home.refresh"

    //家庭下数据刷新
    const val KEY_HOME_DATA_REFRESH = "home.data.refresh"

    //设备mesh数据
    const val KEY_DEVICE_MESH_DATA = "device.mesh.data"

    //最后一个mesh设备配对成功的Mac地址
    const val KEY_MESH_PAIR_LAST_MAC = "last.pair.mesh.mac"

    //云端socket数据
    const val KEY_DEVICE_CLOUD_DATA = "device.cloud.data"

    const val KEY_DEVICE_WARN_INFO = "device.warn.info"

    const val KEY_SHARE_PROGRESS_REFRESH = "share.progress"
}

enum class HomeDataRefresh {
    SMART_HOME, HOME, ROOM_LIST, SCENE, DEVICE, SHARED_DEVICE
}