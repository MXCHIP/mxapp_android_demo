package com.mxchip.mxapp.family.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mxchip.mxapp.base.base.BasePagingAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.databinding.FamilyItFamilyListManageBinding

/**
 * 家庭列表管理
 */
class FamilyManageAdapter(private val context: Context) :
    BasePagingAdapter<FamilyItFamilyListManageBinding, HomeBean>(
        itemSame = { old, new ->
            old.home_id == new.home_id
        }
    ) {


    override fun getViewBinding(parent: ViewGroup) =
        FamilyItFamilyListManageBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<FamilyItFamilyListManageBinding>,
        position: Int
    ) {
        val data = getItem(position)
        data?.apply {
            holder.binding.roomCount.text =
                "${context.getString(com.mxchip.mxapp.base.R.string.mx_room)} ${data.room_count}"
            holder.binding.deviceCount.text =
                "${context.getString(com.mxchip.mxapp.base.R.string.mx_devices)} ${data.device_count}"
            holder.binding.memberCount.text =
                "${context.getString(com.mxchip.mxapp.base.R.string.mx_member)} ${data.member_count}"
            if (data.address.isNullOrEmpty()) {
                holder.binding.lineLocation.visibility = View.GONE
            } else {
                holder.binding.lineLocation.visibility = View.VISIBLE
                holder.binding.location.text = data.address
            }
            holder.binding.familyName.text = data.name

            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(data)
            }
        }
    }

    private var itemClickListener: ((HomeBean) -> Unit)? = null
    fun setItemClickListener(listener: (HomeBean) -> Unit) {
        itemClickListener = listener
    }
}