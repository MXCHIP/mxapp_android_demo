package com.mxchip.mxapp.device.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.device.bean.ShareDeviceListBean
import com.mxchip.mxapp.base.base.BasePagingAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.device.R
import com.mxchip.mxapp.device.consts.ShareConstants.TYPE_MINE_DEVICE
import com.mxchip.mxapp.device.databinding.DeviceItShareDeviceBinding

class ShareDeviceAdapter(private val context: Context, private val type: Int) :
    BasePagingAdapter<DeviceItShareDeviceBinding, ShareDeviceListBean>(itemSame = { old, new ->
        old.iotid == new.iotid
    }) {

    private var isEdit = false
    private val selectList = ArrayList<ShareDeviceListBean>()
    private var itemClickListener: ((Int, Boolean, ShareDeviceListBean, ArrayList<ShareDeviceListBean>) -> Unit)? =
        null

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItShareDeviceBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<DeviceItShareDeviceBinding>, position: Int) {
        val data = getItem(position)
        holder.binding.clMine.visibility = if (type == TYPE_MINE_DEVICE) View.VISIBLE else View.GONE
        holder.binding.clShare.visibility =
            if (type == TYPE_MINE_DEVICE) View.GONE else View.VISIBLE
        data?.apply {
            if (type == TYPE_MINE_DEVICE) {
                if (isEdit) {
                    holder.binding.rightArrow.visibility = View.GONE
                    holder.binding.ivSelectState.visibility = View.VISIBLE
                    holder.binding.mineDeviceName.text = name
                    holder.binding.mineIcon.load(image_url)
                } else {
                    holder.binding.rightArrow.visibility = View.VISIBLE
                    holder.binding.ivSelectState.visibility = View.GONE
                    holder.binding.mineDeviceName.text = name
                    holder.binding.mineIcon.load(image_url)
                }
            } else {
                holder.binding.shareDeviceName.text = name
                holder.binding.shareIcon.load(image_url)
                holder.binding.shareDeviceDesc.text =
                    String.format(context.getString(com.mxchip.mxapp.base.R.string.mx_of_device), master_username)
            }
            holder.binding.ivSelectState.setImageResource(R.drawable.device_ic_round_unselected)
            holder.itemView.setOnClickListener {
                if (type == TYPE_MINE_DEVICE && isEdit) {
                    if (selectList.contains(data)) {
                        selectList.remove(data)
                        holder.binding.ivSelectState.setImageResource(R.drawable.device_ic_round_unselected)
                    } else {
                        selectList.add(data)
                        holder.binding.ivSelectState.setImageResource(R.drawable.device_ic_round_selected)
                    }
                }
                if (type == TYPE_MINE_DEVICE)
                    itemClickListener?.invoke(type, isEdit, this, selectList)
            }

            holder.itemView.setOnLongClickListener {
                if (type == TYPE_MINE_DEVICE) false
                else {
                    itemClickListener?.invoke(type, isEdit, this, selectList)
                    true
                }
            }
        }
    }

    fun setItemClickListener(listener: (Int, Boolean, ShareDeviceListBean, ArrayList<ShareDeviceListBean>) -> Unit) {
        itemClickListener = listener
    }

    fun notifyEdit(edit: Boolean) {
        isEdit = edit
        notifyDataSetChanged()
    }
}