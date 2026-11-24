package com.mxchip.mxapp.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import com.mxchip.log.MXLog
import com.mxchip.log.MXLogStoreHelper
import com.mxchip.mesh.rule.utils.MeshRuleLogger
import com.mxchip.mxapp.base.base.AppLifecycleCallback
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.MXCrashHandler
import com.mxchip.scanner.ScannerLogger
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.nordicmesh.logger.MeshLogger
import com.tencent.mmkv.MMKV
import java.io.File
import java.util.Calendar
import java.util.Stack


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/6/21
 * @desc:
 */
open class MXBusinessApplication : Application() {

    private val stack = Stack<Activity>()

    companion object {
        private const val TAG = "MXBusinessApplication"
        lateinit var mxBusinessApp: MXBusinessApplication
            private set
    }

    private var isFirstLaunch = true
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private val lifecycleCallbacks by lazy { mutableListOf<AppLifecycleCallback>() }

    override fun onCreate() {
        super.onCreate()
        mxBusinessApp = this
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
        registerActivityLifeCycle()
        initMMKV(this)
        initLog()
        initMesh()
    }

    private fun registerActivityLifeCycle() {
        val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                stack.push(activity)
                MXLog.d(
                    TAG, "ActivityLifecycle--->onActivityCreated-->${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityStarted(activity: Activity) {
                MXLog.d(
                    TAG, "ActivityLifecycle--->onActivityStarted-->${activity.javaClass.simpleName}"
                )
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    // 检查是否是首次启动
                    if (!isFirstLaunch) {
                        // 只有不是首次启动时，才是应用从后台回到前台
                        for (callback in lifecycleCallbacks) {
                            callback.onAppForeground()
                        }
                    }
                }
            }

            override fun onActivityResumed(activity: Activity) {
                MXLog.d(
                    TAG, "ActivityLifecycle--->onActivityResumed-->${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityPaused(activity: Activity) {
                MXLog.d(
                    TAG, "ActivityLifecycle--->onActivityPaused-->${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityStopped(activity: Activity) {
                MXLog.d(
                    TAG, "ActivityLifecycle--->onActivityStopped-->${activity.javaClass.simpleName}"
                )
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // 应用进入后台
                    isFirstLaunch = false  // 设置首次启动标志为 false
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                MXLog.d(
                    TAG,
                    "ActivityLifecycle--->onActivitySaveInstanceState-->${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityDestroyed(activity: Activity) {
                stack.remove(activity)
                MXLog.d(
                    TAG,
                    "ActivityLifecycle--->onActivityDestroyed-->${activity.javaClass.simpleName}"
                )
            }
        }
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun initMMKV(context: Context) {
        MMKV.initialize(context)
    }

    private val mmkv by lazy {
        MMKV.mmkvWithID("app_log")
    }

    private fun initLog() {
        val calendar = Calendar.getInstance()
        val logDir = File(externalCacheDir, "log")
        if (logDir.exists()) {
            val currentDate = calendar.get(Calendar.DATE)
            val lastModifyDate = mmkv.decodeInt("log_date", -1)
            if (lastModifyDate != -1 && currentDate != lastModifyDate)
                logDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            mmkv.encode("log_date", currentDate)
        }

        //日志打印
        MXLog.enableLog(true)
        //将打印的日志，写入文件，不指定地址，默认：Android/Data/应用包名/cache/log/mx_log.log
        MXLogStoreHelper.enableLogStore(this, Log.DEBUG).create(this)
        MXLogStoreHelper.flush()

        //设置crash操作类
        Thread.setDefaultUncaughtExceptionHandler(MXCrashHandler())
    }

    override fun onLowMemory() {
        super.onLowMemory()
        MXLogStoreHelper.flush()
    }

    override fun onTerminate() {
        super.onTerminate()
        MXLogStoreHelper.flush()
    }

    private fun initMesh() {
        val log: ((Int, String, String) -> Unit) = { i, tag, msg ->
            MXLog.println(i, tag, msg)
        }
        ScannerLogger.setLogHandler(log)
        MeshRuleLogger.setLogHandler(log)
        MeshLogger.setLogHandler(log)
        MeshLogger.setLogLevel(Log.DEBUG)
        MXMesh.initMesh(this, true, messageQueueIntervalMillisecond = 200L)
    }

    /**
     * 结束所有Activity
     */
    fun finishAllActivity() {
        stack.forEach {
            if (!it.isFinishing) it.finish()
        }
    }

    /**
     * 保留指定的Activity，其他Activity销毁
     */
    fun finishOtherActivity(routerName: String) {
        val activitySimpleName = when (routerName) {
            RouterConstants.LOGIN_GUIDE_ACTIVITY -> "LoginGuideActivity"
            RouterConstants.LOGIN_ACTIVITY -> "UserNameActivity"
            RouterConstants.MAIN_ACTIVITY -> "MainActivity"
            else -> ""
        }
        stack.forEach {
            if (it.javaClass.simpleName.equals(activitySimpleName).not()) {
                if (!it.isFinishing) it.finish()
            }
        }
    }

    fun registerAppLifecycleCallback(callback: AppLifecycleCallback) {
        lifecycleCallbacks.add(callback)
    }

    fun unregisterAppLifecycleCallback(callback: AppLifecycleCallback) {
        lifecycleCallbacks.remove(callback)
    }
}