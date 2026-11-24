package com.mxchip.mxapp.device.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.device.R
import com.mxchip.mxapp.device.databinding.DeviceItChooseSyncRoomBinding

/**
 * 选择同步数据房间
 */
class ChooseSyncRoomAdapter : BaseAdapter<DeviceItChooseSyncRoomBinding>() {

    private val list = mutableListOf<RoomBean>()

    private val selectedRoomIds = mutableListOf<Int>()

    override fun getItemCount() = list.size

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItChooseSyncRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItChooseSyncRoomBinding>,
        position: Int
    ) {
        val data = list[position]
        val roomId = data.room_id
        if (selectedRoomIds.contains(roomId))
            holder.binding.ivImage.setImageResource(R.drawable.device_ic_round_selected)
        else
            holder.binding.ivImage.setImageResource(R.drawable.device_ic_round_unselected)
        holder.binding.name.text = data.name
        val count = data.device_count
        val unSync = data.unWrittenDeviceCount
        val mContext = holder.itemView.context
        //共计：%1$d，未同步：5
        val desStr =
            String.format(mContext.getString(com.mxchip.mxapp.base.R.string.mx_choose_sync_room_desc), count, unSync)
        if (unSync == 0) {
            holder.binding.desc.text = desStr
        } else {
            val descSp = SpannableString(desStr)
            val startIndex = desStr.length - ("$unSync").length
            val endIndex = desStr.length
            descSp.setSpan(
                ForegroundColorSpan(mContext.getColor(com.mxchip.mxapp.base.R.color.core_color_error)),
                startIndex,
                endIndex,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            holder.binding.desc.text = descSp
        }
        holder.itemView.setOnClickListener {
            if (selectedRoomIds.contains(roomId)) {
                selectedRoomIds.remove(roomId)
            } else {
                selectedRoomIds.add(roomId)
            }
            notifyItemChanged(position)
        }
    }

    fun setData(buildRooms: List<RoomBean>) {
        selectedRoomIds.clear()
        buildRooms.forEach { selectedRoomIds.add(it.room_id) }
        list.clear()
        list.addAll(buildRooms)
        notifyDataSetChanged()
    }

    fun clearSelectedRoom() {
        selectedRoomIds.clear()
    }

    fun getSelectedRoomIds() = selectedRoomIds
}