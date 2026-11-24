package com.mxchip.mxapp.pair.consts

object PairConstants {
    const val KEY_WIFI_REQUIRED = "wifi_required"
    const val KEY_CATEGORY_LIST = "category.list"

    const val CLOUD_PLATFORM_FEIYAN = 1
    const val CLOUD_PLATFORM_FOG = 2
}

enum class PairState {
    //配网开始,设备mesh连接
    PAIR_STATE_START,

    //配网中步骤-获取4元组
    PAIR_STEP_GET_QUADRUPLES,

    //配网中步骤-设置Wi-Fi信息
    PAIR_STEP_SET_WIFI,

    //同步完成
    SYNCING,

    BINDING,

    //配网成功
    PAIR_STATE_SUCCESS,

    PAIR_STATE_FAIL,
}

enum class MxLinkType(val code: Int) {
    // 文档：https://mxchip.yuque.com/xofya6/hszhff/du6rrr#W3PxM
    ALI_BROADCAST(2),//一键配网
    ALI_TMALL(3),//天猫精灵配网
    ALI_WIFI_BATCH(4),//Wi-Fi批量配网
    ALI_BLE(5),//蓝牙辅助配网
    MXCHIP_MESH(7),//蓝牙Mesh配网
    MXCHIP_MESH_WIFI(10),// 蓝牙Mesh辅助配网
    MXCHIP_MESH_WIFI_2(11),// 蓝牙Mesh辅助配网_泰乐(Wi-Fi可选
    FOG_BLE_WIFI(12), //蓝牙辅助配网（fog）
    MX_BLE_MESH_SCAN(13), // 蓝牙mesh辅助配网（扫码）
}



