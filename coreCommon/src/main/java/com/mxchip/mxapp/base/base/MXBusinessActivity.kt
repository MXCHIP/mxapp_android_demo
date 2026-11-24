package com.mxchip.mxapp.base.base

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.mxchip.http.internal.HttpError
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.dialog.LoadingDialog
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/6/24
 * @desc:
 */
abstract class MXBusinessActivity<Binding : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: Binding

    /**
     * 使用viewbinding，返回参数为view实例
     */
    abstract fun getLayoutBinding(): Binding

    abstract fun onInit()

    private val loadingDialog by lazy {
        LoadingDialog(this)
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getLayoutBinding()
        enableEdgeToEdge()
        setContentView(binding.root)
        //初始化状态栏/导航栏高度
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            AppUtil.setStatusBarHeight(systemBars.top)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        onInit()
    }

    override fun onDestroy() {
        dismissLoading()
        super.onDestroy()
    }

    fun showLoading() {
        loadingDialog.show()
    }

    fun dismissLoading() {
        loadingDialog.dismiss()
    }

    fun showToast(msg: String) {
        ToastUtil.showToast(this, msg)
    }

    private var toastTime = 0L

    protected fun onException(code: Int, showNetErrToast: Boolean = true): Boolean {
        if (code == HttpError.HTTP_STATUS_ERROR
            || code == HttpError.DNS_ERROR
            || code == HttpError.TIMEOUT_ERROR
            || code == HttpError.IO_ERROR
            || code == HttpError.JSON_PARSE_ERROR
        ) {
            if (showNetErrToast) {
                val curt = System.currentTimeMillis()
                if (curt - toastTime > 2000) {
                    toastTime = curt
                    showNetErrToast()
                }
            }
            return true
        }else if (code == HttpError.UNKNOWN_ERROR){
            MXLog.d("http","unknown error")
            return true
        }
        return false
    }

    protected fun showNetErrToast() {
        lifecycleScope.launch(Dispatchers.Main) {
            ToastUtil.showToast(
                this@MXBusinessActivity,
                MXBusinessApplication.mxBusinessApp.getString(com.mxchip.mxapp.base.R.string.mx_network_error)
            )
        }
    }
}