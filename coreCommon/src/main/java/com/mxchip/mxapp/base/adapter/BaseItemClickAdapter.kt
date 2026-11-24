package com.mxchip.mxapp.base.adapter

import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder

abstract class BaseItemClickAdapter<Binding : androidx.viewbinding.ViewBinding> :
    BaseAdapter<Binding>() {
    private var mItemClick: ((Int) -> Unit)? = null
    private var mItemLongClick: ((Int) -> Unit)? = null

    override fun onBindViewHolder(holder: BaseViewHolder<Binding>, position: Int) {
        mItemClick?.apply {
            holder.itemView.setOnClickListener {
                this.invoke(position)
            }
        }
        mItemLongClick?.apply {
            holder.itemView.setOnLongClickListener {
                this.invoke(position)
                true
            }
        }
    }

    fun setItemClick(itemClick: (Int) -> Unit) {
        mItemClick = itemClick
    }

    fun setItemLongClick(itemLongClick: (Int) -> Unit) {
        mItemLongClick = itemLongClick
    }
}