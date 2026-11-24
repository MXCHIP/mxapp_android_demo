package com.mxchip.mxapp.bean
data class WeatherBean(
    val icon_image: String = "", //	天气 icon 地址
    val icon_phrase: String, //	天气描述
    val temperature: String, //	温度，单位 ℃
    val wind_level: String, //	风力等级
    val wind_direction: String, //	风向
    val relative_humidity: Int?, //	湿度
    val city_name: String?, //	城市
    val aqi: Int?, //	空气质量指数
    val aqi_level: String?, //	空气质量等级
    val tips: String?, //	建议
    val weather_icon: Int, //	天气 icon id
)