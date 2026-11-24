package com.mxchip.mxapp.bean

data class AppUpgradeBean(
    val version: String,
    val version_code: Long,
    val remark: String?,
    val url: String,
    /**
     * 升级方式：0-提醒升级，1-强制升级 2-不用升级
     */
    val upgrade_type: Int,
    val showPolicyVersion: String?
)
