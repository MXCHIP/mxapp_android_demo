package com.mxchip.mxapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import com.mxchip.mxapp.R
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.GroupBean
import com.mxchip.mxapp.bean.RoomDeviceManagerTitleBean
import com.mxchip.mxapp.databinding.ItemDeviceEditTitleBinding
import com.mxchip.mxapp.databinding.ItemRoomDeviceManagerBinding
import com.mxchip.sdk.mesh.MXMesh

class RoomDeviceManagerAdapter(
    private val selectedIotIds: ArrayList<String>,
    private val selectedGroupIds: MutableList<Int>,
    val itemClick: (Int) -> Unit,
    val detailClick: (Int) -> Unit,
    val selectedAll: (Int) -> Unit
) : RecyclerView.Adapter<RoomDeviceManagerAdapter.MultiViewHolder>() {

    inner class MultiViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    private var mData = mutableListOf<Any>()
    private var editState = false

    fun setData(data: List<Any>) {
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }


    fun updateDataEditState(editState: Boolean) {
        this.editState = editState
        notifyDataSetChanged()
    }

    fun updateDataVisibleState(show: Boolean) {
        mData.forEach {
            if (it is DeviceBean) {
                if (selectedIotIds.contains(it.iotid)) {
                    it.show = show
                }
            } else if (it is GroupBean) {
                if (selectedGroupIds.contains(it.group_id)) {
                    it.show = show
                }
            }
        }
        notifyDataSetChanged()
    }

    fun getData(): MutableList<Any> {
        return mData
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            mData[position] is RoomDeviceManagerTitleBean -> 0
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiViewHolder {
        val binding = when (viewType) {
            0 -> ItemDeviceEditTitleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            else -> ItemRoomDeviceManagerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
        return MultiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MultiViewHolder, position: Int) {
        when (holder.binding) {
            is ItemDeviceEditTitleBinding -> {
                val titleBean = mData[position] as RoomDeviceManagerTitleBean
                holder.binding.tvTitle.text = titleBean.title + ": ${titleBean.dataCount}"
                if (editState) {
                    changeIvSelectedState(holder.binding.ivSelect, titleBean.isSelected)
                    holder.binding.layoutSelectAll.visibility = View.VISIBLE
                    holder.binding.layoutSelectAll.setOnClickListener {
                        selectedAll.invoke(position)
                    }
                } else {
                    titleBean.isSelected = false
                    holder.binding.layoutSelectAll.visibility = View.GONE
                    holder.binding.layoutSelectAll.setOnClickListener(null)
                }
            }

            else -> {
                holder.binding as ItemRoomDeviceManagerBinding
                val data = mData[position]
                if (data is GroupBean)
                    groupData(data, holder.binding)
                else if (data is DeviceBean)
                    deviceData(data, holder.binding)
                if (editState) {
                    holder.itemView.setBackgroundResource(com.mxchip.mxapp.base.R.drawable.core_bg_solid_white_r20)
                    if (data is GroupBean)
                        changeIvSelectedState(
                            holder.binding.ivSelect,
                            selectedGroupIds.contains(data.group_id)
                        )
                    else if (data is DeviceBean)
                        changeIvSelectedState(
                            holder.binding.ivSelect,
                            selectedIotIds.contains(data.iotid)
                        )
                    holder.binding.ivSelect.visibility = View.VISIBLE
                    holder.binding.ivEdit.visibility = View.GONE
                    holder.itemView.setOnClickListener {
                        itemClick.invoke(position)
                    }
                } else {
                    //有点击效果
                    holder.itemView.setBackgroundResource(R.drawable.bg_room_selector_device_card)
                    holder.binding.ivSelect.visibility = View.GONE
                    holder.binding.ivEdit.visibility = View.VISIBLE
                    holder.binding.ivEdit.setOnClickListener {
                        detailClick.invoke(position)
                    }
                    holder.itemView.setOnClickListener {
                        itemClick.invoke(position)
                    }
                }
            }
        }
    }

    private fun changeIvSelectedState(view: ImageView, isSelected: Boolean) {
        if (isSelected) {
            view.setImageResource(R.drawable.ic_round_selected)
        } else {
            view.setImageResource(R.drawable.ic_round_unselected)
            view.clearColorFilter()
        }
    }

    private fun deviceData(device: DeviceBean, binding: ItemRoomDeviceManagerBinding) {
        binding.ivDeviceIcon.load(device.product_image)
        binding.tvDeviceName.text = device.nick_name.ifEmpty { device.product_name }
        if (device.isHideOnlineStatus()) {
            binding.tvHide.text =
                if (device.show) "" else binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_hide)
            binding.tvStatus.text = ""
        } else {
            val localState =
                if (device.uuid.isEmpty()) false else MXMesh.isConnected() //Mesh连接上即Mesh设备在线
            if (device.online || localState) {
                binding.tvHide.text =
                    if (device.show) {
                        ""
                    } else {
                        binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_hide)
                    }

                binding.tvStatus.text = ""
            } else {
                binding.tvHide.text =
                    if (device.show) {
                        ""
                    } else {
                        binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_hide) + " | "
                    }
                binding.tvStatus.text = binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_offline)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        com.mxchip.mxapp.base.R.color.core_color_warning
                    )
                )
            }
        }
    }

    private fun groupData(group: GroupBean, binding: ItemRoomDeviceManagerBinding) {
        binding.ivDeviceIcon.load(group.category_group_image)
        binding.tvDeviceName.text = group.name
        binding.tvHide.text =
            if (group.show) "" else binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_hide)
        binding.tvStatus.text = ""
    }
}