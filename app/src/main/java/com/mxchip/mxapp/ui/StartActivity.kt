package com.mxchip.mxapp.ui

import android.os.CountDownTimer
import androidx.activity.addCallback
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants.LOGIN_GUIDE_ACTIVITY
import com.mxchip.mxapp.base.router.RouterConstants.MAIN_ACTIVITY
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.databinding.ActivitySplashBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/12
 * @desc:
 */
class StartActivity : MXBusinessActivity<ActivitySplashBinding>() {

    override fun getLayoutBinding() = ActivitySplashBinding.inflate(layoutInflater)

    companion object {
        private const val TAG = "StartActivity"
    }

    override fun onInit() {
        onBackPressedDispatcher.addCallback {
            // 禁止
        }
        binding.versionName.text = "V${AppUtil.getVersionName()}"
        countdown(200L)
    }

    private fun countdown(millisInFuture: Long, isADCountDown: Boolean = false) {
        val timer = object : CountDownTimer(millisInFuture, 100) {
            override fun onTick(millisUntilFinished: Long) {
                MXLog.d(TAG, "CountDownTimer onTick: $millisUntilFinished")
            }

            override fun onFinish() {
                checkLogin()
            }
        }
        timer.start()
    }

    private fun checkLogin() {
        if (LoginManager.isLogin()) {
            RouterManager.navigation(MAIN_ACTIVITY)
        } else {
            RouterManager.navigation(LOGIN_GUIDE_ACTIVITY)
        }
        finish()
    }
}