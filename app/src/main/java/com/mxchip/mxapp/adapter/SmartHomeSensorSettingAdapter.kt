package com.mxchip.mxapp.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import coil.load
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.bean.SmartHomePropertiesBean
import com.mxchip.mxapp.databinding.ItemSmarthomeSensorBinding


/**
 * 全屋模式，传感器列表
 */
class SmartHomeSensorSettingAdapter : BaseItemClickAdapter<ItemSmarthomeSensorBinding>() {

    private var mData = mutableListOf<SmartHomePropertiesBean>()

    private var selectedIotId: String? = null

    fun setSelectedIotId(iotId: String?) {
        selectedIotId = iotId
    }

    fun setData(data: List<SmartHomePropertiesBean>) {
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    fun getData() = mData

    override fun getItemCount(): Int {
        return mData.size
    }

    private var selected: GradientDrawable? = null
    private var unSelected: GradientDrawable? = null

    override fun getViewBinding(parent: ViewGroup): ItemSmarthomeSensorBinding {
        if (selected == null)
            selected = generateStoke(
                3, ContextCompat.getColor(parent.context, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 90
            )
        if (unSelected == null)
            unSelected = generateShape(
                ContextCompat.getColor(parent.context, com.mxchip.mxapp.base.R.color.core_color_dividers),
                90
            )
        return ItemSmarthomeSensorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemSmarthomeSensorBinding>,
        position: Int
    ) {
        super.onBindViewHolder(holder, position)
        val data = mData[position]
        holder.binding.ivIcon.load(data.product_image)
        holder.binding.tvName.text = data.nick_name
        if (data.iotid == selectedIotId)
            holder.binding.ivSelect.setImageDrawable(selected)
        else
            holder.binding.ivSelect.setImageDrawable(unSelected)
    }
}