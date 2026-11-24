package com.mxchip.mxapp.account.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/6/23
 * @desc:
 */
class AccountRepository : BaseRepository<AccountApiService>(AccountApiService::class.java) {

    suspend fun login(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.login(params) }
    }

    suspend fun resetPwdLogin(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.resetPwdLogin(params) }
    }

    suspend fun register(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.register(params) }
    }

    suspend fun sendVerifyCode(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.sendVerifyCode(params) }
    }

    suspend fun checkVerifyCode(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.checkVerifyCode(params) }
    }

    suspend fun checkAccountExist(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.checkAccountExist(params) }
    }

    suspend fun loginByVerCode(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.loginByVerCode(params) }
    }

    suspend fun docInfo(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.docInfo(params) }
    }

    suspend fun fastLoginSetPwd(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.fastLoginSetPwd(params) }
    }

    suspend fun autoLogin(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.autoLogin(param) }
    }

    suspend fun addPhone(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.addPhone(param) }
    }
}