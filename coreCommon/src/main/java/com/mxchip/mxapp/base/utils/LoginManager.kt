package com.mxchip.mxapp.base.utils

import com.mxchip.apicore.utils.MXApiConfigUtil
import com.tencent.mmkv.MMKV

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/12
 * @desc:
 */
object LoginManager {
    private const val TOKEN = "login.token"
    private const val ACCOUNT = "login.account"
    private const val PASSWORD = "login.password"

    private const val SELECT_CODE_STATE = "select.region.state"

    private val mmkv by lazy {
        MMKV.mmkvWithID("login")
    }

    fun isLogin(): Boolean = getToken().isNotEmpty()

    fun saveToken(token: String) {
        MXApiConfigUtil.setToken(token) // Save token to MXApiConfigUtil
        mmkv.encode(TOKEN, token)
    }

    fun getToken(): String = mmkv.decodeString(TOKEN, "")!!

    fun removeToken() {
        MXApiConfigUtil.setToken("") // Clear token in MXApiConfigUtil
        mmkv.remove(TOKEN)
    }

    fun saveLoginInfo(account: String, password: String) {
        mmkv.putString(ACCOUNT, account).putString(PASSWORD, password).commit()
    }

    fun saveAccount(account: String) {
        mmkv.encode(ACCOUNT, account)
    }

    fun getAccount(): String = mmkv.decodeString(ACCOUNT, "")!!

    fun savePassword(password: String) {
        mmkv.encode(PASSWORD, password)
    }

    fun getPassword(): String = mmkv.decodeString(PASSWORD, "")!!

    fun saveSelectRegionCode(iso3: String) {
        mmkv.encode(SELECT_CODE_STATE, iso3)
    }

    fun getSelectRegionCode() = mmkv.decodeString(SELECT_CODE_STATE)
}