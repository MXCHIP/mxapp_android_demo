package com.mxchip.mxapp.pair.ui

import coil.load
import com.mxchip.router.annotation.Router
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.pair.databinding.PairAcPairGuideHelpBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/7/28
 * @desc:
 */
@Router(RouterConstants.PAIR_GUIDE_HELP_ACTIVITY)
class PairGuideHelpActivity: MXBusinessActivity<PairAcPairGuideHelpBinding>() {
    override fun getLayoutBinding() = PairAcPairGuideHelpBinding.inflate(layoutInflater)

    override fun onInit() {
        val imgUrl = intent.getStringExtra("image") ?: return
        binding.ivGuide.load(imgUrl)

        binding.toolbar.left { finish() }
    }
}