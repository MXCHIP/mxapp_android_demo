package com.mxchip.mxapp.pair.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import coil.load
import com.mxchip.mxapp.pair.bean.PairDeviceBean
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.databinding.PairItAutoFindBinding

/**
 *自动发现设备
 */
class AutoFindAdapter(private val context: Context) : BaseItemClickAdapter<PairItAutoFindBinding>() {
    private var datas: MutableList<PairDeviceBean>? = null
    private var isReplaceMode = false
    fun getData(): List<PairDeviceBean>? {
        return datas
    }

    fun clearData() {
        datas?.clear()
        notifyDataSetChanged()
    }

    fun append(data: PairDeviceBean) {
        if (null == datas)
            datas = mutableListOf()
        datas!!.add(data)
    }

    fun setReplaceMode(replaceMode: Boolean) {
        isReplaceMode = replaceMode
    }

    override fun getItemCount(): Int {
        return datas?.size ?: 0
    }

    override fun getViewBinding(parent: ViewGroup) = PairItAutoFindBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<PairItAutoFindBinding>,
        position: Int
    ) {
        super.onBindViewHolder(holder, position)
        datas?.get(position)?.apply {
            holder.binding.icon.load(this.icon)
            holder.binding.name.text = this.name
            holder.binding.mac.text = this.mac
            if (isReplaceMode)
                holder.binding.ivSelect.setImageResource(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
            else {
                changeIvSelectedState(holder.binding.ivSelect, this.isSelected)
            }
        }
        holder.itemView.background =
            generateShape(context.getColor(com.mxchip.mxapp.base.R.color.core_color_background_list), 16)
    }

    private fun changeIvSelectedState(view: ImageView, isSelected: Boolean) {
        if (isSelected) {
            view.setImageResource(R.drawable.pair_ic_round_selected)
        } else {
            view.setImageResource(R.drawable.pair_ic_round_unselected)
            view.clearColorFilter()
        }
    }
}