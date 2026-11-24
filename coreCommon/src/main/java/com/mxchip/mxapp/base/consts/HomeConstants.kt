package com.mxchip.mxapp.base.consts

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/18
 * @desc:
 */
object HomeConstants{
    const val KEY_HOME_BEAN = "home.bean"
    const val KEY_HOME_ID = "home.id"
    const val KEY_HOME_NAME = "home.name"

    const val KEY_MEMBER_ID = "member.id"
    const val KEY_MEMBER_NAME = "member.name"
    const val KEY_MEMBER_AVATAR = "member.avatar"
    const val KEY_MEMBER_ACCOUNT = "member.account"
    const val KEY_SELECT_MEMBER_ROLE = "member.role" //成员列表中，选中的成员角色
    const val KEY_SELECT_HOME_OWNER_ROLE = "current.user.role" // 当前账号在选择的家庭中的角色

    const val KEY_FAMILY_COUNT = "family.count"
    const val KEY_ROOM_IDS = "room.ids"

    const val JOIN_HOME_TYPE_INVITE_CODE = 0
    const val JOIN_HOME_TYPE_QRCODE = 1

    const val LOCATION_SELECTED = "location.selected"

    const val KEY_ROOM_PERMISSION_TYPE = "room.permission.type"
    const val TYPE_QRCODE_INVITE = 0 //二维码
    const val TYPE_CODE_INVITE = 1 //邀请码
    const val TYPE_ACCOUNT_INVITE = 2 //账号
    const val TYPE_MEMBER_SET_PERMISSION = 3 //家庭所有者设置成员的角色和权限
}
