package com.mxchip.mxapp.base.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/25
 * @desc:
 */
object PermissionUtil {

    private const val TAG = "PermissionUtil"

    private fun nativeManager(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    }

    fun gotoManager(context: Context, os: String) {
        when (os.lowercase()) {
            "huawei" -> {
                try {
                    val intent = Intent()
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("packageName", context.packageName)
                    val comp = ComponentName(
                        "com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity"
                    )
                    intent.component = comp
                    context.startActivity(intent)
                } catch (e: Exception) {
                    nativeManager(context)
                }
            }

            "xiaomi" -> {
                try {
                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                    val componentName = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
                    )
                    intent.component = componentName
                    intent.putExtra("extra_pkgname", context.packageName)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    nativeManager(context)
                }
            }

            "meizu" -> {
                try {
                    val intent = Intent("com.meizu.safe.security.SHOW_APPSEC")
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.putExtra("packageName", context.packageName)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    nativeManager(context)
                }
            }

            "oppo" -> {
                try {
                    val intent = Intent()
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("packageName", context.packageName)
                    val comp = ComponentName(
                        "com.color.safecenter",
                        "com.color.safecenter.permission.PermissionManagerActivity"
                    )
                    intent.component = comp
                    context.startActivity(intent)
                } catch (e: Exception) {
                    nativeManager(context)
                }
            }

            "lg" -> {
                try {
                    val intent = Intent("android.intent.action.MAIN")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("packageName", context.packageName)
                    val comp = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AccessLockSummaryActivity"
                    )
                    intent.component = comp
                    context.startActivity(intent)
                } catch (e: Exception) {
                    nativeManager(context)
                }
            }

            "sony" -> {
                try {
                    val intent = Intent()
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("packageName", context.packageName)
                    val comp = ComponentName(
                        "com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity"
                    )
                    intent.component = comp
                    context.startActivity(intent)
                } catch (e: Exception) {
                    nativeManager(context)
                }
            }

            else -> nativeManager(context)
        }
    }

    fun isLocationEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun remindStartLocateService(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun remindStartBlueService(context: Context) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun checkFindBlePermission(
        context: Context,
        permissionResult: ((locationPermissionState: Boolean, locationSwitchState: Boolean, bluePermissionState: Boolean, blueSwitchState: Boolean) -> Unit)? = null
    ) {
        val locationPermissionState: Boolean
        val bluePermissionState: Boolean
        val locationSwitchState: Boolean
        //android12以下需要位置权限，12版本以上需要请求BLUETOOTH_SCAN和BLUETOOTH_CONNECT权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluePermissionState = XXPermissions.isGranted(
                context,
                Permission.BLUETOOTH_SCAN,
                Permission.BLUETOOTH_CONNECT,
            )
            locationPermissionState = true
            //位置开关 android12不需要位置权限，直接返回true todo 鸿蒙系统需要位置开关开启
            locationSwitchState = if (isHarmonyOs()) isLocationEnable(context) else true
        } else {
            bluePermissionState = true
            locationPermissionState = XXPermissions.isGranted(
                context, Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION
            )
            locationSwitchState = isLocationEnable(context)
        }

        val bleAdapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        //蓝牙开关
        val bleSwitchState = bleAdapter.isEnabled
        MXLog.d(
            TAG,
            "locationPermissionState->$locationPermissionState，locationSwitchState->$locationSwitchState \n bluePermissionState->$bluePermissionState，bleSwitchState->$bleSwitchState"
        )
        permissionResult?.invoke(
            locationPermissionState,
            locationSwitchState,
            bluePermissionState,
            bleSwitchState
        )
    }

    /**
     * 当前是否是鸿蒙系统
     * 根据是否能调用Harmony JAVA API判断
     */
    private fun isHarmonyOs(): Boolean {
        return try {
            Class.forName("ohos.utils.system.SystemCapability")
            true
        } catch (e: Exception) {
            false
        }
    }
}