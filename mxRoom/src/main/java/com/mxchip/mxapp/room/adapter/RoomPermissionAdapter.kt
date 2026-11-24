package com.mxchip.mxapp.room.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.setPadding
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.room.databinding.RoomItRoomPermissionBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/5/20
 * @desc:
 */
class RoomPermissionAdapter : BaseAdapter<RoomItRoomPermissionBinding>() {
    private lateinit var context: Context
    private val selectList = arrayListOf<Int>()
    private var itemClickListener: ((selectList: ArrayList<Int>, RoomBean) -> Unit)? = null
    private val mList = arrayListOf<RoomBean>()

    override fun getViewBinding(parent: ViewGroup): RoomItRoomPermissionBinding {
        context = parent.context
        return RoomItRoomPermissionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<RoomItRoomPermissionBinding>,
        position: Int
    ) {
        val roomBean = mList[position]
        holder.binding.tvName.text = roomBean.name
        holder.binding.shapIvPhoto.setPadding(10.dp2px())
        holder.binding.shapIvPhoto.setImageResource(
            if (selectList.contains(roomBean.room_id)) R.drawable.core_ic_round_selected else R.drawable.core_ic_round_unselected
        )
        holder.itemView.setOnClickListener {
            if (selectList.contains(roomBean.room_id)) {
                selectList.remove(roomBean.room_id)
            } else {
                selectList.add(roomBean.room_id)
            }
            notifyItemChanged(position)
            itemClickListener?.invoke(selectList, roomBean)
        }


    }

    fun setItemClickListener(listener: (selectList: ArrayList<Int>, RoomBean) -> Unit) = apply {
        itemClickListener = listener
    }

    fun selectAll(isSelectAll: Boolean) {
        selectList.clear()
        if (isSelectAll) {
            for (position in 0 until itemCount) {
                val data = mList[position]
                selectList.add(data.room_id)
            }
        }
        notifyDataSetChanged()
    }

    fun select(roomIds: List<Int>) {
        selectList.clear()
        selectList.addAll(roomIds)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun setData(list: List<RoomBean>) {
        mList.clear()
        mList.addAll(list)
        notifyDataSetChanged()
    }

    fun getSelectedRooms(): ArrayList<Int> = selectList
}