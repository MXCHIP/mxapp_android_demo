package com.mxchip.mxapp.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import com.mxchip.mxapp.R
import com.mxchip.mxapp.adapter.HomePopupAdapter
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.VerticalMaxLineLinearLayoutManager
import com.mxchip.mxapp.databinding.PopwindowHomeBinding
import com.mxchip.mxapp.family.bean.HomeBean

/**
 *家列表顶部弹窗
 */
class HomeFamilyPopupWindow(context: Context) : PopupWindow(context) {
    private val binding = PopwindowHomeBinding.inflate(LayoutInflater.from(context), null, false)
    private val adapter: HomePopupAdapter
    private var familyExchangeClick: ((HomeBean) -> Unit)? = null

    private var familyManageListener: (() -> Unit)? = null
    private var inviteListener: ((HomeBean) -> Unit)? = null

    private val mAllFamils by lazy { ArrayList<HomeBean>() }

    init {
        contentView = binding.root
//        isOutsideTouchable = true
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
        isClippingEnabled = false
        isFocusable = true
        setBackgroundDrawable(0x66000000.toDrawable())
        binding.rvHome.layoutManager = VerticalMaxLineLinearLayoutManager(context, 4)
        binding.llPop.setPadding(0, AppUtil.getStatusBarHeight(), 0, 0)
        adapter = HomePopupAdapter {
            dismiss()
            familyExchangeClick?.invoke(it)
        }
        binding.rvHome.adapter = adapter
        val mainColor = ContextCompat.getColor(context, R.color.app_main_color)
        binding.ivCreateFamily.setColorFilter(mainColor)
        binding.tvCreateFamily.setTextColor(mainColor)
        binding.ivFamilyManage.setColorFilter(mainColor)
        binding.tvFamilyManage.setTextColor(mainColor)
        inject()
    }

    private fun inject() {
        binding.root.setOnClickListener {
            dismiss()
        }
        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
        }
        binding.etSearch.addTextChangedListener {
            val keyword = it.toString()
            if (keyword.isEmpty()) {
                binding.ivClear.visibility = View.GONE
                binding.ivNoData.visibility = View.GONE
                binding.tvNoData.visibility = View.GONE
                adapter.addAll(mAllFamils)
                adapter.notifyDataSetChanged()
            } else {
                binding.ivClear.visibility = View.VISIBLE
                searchResult(keyword.trim())
            }
        }
        adapter.setHomeInviteClick {
            inviteListener?.invoke(it)
        }

        binding.layoutFamilyManage.setOnClickListener {
            familyManageListener?.invoke()
        }
    }

    private fun searchResult(keyword: String) {
        val filter = mAllFamils.filter { family -> family.name.contains(keyword) }
        adapter.addAll(filter)
        adapter.notifyDataSetChanged()
        if (filter.isEmpty()) {
            binding.ivNoData.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.VISIBLE
        } else {
            binding.ivNoData.visibility = View.GONE
            binding.tvNoData.visibility = View.GONE
        }
    }

    fun setData(data: List<HomeBean>, currentHomeId: Int) {
        binding.lySearch.visibility = if (data.size >= 10) View.VISIBLE else View.GONE
        mAllFamils.clear()
        mAllFamils.addAll(data)// 保存所有数据
        binding.etSearch.setText("")//清空搜索框，会自动回调监听
        adapter.addAll(data)
        adapter.setCurrentHomeId(currentHomeId)
    }

    //创建家庭，提供外部监听，方便api
    fun setFamilyCreateClick(clickListener: View.OnClickListener) = apply {
        binding.layoutCreateFamily.setOnClickListener(clickListener)
    }

    fun setFamilyExchangeClick(exchangeClick: (HomeBean) -> Unit) = apply {
        familyExchangeClick = exchangeClick
    }

    fun setFamilyManageListener(listener: () -> Unit) = apply {
        familyManageListener = listener
    }

    fun setInviteListener(listener: (HomeBean) -> Unit) = apply {
        inviteListener = listener
    }
}