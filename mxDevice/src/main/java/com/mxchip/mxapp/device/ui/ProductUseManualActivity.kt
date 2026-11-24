package com.mxchip.mxapp.device.ui

import androidx.lifecycle.lifecycleScope
import com.mxchip.http.download.DownloadManager
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.LanguageUtil
import com.mxchip.mxapp.device.databinding.DeviceAcProductUseManualBinding
import com.mxchip.router.annotation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 产品使用手册
 */
@Router(RouterConstants.PRODUCT_USE_MANUAL)
class ProductUseManualActivity :
    MXBusinessActivity<DeviceAcProductUseManualBinding>() {

    override fun getLayoutBinding() = DeviceAcProductUseManualBinding.inflate(layoutInflater)

    companion object {
        private const val TAG = "ProductUseManualActivity"
    }

    override fun onInit() {
        val pk = intent.getStringExtra(ProductConstants.KEY_PRODUCT_KEY)
        if (pk.isNullOrBlank()) {
            finish()
            return
        }
        val pn = intent.getStringExtra(ProductConstants.KEY_PRODUCT_NAME) ?: ""
        val link = intent.getStringExtra("link") ?: ""
        binding.topbar.title(pn).left { finish() }
//        binding.pdfView.initWithUrl(
//            url = "https://vbs9010dev.oss-cn-shanghai.aliyuncs.com/app/productCustom/1760153143294.pdf",
//            lifecycleCoroutineScope = lifecycleScope,
//            lifecycle = lifecycle
//        )
//        downloadPdf(pk, "https://oss.cloud.mxchip.com/app/productCustom/test_oss_pdf.pdf")
        showLoading()
        downloadPdf(pk, "https://oss.cloud.mxchip.com/app/productCustom/test_oss_pdf.pdf")
    }

    private fun downloadPdf(pk: String, url: String) {
        val cut = LanguageUtil.getCurrentLanguage()
        val fileName = "$pk-$cut.pdf"
        val file = File(cacheDir, fileName)
        if (file.exists()) {
            dismissLoading()
            binding.pdfView.initWithFile(file)
        } else {
            lifecycleScope.launch(Dispatchers.IO){
                DownloadManager.url(url).file(cacheDir, fileName)
                    .onStart {
                        MXLog.d(TAG, "start download Pdf")
                    }
                    .onSuccess { filePatch ->
                        MXLog.d(TAG, "onSuccess: $filePatch")
                        lifecycleScope.launch(Dispatchers.Main) {
                            dismissLoading()
                            if (!filePatch.isNullOrEmpty()) {
                                val file = File(filePatch)
                                binding.pdfView.initWithFile(file)
                            }
                        }

                    }.onError { code, msg ->
                        MXLog.d(TAG, "onError: $code,$msg")
                        lifecycleScope.launch(Dispatchers.Main) {
                            dismissLoading()
                        }
                    }.download()
            }
        }
    }
}