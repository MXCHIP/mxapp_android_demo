package com.mxchip.mxapp.device.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.bean.DeviceWriteStateBean
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.device.databinding.DeviceItDeviceRecoveryListBinding

class DeviceRecoveryAdapter : BaseAdapter<DeviceItDeviceRecoveryListBinding>() {

    private val datas = mutableListOf<DeviceWriteStateBean>()

    fun setData(list: List<DeviceWriteStateBean>) {
        datas.clear()
        datas.addAll(list)
    }

    fun appendData(list: List<DeviceWriteStateBean>) {
        datas.addAll(list)
    }

    fun getDatas(): List<DeviceWriteStateBean> = datas

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
        holder.binding.tvDeviceName.text =
            if (data.nick_name.isNullOrEmpty()) data.product_name else data.nick_name
        val previous = if (position - 1 > -1) datas[position - 1] else null
        if (previous?.room_id != data.room_id) {
            holder.binding.roomName.visibility = View.VISIBLE
            holder.binding.roomName.text = data.room_name
        } else {
            holder.binding.roomName.visibility = View.GONE
        }
    }
}