package com.mxchip.mxapp.account.bean

/**
 * 区域json转化类
 */
data class CountryBean(
    val zh_name: String,
    val en_name: String,
    val server_station: String,
    val iso2: String,
    val iso3: String,
    val code: Int,
    val pinyin: String,
)