package com.mxchip.mxapp.base.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/18
 * @desc:
 */
object ClipboardManager {

    fun getTextFromClip(context: Context): String? {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboardManager.hasPrimaryClip())
            return null
        val clipData = clipboardManager.primaryClip
        return if (clipData!!.getItemAt(0).text == null) "" else clipData!!.getItemAt(0).text.toString()
    }


    fun setTextToClip(context: Context, string: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Label", string)
        clipboardManager.setPrimaryClip(clipData)
    }

}