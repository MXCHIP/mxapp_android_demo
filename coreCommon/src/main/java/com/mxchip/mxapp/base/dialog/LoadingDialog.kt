package com.mxchip.mxapp.base.dialog

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.databinding.CoreDgLoadingBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/6/22
 * @desc:
 */
class LoadingDialog(context: Context) {

    private var binding : CoreDgLoadingBinding

    private val dialog = AlertDialog.Builder(context).create().apply {
        binding = CoreDgLoadingBinding.inflate(LayoutInflater.from(context))
        setView(binding.root)
    }

    init {
        // 设置状态栏为透明并沉浸
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        dialog.window?.statusBarColor = Color.TRANSPARENT
        dialog.window?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.window?.decorView?.background = null
        dialog.window?.setDimAmount(0f)
        dialog.setCancelable(false)
        binding.lottie.setAnimation(R.raw.core_lottie_loading)
    }

    fun show() {
        binding.lottie.playAnimation()
        dialog.show()
    }

    fun dismiss() {
        dialog.cancel()
    }
}