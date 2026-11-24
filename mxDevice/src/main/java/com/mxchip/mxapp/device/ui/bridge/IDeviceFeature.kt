package com.mxchip.mxapp.device.ui.bridge

import com.mxchip.mxapp.base.widget.web.CompletionHandler
import org.json.JSONObject

/**
 * 设备功能提供者接口
 * 用于统一管理所有可选功能
 */
interface IDeviceFeature {

    /**
     * 启动音乐律动
     * @param enable 是否启用 (1-启用, 0-禁用)
     */
    fun startRhythm(enable: Int, uuid: String? = null, groupAddress: Int = -1)

    /**
     * 销毁音乐律动资源
     */
    fun destroyRhythm()

    /**
     * 计算体脂数据
     * @param msg 包含体重、身高、性别、年龄等参数的JSON对象
     * @param handler 回调处理器
     */
    fun calculateBodyFat(msg: Any, handler: CompletionHandler<JSONObject>)
} 