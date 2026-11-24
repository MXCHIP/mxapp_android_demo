package com.mxchip.mxapp.pair.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.base.widget.FlowLayout
import com.mxchip.mxapp.pair.R


/**
 * 设备item中房间列表
 */
class PairSuccessDeviceRoomAdapter :
    FlowLayout.Adapter<PairSuccessDeviceRoomAdapter.FlowViewHolder>() {

    private val mList = arrayListOf<RoomBean>()

    override fun onCreateViewHolder(parent: ViewGroup?): FlowViewHolder {
        val view = LayoutInflater.from(parent?.context)
            .inflate(R.layout.pair_it_pair_success_room, parent, false)
        // 给 View 设置 margin
        val mlp = MarginLayoutParams(view.layoutParams)
        val margin = 5.dp2px()
        mlp.setMargins(margin, margin, margin, margin)
        view.layoutParams = mlp
        return FlowViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: FlowViewHolder,
        position: Int
    ) {
        val room = mList[position]
        holder.content.text = room.name
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(room.room_id)
        }
        if (room.room_id == selectedRoomId) {
            val mainColor = ContextCompat.getColor(holder.itemView.context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
            holder.itemView.background = generateStoke(1, mainColor, 4)
            holder.content.setTextColor(mainColor)
        } else {
            holder.itemView.background =
                generateStoke(
                    1,
                    holder.itemView.context.getColor(com.mxchip.mxapp.base.R.color.core_color_dividers),
                    4
                )
            holder.content.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    com.mxchip.mxapp.base.R.color.core_color_text_dialog_content
                )
            )
        }
    }

    override fun getItemCount() = mList.size

    inner class FlowViewHolder(itemView: View) : FlowLayout.ViewHolder(itemView) {
        var content: TextView = itemView.findViewById(R.id.content)
    }

    private var itemClickListener: ((Int) -> Unit)? = null
    fun setItemClickListener(click: (Int) -> Unit) {
        itemClickListener = click
    }

    private var selectedRoomId: Int? = null
    fun setSelectedRoomId(roomId: Int?) {
        selectedRoomId = roomId
    }

    fun notifyList(list: List<RoomBean>?) {
        list?.apply {
            mList.clear()
            mList.addAll(list)
        }
    }
}