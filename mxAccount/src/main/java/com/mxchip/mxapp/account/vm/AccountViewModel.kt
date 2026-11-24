package com.mxchip.mxapp.account.vm

import androidx.lifecycle.MutableLiveData
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.base.BaseViewModel
import com.mxchip.mxapp.account.api.AccountRepository
import com.mxchip.mxapp.base.utils.RegexUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/6/23
 * @desc:
 */
class AccountViewModel : BaseViewModel() {

    private val repository by lazy { AccountRepository() }
    val loginState = MutableLiveData<NetStateResponse>()
    val registerState = MutableLiveData<NetStateResponse>()
    val sendVerState = MutableLiveData<NetStateResponse>()
    val resetPwdLoginState = MutableLiveData<NetStateResponse>()
    val docInfoState = MutableLiveData<NetStateResponse>()
    private val _batchDocState = MutableStateFlow<Map<String, String>?>(null)
    val batchDocState: StateFlow<Map<String, String>?> = _batchDocState

    fun login(
        username: String,
        pwd: String,
        clientId: String,
        area: String,
        isEmail: Boolean,
        countryCode: String
    ) {
        launchMain {
            val params = mapOf(
                "account" to username,
                "password" to pwd,
                "clientid" to clientId,
                "area" to area,
                "account_type" to if (isEmail) 1 else 0,
                "country_code" to countryCode
            )
            launchFlowUnit(loginState) {
                repository.login(params)
            }
        }
    }

    fun resetPwdLogin(
        account: String,
        pwd: String,
        code: String,
        clientId: String,
        area: String,
        isEmail: Boolean,
        countryCode: String
    ) {
        launchMain {
            val params = mapOf(
                "code" to code,
                "account" to account,
                "password" to pwd,
                "clientid" to clientId,
                "area" to area,
                "account_type" to if (isEmail) 1 else 0,
                "country_code" to countryCode
            )
            launchFlowUnit(resetPwdLoginState) {
                repository.resetPwdLogin(params)
            }
        }
    }

    fun register(
        account: String,
        pwd: String,
        code: String,
        clientId: String,
        area: String,
        isEmail: Boolean,
        countryCode: String
    ) {
        launchMain {
            val params = mapOf(
                "code" to code,
                "account" to account,
                "password" to pwd,
                "clientid" to clientId,
                "area" to area,
                "account_type" to if (isEmail) 1 else 0,
                "country_code" to countryCode
            )
            launchFlowUnit(registerState) {
                repository.register(params)
            }
        }
    }

    /**
     * @param scene 1:注册 0:登录
     */
    fun sendVerifyCode(type: Int, scene: Int, account: String, area: String?) {
        launchMain {
            val params = HashMap<String, Any>()
            params["account_type"] = type
            params["scene"] = scene
            if (type == 0)
                params["phone"] = account
            else
                params["email"] = account
            if (!area.isNullOrBlank()) params["area"] = area
            launchFlowUnit(sendVerState) { repository.sendVerifyCode(params) }
        }
    }

    fun checkVerifyCode(code: String, phone: String): Flow<NetStateResponse> {
        val params = HashMap<String, Any>()
        params["code"] = code
        if (RegexUtil.isEmail(phone)) {
            params["email"] = phone
            params["account_type"] = 1
        } else {
            params["area"] = "86"
            params["phone"] = phone
        }
        return launchFlow {
            repository.checkVerifyCode(params)
        }
    }

    fun checkVerifyCodeFlow(
        code: String,
        area: String,
        phone: String,
        isEmail: Boolean
    ): Flow<NetStateResponse> {
        val params = HashMap<String, Any>()
        params["code"] = code
        params["area"] = area
        if (isEmail) {
            params["email"] = phone
            params["account_type"] = 1
        } else {
            params["phone"] = phone
        }
        return launchFlow { repository.checkVerifyCode(params) }
    }

    fun checkAccountExist(
        type: Int,
        account: String,
        area: String?
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["account_type"] = type
        param["account"] = account
        if (!area.isNullOrBlank()) param["area"] = area
        return launchFlow { repository.checkAccountExist(param) }
    }

    fun loginByVerCode(
        type: Int,
        account: String,
        code: String,
        area: String?,
        countryCode: String,
        clientId: String? = null
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["account_type"] = type
        param["account"] = account
        param["code"] = code
        param["country_code"] = countryCode
        if (!clientId.isNullOrBlank()) param["clientid"] = clientId
        if (!area.isNullOrBlank()) param["area"] = area
        return launchFlow { repository.loginByVerCode(param) }
    }

    fun docInfo(type: Int) {
        launchMain {
            launchFlowUnit(docInfoState) {
                repository.docInfo(mapOf("doc_type" to type))
            }
        }
    }

    fun getBatchDoc() {
        launchMain {
            //隐私政策
            val policy = async {
                repository.docInfo(mapOf("doc_type" to 1))
            }
            //服务协议
            val proto = async {
                repository.docInfo(mapOf("doc_type" to 2))
            }
            val policya = policy.await()
            val protoa = proto.await()

            val batchDocMap = hashMapOf<String, String>()
            policya.data?.apply {
                val docJson = JSONObject(this)
                val title = docJson.getString("title")
                val url = docJson.getString("url")
                batchDocMap[title] = url

            }
            protoa.data?.apply {
                val docJson = JSONObject(this)
                val title = docJson.getString("title")
                val url = docJson.getString("url")
                batchDocMap[title] = url
            }
            _batchDocState.value = batchDocMap
        }
    }

    fun fastLoginSetPwd(
        password: String,
        account: String,
        token: String,
        clientid: String? = null
    ): Flow<NetStateResponse> {
        val param = mapOf(
            "password" to password,
            "account" to account,
            "token" to token,
            "clientid" to (clientid ?: "")
        )
        return launchFlow { repository.fastLoginSetPwd(param) }
    }

    fun autoLogin(param: Map<String, Any>): Flow<NetStateResponse> {
        return launchFlow { repository.autoLogin(param) }
    }

    fun addPhone(
        registerToken: String,
        account: String,
        pwd: String?,
        code: String,
        area: String,
        clientId: String?
    ): Flow<NetStateResponse> {
        val param = HashMap<String, Any>()
        param["register_token"] = registerToken
        param["phone"] = account
        pwd?.apply { //关联的手机号未注册过时，需要密码
            param["password"] = this
        }
        param["smscode"] = code
        param["area"] = area
        clientId?.apply {
            param["clientid"] = this
        }
        return launchFlow { repository.addPhone(param) }
    }
}