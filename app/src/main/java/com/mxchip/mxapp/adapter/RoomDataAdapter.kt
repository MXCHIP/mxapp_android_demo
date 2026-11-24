package com.mxchip.mxapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.GroupBean
import com.mxchip.mxapp.base.consts.DeviceTypeConstants
import com.mxchip.mxapp.bean.DataListTitleBean
import com.mxchip.mxapp.databinding.ItemRoomDataTitleBinding
import com.mxchip.mxapp.databinding.ItemRoomDeviceBinding
import com.mxchip.mxapp.utils.DeviceUIStateUtil
import com.mxchip.sdk.mesh.MXMesh

class RoomDataAdapter(
    private val itemClick: (Int) -> Unit,
    private val detailClick: (Int) -> Unit,
    private val hideClick: (Int) -> Unit
) : RecyclerView.Adapter<RoomDataAdapter.MultiViewHolder>() {

    inner class MultiViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    private var mData = mutableListOf<Any>()

    private val _hideDataTitle by lazy { mutableSetOf<Int>() }
    fun getHideDataTitle(): Set<Int> = _hideDataTitle

    private var isSharedDevice = false
    fun setData(data: List<Any>, isSharedDevice: Boolean) {
        this.isSharedDevice = isSharedDevice
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    fun getData(): List<Any> = mData

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (mData[position]) {
            is DataListTitleBean -> 0
            else -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiViewHolder {
        val binding = when (viewType) {
            0 -> ItemRoomDataTitleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            else -> ItemRoomDeviceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
        return MultiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MultiViewHolder, position: Int) {
        when (holder.binding) {
            is ItemRoomDataTitleBinding -> {
                val titleBean = mData[position] as DataListTitleBean
                val title = when (titleBean.type) {
                    DeviceTypeConstants.SHORTCUT_DEVICE -> holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_shortcut)
                    DeviceTypeConstants.SCENE -> holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_scene)
                    DeviceTypeConstants.GROUP -> holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_group)
                    DeviceTypeConstants.AUTO -> holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_scene_auto)
                    else -> holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_devices)
                }
                holder.binding.tvTitle.text = "$title：${titleBean.dataCount}"
                holder.binding.ivExpand.setImageResource(
                    if (_hideDataTitle.contains(titleBean.type)) com.mxchip.mxapp.base.R.drawable.core_ic_arrow_down
                    else com.mxchip.mxapp.base.R.drawable.core_ic_arrow_top
                )
                holder.binding.ivExpand.setOnClickListener {
                    if (_hideDataTitle.contains(titleBean.type))
                        _hideDataTitle.remove(titleBean.type)
                    else
                        _hideDataTitle.add(titleBean.type)
                    hideClick.invoke(titleBean.type)
                }
            }

            else -> {
                holder.binding as ItemRoomDeviceBinding
                val data = mData[position]
                if (data is GroupBean)
                    groupData(data, holder.binding)
                else if (data is DeviceBean)
                    deviceData(data, holder.binding)
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

    private fun deviceData(device: DeviceBean, binding: ItemRoomDeviceBinding) {
        binding.ivDeviceIcon.load(device.product_image)
        binding.tvDeviceName.text = device.nick_name.ifEmpty { device.product_name }
        if (device.isHideOnlineStatus()) {
            binding.tvStatus.text = ""
        } else {
            val localState =
                if (device.uuid.isEmpty() || isSharedDevice || device.node_type_v2 == "gateway") false else MXMesh.isConnected()
            if (device.online || localState) {
                when (DeviceUIStateUtil.parseSwitchState(device)) {
                    0 -> {
                        binding.tvStatus.text =
                            binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_closed)
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                com.mxchip.mxapp.base.R.color.core_color_text_list_content
                            )
                        )
                    }

                    1 -> {
                        binding.tvStatus.text = binding.root.resources.getString(com.mxchip.mxapp.base.R.string.mx_opened)
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                com.mxchip.mxapp.base.R.color.core_color_success
                            )
                        )
                    }

                    else -> {
                        binding.tvStatus.text = ""
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(
                                binding.root.context,
                                com.mxchip.mxapp.base.R.color.core_color_text_list_content
                            )
                        )
                    }
                }
            } else {
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

    private fun groupData(group: GroupBean, binding: ItemRoomDeviceBinding) {
        binding.ivDeviceIcon.load(group.category_group_image)
        binding.tvDeviceName.text = group.name
//        if (group.group_type == 1) {
//            //wifi组
//            wifiGroupState(group)
//        } else {//mesh组
        binding.tvStatus.text = ""
//        }
    }

//    private fun wifiGroupState(group: GroupBean) {    }

    fun hideData(titlePos: Int, hideCount: Int) {
        for (i in hideCount downTo 1) {
            mData.removeAt(i + titlePos)
        }
        notifyItemRangeRemoved(titlePos + 1, hideCount)
        //+1变化包含标题
        notifyItemRangeChanged(titlePos, hideCount + 1)
    }

    fun showData(titlePos: Int, data: List<Any>) {
        mData.addAll(titlePos + 1, data)
        notifyItemRangeInserted(titlePos + 1, data.size)
        //+1变化包含标题
        notifyItemRangeChanged(titlePos, data.size + 1)
    }
}