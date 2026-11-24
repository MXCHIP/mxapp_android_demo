package com.mxchip.mxapp.pair.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.pair.databinding.PairItPairSuccessRoomBinding

/**
 *添加设备完成房间列表
 */
class PairSuccessRoomAdapter :
    BaseAdapter<PairItPairSuccessRoomBinding>() {
    private var selectedRoomId: Int? = null

    private val datas = arrayListOf<RoomBean>()
    private var itemClickListener: ((Int) -> Unit)? = null

    fun setSelectedRoomId(roomId: Int?) {
        selectedRoomId = roomId
        notifyDataSetChanged()
    }

    fun setRoomList(list: List<RoomBean>) {
        this.datas.clear()
        this.datas.addAll(list)
    }

    fun getItems(): List<RoomBean> = datas

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun getViewBinding(parent: ViewGroup) =
        PairItPairSuccessRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<PairItPairSuccessRoomBinding>,
        position: Int
    ) {
        val context = holder.itemView.context
        if (datas.isNotEmpty() && position != datas.size) {
            val room = datas[position]
            holder.binding.content.text = room.name
            if (room.room_id == selectedRoomId) {
                val mainColor = ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
                holder.binding.content.background = generateStoke(1, mainColor, 4)
                holder.binding.content.setTextColor(mainColor)
            } else {
                holder.binding.content.background =
                    generateStoke(
                        1,
                        context.getColor(com.mxchip.mxapp.base.R.color.core_color_dividers),
                        4
                    )
                holder.binding.content.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.mxchip.mxapp.base.R.color.core_color_text_dialog_content
                    )
                )
            }
            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(room.room_id)
            }
        } else {
            holder.binding.content.text = "+"
            holder.binding.content.background =
                generateStoke(
                    1,
                    context.getColor(com.mxchip.mxapp.base.R.color.core_color_dividers),
                    4
                )
            holder.binding.content.setTextColor(
                ContextCompat.getColor(
                    context,
                    com.mxchip.mxapp.base.R.color.core_color_text_dialog_content
                )
            )
            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(-1)
            }
        }
    }

    fun setItemClickListener(listener: (Int) -> Unit) {
        itemClickListener = listener
    }

    fun appendRoom(roomBean: RoomBean) {
        val inset = datas.size - 1
        datas.add(inset, roomBean)
        notifyItemRangeChanged(inset, 2)
    }
}