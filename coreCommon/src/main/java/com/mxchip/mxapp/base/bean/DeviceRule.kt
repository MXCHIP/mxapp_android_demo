package com.mxchip.mxapp.base.bean

data class DeviceRuleBean(
    val device_id: Int? = null,
    val one_click: List<RuleOneClickBean>,
    val local_auto: List<RuleLocalAutoBean>,
    val extend_rule: List<RuleExtendBean>? = null,
    val groups: List<RuleGroupBean>? = null,
    var config_propertys: List<PropertyBean>? = null,
    var remote_controller_virtual_device: List<VirtualController>? = null,
    val cevt: List<RuleCEVTBean>? = null,
    var room_address: Int? = null //预置房间组地址
){
    @Transient
    var writeRemoteControllerVirtual = false

    @Transient
    var writeGroup = false

    @Transient
    var writeRule = false

    @Transient
    var writeExtend = false

    @Transient
    var writeRuleSuccessfulRuleIds: List<Int>? = null

    @Transient
    var writeCEVT = false

    @Transient
    var writePresetSceneAddress = false

    fun resetWriteSuccessfulState() {
        writeRemoteControllerVirtual = false
        writeGroup = false
        writeExtend = false
        writeRule = false
        writeRuleSuccessfulRuleIds = null
        writeCEVT = false
        writePresetSceneAddress = false
    }
}

data class RuleOneClickBean(
    val ruleid: Int, val vid: Int, var actions: List<RuleActionBean>
) 

data class RuleActionBean(val identifier: String, val value: Any?)

data class RuleLocalAutoBean(
    val device_id: Int? = null,
    val ruleid: Int,
    val mesh_address: Int? = null,
    val identifier: String,
    val value: Any?,
    var actions: List<RuleActionBean>
)

data class RuleExtendBean(
    val type: Int,//	写入配置类型：0 - 事件绑定场景；1- 新版多事件绑定场景
    val attr_type: String? = null,
    val attr_value: String? = null,
    val opcode: String? = null,//规则内部opcode
    val bind_value: String? = null,
    val event_bind_scene: List<RuleExtendBindScene>? = null
) 

data class RuleExtendBindScene(
    val identifier: String, val value: Int, val vid: Int, val scene_id: Int
) 

data class RuleCEVTBean(
    val ruleid: Int,
    val conditions: CEVTConditionsBean,
    val attachments: ConditionsBean,
    val actions: List<ItemsBean>
)

data class CEVTConditionsBean(
    val uri: String, val items: List<CEVTConditionsPropBean>
)
data class CEVTConditionsPropBean(val propertys: List<PropertyBean>)

data class ConditionsBean(
    val uri: String,
    val items: List<ItemsBean>
)

data class ItemsBean(
    val uri: String,
    var params: ParamsBean
) {
    @Transient
    var timerRepeat: String? = null //因为定时的重复设置在生效时间段中，为了ui显示此处存一份

    @Transient
    var hideItemEdit = false
}

data class ParamsBean(
    var room_name: String? = null,
    val device_id: Int? = null,
    val product_key: String? = null,
    var iotid: String? = null,
    var propertys: List<PropertyBean>? = null,
    var group_id: Int = 0,
    var group_type: Int = 0, //0 mesh灯组，1-Wi-Fi组
    var group_address: Int? = null,
    var cron: String? = null,//"$minute $hour * * *"
    var timezoneid: String? = null,//timezoneid = TimeZone.getDefault().id
    var one_click_id: Int = 0,
    var image: String? = null,
    var icon_image: String? = null,
    var icon_color: String? = null,
    var name: String? = null,
    var written: Boolean? = null,
    val uuid: String? = null,
    val mesh_address: Int? = null,
    var message: String? = null, //发送信息
    var once: Boolean? = null,
    var valid: Boolean? = null,
    var status: Int = 0, //操作写入状态:0 新增未写入  1同步成功  2修改未同步 3删除未同步
    var scene_id: Int? = null,
    var scene_name: String? = null,
    var switch: Boolean? = null,
    var nodes: List<GroupNode>? = null,
    var ruleid: Int? = null,//此字段回传给云端
    var vid: Int? = null, //执行的场景
    var delay_time: Int? = null, //延迟触发（条件），单位ms
    var trigger_method: Int? = null, //触发方式： 0 - 边缘触发；1 - 连续触发
    var duration: Int? = null, //执行间隔/延迟执行（动作）
)  {
    @Transient
    var selected: Boolean = false
}

data class RuleGroupBean(
    val address: Int, val is_master: Boolean, val service: Int
)

data class VirtualController(
    val name: String,//"电视机"
    val index: Int,      // 下标
    val brand_name: String,//"长虹电视机", // 品牌名称
    val brand_type: Int,  // 品牌类型: 0 - 空调, 1 - 电视, 2 - 机顶盒
    val pack_id: Int,  // 品牌 pack id
) 