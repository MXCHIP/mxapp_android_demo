package com.mxchip.mxapp.family.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import coil.load
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.base.utils.formatEmail
import com.mxchip.mxapp.base.utils.formatPhone
import com.mxchip.mxapp.base.utils.isEmail
import com.mxchip.mxapp.family.R
import com.mxchip.mxapp.family.bean.MemberBean
import com.mxchip.mxapp.family.databinding.FamilyItHomeSettingInvateMemberBinding
import com.mxchip.mxapp.family.databinding.FamilyItHomeSettingMemberBinding

class FamilySettingMemberAdapter(private val context: Context, private val role: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mList by lazy { mutableListOf<MemberBean>() }
    companion object{
        const val INVITE = 1
        const val MEMBER = 0
    }

    private var itemClickListener: ((Int, MemberBean?) -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        return if (role == RoleConstants.COMMON) {
            MEMBER
        } else {
            if (position < mList.size) MEMBER else INVITE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == MEMBER) {
            MemberViewHolder(
                FamilyItHomeSettingMemberBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        } else {
            InviteMemberViewHolder(
                FamilyItHomeSettingInvateMemberBinding.inflate(
                    LayoutInflater.from(
                        context
                    ), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MemberViewHolder) {
            mList[position].apply {
                if (avatar_url.isBlank()) holder.binding.memberIcon.setImageResource(R.drawable.family_ic_default_avatar)
                else holder.binding.memberIcon.load(avatar_url)
                holder.binding.memberName.text = name
                holder.binding.memberPhone.text =
                    if (account.isEmail()) account.formatEmail() else account.formatPhone()
                holder.binding.memberType.text =
                    when (role) {
                        0 -> context.getString(com.mxchip.mxapp.base.R.string.mx_role_owner)
                        1 -> context.getString(
                            com.mxchip.mxapp.base.R.string.mx_role_admin
                        )

                        else -> context.getString(com.mxchip.mxapp.base.R.string.mx_role_user)
                    }
            }
        } else {
            holder as InviteMemberViewHolder
            val mainColor = ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
            holder.binding.memberType.setTextColor(mainColor)

        }
        holder.itemView.setOnClickListener {
            if (getItemViewType(position) == INVITE) {
                itemClickListener?.invoke(INVITE, null)
            } else {
                itemClickListener?.invoke(MEMBER, mList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return if (role == RoleConstants.COMMON) {
            mList.size
        } else {
            mList.size + 1
        }
    }

    fun getItems(): List<MemberBean> = mList

    fun setData(data: List<MemberBean>) {
        mList.clear()
        mList.addAll(data)
        notifyDataSetChanged()
    }

    fun setItemClickListener(listener: (Int, MemberBean?) -> Unit) {
        itemClickListener = listener
    }
}

class MemberViewHolder(val binding: FamilyItHomeSettingMemberBinding) :
    RecyclerView.ViewHolder(binding.root)

class InviteMemberViewHolder(val binding: FamilyItHomeSettingInvateMemberBinding) :
    RecyclerView.ViewHolder(binding.root)