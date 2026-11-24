package com.mxchip.mxapp.account.ui

import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.account.adapter.RegionAdapter
import com.mxchip.mxapp.account.bean.CountryBean
import com.mxchip.mxapp.account.databinding.AccountAcSearchRegionBinding
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AssetsUtil
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.LanguageUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.router.annotation.Router

@Router(RouterConstants.ACCOUNT_REGION_SEARCH_ACTIVITY)
class AccountRegionSearchActivity : MXBusinessActivity<AccountAcSearchRegionBinding>() {

    private val adapter by lazy { RegionAdapter() }
    private val regionList = mutableListOf<CountryBean>()

    override fun getLayoutBinding() = AccountAcSearchRegionBinding.inflate(layoutInflater)

    override fun onInit() {
        binding.tvCancel.setOnClickListener {
            finish()
        }
        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
        }
        binding.etSearch.addTextChangedListener {
            val keyword = it.toString()
            if (keyword.isEmpty()) {
                binding.ivClear.visibility = View.GONE
                adapter.setData(null)
            } else {
                binding.ivClear.visibility = View.VISIBLE
                searchResult(keyword.trim())
            }
        }
        binding.rvRegion.layoutManager = LinearLayoutManager(this)
        binding.rvRegion.adapter = adapter
        adapter.setItemClickListener { _, regionBean ->
            LoginManager.saveSelectRegionCode(regionBean.iso3)
            RouterManager.navigation(RouterConstants.LOGIN_GUIDE_ACTIVITY)
        }
        getCountryList()
    }

    private fun searchResult(keyword: String) {
        val isZh = LanguageUtil.isZh()
        val filter = regionList.filter { country ->
            if (isZh) {
                country.zh_name.contains(keyword)
                        || country.pinyin.contains(keyword, true)
            } else country.en_name.contains(keyword, true)
        }
        adapter.setData(filter)
    }

    private fun getCountryList() {
        JsonUtil.fromJsonToArray(
            AssetsUtil.readJsonFromAssets(this, "countrys.json"),
            CountryBean::class.java
        )?.apply {
            regionList.addAll(this)
        }
    }
}