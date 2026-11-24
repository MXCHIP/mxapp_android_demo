package com.mxchip.mxapp.family.bean

import com.mxchip.mxapp.base.consts.RoleConstants


data class HomeBean(
 val home_id: Int,
 val network_key: String,
 val app_key: String,
 var name: String = "",
 var room_count: Int = 0,
 var device_count: Int = 0,
 var member_count: Int = 0,
 var current_home: Boolean = false,
 val address: String?,
 val country_name: String?,
 val province_name: String?,
 val city_name: String?,
 val location_key: String?,
 val role: Int = RoleConstants.COMMON,//0 - 家庭拥有者（管理员）；1 - 管理员； 2 -  普通成员
 val show_whole_home: Boolean = false //是否展示全屋页面； false - 不展示；true - 展示
) 