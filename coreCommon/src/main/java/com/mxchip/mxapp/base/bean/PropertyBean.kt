package com.mxchip.mxapp.base.bean

/**
 * 设备属性
 */

data class PropertyBean(
    val identifier: String,
    var name: String? = null,
    var value: Any? = null,
    var compare_type: String? = null,
    var dataType: DataType? = null,
    var duration: Int? = null, //持续时间
    val one_click_action: Boolean? = null,
    val local_auto_condtion: Boolean? = null,
    val local_auto_action: Boolean? = null,
    val gateway_auto_condtion: Boolean? = null,
    val gateway_auto_action: Boolean? = null,
    val cloud_auto_condtion: Boolean? = null,
    val cloud_auto_action: Boolean? = null,
) {
    @Transient
    var isChecked: Boolean = false

    @Transient
    var iotId: String? = null //标记是哪个设备的属性

    @Transient
    var roomId: Int = -1 //标记是哪个房间的属性

    override fun equals(other: Any?): Boolean {
        if (other !is PropertyBean) return false
        return other.identifier == identifier
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}

/**
 *
 */

data class DataType(
    val type: String,
    val specs: Any? = null
) 