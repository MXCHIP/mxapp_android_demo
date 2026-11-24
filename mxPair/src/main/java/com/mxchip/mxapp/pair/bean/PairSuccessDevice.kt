package com.mxchip.mxapp.pair.bean

import com.squareup.moshi.JsonClass

/**
 * 配网成功设备
 */
@JsonClass(generateAdapter = true)
data class PairSuccessDevice(
    val iotid: String,
    var nick_name: String? = null,
    var show: Boolean = true,
    var room_id: Int? = null
) {
    @Transient
    var icon: String? = null

    @Transient
    var productKey: String? = null

    @Transient
    var productName: String? = null

    @Transient
    var uuid: String? = null

    @Transient
    var isPresetScene: Boolean = false
    override fun equals(other: Any?): Boolean {
        return if (other is PairSuccessDevice)
            other.iotid == this.iotid
        else
            false
    }

    override fun hashCode(): Int {
        return iotid.hashCode()
    }
}