package com.mxchip.mxapp.family

/**
 *
 */
object HomeManager {
    private var _homeLocation = true
    private var _homeShare = true

    /**
     * 设置是否显示家庭位置信息
     */
    fun setSupportHomeLocation(show: Boolean) = apply {
        _homeLocation = show
    }

    /**
     * 是否显示家庭位置信息
     */
    fun isSupportHomeLocation() = _homeLocation


    /**
     * @param show 设置是否支持家庭分享，成员邀请
     */
    fun setSupportHomeShare(show: Boolean) = apply {
        _homeShare = show
    }

    /**
     * 是否支持家庭分享，成员邀请
     */
    fun isSupportHomeShare() = _homeShare
}