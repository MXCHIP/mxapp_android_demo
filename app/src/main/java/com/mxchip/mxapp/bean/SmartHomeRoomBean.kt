package com.mxchip.mxapp.bean


data class SmartHomeRoomBean(
    val room_id: Int,
    val room_name: String,
    val properties: List<SmartHomePropertiesBean>,
    val scenes: List<SmartHomeSceneBean>,
    val light: List<SmartHomeLightBean>,
    val air_conditoner: List<SmartHomeAirConditonerBean>,
    val curtain: List<SmartHomeCurtainBean>,
)

data class SmartHomePropertiesBean(
    val iotid: String,
    var online: Boolean,
    val nick_name: String,
    val product_image: String,
    val identifier: String? = null,
    var value: Any? = null
)

data class SmartHomeLightBean(
    val iotid: String,
    val online: Boolean,
    val uuid: String,
    val identifies: List<String>
)

data class SmartHomeAirConditonerBean(
    val iotid: String,
    val online: Boolean,
    val uuid: String,
    val identifies: List<String>
)

data class SmartHomeCurtainBean(
    val iotid: String,
    val online: Boolean,
    val uuid: String,
    val identifies: List<String>
)

data class SmartHomeSceneBean(
    val scene_id: Int,
    val name: String,
    val vid: Int,
    val icon_image: String,
    val icon_color: String
)