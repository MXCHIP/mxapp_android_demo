package com.mxchip.mxapp.device.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.device.databinding.DeviceItDeviceRecoveryListBinding

class DeviceSyncMatterAdapter : BaseAdapter<DeviceItDeviceRecoveryListBinding>() {

    private var itemClickInvoke: ((DeviceBean) -> Unit)? = null
    private val datas = mutableListOf<DeviceBean>()
    fun setItemClick(itemClick: (DeviceBean) -> Unit) {
        itemClickInvoke = itemClick
    }

    fun setData(list: List<DeviceBean>) {
        datas.clear()
        datas.addAll(list)
        notifyDataSetChanged()
    }

    fun appendData(list: List<DeviceBean>) {
        datas.addAll(list)
        notifyItemRangeChanged(datas.size - list.size, list.size)
    }

    fun getDatas(): List<DeviceBean> = datas

    override fun getItemCount(): Int = datas.size

    override fun getViewBinding(parent: ViewGroup): DeviceItDeviceRecoveryListBinding {
        return DeviceItDeviceRecoveryListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItDeviceRecoveryListBinding>, position: Int
    ) {
        val data = datas[position]
        holder.binding.icon.load(data.product_image)
        holder.binding.tvDeviceName.text = data.nick_name.ifEmpty { data.product_name }
        val previous = if (position - 1 > -1) datas[position - 1] else null
        if (previous?.room_id != data.room_id) {
            holder.binding.roomName.visibility = View.VISIBLE
            holder.binding.roomName.text = data.room_name
        } else {
            holder.binding.roomName.visibility = View.GONE
        }
        itemClickInvoke?.apply {
            holder.itemView.setOnClickListener {
                this.invoke(data)
            }
        }
    }
}