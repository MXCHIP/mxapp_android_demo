package com.mxchip.mxapp.pair.bean

import com.mxchip.mxapp.pair.consts.PairState
import com.mxchip.scanner.bean.MxBluetoothDevice

/**
 *配网设备数据组装类
 * @param extendedData 扩展的设备数据，根据deviceType对应
 * @param pairState 配网设备状态
 * @param linkTypeId 配网类型id[MxLinkType]
 *
 */
data class PairDeviceBean(
    val icon: String,
    val name: String,
    val mac: String,
    val pk: String,
    val cloudPlatform: Int,
    val linkTypeId: Int,
    val compositeProductKey: String? = null//复合产品的PK
) {
    var extBluetoothDevice: MxBluetoothDevice? = null
    var productId: Int? = null
    var deviceName: String? = null
    var isSelected: Boolean = false
    var pairState: PairState? = null

    override fun equals(other: Any?): Boolean {
        if (other is PairDeviceBean) {
            return other.mac == mac && other.pk == pk
        }
        return false
    }
}