package com.mxchip.mxapp.pair.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.pair.bean.FirstCategory
import com.mxchip.mxapp.pair.databinding.PairItCategoryLeftBinding

/**
 * 手动添加，产品选择，左边大类
 */
class CategoryLeftAdapter(private val context: Context) :
    BaseItemClickAdapter<PairItCategoryLeftBinding>() {
    private var datas = mutableListOf<FirstCategory>()

    fun addAll(dats: List<FirstCategory>) {
        datas.clear()
        datas.addAll(dats)
        notifyDataSetChanged()
    }

    fun getData(): List<FirstCategory> {
        return datas
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun getViewBinding(parent: ViewGroup) =
        PairItCategoryLeftBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<PairItCategoryLeftBinding>, position: Int) {
        super.onBindViewHolder(holder, position)
        datas[position].apply {
            holder.binding.tvName.text = this.name
            if (this.isSelected) {
                    val mainColor = ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
                holder.binding.line.setBackgroundColor(mainColor)
                holder.binding.tvName.setTextColor(mainColor)
                holder.binding.root.setBackgroundColor(context.getColor(com.mxchip.mxapp.base.R.color.core_color_background_tab))
            } else {
                holder.binding.tvName.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        com.mxchip.mxapp.base.R.color.core_color_text_dialog_content
                    )
                )
                holder.binding.line.background = null
                holder.binding.root.background = null
            }
        }
    }
}