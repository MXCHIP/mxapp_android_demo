package com.mxchip.mxapp.family.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.family.bean.MemberBean
import com.mxchip.mxapp.base.utils.formatEmail
import com.mxchip.mxapp.base.utils.formatPhone
import com.mxchip.mxapp.base.utils.isEmail
import com.mxchip.mxapp.family.R
import com.mxchip.mxapp.family.databinding.FamilyItHandOverMemberBinding

class HandOverMemberListAdapter: BaseAdapter<FamilyItHandOverMemberBinding>() {

    private var itemClickListener: ((MemberBean) -> Unit)? = null
    private val list = arrayListOf<MemberBean>()

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getViewBinding(parent: ViewGroup) = FamilyItHandOverMemberBinding.inflate(
        LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<FamilyItHandOverMemberBinding>,
        position: Int
    ) {
        val data = list[position]
        if (data.avatar_url.isEmpty()) holder.binding.memberIcon.setImageResource(R.drawable.family_ic_default_avatar)
        else holder.binding.memberIcon.load(data.avatar_url)
        holder.binding.memberName.text = data.name
        holder.binding.memberPhone.text = if (data.account.isEmail()) data.account.formatEmail() else data.account.formatPhone()
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(data)
        }
    }

    fun setItemClickListener(itemClick: (MemberBean) -> Unit) {
        itemClickListener = itemClick
    }

    fun setMembers(list: List<MemberBean>) {
        this.list.clear()
        this.list.addAll(list)
    }
}