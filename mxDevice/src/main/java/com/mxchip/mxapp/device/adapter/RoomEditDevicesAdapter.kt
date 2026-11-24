package com.mxchip.mxapp.device.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.widget.ItemTouchHelperAdapter
import com.mxchip.mxapp.device.R
import com.mxchip.mxapp.device.databinding.DeviceItRoomEditDevicesBinding
import java.util.Collections

/**
 * 编辑房间内的设备
 */
class RoomEditDevicesAdapter : BaseAdapter<DeviceItRoomEditDevicesBinding>(),
    ItemTouchHelperAdapter {

    private var roomList: ArrayList<DeviceBean>? = null
    private var isDefaultRoom = false

    private var removeListener: ((DeviceBean) -> Unit)? = null
    fun setRemoveListener(listener: ((DeviceBean) -> Unit)?) = apply {
        removeListener = listener
    }

    private var itemStateChanged: (() -> Unit)? = null
    fun setItemStateChangedListener(listener: (() -> Unit)?) = apply {
        itemStateChanged = listener
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        roomList?.apply {
            //交换位置
            Collections.swap(this, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
            itemStateChanged?.invoke()
        }
    }

    override fun getItemCount() = roomList?.size ?: 0

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItRoomEditDevicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItRoomEditDevicesBinding>,
        position: Int
    ) {
        roomList?.apply {
            val data = this[position]
            if (isDefaultRoom) {
                holder.binding.ivRemove.visibility = View.GONE
            } else {
                holder.binding.ivRemove.visibility = View.VISIBLE
            }
//            if (_deviceType == DeviceTypeConstants.GROUP) {
//                holder.binding.deviceName.text = data.name
//                holder.binding.deviceIcon.load(data.category_group_image)
//            } else {
            holder.binding.deviceName.text = data.nick_name.ifEmpty { data.product_name }
            holder.binding.deviceIcon.load(data.product_image)
//            }
            if (data.show) {
                holder.binding.ivVisi.setImageResource(R.drawable.device_ic_password_show)
                holder.binding.deviceName.alpha = 1f
                holder.binding.deviceIcon.alpha = 1f
            } else {
                holder.binding.ivVisi.setImageResource(R.drawable.device_ic_password_hide)
                holder.binding.deviceName.alpha = 0.5f
                holder.binding.deviceIcon.alpha = 0.5f
            }
            holder.binding.ivVisi.setOnClickListener {
                data.show = !data.show
                notifyItemChanged(position)
                itemStateChanged?.invoke()
            }
            holder.binding.ivRemove.setOnClickListener {
                deleteDevice(data)
                removeListener?.invoke(data)
            }
        }

    }

    fun notifyList(isDefaultRoom: Boolean, list: List<DeviceBean>) {
        this.isDefaultRoom = isDefaultRoom
        if (null == roomList)
            roomList = ArrayList()
        roomList?.clear()
        roomList?.addAll(list)
        notifyDataSetChanged()
    }

    fun appendNotifyList(list: List<DeviceBean>) {
        if (null == roomList)
            roomList = ArrayList()
        roomList?.addAll(list)
        notifyDataSetChanged()
    }

    fun addDevice(deviceBean: DeviceBean) {
        if (roomList == null) roomList = ArrayList()
        roomList!!.apply {
            if (!this.contains(deviceBean)) {
                this.add(deviceBean)
                notifyDataSetChanged()
            }
        }
    }

    private fun deleteDevice(deviceBean: DeviceBean) {
        roomList?.apply {
            if (this.contains(deviceBean)) {
                this.remove(deviceBean)
                notifyDataSetChanged()
            }
        }
    }

    fun getDeviceList() = roomList
}