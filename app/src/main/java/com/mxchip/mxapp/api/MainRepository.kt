package com.mxchip.mxapp.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/13
 * @desc:
 */
class MainRepository : BaseRepository<MainApiService>(MainApiService::class.java) {

    suspend fun checkUpdateApp(version: String): NetStateResponse {
        return stateApiCall { apiService.checkUpdateApp(version) }
    }

    suspend fun getStartupAD(): NetStateResponse {
        return stateApiCall { apiService.getStartupAD() }
    }

    suspend fun getWebSocket(): NetStateResponse {
        return stateApiCall {
            apiService.getWebSocket()
        }
    }

    suspend fun getMQTT(): NetStateResponse {
        return stateApiCall {
            apiService.getMQTT()
        }
    }

    suspend fun findHomeAttrConfig(): NetStateResponse {
        return stateApiCall { apiService.findHomeAttrConfig() }
    }

    suspend fun privacyChange(): NetStateResponse {
        return stateApiCall { apiService.privacyChange() }
    }

    suspend fun readPrivacy(param: Map<String, Int>): NetStateResponse {
//        val param = mapOf("doc_id" to docId)
        return stateApiCall { apiService.readPrivacy(param) }
    }

    suspend fun fastLogin(param: Map<String, String>): NetStateResponse {
//        val param = mapOf(
//            "mob_token" to mob_token,
//            "op_token" to opToken,
//            "operator" to operator,
//            "clientid" to clientid
//        )
        return stateApiCall { apiService.fastLogin(param) }
    }

    suspend fun userStat(): NetStateResponse {
        return stateApiCall { apiService.userStat() }
    }

    suspend fun logout(): NetStateResponse {
        return stateApiCall { apiService.logout() }
    }

    suspend fun checkAppUpgrade(): NetStateResponse {
        return stateApiCall { apiService.checkAppUpgrade() }
    }

    suspend fun updateUserInfo(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.updateUserInfo(params) }
    }

    suspend fun setMeshSeq(params: Map<String, Int>): NetStateResponse {
        return stateApiCall {
            apiService.setMeshSeq(params)
        }
    }

    suspend fun getMeshAddress(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.getMeshAddress(params)
        }
    }

    suspend fun getMeshSeq(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.getMeshSeq(params)
        }
    }

    suspend fun getHomeMeshConfig(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.getHomeMeshConfig(params)
        }
    }

    suspend fun findSmartHomeRoomList(homeId: Int): NetStateResponse {
        return stateApiCall {
            apiService.findSmartHomeRoomList(homeId)
        }
    }

    suspend fun findRoomSensorList(params: Map<String, Int>): NetStateResponse {
        return stateApiCall {
            apiService.findRoomSensorList(params)
        }
    }

    suspend fun settingRoomUseSensor(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.settingRoomUseSensor(params)
        }
    }

    suspend fun setRoomProperties(params: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.setRoomProperties(params)
        }
    }

    suspend fun getRecommendSceneList(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getRecommendSceneList(param) }
    }
}