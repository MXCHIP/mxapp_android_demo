package com.mxchip.mxapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.R
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.databinding.ItemPopupMasterGatewayFamilyBinding

/**
 *家顶部列表弹出框
 */
class MasterGatewayFamilyPopWinAdapter :
    BaseItemClickAdapter<ItemPopupMasterGatewayFamilyBinding>() {

    private var mData: MutableList<HomeBean>? = null

    fun getData(): List<HomeBean>? {
        return mData
    }

    fun addAll(data: List<HomeBean>) {
        if (mData == null)
            mData = mutableListOf()
        else
            mData?.clear()
        mData!!.addAll(data)
    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    override fun getViewBinding(parent: ViewGroup): ItemPopupMasterGatewayFamilyBinding {
        return ItemPopupMasterGatewayFamilyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemPopupMasterGatewayFamilyBinding>,
        position: Int
    ) {
        super.onBindViewHolder(holder, position)
        mData?.get(position)?.apply {
            holder.binding.tvHomeName.text = this.name
            if (currentHomeId == this.home_id) {
                holder.binding.ivSelect.visibility = View.VISIBLE
                holder.binding.root.setBackgroundResource(R.drawable.bg_family_item_selected)
            } else {
                holder.binding.ivSelect.visibility = View.GONE
                holder.binding.root.background = null
            }
        }
    }

    private var currentHomeId: Int = AppConfigManager.getUseHomeId()
    fun setSelect(homeId: Int) {
        currentHomeId = homeId
    }
}