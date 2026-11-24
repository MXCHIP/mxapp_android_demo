package com.mxchip.mxapp.pair.ui

import android.view.MotionEvent
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.KeyboardUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.pair.adapter.CategorySearchAdapter
import com.mxchip.mxapp.pair.bean.FirstCategory
import com.mxchip.mxapp.pair.consts.PairConstants.KEY_CATEGORY_LIST
import com.mxchip.mxapp.pair.databinding.PairAcCategorySearchBinding
import com.mxchip.mxapp.pair.utils.CategorySearchManager
import com.mxchip.router.annotation.Router

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/3/3
 * @desc:
 */
@Router(RouterConstants.CATEGORY_SEARCH_ACTIVITY)
class CategorySearchActivity : MXBusinessActivity<PairAcCategorySearchBinding>() {
    override fun getLayoutBinding() = PairAcCategorySearchBinding.inflate(layoutInflater)

    private val adapter by lazy {
        CategorySearchAdapter().apply {
            setItemClickListener { productInfo ->
                CategorySearchManager.saveProductKeyRecord(productInfo.product_key)
                //去引导页面
                RouterManager.navigation(
                    RouterConstants.PAIR_GUIDE_ACTIVITY, mapOf(
                        ProductConstants.KEY_PRODUCT_KEY to productInfo.product_key,
                        ProductConstants.KEY_PRODUCT_KEY_LIST to arrayListOf(productInfo.product_key)
                    )
                )
                finish()
            }
        }
    }
    private val devices = arrayListOf<ProductInfo>()

    override fun onInit() {
        initView()
        initEvent()
        initData()
    }

    private fun initView() {
        binding.etSearch.background =
            generateShape(
                color = getColor(com.mxchip.mxapp.base.R.color.core_color_background_cancel),
                radius = 20
            )
        binding.deviceList.background =
            generateShape(getColor(com.mxchip.mxapp.base.R.color.core_color_background_list), 16)
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = adapter
        binding.etSearch.requestFocus()
        binding.emptyView.setIconColor(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        history()
    }

    private fun initEvent() {
        binding.etSearch.addTextChangedListener {
            val keyword = it.toString()
            binding.clearText.visibility = if (keyword.isBlank()) View.GONE else View.VISIBLE
            val keywordEmpty = keyword.isEmpty()
            if (keywordEmpty) {
                history()
            } else {
                searchResult(keyword)
            }
        }

        binding.delete.setOnClickListener {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_search_history_clear))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                    alertDialog.cancel()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                    alertDialog.cancel()
                    CategorySearchManager.clearAll()
                    history()
                }.create().show()
        }

        binding.cancel.setOnClickListener {
            finish()
        }

        binding.clearText.setOnClickListener {
            binding.etSearch.setText("")
        }
    }

    private fun initData() {
        val categoryStr = intent.getStringExtra(KEY_CATEGORY_LIST)
        JsonUtil.fromJsonToArray(categoryStr, FirstCategory::class.java)?.forEach {
            it.categorys.forEach { second ->
                devices.addAll(second.products)
            }
        }
    }

    private fun history() {
        binding.emptyView.visibility = View.GONE
        binding.deviceList.visibility = View.VISIBLE
        binding.tvSearchResult.text = getString(com.mxchip.mxapp.base.R.string.mx_search_history)
        val history = CategorySearchManager.getProductKeyRecord()
        val productInfos = mutableListOf<ProductInfo>()
        history.forEach {
            JsonUtil.fromJson(ProductInfoManager.getProductInfo(it), ProductInfo::class.java)
                ?.apply {
                    productInfos.add(this)
                }
        }
        adapter.setData(productInfos)
        binding.tvSearchResult.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
        binding.delete.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun searchResult(keyword: String) {
        binding.tvSearchResult.visibility = View.VISIBLE
        binding.tvSearchResult.text = getString(com.mxchip.mxapp.base.R.string.mx_search_result)
        binding.delete.visibility = View.GONE
        val filter = devices.filter { productInfo ->
            productInfo.name.contains(keyword, true)
        }
        if (filter.isEmpty()) {
            binding.deviceList.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.deviceList.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            adapter.setData(filter)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        this.currentFocus?.apply {
            KeyboardUtil.hideKeyboard(this)
        }
        return super.onTouchEvent(event)
    }
}