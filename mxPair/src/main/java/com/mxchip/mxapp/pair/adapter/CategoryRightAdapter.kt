package com.mxchip.mxapp.pair.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.mxapp.pair.databinding.PairItCategoryRightBinding
import com.mxchip.mxapp.pair.databinding.PairItCategoryRightTitleBinding

/**
 *
 */
class CategoryRightAdapter : RecyclerView.Adapter<CategoryRightAdapter.MyViewHolder>() {
    inner class MyViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    private val datas = mutableListOf<Any>()

    fun addAll(dt: List<Any>) {
        datas.clear()
        datas.addAll(dt)
        notifyDataSetChanged()
    }

    fun getData(): List<Any> {
        return datas
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    private var mItemClick: ((pos: Int) -> Unit)? = null

    fun setItemClick(itemClick: ((pos: Int) -> Unit)) {
        mItemClick = itemClick
    }

    override fun getItemViewType(position: Int): Int {
        return if (datas[position] is String) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = if (viewType == 1)
            PairItCategoryRightTitleBinding.inflate(LayoutInflater.from(parent.context))
        else
            PairItCategoryRightBinding.inflate(LayoutInflater.from(parent.context))
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (holder.binding is PairItCategoryRightTitleBinding) {
            holder.binding.tvTitle.text = datas[position] as String
            holder.itemView.setOnClickListener(null)
        } else {
            val bean = datas[position] as ProductInfo
            holder.binding as PairItCategoryRightBinding
            holder.binding.ivIcon.load(bean.image)
            holder.binding.tvName.text = bean.name
            mItemClick?.apply {
                holder.itemView.setOnClickListener {
                    this.invoke(position)
                }
            }
        }
    }
}