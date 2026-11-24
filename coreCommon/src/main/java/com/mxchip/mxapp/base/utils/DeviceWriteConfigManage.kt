package com.mxchip.mxapp.base.utils

import com.mxchip.log.MXLog
import com.mxchip.mesh.rule.IRuleWrite
import com.mxchip.mesh.rule.WriteCEVT
import com.mxchip.mesh.rule.WriteConfigProperties
import com.mxchip.mesh.rule.WriteExtend
import com.mxchip.mesh.rule.WriteGroup
import com.mxchip.mesh.rule.WriteKV
import com.mxchip.mesh.rule.WritePresetGroup
import com.mxchip.mesh.rule.WriteRule
import com.mxchip.mesh.rule.WriteVirtualController
import com.mxchip.mesh.rule.bean.WriteBindSceneBean
import com.mxchip.mesh.rule.bean.WriteCEVTActionBean
import com.mxchip.mesh.rule.bean.WriteCEVTAttachBean
import com.mxchip.mesh.rule.bean.WriteCEVTBean
import com.mxchip.mesh.rule.bean.WriteExtendBean
import com.mxchip.mesh.rule.bean.WriteGroupBean
import com.mxchip.mesh.rule.bean.WriteLocalAutoBean
import com.mxchip.mesh.rule.bean.WriteOneClickBean
import com.mxchip.mesh.rule.bean.WritePropertyBean
import com.mxchip.mesh.rule.bean.WriteVirtualControllerBean
import com.mxchip.mesh.rule.utils.MeshTranscodeUtil
import com.mxchip.mxapp.base.bean.DeviceRuleBean
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.mesh.MeshCommand
import org.json.JSONObject
import java.util.regex.Pattern

/**
 *
 */
object DeviceWriteConfigManage {
    private const val TAG = "DeviceRuleManage"


    private const val REGEX_MESH_RULE_KV = "^(KV|KV_)([0-9a-zA-Z]{4})$"
    fun isMeshRuleKv(identifier: String) = Pattern.matches(REGEX_MESH_RULE_KV, identifier)

    fun parseMeshWriteRule(
        uuid: String,
        ruleBean: DeviceRuleBean?,
        homeAttrConfigJson: JSONObject,
        meshCommand: MeshCommand,
        getLocalAutoTriggerAddressByDeviceId: (Int) -> Int?
    ): List<IRuleWrite> {
        val writeRuleList = mutableListOf<IRuleWrite>()
        //先检查pk（fog以pk为key）
        val productKey = DeviceManage.getPkByUUID(uuid)
        MXLog.d(TAG, "find product pk: $productKey")
        var productStr = ProductInfoManager.getProductInfo(productKey)
        if (productStr.isNullOrEmpty()) {//fog没找到，通过pid找飞燕
            val pid = DeviceManage.getFeiYanPidByUUID(uuid).toString()
            MXLog.d(TAG, "find product pid: $pid")
            //pid换pk
            ProductInfoManager.getProductPkByPid(pid)?.apply {
                productStr = ProductInfoManager.getProductInfo(this)
            }
        }
        val productInfo = JsonUtil.fromJson(productStr, ProductInfo::class.java)
        val actionAttrConfig = if (productInfo == null) {
            MXLog.e(TAG, "product not found, use home attrConfig")
            homeAttrConfigJson
        } else {
            if (productInfo.attr_map.length < 4) {
                MXLog.i(TAG, "attrType not configured, use home attrConfig")
                homeAttrConfigJson
            } else {
                JSONObject(productInfo.attr_map)
            }
        }

        //1 属性配置
        ruleBean?.config_propertys?.apply {
            MXLog.d(TAG, "configProperties size: ${this.size}")
            forEach {
                val identifier = it.identifier
                val configValue = it.value
                //比如 "KV61FB"："01"。 那就写入 2B 00 FB 61 01， 2B 00表示KV读写读写命令
                if (isMeshRuleKv(identifier)) {
                    val key = identifier.substring(identifier.length - 4)
                    if (configValue == null) {
                        writeRuleList.add(WriteKV(meshCommand, uuid, Pair(key, null)))
                    } else {
                        val attrPairs = MeshTranscodeUtil.propertiesToAttrType(
                            actionAttrConfig,
                            mapOf(identifier to configValue),
                            false
                        )
                        if (attrPairs.isEmpty()) {//mesh转码 没有找到对应的属性
                            writeRuleList.add(
                                WriteKV(
                                    meshCommand,
                                    uuid,
                                    Pair(key, configValue)
                                )
                            )
                        } else {
                            writeRuleList.add(WriteKV(meshCommand, uuid, attrPairs.first()))
                        }
                    }
                } else {
                    if (configValue != null) {
                        when (configValue) {
                            is List<*> -> {
                                if (configValue.isEmpty()) {
                                    MXLog.w(
                                        TAG,
                                        "configProperties identifier: ${it.identifier}, value is empty"
                                    )
                                } else {
                                    writeRuleList.add(
                                        WriteConfigProperties(
                                            meshCommand,
                                            actionAttrConfig,
                                            uuid,
                                            listOf(
                                                WritePropertyBean(
                                                    it.identifier,
                                                    configValue
                                                )
                                            )
                                        )
                                    )
                                }
                            }

                            is String -> {
                                if (configValue.isEmpty()) {
                                    MXLog.w(
                                        TAG,
                                        "configProperties identifier: ${it.identifier}, value is empty"
                                    )
                                } else {
                                    writeRuleList.add(
                                        WriteConfigProperties(
                                            meshCommand,
                                            actionAttrConfig,
                                            uuid,
                                            listOf(
                                                WritePropertyBean(
                                                    it.identifier,
                                                    configValue
                                                )
                                            )
                                        )
                                    )
                                }
                            }

                            else -> {
                                writeRuleList.add(
                                    WriteConfigProperties(
                                        meshCommand,
                                        actionAttrConfig,
                                        uuid,
                                        listOf(WritePropertyBean(it.identifier, configValue))
                                    )
                                )
                            }
                        }
                    } else {
                        MXLog.w(
                            TAG,
                            "configProperties identifier: ${it.identifier}, value is null"
                        )
                    }
                }
            }
        }
        //2 群组
        if (ruleBean?.writeGroup == false) {
            val writeGPList = mutableListOf<WriteGroupBean>()
            ruleBean.groups?.forEach {
                writeGPList.add(WriteGroupBean(it.address, it.is_master, it.service))
            }
            MXLog.d(TAG, "Group size: ${writeGPList.size}")
            writeRuleList.add(WriteGroup(meshCommand, uuid, writeGPList))
        }

        //3 0017规则
        if (ruleBean?.writeRule == false) {
            val writeOneList = mutableListOf<WriteOneClickBean>()
            ruleBean.one_click.forEach { click ->
                //将未写入成功的规则加入
                if (ruleBean.writeRuleSuccessfulRuleIds?.contains(click.ruleid) != true) {
                    val actions = mutableListOf<WritePropertyBean>()
                    click.actions.forEach { action ->
                        val actionValue = action.value
                        if (actionValue == null) {
                            MXLog.w(
                                TAG,
                                "oneClick actionValue is null, ruleId: ${click.ruleid}"
                            )
                        } else {
                            actions.add(WritePropertyBean(action.identifier, actionValue))
                        }
                    }
                    writeOneList.add(WriteOneClickBean(click.ruleid, click.vid, actions))
                }
            }
            val writeLocalList = mutableListOf<WriteLocalAutoBean>()
            ruleBean.local_auto.forEach { local ->
                //将未写入成功的规则加入
                if (ruleBean.writeRuleSuccessfulRuleIds?.contains(local.ruleid) != true) {
                    val actions = mutableListOf<WritePropertyBean>()
                    local.actions.forEach { action ->
                        action.value?.apply {
                            actions.add(WritePropertyBean(action.identifier, this))
                        }
                    }
                    var meshAddress = local.mesh_address
                    if (meshAddress == null) {
                        if (local.device_id == null) {
                            MXLog.e(
                                TAG,
                                "meshAddress and deviceId is null, ruleId: ${local.ruleid}"
                            )
                            return@forEach
                        } else {
                            meshAddress = getLocalAutoTriggerAddressByDeviceId(local.device_id)
                        }
                    }
                    if (meshAddress != null && local.value != null) {
                        val product = ProductInfoManager.getProductByDevice(meshAddress)
                        val triggerAttrConfig =
                            if (product == null || product.attr_map.length < 4) {
                                homeAttrConfigJson
                            } else {
                                MXLog.i(
                                    TAG,
                                    "local auto trigger use product attr: ${product.attr_map}"
                                )
                                JSONObject(product.attr_map)
                            }
                        writeLocalList.add(
                            WriteLocalAutoBean(
                                meshAddress,
                                local.ruleid,
                                local.identifier,
                                local.value,
                                triggerAttrConfig,
                                actions
                            )
                        )
                    }
                }
            }
            MXLog.d(
                TAG,
                "OneClick size: ${writeOneList.size}, LocalAuto size: ${writeLocalList.size}"
            )
            writeRuleList.add(
                WriteRule(
                    meshCommand,
                    actionAttrConfig,
                    uuid,
                    writeOneList,
                    writeLocalList
                )
            )
        }

        //4 只有插卡取电检查
        if (productInfo?.category_id == 100317) {
            //CEVT
            if (ruleBean?.writeCEVT == false) {
                val writeCEVTlList = mutableListOf<WriteCEVTBean>()
                ruleBean.cevt?.forEach {
                    val conds = mutableListOf<WritePropertyBean>()
                    it.conditions.items.forEach { condProp ->
                        condProp.propertys.forEach { prop ->
                            prop.value?.apply {
                                conds.add(WritePropertyBean(prop.identifier, this))
                            }
                        }
                    }
                    var delayTime = 0
                    var triggerMethod = 0
                    it.attachments.items.forEach { item ->
                        when (item.uri) {
                            "mx/attachment/delay/exec" -> item.params.delay_time?.apply {
                                delayTime = this
                            }

                            "mx/attachment/trigger/method" -> item.params.trigger_method?.apply {
                                triggerMethod = this
                            }
                        }
                    }
                    val actions = mutableListOf<WriteCEVTActionBean>()
                    it.actions.forEach { item ->
                        item.params.vid?.apply {
                            actions.add(WriteCEVTActionBean(this))
                        }
                    }
                    writeCEVTlList.add(
                        WriteCEVTBean(
                            it.ruleid,
                            conds,
                            WriteCEVTAttachBean(delayTime, triggerMethod),
                            actions
                        )
                    )
                }
                MXLog.d(TAG, "CEVT size: ${writeCEVTlList.size}")
                writeRuleList.add(
                    WriteCEVT(
                        meshCommand,
                        actionAttrConfig,
                        uuid,
                        writeCEVTlList
                    )
                )

            }
        }

        //5 扩展规则
        if (ruleBean?.writeExtend == false) {
            val writeERList = mutableListOf<WriteExtendBean>()
            ruleBean.extend_rule?.forEach {
//                val bindScenes = mutableListOf<WriteBindSceneBean>()
                it.event_bind_scene?.forEach { bindScene ->
//                    bindScenes.add(
//                        WriteBindSceneBean(
//                            bindScene.identifier, bindScene.value, bindScene.vid
//                        )
//                    )
                    writeERList.add(
                        WriteExtendBean(
                            it.type, listOf(
                                WriteBindSceneBean(
                                    bindScene.identifier,
                                    bindScene.value,
                                    bindScene.vid
                                )
                            )
                        )
                    )
                }
//                writeERList.add(WriteExtendBean(it.type, bindScenes))
                //因离线语音面板不支持一条指令写多个配置，调整为一条指令写一条配置
            }
            MXLog.d(
                TAG,
                "Extend size: ${writeERList.size}"
            )
            if (writeERList.isNotEmpty()) {
                writeRuleList.add(
                    WriteExtend(
                        meshCommand,
                        actionAttrConfig,
                        uuid,
                        writeERList
                    )
                )
            } else {//规则为空，直接置为写成功
                ruleBean.writeExtend = true
            }
        }

        //6 红外遥控器
        if (ruleBean?.writeRemoteControllerVirtual == false) {
            //该点位是否未写成功
            val writeVCList = mutableListOf<WriteVirtualControllerBean>()
            ruleBean.remote_controller_virtual_device?.forEach {
                writeVCList.add(WriteVirtualControllerBean(it.index, it.brand_type, it.pack_id))
            }
            MXLog.d(
                TAG,
                "RemoteControllerVirtual size: ${writeVCList.size}"
            )
            if (writeVCList.isNotEmpty()) {
                writeRuleList.add(
                    WriteVirtualController(
                        meshCommand,
                        uuid,
                        writeVCList
                    )
                )
            } else {//规则为空，直接置为写成功
                ruleBean.writeRemoteControllerVirtual = true
            }
        }

        //7 预设群组
        if (ruleBean?.writePresetSceneAddress == false) {
            val roomAddress = ruleBean.room_address
            if (roomAddress != null) {
                val product = ProductInfoManager.getProductByDevice(uuid)
                if (product?.is_scene_preset == true) {
                    MXLog.d(
                        TAG,
                        "PresetScene room address: $roomAddress"
                    )
                    writeRuleList.add(WritePresetGroup(meshCommand, uuid, roomAddress))
                } else {
                    ruleBean.writePresetSceneAddress = true
                }
            } else {
                ruleBean.writePresetSceneAddress = true
            }
        }
        return writeRuleList
    }

    fun clearAllConfig(
        writeRuleList: List<IRuleWrite>,
        isClearRule: Boolean,
        clearIndex: Int,
        clearInvoke: (Boolean) -> Unit
    ) {
        if (clearIndex >= writeRuleList.size) {
            clearInvoke(true)
            return
        }
        val write = writeRuleList[clearIndex]
        if (write is WriteRule) {
            if (isClearRule) { //当前批次 不存在 成功写入的规则，进行clear
                write.clear { clearResult -> //0017类普通规则，需要判断结果
                    if (clearResult) {
                        val next = clearIndex + 1
                        clearAllConfig(writeRuleList, isClearRule, next, clearInvoke)
                    } else {//clear失败
                        clearInvoke(false)
                    }
                }
            } else {
                val next = clearIndex + 1
                clearAllConfig(writeRuleList, false, next, clearInvoke)
            }
        } else {
            write.clear { _ ->
                val next = clearIndex + 1
                clearAllConfig(writeRuleList, isClearRule, next, clearInvoke)
            }
        }
    }

    /**
     * 替换设备写入规则
     */
    fun writeDeviceRule(
        writeRuleList: List<IRuleWrite>,
        writeRuleIndex: Int,
        writeResult: (Boolean) -> Unit,
    ) {
        if (writeRuleIndex >= writeRuleList.size) {
            writeResult.invoke(true)
            return
        }
        val ruleWrite = writeRuleList[writeRuleIndex]
        writeRule(ruleWrite) { writeState ->
            if (writeState) {
                writeDeviceRule(writeRuleList, writeRuleIndex + 1, writeResult)
            } else {
                writeResult.invoke(false)
            }
        }
    }

    fun writeRule(ruleWrite: IRuleWrite, writeResult: (Boolean) -> Unit) {
        if (ruleWrite is WriteConfigProperties || ruleWrite is WriteKV) {
            ruleWrite.writing { writeState ->
                //写入类型为设备属性配置，不检查结果
                MXLog.d(TAG, "writeState: $writeState, not check result")
                writeResult.invoke(true)
            }
        } else {
            ruleWrite.writing { writeState ->
                MXLog.d(TAG, "writeState: $writeState")
                writeResult.invoke(writeState)
            }
        }
    }
}