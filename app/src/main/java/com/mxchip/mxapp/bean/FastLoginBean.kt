package com.mxchip.mxapp.bean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/16
 * @desc:
 */
data class FastLoginBean(
    val token: String?,
    val phone: String?,
    val is_first: Int,
    val identityid: String?
)
