package com.mxchip.mxapp.base.bean


/**
 *设备列表数据
 * @param isSelected //ui字段
 * @param link_type_id 配网类型id; 0-未知产品类型  1-热点配网 2-一键配网 3-天猫精灵配网
 * 4-Wi-Fi批量配网 5-蓝牙辅助配网 6-BLE配对 7-蓝牙Mesh配网 8-蓝牙Mesh批量配网
 * 9-蓝牙辅助Wi-Fi配网+蓝牙Mesh配网 10-蓝牙Mesh辅助配网 11-蓝牙Mesh辅助配网_泰乐
 * @param category_id 产品类型 https://axhub.im/pro/cda42b1226ad38ba/#g=1&p=庆科品类标准物模型定义
 */

data class DeviceBean(
    val iotid: String = "",
    var room_id: Int = -1,
    var room_name: String = "",
    val product_key: String = "",
    val product_name: String = "",
    val product_image: String = "",
    val category_id: Int = -1,
    val link_type_id: Int = -1,
    val share_type: Int = 0,//是否可以被分享[0：不能 ,1：可以]
    val heartbeat_interval: Int = 0,
    val propertys: List<PropertyBean>? = null,
    val device_name: String = "",//设备 device_name （三元组 DN）
    val is_share: Boolean = false,//true - 是的分享设备； false - 不是分享设备
    val firmware_version: String? = null,//    板卡版本号
    val mcu_version: String? = null,//    mcu版本号
    val bind_time: Long? = null,//设备绑定时间戳 10位
    val uuid: String = "",
    var show: Boolean = false,  //是否显示在首页房间列表
    var nick_name: String = "",
    var online: Boolean = false,
    val device_id: Int = 0,
    val group_id: Int = 0,
    val group_type: Int = 0,//群组类型：0-灯组（mesh)，1-wifi组
    val group_address: Int = 0,
    val group_written: Boolean = false, //group 写入状态
    val category_group_image: String = "",
    val name: String = "", //群组设备时显示的名称
    var nodes: List<GroupNode>? = null,
    var node_type_v2: String? = null,  //节点类型  网关设备：gateway 网关子设备：gateway-sub 直连设备：direct
    var not_receive_message: Boolean? = null,
    val gateway_is_main: Int = -1, //是否是主网关: -1 - 未配置; 0 - 从网关; 1 - 主网关；
    val gateway_network_mode: Int = -1, //联网方式： -1 - 未配置；0- 使用以太网络；1- 使用WiFi网络
    val wifi_group_enable: Int = 0,//是否支持 wifi 组：true - 支持，false - 不支持，默认不支持；
    val number: Int = 0, //（哪一路）子设备时显示的序号
    var mac: String? = null,
) {
    fun isHideOnlineStatus(): Boolean {
        return heartbeat_interval == 0
    }

    fun isGateway() = node_type_v2 == "gateway"
}

data class DeviceOtaV3Bean(
    val oss_url: String, //	下载固件的地址
    val upgrade_type: String, // mesh 升级方式：1-提醒升级，2-强制升级
    val version: String,    //下载的固件的版本号
    val current_version: String,    //当前设备版本号
    val algorithms: Int,    //摘要算法：1-md5
    val remark: String,    //版本信息说明
    val sign: String,    //固件md5签名
    val hash: String?,    //hash值
    val sign_method: String,    //摘要算法，固定为md5
    val module: String?,    //模块名称
    val task_id: Int?,    //当前固件升级任务唯一标识
    val description: String,    //固件描述
    val need_app_confirm: Boolean = false,    //是否需要app确认（4.13.0新增）
    val ota_type: Int //fog 升级方式 1-WiFi，2-mesh
)

//设备属性关联功能
data class DevicePropertiesFunctionBean(
    val name: String,
    val product_key: String,
    val identifier: String,
    val value: Any,
    val extend_rule: RuleExtendBean? = null
)