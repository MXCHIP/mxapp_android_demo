package com.mxchip.mxapp.base.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.camera.scan.analyze.Analyzer
import com.king.zxing.BarcodeCameraScanActivity
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.QRCodeAnalyzer
import com.king.zxing.util.CodeUtils
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.router.annotation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 二维码扫描
 */
@Router(RouterConstants.QR_SCAN_ACTIVITY)
class QrScanActivity : BarcodeCameraScanActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 设置状态栏为透明并沉浸
        window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window?.statusBarColor = Color.TRANSPARENT
        window?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        super.onCreate(savedInstanceState)
        val back = findViewById<View>(R.id.iv_back)
        back.setOnClickListener {
            finish()
        }
        back.setPadding(0,AppUtil.getStatusBarHeight(),0,0)
    }

    override fun initCameraScan(cameraScan: CameraScan<com.google.zxing.Result?>) {
        super.initCameraScan(cameraScan)
        // 根据需要设置CameraScan相关配置

        cameraScan.setPlayBeep(true)
    }

    override fun createAnalyzer(): Analyzer<com.google.zxing.Result?>? {
        // 初始化解码配置
        val decodeConfig = DecodeConfig()
        decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS) //如果只有识别二维码的需求，这样设置效率会更高，不设置默认为DecodeFormatManager.DEFAULT_HINTS
            .setFullAreaScan(false) //设置是否全区域识别，默认false
//            .setAreaRectRatio(0.8f) //设置识别区域比例，默认0.8，设置的比例最终会在预览区域裁剪基于此比例的一个矩形进行扫码识别
//            .setAreaRectVerticalOffset(0) //设置识别区域垂直方向偏移量，默认为0，为0表示居中，可以为负数
//            .setAreaRectHorizontalOffset(0) //设置识别区域水平方向偏移量，默认为0，为0表示居中，可以为负数
        // BarcodeCameraScanActivity默认使用的MultiFormatAnalyzer，如果只识别二维码，这里可以改为使用QRCodeAnalyzer
        return QRCodeAnalyzer(decodeConfig)
    }

    /**
     * 布局ID；通过覆写此方法可以自定义布局
     *
     * @return 布局ID
     */
    override fun getLayoutId(): Int {
        return R.layout.core_ac_qr_scan
    }

    override fun onScanResultCallback(result: AnalyzeResult<com.google.zxing.Result?>) {
        // 停止分析
        cameraScan.setAnalyzeImage(false)
        postScanResult(result.getResult().text)
    }

    private fun postScanResult(content: String) {
        // 返回结果
        val intent = Intent()
        intent.putExtra("originalValue", content)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun gotoPhoto() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        albumLaunch.launch(Intent.createChooser(intent, ""))
    }

    private val albumLaunch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                try {
                    val bitmap =
                        MediaStore.Images.Media.getBitmap(this.contentResolver, it.data?.data)
                    lifecycleScope.launch(Dispatchers.IO) {
                        CodeUtils.parseQRCode(bitmap)?.apply {
                            postScanResult(this)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
}