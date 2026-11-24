package com.mxchip.mxapp.base.device.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.databinding.ItemWifiGroupListDialogBinding
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.generateShape

/**
 *
 */
class WifiGroupDeviceDialogAdapter : BaseItemClickAdapter<ItemWifiGroupListDialogBinding>() {

    private val deviceList = mutableListOf<DeviceBean>()
    private val Start = 0
    private val Mid = 1
    private val End = 2
    private val Full = 3

    fun setData(list: List<DeviceBean>) {
        this.deviceList.clear()
        this.deviceList.addAll(list)
        notifyDataSetChanged()
    }

    fun getData() = deviceList

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun getViewBinding(parent: ViewGroup): ItemWifiGroupListDialogBinding {
        return ItemWifiGroupListDialogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemWifiGroupListDialogBinding>,
        position: Int
    ) {
        val context = holder.itemView.context
        val bean = deviceList[position]
        if (shouldShowRoomName(position)) {
            holder.binding.llRoom.visibility = View.VISIBLE
            holder.binding.tvRoomName.text = bean.room_name
        } else {
            holder.binding.llRoom.visibility = View.GONE
        }
        val calResult = calPosition(position)
        val radius = ShapeRadius(
            leftTop = if (calResult == Start || calResult == Full) 16 else 0,
            rightTop = if (calResult == Start || calResult == Full) 16 else 0,
            leftBottom = if (calResult == End || calResult == Full) 16 else 0,
            rightBottom = if (calResult == End || calResult == Full) 16 else 0,
        )
        holder.binding.llDevice.background =
            generateShape(context.getColor(R.color.core_color_background_tab), radius)
        holder.binding.icon.load(bean.product_image)
        holder.binding.tvDeviceName.text =
            bean.nick_name.ifEmpty { bean.name.ifEmpty { bean.product_name } }
        holder.binding.ivDeviceSelect.setImageResource(
            if (mSelectedIotIds.contains(bean.iotid)) R.drawable.core_ic_selected else R.drawable.core_ic_unselected
        )
        holder.binding.llDevice.setOnClickListener {
            if (mSelectedIotIds.contains(bean.iotid)) {
                mSelectedIotIds.remove(bean.iotid)
            } else {
                mSelectedIotIds.add(bean.iotid)
            }
            notifyItemChanged(position)
        }

        holder.binding.ivRoomAllSelect.setOnClickListener {
            it.isSelected = !it.isSelected
            holder.binding.ivRoomAllSelect.setImageResource(
                if (it.isSelected) R.drawable.core_ic_selected else R.drawable.core_ic_unselected
            )
            if (it.isSelected) {
                deviceList.forEach { inDev ->
                    if (bean.room_id == inDev.room_id) {
                        mSelectedIotIds.add(inDev.iotid)
                    }
                }
            } else {
                deviceList.forEach { inDev ->
                    if (bean.room_id == inDev.room_id) {
                        mSelectedIotIds.remove(inDev.iotid)
                    }
                }
            }
            notifyDataSetChanged()
        }
    }

    private fun shouldShowRoomName(position: Int): Boolean {
        if (position == 0) return true
        val prevRoomId = deviceList[position - 1].room_id
        val currentRoomId = deviceList[position].room_id
        return prevRoomId != currentRoomId
    }

    private fun calPosition(position: Int): Int {
        val currentRoomName = deviceList[position].room_name
        val nextRoomName = if (position + 1 < itemCount) deviceList[position + 1].room_name else null
        if (position == 0) {
            return if (nextRoomName == currentRoomName) Start
            else Full
        }
        val prevRoomName = deviceList[position - 1].room_name
        return if (currentRoomName == prevRoomName && nextRoomName == currentRoomName) {
            Mid
        } else {
            if (prevRoomName == currentRoomName) End
            else if (currentRoomName == nextRoomName) Start
            else Full
        }
    }

    private val mSelectedIotIds by lazy { mutableSetOf<String>() }

    fun getSelectedIotIdList(): Set<String> = mSelectedIotIds

    fun setSelectedIotIdList(iotIds: List<String>) {
        mSelectedIotIds.clear()
        mSelectedIotIds.addAll(iotIds)
    }
}