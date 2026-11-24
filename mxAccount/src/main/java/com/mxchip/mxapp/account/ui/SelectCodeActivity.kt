package com.mxchip.mxapp.account.ui

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.account.R
import com.mxchip.mxapp.account.adapter.RegionAdapter
import com.mxchip.mxapp.account.bean.CountryBean
import com.mxchip.mxapp.account.databinding.AccountAcSelectRegionCodeBinding
import com.mxchip.mxapp.account.widget.RegionTitleItemDecoration
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AssetsUtil
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.LanguageUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.router.annotation.Router


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/16
 * @desc:
 */
@Router(RouterConstants.SELECT_REGION_ACTIVITY)
class SelectCodeActivity : MXBusinessActivity<AccountAcSelectRegionCodeBinding>() {
    override fun getLayoutBinding() = AccountAcSelectRegionCodeBinding.inflate(layoutInflater)

    private val countrys by lazy {
        JsonUtil.fromJsonToArray(
            AssetsUtil.readJsonFromAssets(this, "countrys.json"),
            CountryBean::class.java
        )
    }

    private val adapter by lazy { RegionAdapter() }

    override fun onInit() {
        binding.toolbar.left {
            finish()
        }
        val linearLayoutManager = LinearLayoutManager(this)
        binding.rvRegion.layoutManager = linearLayoutManager
        binding.rvRegion.adapter = adapter
        val isZh = LanguageUtil.isZh()
        binding.sidebar.setOnTouchLetterListener {
            var position = -1
            run {
                adapter.getList().forEachIndexed { index, countryBean ->
                    val firstChar =
                        if (isZh) countryBean.pinyin[0].uppercase() else countryBean.en_name[0].uppercase()
                    if (firstChar == it) {
                        position = index
                        return@run
                    }
                }
            }
            if (position != -1) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0)
            }
        }

        binding.btnSearch.setOnClickListener {
            RouterManager.navigation(RouterConstants.ACCOUNT_REGION_SEARCH_ACTIVITY)
        }
        adapter.setItemClickListener { position, regionBean ->
            LoginManager.saveSelectRegionCode(regionBean.iso3)
            finish()
        }

        parseCountry()
    }

    private fun parseCountry() {
        val region = ArrayList<CountryBean>()
        val isZh = LanguageUtil.isZh()
        val letterList = mutableListOf<String>()
        countrys?.apply {
            region.addAll(this)
        }
        region.sortBy { rn ->
            val cht = if (isZh) rn.pinyin[0] else rn.en_name[0]
            if (letterList.contains("$cht").not()) {
                letterList.add("$cht")
            }
            cht
        }

        val decoration = RegionTitleItemDecoration(this, region)
        decoration.backgroundColor(ContextCompat.getColor(this,R.color.account_color_00afff_a8))
            .textColor(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        binding.rvRegion.addItemDecoration(decoration)
        adapter.setData(region)

        letterList.sort()
        binding.sidebar.setLetters(letterList.toTypedArray())
    }
}