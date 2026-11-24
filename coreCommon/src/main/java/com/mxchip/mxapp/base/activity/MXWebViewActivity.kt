package com.mxchip.mxapp.base.activity

import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mxchip.router.annotation.Router
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.databinding.CoreAcWebviewBinding
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.NightModeManager

/**
 * webView展示内容
 */
@Router(RouterConstants.PROTOCOL_ACTIVITY)
class MXWebViewActivity : MXBusinessActivity<CoreAcWebviewBinding>() {

    companion object {
        const val TOP_BAR_TITLE = "top.bar.title"
        const val LOAD_URL = "load.url"
        const val SHOW_TITLE = "show.title"
    }

    override fun getLayoutBinding() = CoreAcWebviewBinding.inflate(layoutInflater)

    override fun onInit() {
        inject()
        initWebView()
        val showTopBar = intent.getBooleanExtra(SHOW_TITLE, true)
        binding.toolbar.visibility = if (showTopBar) View.VISIBLE else View.GONE
        val title = intent.getStringExtra(TOP_BAR_TITLE)
        val url = intent.getStringExtra(LOAD_URL)
        title?.apply {
            binding.toolbar.title(this)
        }
        url?.apply {
            loadUrl(this)
        }
    }

    private fun initWebView() {
        setSettings()
        setClient()
    }

    private fun inject() {
        binding.toolbar.left { finish() }
    }

    private fun setSettings() {
        val settings = binding.webview.settings
        settings.javaScriptEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.savePassword = false
        //适配5.0不允许http和https混合使用情况
        settings.mixedContentMode = 0
        binding.webview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        settings.textZoom = 100
        settings.databaseEnabled = true

        settings.setSupportMultipleWindows(false)
        // 是否阻塞加载网络图片  协议http or https
        settings.blockNetworkImage = false
        // 允许加载本地文件html  file协议
        settings.allowFileAccess = true
        // 通过 file url 加载的 Javascript 读取其他的本地文件 .建议关闭
        settings.setAllowFileAccessFromFileURLs(true)
        // 允许通过 file url 加载的 Javascript 可以访问其他的源，包括其他的文件和 http，https 等其他的源
        settings.setAllowUniversalAccessFromFileURLs(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.loadsImagesAutomatically = true
        settings.loadWithOverviewMode = false
        settings.useWideViewPort = false
        settings.domStorageEnabled = true
        settings.setNeedInitialFocus(true)
        settings.defaultTextEncodingName = "utf-8" //设置编码格式
        settings.defaultFontSize = 16
        settings.minimumFontSize = 12 //设置 WebView 支持的最小字体大小，默认为 8
        settings.setGeolocationEnabled(true)
    }

    private fun setClient() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(p0: WebView?, p1: String?) {
                if (!binding.webview.settings.loadsImagesAutomatically) {
                    binding.webview.settings.loadsImagesAutomatically = true
                }
            }
        }
    }

    private fun loadUrl(url: String) {
        val isDark = NightModeManager.isDarkMode(this)
        binding.webview.loadUrl(url + "?themecolor=FF33D1FF&theme=${if (isDark) "dark" else "light"}")
    }
}