package com.mxchip.mxapp.base.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/15
 * @desc:
 */
object ToastUtil {

    fun showToast(context: Context, msg: String, duration: Int, gravity: Int) {
        val toast = Toast.makeText(context,msg,duration)
//        toast.setText(msg)
//        toast.duration = duration
        if (gravity != -1)
            toast.setGravity(gravity, 0, 0)
        else
            toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    fun showToast(context: Context, msg: String, gravity: Int = -1) {
        showToast(context, msg, Toast.LENGTH_SHORT, gravity)
    }

    fun showLongToast(context: Context, msg: String, gravity: Int = -1) {
        showToast(context, msg, Toast.LENGTH_SHORT, gravity)
    }
}