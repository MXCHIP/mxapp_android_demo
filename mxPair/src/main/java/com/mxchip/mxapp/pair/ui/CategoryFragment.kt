package com.mxchip.mxapp.pair.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessFragment
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.pair.adapter.CategoryLeftAdapter
import com.mxchip.mxapp.pair.adapter.CategoryRightAdapter
import com.mxchip.mxapp.pair.bean.FirstCategory
import com.mxchip.mxapp.pair.databinding.PairFrCategoryBinding

/**
 *品类选择
 */
class CategoryFragment : MXBusinessFragment<PairFrCategoryBinding>() {
    private val leftAdapter by lazy { CategoryLeftAdapter(requireContext()) }
    private val rightAdapter by lazy { CategoryRightAdapter() }
    private var oldPos = -1

    override fun getLayoutBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = PairFrCategoryBinding.inflate(layoutInflater)

    override fun onInit(view: View, savedInstanceState: Bundle?) {
        binding.rvLeft.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeft.adapter = leftAdapter

        val rightLM = GridLayoutManager(requireContext(), 3)
        rightLM.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                //父类名称==null，产品信息
                return if (rightAdapter.getData()[position] is String) 3 else 1
            }
        }
        binding.rvRight.layoutManager = rightLM
        binding.rvRight.adapter = rightAdapter
        binding.emptyView.setIconColor(ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        inject()
    }

    private fun inject() {
        leftAdapter.setItemClick { pos ->
            categoryData(pos)
        }

        rightAdapter.setItemClick { pos ->
            val productInfo = rightAdapter.getData()[pos]
            if (productInfo is ProductInfo) {
                //去引导页面
                RouterManager.navigation(
                    RouterConstants.PAIR_GUIDE_ACTIVITY, mapOf(
                        ProductConstants.KEY_PRODUCT_KEY to productInfo.product_key,
                        ProductConstants.KEY_PRODUCT_KEY_LIST to
                                arrayListOf(productInfo.product_key)
                    )
                )
            }
        }
    }

    fun categoryData(data: List<FirstCategory>) {
        if (leftAdapter.getData().isNotEmpty()) return
        if (data.isNotEmpty()) {
            binding.emptyView.visibility = View.GONE
            leftAdapter.addAll(data)
            //取第一个一级品类，默认选中
            categoryData(0)
        } else {
            binding.emptyView.visibility = View.VISIBLE
        }
    }

    private fun categoryData(position: Int) {
        if (oldPos == position) {
            return
        }
        val selectedFirstCategory = leftAdapter.getData()[position]
        selectedFirstCategory.isSelected = true
        leftAdapter.notifyItemChanged(position)
        if (oldPos > -1) {
            leftAdapter.getData()[oldPos].isSelected = false
            leftAdapter.notifyItemChanged(oldPos)
        }
        oldPos = position
        val rightDatas = mutableListOf<Any>()
        for (category in selectedFirstCategory.categorys) {
            //二级父类名称
            rightDatas.add(category.name)
            //产品
            rightDatas.addAll(category.products)
        }
        rightAdapter.addAll(rightDatas)
    }
}