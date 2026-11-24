package com.mxchip.mxapp.account.bean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2023/7/13
 * @desc:
 */
data class AutoLoginBean(
    val access_token: String?,
    val register_token: String?,
    val status: Int?
)