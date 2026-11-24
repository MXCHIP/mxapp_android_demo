package com.mxchip.mxapp.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import coil.load
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.adapter.BaseItemClickAdapter
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.databinding.ItemMasterGatewayBinding

/**
 * 主网关选择
 */
class MasterGatewayAdapter : BaseItemClickAdapter<ItemMasterGatewayBinding>() {
    private val mDeviceList = arrayListOf<DeviceBean>()
    private lateinit var selectDrawable: Drawable
    private lateinit var normalDrawable: Drawable

    override fun getItemCount(): Int {
        return mDeviceList.size
    }

    override fun getViewBinding(parent: ViewGroup): ItemMasterGatewayBinding {
        selectDrawable =
            generateStoke(3, ContextCompat.getColor(parent.context, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 10)
        normalDrawable =
            generateShape(ContextCompat.getColor(parent.context, R.color.core_color_dividers), 10)
        return ItemMasterGatewayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ItemMasterGatewayBinding>, position: Int) {
        super.onBindViewHolder(holder, position)
        val bean = mDeviceList[position]
        holder.binding.ivIcon.load(bean.product_image)
        holder.binding.tvName.text = bean.nick_name.ifEmpty { bean.product_name }
        holder.binding.tvRoomName.text = bean.room_name
        holder.binding.viewState.background =
            if (setMasterGatewayIotId == null) {
                if (bean.gateway_is_main == 1) selectDrawable else normalDrawable
            } else {
                if (setMasterGatewayIotId == bean.iotid) selectDrawable else normalDrawable
            }
    }

    fun setData(list: List<DeviceBean>?) {
        this.mDeviceList.clear()
        list?.apply {
            mDeviceList.addAll(list)
        }
        notifyDataSetChanged()
    }

    fun getData() = mDeviceList

    private var setMasterGatewayIotId: String? = null
    fun setMasterGatewayIotId(iotId: String) {
        setMasterGatewayIotId = iotId
        notifyDataSetChanged()
    }
}