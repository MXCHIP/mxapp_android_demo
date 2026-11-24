package com.mxchip.mxapp.pair.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.pair.databinding.PairItCategorySearchBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/3/3
 * @desc:
 */
class CategorySearchAdapter: BaseAdapter<PairItCategorySearchBinding>() {

    private val devices = arrayListOf<ProductInfo>()
    private var itemClick: ((ProductInfo) -> Unit)? = null

    override fun getItemCount() = devices.size

    override fun getViewBinding(parent: ViewGroup) = PairItCategorySearchBinding.inflate(
        LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<PairItCategorySearchBinding>,
        position: Int
    ) {
        val data = devices[position]
        holder.binding.deviceImage.load(data.image)
        holder.binding.deviceName.text = data.name
        holder.itemView.setOnClickListener {
            itemClick?.invoke(data)
        }
    }

    fun setData(list: List<ProductInfo>?) {
        list?.apply {
            devices.clear()
            devices.addAll(list)
            notifyDataSetChanged()
        }

    }

    fun setItemClickListener(itemClick: (ProductInfo) -> Unit) {
        this.itemClick = itemClick
    }
}