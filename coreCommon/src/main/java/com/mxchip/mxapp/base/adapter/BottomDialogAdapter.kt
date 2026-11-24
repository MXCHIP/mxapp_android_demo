package com.mxchip.mxapp.base.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.databinding.CoreItBottomActionBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/17
 * @desc:
 */
class BottomDialogAdapter(private val context: Context): BaseAdapter<CoreItBottomActionBinding>() {

    private val datas = ArrayList<String>()
    private var itemClickListener: ((String, Int) -> Unit)? = null

    override fun getItemCount() = datas.size

    override fun getViewBinding(parent: ViewGroup) =  CoreItBottomActionBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<CoreItBottomActionBinding>, position: Int) {
        val data = datas[position]
        holder.binding.content.text = data
        holder.itemView.setOnClickListener { itemClickListener?.invoke(data, position) }
    }

    fun appendData(content: String) {
        datas.add(content)
        notifyDataSetChanged()
    }

    fun setItemClickListener(listener: (String, Int) -> Unit) {
        itemClickListener = listener
    }
}