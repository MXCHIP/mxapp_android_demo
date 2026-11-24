package com.mxchip.mxapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.databinding.ItemPopupHomeBinding
import com.mxchip.mxapp.family.HomeManager
import com.mxchip.mxapp.family.bean.HomeBean

/**
 *家顶部列表弹出框
 */
class HomePopupAdapter(private val itemClick: (HomeBean) -> Unit) :
    BaseAdapter<ItemPopupHomeBinding>() {


    private val mData by lazy { mutableListOf<HomeBean>() }
    private var mInviteClick: ((HomeBean) -> Unit)? = null

    fun addAll(data: List<HomeBean>?) {
        mData.clear()
        data?.apply {
            mData.addAll(data)
        }
    }

    private var mCurrentHomeId: Int? = null
    fun setCurrentHomeId(homeId: Int) {
        mCurrentHomeId = homeId
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun getViewBinding(parent: ViewGroup): ItemPopupHomeBinding {
        return ItemPopupHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ItemPopupHomeBinding>, position: Int) {
        val home = mData[position]
        holder.itemView.setOnClickListener {
            mCurrentHomeId = home.home_id
            notifyDataSetChanged()
            itemClick.invoke(home)
        }
        val mainColor = ContextCompat.getColor(holder.binding.root.context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        holder.binding.tvHomeName.text = home.name
        val textColor = if (mCurrentHomeId == home.home_id) {
            holder.binding.ivHomeIcon.setColorFilter(mainColor)
            mainColor
        } else {
            holder.binding.ivHomeIcon.clearColorFilter()
            ContextCompat.getColor(
                holder.binding.root.context,
                R.color.core_color_text_title
            )
        }
        holder.binding.tvHomeName.setTextColor(textColor)
        //app设置支持邀请功能，且不是普通成员
        if (HomeManager.isSupportHomeShare() && RoleConstants.COMMON != home.role) {
            holder.binding.tvInvite.visibility = View.VISIBLE
            holder.binding.tvInvite.setTextColor(mainColor)
            holder.binding.tvInvite.setOnClickListener {
                mInviteClick?.invoke(home)
            }
        } else {
            holder.binding.tvInvite.visibility = View.GONE
        }
    }

    fun setHomeInviteClick(inviteClick: (HomeBean) -> Unit) {
        mInviteClick = inviteClick
    }
}