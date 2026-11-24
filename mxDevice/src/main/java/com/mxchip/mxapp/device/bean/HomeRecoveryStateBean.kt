package com.mxchip.mxapp.device.bean

data class HomeRecoveryStateBean(
    val recovery_status: Int    //家庭一键恢复状态：0 - 没有备份数据；1 - 未开始；2 - 进行中
)