package com.mxchip.mxapp.device.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.device.databinding.DeviceItRoomEditDevicesOutsideBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/7
 * @desc:
 */
class RoomEditDevicesOutsideAdapter : BaseAdapter<DeviceItRoomEditDevicesOutsideBinding>() {

    private var roomList: ArrayList<DeviceBean>? = null
    private var addListener: ((DeviceBean) -> Unit)? = null
    override fun getItemCount() = roomList?.size ?: 0

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItRoomEditDevicesOutsideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItRoomEditDevicesOutsideBinding>,
        position: Int
    ) {
        roomList?.apply {
            val data = this[position]
//            if (_deviceType == DeviceTypeConstants.GROUP) {
//                holder.binding.deviceName.text = data.name
//                holder.binding.deviceIcon.load(data.category_group_image)
//            } else {
                holder.binding.deviceName.text = data.nick_name.ifEmpty { data.product_name }
                holder.binding.deviceIcon.load(data.product_image)
//            }
            holder.binding.deviceLocation.text = data.room_name
            holder.binding.ivAdd.setOnClickListener {
                addListener?.invoke(data)
            }
        }

    }

    fun notifyList(list: List<DeviceBean>) {
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

    fun addToRoom(deviceBean: DeviceBean) {
        roomList?.apply {
            if (this.contains(deviceBean)) {
                this.remove(deviceBean)
                notifyDataSetChanged()
            }
        }
    }

    fun deleteFromRoom(deviceBean: DeviceBean) {
        roomList?.apply {
            if (!this.contains(deviceBean)) {
                this.add(deviceBean)
                notifyDataSetChanged()
            }
        }
    }

    fun setAddListener(listener: ((DeviceBean) -> Unit)?) = apply {
        addListener = listener
    }
}