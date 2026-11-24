package com.mxchip.mxapp.family.bean


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/24
 * @desc:
 */

data class LocationBean(
    val city_name: String,
    val country_english_name: String?,
    val country_id: String?,
    val country_name: String,
    val location_key: String,
    val province_name: String
)