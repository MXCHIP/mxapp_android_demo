package com.mxchip.mxapp.device.api

import com.mxchip.http.repository.BaseRepository
import com.mxchip.http.response.NetStateResponse

/**
 *
 */
class DeviceRepository : BaseRepository<DeviceAPIService>(DeviceAPIService::class.java) {

    suspend fun findSharedDevices(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.findSharedDevices(map) }
    }

    suspend fun findDevices(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.findDevices(map) }
    }

    suspend fun updateDeviceLocation(params: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.updateDeviceLocation(params) }
    }

    suspend fun getAllProperties(params: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.getAllProperties(params) }
    }

    suspend fun setProperties(map: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.setProperties(map) }
    }

    suspend fun getPanelInfo(params: Map<String, Any>): NetStateResponse {//<DevicePanelInfo> {
        return stateApiCall { apiService.getPanelInfo(params) }
    }

    suspend fun unBindDevice(params: Map<String, List<String>>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.unBindDevice(params) }
    }

    /**
     * 用户解绑分享设备
     */
    suspend fun unBindSharedDevice(params: Map<String, List<String>>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.unBindSharedDevice(params) }
    }

    suspend fun getDeviceInfo(iotId: String): NetStateResponse {//<DeviceInfoBean> {
        return stateApiCall { apiService.getDeviceInfo(iotId) }
    }

    suspend fun setDeviceName(map: Map<String, String>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.setDeviceName(map) }
    }

    suspend fun setDeviceShow(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.setDeviceShow(map) }
    }

    suspend fun setCommonShow(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.setCommonShow(map) }
    }

    suspend fun updateOTAVersion(params: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.updateOTAVersion(params) }
    }

    suspend fun getOtaUpgradeInfoV3(params: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getOtaUpgradeInfoV3(params) }
    }

    suspend fun hasOtaUpgrade(params: Map<String, Any>): NetStateResponse {//<OtaUpgradeRemindBean> {
        return stateApiCall { apiService.hasOtaUpgrade(params) }
    }

    suspend fun sendCloudMeshMessage(param: Map<String, Any>): NetStateResponse {
        return stateApiCall {
            apiService.sendCloudMeshMessage(param)
        }
    }

    suspend fun getRecoveryStatus(map: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getRecoveryStatus(map) }
    }

    suspend fun findRecoveryDeviceList(map: Map<String, Int>): NetStateResponse {//<BaseListWrapperData<DeviceWriteStateBean>> {
        return stateApiCall { apiService.findRecoveryDeviceList(map) }
    }

    suspend fun startHomeRecovery(map: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.startHomeRecovery(map) }
    }

    suspend fun getRecoveryDeviceRule(map: Map<String, Any>): NetStateResponse {//<DeviceRuleBean> {
        return stateApiCall { apiService.getRecoveryDeviceRule(map) }
    }

    suspend fun updateRecoveryDeviceWriteState(map: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.updateRecoveryDeviceWriteState(map) }
    }

    suspend fun recoveryCompleted(map: Map<String, Any>): NetStateResponse {//<Any> {
        return stateApiCall { apiService.recoveryCompleted(map) }
    }

    suspend fun roomListV4(params: Map<String, Any>): NetStateResponse {//<BaseListWrapperData<RoomBean>> {
        return stateApiCall { apiService.roomListV4(params) }
    }

    suspend fun updateMasterGateway(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.updateMasterGateway(param) }
    }

    suspend fun findShortcutDevice(param: Map<String, Int>): NetStateResponse {
        return stateApiCall { apiService.findShortcutDevice(param) }
    }

    suspend fun getShortcutDeviceInfo(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getShortcutDeviceInfo(param) }
    }

    suspend fun getDeviceShortcutConfig(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.getDeviceShortcutConfig(param) }
    }

    suspend fun updateDeviceShortcutConfig(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.updateDeviceShortcutConfig(param) }
    }

    suspend fun updateDeviceIdentifierName(param: Map<String, String>): NetStateResponse {
        return stateApiCall { apiService.updateDeviceIdentifierName(param) }
    }

    suspend fun updateRoomDevice(param: Map<String, Any>): NetStateResponse {
        return stateApiCall { apiService.updateRoomDevice(param) }
    }

    suspend fun findDeviceAlarms(homeId: Int): NetStateResponse {
        return stateApiCall {
            apiService.findDeviceAlarms(homeId)
        }
    }

    suspend fun getProductUseManual(productKey: String): NetStateResponse {
        return stateApiCall {
            apiService.getProductUseManual(productKey)
        }
    }
}