package com.mxchip.mxapp.room.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.room.R
import com.mxchip.mxapp.room.databinding.RoomItRoomManageBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/7
 * @desc:
 */
class RoomManageAdapter(
    private val context: Context
): BaseAdapter<RoomItRoomManageBinding>() {

    private var isEdit = false
    private var roomList: List<RoomBean>? = null
    private var removeListener: ((RoomBean) -> Unit)? = null
    private var itemClickListener: ((RoomBean) -> Unit)? = null

    override fun getItemCount() = roomList?.size ?: 0

    override fun getViewBinding(parent: ViewGroup) = RoomItRoomManageBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<RoomItRoomManageBinding>, position: Int) {
        if (isEdit) {
            holder.binding.ivRemove.visibility = ViewGroup.VISIBLE
            holder.binding.ivEdit.setImageResource(R.drawable.room_ic_sort)
        } else {
            holder.binding.ivRemove.visibility = ViewGroup.GONE
            holder.binding.ivEdit.setImageResource(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
        }
        roomList?.apply {
            val data = this[position]
            holder.binding.roomName.text = data.name

            holder.binding.ivRemove.setOnClickListener { 
                removeListener?.invoke(data)
            }

            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(data)
            }
        }

    }

    fun notifyList(list: List<RoomBean>?) {
        roomList = list
        notifyDataSetChanged()
    }

    fun notifyEdit(edit: Boolean) {
        isEdit = edit
        notifyDataSetChanged()
    }

    fun setRemoveListener(listener: ((RoomBean) -> Unit)?) = apply {
        removeListener = listener
    }

    fun setItemClickListener(listener: ((RoomBean) -> Unit)?) = apply {
        itemClickListener = listener
    }
}