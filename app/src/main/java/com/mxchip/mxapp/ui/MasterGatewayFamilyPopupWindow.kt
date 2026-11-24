package com.mxchip.mxapp.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import androidx.core.graphics.drawable.toDrawable
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.adapter.MasterGatewayFamilyPopWinAdapter
import com.mxchip.mxapp.base.utils.VerticalMaxLineLinearLayoutManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.databinding.PopwindowMasterGatewayFamilyListBinding

/**
 *主从网关设置列表顶部弹窗
 */
class MasterGatewayFamilyPopupWindow(context: Context) : Dialog(context) {
    private val binding =
        PopwindowMasterGatewayFamilyListBinding.inflate(LayoutInflater.from(context), null, false)
    private val adapter by lazy { MasterGatewayFamilyPopWinAdapter() }

    private var familyExchangeClick: ((HomeBean) -> Unit)? = null


    init {
        setContentView(binding.root)
        window?.setGravity(Gravity.TOP or Gravity.START)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val params = window?.attributes
        params?.y = 90.dp2px()
        params?.x = 10.dp2px()
        window?.attributes = params
        initView()
        initEvent()
    }

    private fun initView() {
        binding.rvHome.layoutManager = VerticalMaxLineLinearLayoutManager(context, 4)
        binding.rvHome.adapter = adapter
    }

    private fun initEvent() {
        adapter.setItemClick { pos ->
            adapter.getData()?.get(pos)?.apply {
                adapter.setSelect(this.home_id)
                adapter.notifyDataSetChanged()
                familyExchangeClick?.invoke(this)
            }
        }
    }

    fun setData(data: List<HomeBean>?) {
        if (data != null) {
            adapter.addAll(data)
            adapter.notifyDataSetChanged()
        }
    }

    fun setFamilyExchangeClick(exchangeClick: (HomeBean) -> Unit) = apply {
        familyExchangeClick = exchangeClick
    }
}