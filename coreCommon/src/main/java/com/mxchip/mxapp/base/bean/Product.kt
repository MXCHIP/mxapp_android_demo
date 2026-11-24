package com.mxchip.mxapp.base.bean

data class ProductInfo(
    val category_id: Int,
    val cloud_platform: Int,
    val product_key: String,
    val name: String,
    val image: String,
    val feiyan_product_id: String,
    val link_type_id: Int,
    val heartbeat_interval: Int, //设备心跳间隔
    val is_scene_preset: Boolean = false,
    val not_receive_message: Boolean = false, //仅发不收设备: true-[不能写规则],false-[能写规则]
    val node_type_v2: String?, //节点类型  网关设备：gateway 网关子设备：gateway-sub 直连设备：direct
    val protocol_type_v2: String?, //通讯方式 "BLE Mesh","WiFi",""WiFi+BLE","WiFi+BLE Mesh"
    val matter: Int = 0, //是否支持matter 0 不支持  1支持
    val attr_map: String = "",//产品mesh转码
    val hide: Boolean = false, //是否隐藏
    val is_composite: Int = 0, //是否是复合产品：0-不是，1-是
    val composite_products: List<CompositeProductInfo> = listOf(),//复合产品关联产品列表
)  {

    fun isGateWay() = node_type_v2 == "gateway"
}


data class CompositeProductInfo(
    val index: Int,
    val product_key: String,
) 
