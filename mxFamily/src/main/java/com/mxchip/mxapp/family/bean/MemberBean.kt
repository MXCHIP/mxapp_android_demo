package com.mxchip.mxapp.family.bean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/18
 * @desc:
 */
data class MemberBean(
    val account: String,
    val area: String,
    val avatar_url: String,
    val member_id: Int,
    val name: String,
    val role: Int,
    val room_ids: List<Int>?
)