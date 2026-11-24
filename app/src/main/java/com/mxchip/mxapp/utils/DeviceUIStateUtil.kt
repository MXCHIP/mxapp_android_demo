package com.mxchip.mxapp.utils

import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.sdk.mesh.MXMesh
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
object DeviceUIStateUtil {

    var isDebugH5 = false
    var debugH5URL = ""

    //key=uuid，value={"LightSwitch":1}
    val meshPropertiesMap by lazy { ConcurrentHashMap<String, JSONObject>() }

    fun parseSwitchState(device: DeviceBean): Int {
        if (device.uuid.isNotEmpty()) {
            if (MXMesh.isConnected()) {//mesh 在线
                //只允许一个主属性的在卡片显示开关状态
                if (device.propertys?.size == 1) {
                    meshPropertiesMap[device.uuid]?.apply {
                        val identifier = device.propertys!![0].identifier
                        if (this.has(identifier)) {
                            return this.getInt(identifier)
                        }
                    }
                }
            }
        }

        if (device.online) {//云端在线
            //只允许一个主属性的在卡片显示开关状态
            if (device.propertys?.size == 1) {
                device.propertys!![0].value?.apply {
                    if (this is Number)
                        return this.toInt()
                }
            }
        }
        return 2
    }
}