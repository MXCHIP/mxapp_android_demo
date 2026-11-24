package com.mxchip.mxapp.family.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.family.bean.ShareInProgressBean
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.utils.formatEmail
import com.mxchip.mxapp.base.utils.formatPhone
import com.mxchip.mxapp.base.utils.isEmail
import com.mxchip.mxapp.family.databinding.FamilyItShareInProgressBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/12/13
 * @desc:
 */
class ShareInProgressAdapter : BaseAdapter<FamilyItShareInProgressBinding>() {

    private val list = ArrayList<ShareInProgressBean>()
    private var recallCallback: ((ShareInProgressBean) -> Unit)? = null

    override fun getItemCount() = list.size

    override fun getViewBinding(parent: ViewGroup) = FamilyItShareInProgressBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )

    override fun onBindViewHolder(
        holder: BaseViewHolder<FamilyItShareInProgressBinding>,
        position: Int
    ) {
        val data = list[position]
        holder.binding.tvShareAccount.text =
            if (data.share_username.isEmail()) data.share_username.formatEmail() else data.share_username.formatPhone()
        val mainColor = ContextCompat.getColor(holder.itemView.context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        holder.binding.tvRecall.setTextColor(mainColor)
        holder.binding.tvRecall.setOnClickListener {
            recallCallback?.invoke(data)
        }
    }

    fun notifyData(datas: List<ShareInProgressBean>) {
        list.clear()
        list.addAll(datas)
        notifyDataSetChanged()
    }

    fun setRecallCallback(callback: (ShareInProgressBean) -> Unit) {
        recallCallback = callback
    }
}