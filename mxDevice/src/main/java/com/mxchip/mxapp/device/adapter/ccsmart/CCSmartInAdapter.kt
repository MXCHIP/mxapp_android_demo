package com.mxchip.mxapp.device.adapter.ccsmart

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import coil.load
import com.mxchip.mxapp.device.bean.BaseBean
import com.mxchip.mxapp.device.consts.CCSmartEnum
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.device.databinding.DeviceItCcsmartInBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/7
 * @desc:
 */
class CCSmartInAdapter(
    private val context: Context,
    private val removeListener: (BaseBean) -> Unit
) : BaseAdapter<DeviceItCcsmartInBinding>() {

    private var roomList = ArrayList<BaseBean>()
    private var type = CCSmartEnum.DEVICE

    override fun getItemCount() = roomList.size

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItCcsmartInBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<DeviceItCcsmartInBinding>, position: Int) {
        val data = roomList[position]
        when (type) {
            CCSmartEnum.DEVICE -> {
                holder.binding.icon.load(data.product_image)
                holder.binding.name.text =
                    if (data.nick_name.isNullOrEmpty()) data.product_name else data.nick_name
            }
            CCSmartEnum.SCENE -> {
                if (!data.icon_color.isNullOrEmpty())
                    holder.binding.icon.imageTintList =
                        ColorStateList.valueOf("#${data.icon_color}".toColorInt())
                holder.binding.icon.load(data.icon_image)
                holder.binding.name.text = data.name
            }
            CCSmartEnum.GROUP -> {
                holder.binding.icon.load(data.category_group_image)
                holder.binding.name.text = data.name
            }
        }

        holder.binding.ivRemove.setOnClickListener {
            removeListener.invoke(data)
        }

    }

    fun notifyList(list: MutableList<BaseBean>?) {
        list?.apply {
            roomList.clear()
            roomList.addAll(list)
            notifyDataSetChanged()
        }
    }

    fun setListType(type: CCSmartEnum) {
        this.type = type
    }

    fun getList() = roomList ?: arrayListOf()

    fun addCCS(bean: BaseBean) {
        if (!roomList.contains(bean)) {
            roomList.add(bean)
            notifyDataSetChanged()
        }
    }

    fun deleteCCS(bean: BaseBean) {
        if (roomList.contains(bean)) {
            roomList.remove(bean)
            notifyDataSetChanged()
        }
    }
}