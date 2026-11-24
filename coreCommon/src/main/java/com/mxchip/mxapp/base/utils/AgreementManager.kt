package com.mxchip.mxapp.base.utils

import com.tencent.mmkv.MMKV

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/12
 * @desc:
 */
object AgreementManager {

    private const val AGREEMENT = "agreement.state"

    private val mmkv by lazy {
        MMKV.mmkvWithID("agreement")
    }

    fun agree() {
        mmkv.encode(AGREEMENT, true)
    }

    fun isAgree(): Boolean {
        return mmkv.decodeBool(AGREEMENT, false)
    }
}