package com.mxchip.mxapp.pair.ui

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import coil.load
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.pair.bean.PairGuideInfo
import com.mxchip.mxapp.pair.consts.MxLinkType
import com.mxchip.mxapp.pair.databinding.PairAcPairGuideBinding
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.mxapp.pair.viewmodel.AppPairBusiness
import com.mxchip.router.MxRouter
import com.mxchip.router.annotation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *配网引导
 */
@Router(RouterConstants.PAIR_GUIDE_ACTIVITY)
class PairGuideActivity : MXBusinessActivity<PairAcPairGuideBinding>() {

    private lateinit var productKey: String

    override fun getLayoutBinding(): PairAcPairGuideBinding {
        return PairAcPairGuideBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        productKey = intent.getStringExtra(ProductConstants.KEY_PRODUCT_KEY)!!
        //发现设备，支持匹配产品组内设备
        binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_init_device))
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvConfirm.setBtnBackgroundColor(mainColor)
        binding.tvStateTip.setTextColor(mainColor)
        inject()
        getPairGuideInfo()
    }

    private fun inject() {
        binding.toolbar.left { finish() }
        binding.tvStateTip.setOnClickListener {
            RouterManager.navigation(
                RouterConstants.PAIR_GUIDE_HELP_ACTIVITY, mapOf(
                    "image" to it.tag.toString(),
                )
            )
        }
        binding.tvConfirm.setOnClickListener {
            val productStr = ProductInfoManager.getProductInfo(productKey)
            val productInfo = JsonUtil.fromJson(productStr, ProductInfo::class.java)
            if (productInfo?.link_type_id == MxLinkType.MX_BLE_MESH_SCAN.code) {//扫码配网
                goScanGateway()
            } else {
                goPairScanPage()
            }
        }
    }

    private fun goPairScanPage() {
        MxRouter.build(RouterConstants.PAIR_SCAN_ACTIVITY).flatBundle(intent.extras).navigation(this)
//        RouterManager.navigation(
//            RouterConstants.PAIR_SCAN_ACTIVITY,
//            mapOf(ProductConstants.KEY_PRODUCT_KEY_LIST to productKeyGroup)
//        )
        finish()
    }

    private val scanResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.getStringExtra("originalValue")?.apply {
                    if (this.length == 12) {
                        showLoading()
                        val dn = this.substring(4).lowercase()
                        PairManager.getPairBusiness()!!
                            .restoreAPGateway(productKey, dn) { result, code, msg ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    dismissLoading()
                                    if (result) {
                                        goPairScanPage()
                                    } else {
                                        //cd:bug 42726
                                        when (code) {
                                            11002 ->
                                                showToast(getString(com.mxchip.mxapp.base.R.string.mx_device_offline))

                                            11009 ->
                                                showToast(getString(com.mxchip.mxapp.base.R.string.mx_device_restore_failed))

                                            else ->
                                                showToast("$msg")
                                        }
                                    }
                                }
                            }
                    } else {
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_qr_format_err))
                    }
                }
            }
        }

    private fun goScanGateway() {
        if (XXPermissions.isGranted(this, Permission.CAMERA)) {
            MxRouter.build(RouterConstants.QR_SCAN_ACTIVITY).navigation(this, scanResult)
        } else openPermissionDialog()
    }

    private fun openPermissionDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_permission_camera_denied))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_permission_camera_denied_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_disagree)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_agree)) { alertDialog, _ ->
                alertDialog.dismiss()
                XXPermissions.with(this)
                    .permission(Permission.CAMERA)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                            if (all) MxRouter.build(RouterConstants.QR_SCAN_ACTIVITY).navigation(this@PairGuideActivity, scanResult)
                        }

                        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                            if (never) MXLog.w("PairGuideActivity", "permanentlyDenied")
                        }
                    })
            }.create().show()
    }

    private fun getPairGuideInfo() {
        //未设置配网API业务操作对象，默认mxapp配置
        val pariBusiness = PairManager.getPairBusiness()
        if (pariBusiness == null)
            PairManager.setPairBusiness(AppPairBusiness())
        PairManager.getPairBusiness()!!.getPairGuideInfo(productKey) {
            if (!it.isNullOrEmpty()) {
                val guideInfo = JsonUtil.fromJson(it, PairGuideInfo::class.java)
                if (guideInfo != null) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (guideInfo.bind_help_image.isNotEmpty()) {
                            binding.tvStateTip.visibility = View.VISIBLE
                            binding.tvStateTip.tag = guideInfo.bind_help_image
                        }
                        binding.ivIcon.load(guideInfo.bind_guide_image)
                        binding.tvStatusDec.text = guideInfo.bind_guide_text
                        binding.tvStateTip.text = guideInfo.bind_help_text
                        binding.tvConfirm.text = guideInfo.bind_confirm_text
                    }
                }
            }
        }
    }
}