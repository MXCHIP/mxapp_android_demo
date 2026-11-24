package com.mxchip.mxapp.device.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.bean.PropertyBean
import com.mxchip.mxapp.device.databinding.DeviceItDialogSwitchBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/9
 * @desc:
 */
class DeviceSwitchAdapter : BaseAdapter<DeviceItDialogSwitchBinding>() {

    private var mList: List<PropertyBean>? = null
    private var itemSwitchListener: ((position: Int, PropertyBean) -> Unit)? = null

    override fun getItemCount() = mList?.size ?: 0

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItDialogSwitchBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItDialogSwitchBinding>,
        position: Int
    ) {
        mList?.apply {
            val property = this[position]
            val value = property.value
            val pValue =  if (value is Number){
                value.toInt()
            }else {
                0
            }
            if (pValue == 1)
                holder.binding.ivSwitch.setColorFilter(ContextCompat.getColor(holder.itemView.context, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
            else
                holder.binding.ivSwitch.setColorFilter(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.core_color_text_list_content
                    )
                )
            holder.binding.switchName.text = property.name
            holder.binding.ivSwitch.setOnClickListener {
                itemSwitchListener?.invoke(position, property)
            }
        }
    }

    fun setData(list: List<PropertyBean>) {
        mList = list
        notifyDataSetChanged()
    }

    fun getData(): List<PropertyBean>? {
        return mList
    }

    fun setItemSwitchListener(listener: ((Int, PropertyBean) -> Unit)?) {
        itemSwitchListener = listener
    }
}