package com.mxchip.mxapp.base.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication

object AppUtil {
    fun getVersionCode(context: Context = MXBusinessApplication.mxBusinessApp): Long {
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.longVersionCode
        } else {
            pi.versionCode.toLong()
        }
    }

    fun getVersionName(): String {
        val context = MXBusinessApplication.mxBusinessApp
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        return pi.versionName ?: "1.0.0"
    }

    fun getPackageName(context: Context = MXBusinessApplication.mxBusinessApp): String {
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        return pi.packageName
    }

    @SuppressLint("MissingPermission")
    fun isNetworkAvailable(context: Context = MXBusinessApplication.mxBusinessApp): Boolean {
        val manager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
        val capabilities = manager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private var statusBarHeight = 0

    fun setStatusBarHeight(height: Int) {
        statusBarHeight = height
    }

    //获取状态栏高度
    fun getStatusBarHeight(): Int {
        val context = MXBusinessApplication.mxBusinessApp
        if (statusBarHeight == 0) {
            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
            }
        }
        val barHeight = if (statusBarHeight > 0) {
            statusBarHeight + 10.dp2px(context)
        } else {
            54.dp2px(context)
        }
        MXLog.i("statusBarHeight: $barHeight")
        return barHeight
    }

    /**
     * 当前是否是鸿蒙系统
     * 根据是否能调用Harmony JAVA API判断
     */
    fun isHarmonyOs(): Boolean {
        return try {
            Class.forName("ohos.utils.system.SystemCapability")
            true
        } catch (e: Exception) {
            false
        }
    }
}