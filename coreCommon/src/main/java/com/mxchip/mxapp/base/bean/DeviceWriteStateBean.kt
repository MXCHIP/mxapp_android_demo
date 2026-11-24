package com.mxchip.mxapp.base.bean


data class DeviceWriteStateBean(
    val iotid: String = "",
    val uuid: String = "",
    val room_id: Int? = null,
    val room_name: String? = null,
    val product_key: String? = null,
    val product_name: String? = null,
    val product_image: String? = null,
    val nick_name: String? = null,
    val node_type_v2: String? = null, //节点类型  网关设备：gateway 网关子设备：gateway-sub 直连设备：direct
    val recovery_status: Int = 0,//一键恢复设备写入状态： 0 - 未写入；1 - 写入
    val groupWriteType: Int? = null //群组： 0-移除，1-添加
) {
    @Transient
    var writeDescription: String? = null //描述信息展示

    @Transient
    var writeState: Int = -1 //STATE_WRITING = 0 ,STATE_SUCCESS = 1 ,STATE_FAILED = 2

    override fun equals(other: Any?): Boolean {
        if (other !is DeviceWriteStateBean) {
            return false
        }
        return iotid == other.iotid
    }

    override fun hashCode(): Int {
        return iotid.hashCode()
    }

    fun isGateWay() = node_type_v2 == "gateway"
}

