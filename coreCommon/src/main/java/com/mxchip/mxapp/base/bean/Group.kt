package com.mxchip.mxapp.base.bean

data class GroupBean(
    val group_id: Int,
    val group_type: Int = 0,
    val group_address: Int,
    var name: String,
    val room_id: Int,
    val room_name: String,
    var show: Boolean = true,
    val ctime: Long = 0L,
    val category_id: Int = -1,
    val category_group_image: String = "",
    val propertys: List<PropertyBean>? = null,
    val nodes: List<GroupNode> = listOf(),//不同API返回的字段名不一致
    val devices: List<GroupNode> = listOf()//不同API返回的字段名不一致
) 


data class GroupNode(
    val device_id: Int?,
    val iotid: String?,
    val uuid: String?,
    val nick_name: String?,
    val product_key: String?,
    val product_name: String?,
    val product_image: String?,
    val room_id: Int?,
    val room_name: String?,
    val valid: Boolean?,
    var status: Int?,
    val ruleid: Int? = null,
    val service: Int? = null,//服务索引
    val is_master: Boolean? = null, //是否是 master
) 