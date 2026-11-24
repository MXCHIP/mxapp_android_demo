package com.mxchip.mxapp.base.router

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/6/24
 * @desc:
 */
object RouterConstants {

    //账号
    const val REGISTER_ACTIVITY = "/page_account/RegisterActivity"
    const val LOGIN_ACTIVITY = "/page_account/LoginActivity"
    const val VERIFICATION_ACTIVITY = "/page_account/VerificationActivity"
    const val PASSWORD_ACTIVITY = "/page_account/PasswordActivity"

    const val SET_PASSWORD_ACTIVITY = "/page_account/SetPasswordActivity"

    const val QR_SCAN_ACTIVITY = "/page/page_start/QrScanActivity"
    const val MAIN_ACTIVITY = "/page_start/MainActivity"
    const val LOGIN_GUIDE_ACTIVITY = "/page_start/LoginGuideActivity"
    const val PROTOCOL_ACTIVITY = "/page_start/ProtocolActivity"
    const val SELECT_REGION_ACTIVITY = "/page_count/SelectRegionCodeActivity"
    const val ACCOUNT_REGION_SEARCH_ACTIVITY = "/page_count/AccountRegionSearchActivity"
    const val APP_STATE_ACTIVITY = "/page_start/AppStateActivity"
    const val SMART_HOME_SENSOR_SETTING_ACTIVITY = "/page_start/SmartHomeSensorSettingActivity"
    const val MASTER_GATEWAY_MANAGE_ACTIVITY = "/page_start/MasterGatewayManageActivity"

    //设备
    const val DEVICE_MANAGER_ACTIVITY = "/page_device/DeviceEditActivity"
    const val DEVICE_MANAGE_SEARCH_ACTIVITY = "/page_device/DeviceManageSearchActivity"
    const val DEVICE_PANEL_ACTIVITY = "/page_device/DevicePanelActivity"
    const val DEVICE_DETAIL_ACTIVITY = "/page/device/detail"//兼容旧版本H5路由写法
    const val SHARED_DEVICE_DETAIL_ACTIVITY = "/page/device/SharedDeviceDetailActivity"
    const val PRODUCT_USE_MANUAL = "/page/device/ProductUseManual"
    const val DEVICE_RECOVERY_LIST_ACTIVITY = "/page_device/DeviceRecoveryListActivity"
    const val DEVICE_SYNC_MATTER_LIST_ACTIVITY = "/page_device/DeviceSyncMatterListActivity"
    const val SYNC_MATTER_HELP_ACTIVITY = "/page_device/SyncMatterHelpActivity"
    const val DEVICE_PHOTO_COLOR_ACTIVITY = "/page/device/cameraColoring"//兼容旧版本H5路由写法
    const val DEVICE_CAMERA_PANEL_ACTIVITY = "/page/device/DeviceCameraPanel"
    const val SHORTCUT_DEVICE_DETAIL = "/page/device/ShortcutDeviceDetailActivity"

    //同步失败
    const val DEVICE_SYNC_DATA_FAILED = "/build/page/DeviceSyncDataFailedActivity"

    //群组
    const val GROUP_PANEL_ACTIVITY = "/page_device/GroupPanelActivity"
    const val WIFI_GROUP_PANEL_ACTIVITY = "/page_device/WifiGroupPanelActivity"
    const val CREATE_WIFI_GROUP_ACTIVITY = "/page/device/CreateWifiGroupActivity"


    const val SMART_CONDITION_ACTIVITY = "/page/scene/settingProperty" // 兼容旧版本H5路由写法
    //配网
    const val PRODUCT_ACTIVITY = "/page_pair/ProductActivity"
    const val PAIRING_ACTIVITY = "/page_pair/PairingActivity"
    const val PAIR_SUCCESS_ACTIVITY = "/page_pair/PairSuccessActivity"
    const val PAIR_FAILED_ACTIVITY = "/page_pair/PairFailedActivity"
    const val PAIR_WIFI_ACTIVITY = "/page_pair/PairWIFIActivity"
    const val PAIR_GUIDE_ACTIVITY = "/page_pair/PairGuideActivity"
    const val PAIR_GUIDE_HELP_ACTIVITY = "/page_pair/PairGuideHelpActivity"
    const val PAIR_SCAN_ACTIVITY = "/page_pair/PairScanActivity"
    const val CATEGORY_SEARCH_ACTIVITY = "/page_pair/CategorySearchActivity"
    const val ONE_KEY_REPLACE_ACTIVITY = "/page_pair/ReplacePairingActivity"

    const val SELECT_REPLACE_DEVICE_ACTIVITY = "/page/mine/SelectReplaceDeviceActivity"

    //房间
    const val ROOM_MANAGE_ACTIVITY = "/page/room/RoomManageActivity"
    const val ROOM_SETTING_ACTIVITY = "/page/room/RoomSettingActivity"
    const val ROOM_WRAPPER_CONFIRM_ACTIVITY = "/page/room/RoomWrapperConfirmActivity"
    const val ROOM_SELECT_ACTIVITY = "/page/room/roomSelectActivity"
    const val ROOM_CREATE_ACTIVITY = "/page/room/RoomCreateActivity"
    const val ROOM_PERMISSION_ACTIVITY = "/page/room/RoomPermissionActivity"
    const val ROOM_SETTING_DEVICE_ACTIVITY = "/page/room/RoomDeviceManagerActivity"

    //家庭
    const val FAMILY_MANAGE_ACTIVITY = "/page/home/HomeManageActivity"
    const val FAMILY_SETTING_ACTIVITY = "/page/home/HomeEditActivity"
    const val CREATE_HOME_GUIDE_ACTIVITY = "/page/home/CreateHomeGuideActivity"
    const val CREATE_ROOM_GUIDE_ACTIVITY = "/page/home/CreateRoomGuideActivity"
    const val HOME_MEMBER_DETAIL_ACTIVITY = "/page/home/HomeMemberDetailActivity"
    const val LOCATION_ACTIVITY = "/page/home/LocationActivity"
    const val HAND_OVER_VERIFICATION_ACTIVITY = "/page/home/HandOverVerificationActivity"

    //设备分享
    const val DEVICE_SHARE_LIST_ACTIVITY = "/page/share/DeviceShareListActivity"
    const val DEVICE_SHARE_ACCOUNT_ACTIVITY = "/page/share/DeviceShareAccountActivity"
    const val MINE_DEVICE_DETAIL_ACTIVITY = "/page/share/MineDeviceDetailActivity"
    const val INVITEE_MEMBER_ACTIVITY = "/page/share/InviteMemberActivity"
    const val JOIN_HOME_BY_INVITE_CODE_ACTIVITY = "/page/share/JoinHomeByInviteCodeActivity"

    const val CENTER_CONTROL_SMART_ACTIVITY = "/page/switch/CenterControlSmartListActivity"

    //反馈
    const val FEEDBACK_EDIT_ACTIVITY = "/page/feedback/FeedbackEditActivity"
}