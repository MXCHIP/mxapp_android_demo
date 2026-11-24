package com.mxchip.mxapp.base.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.mxchip.mxapp.base.MXBusinessApplication

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/31
 * @desc:
 */
object NotificationUtil {

    private const val CHANNEL_ID = "mxapp_channel"
    private const val CHANNEL_NAME = "mxapp_channel_NAME"

    private val manager by lazy {
        val manager = MXBusinessApplication.mxBusinessApp.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.apply {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                this.createNotificationChannel(channel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotificationBuilder(context: Context, title: String?, content: String?, @DrawableRes smallIcon: Int, ongoing: Boolean = false,  autoCancel: Boolean = false): Notification.Builder {
        return Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIcon)
            .setOngoing(ongoing)
            .setAutoCancel(autoCancel)
    }

    /**
     * 检查通知权限 (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 以下默认有通知权限
        }
    }

    fun getNotificationCompatBuilder(context: Context, title: String?, content: String?, @DrawableRes smallIcon: Int, ongoing: Boolean = false,  autoCancel: Boolean = false): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIcon)
            .setOngoing(ongoing)
            .setAutoCancel(autoCancel)
    }

    fun getNotificationManager() = manager
}