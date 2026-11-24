package com.mxchip.mxapp.room.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import coil.load
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.room.databinding.RoomItRoomSelectBinding

class RoomSelectAdapter(private val context: Context) :
    BaseItemClickAdapter<RoomItRoomSelectBinding>() {

    private var currentSelectedRoomId = -1


    private val list = ArrayList<RoomBean>()

    override fun getItemCount(): Int {
        return list.size
    }

    private val selected by lazy { generateStoke(3, ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 90) }
    private val unSelected by lazy {
        generateShape(
            ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_dividers),
            90
        )
    }

    override fun getViewBinding(parent: ViewGroup) =
        RoomItRoomSelectBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<RoomItRoomSelectBinding>, position: Int) {
        super.onBindViewHolder(holder, position)
        val data = list[position]
        if (data.room_id == -1) {//执行添加
            holder.binding.btnAdd.visibility = View.VISIBLE
            holder.binding.btnAdd.setTextColor(ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        } else {
            holder.binding.btnAdd.visibility = View.GONE
            holder.binding.roomName.text = data.name
            if (data.room_id == currentSelectedRoomId) {
                holder.binding.ivSelect.load(selected)
                holder.binding.roomName.setTextColor(ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
            } else {
                holder.binding.ivSelect.load(unSelected)
                holder.binding.roomName.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        com.mxchip.mxapp.base.R.color.core_color_text_title
                    )
                )
            }
        }
    }

    fun setData(list: List<RoomBean>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }

    fun appendData(room: RoomBean) {
        val insetIndex = this.list.size-1
        this.list.add(insetIndex,room)
        notifyItemInserted(insetIndex)
        notifyItemRangeChanged(insetIndex, this.list.size)
    }

    fun setSelect(selectedRoomId: Int) {
        currentSelectedRoomId = selectedRoomId
    }

    fun getRoom(pos: Int) = list[pos]
}