package com.mxchip.mxapp.base.utils

import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import java.io.PrintWriter
import java.io.StringWriter

class MXCrashHandler : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        MXLog.e("MXCrash", sw.toString())
        MXBusinessApplication.mxBusinessApp.finishAllActivity()
    }
}