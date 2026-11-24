package com.mxchip.mxapp.device.adapter.ccsmart

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.device.bean.BaseBean
import com.mxchip.mxapp.device.consts.CCSmartEnum
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.device.databinding.DeviceItCcsmartOutBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/7
 * @desc:
 */
class CCSmartOutAdapter(
    private val context: Context,
    private val addListener: (BaseBean) -> Unit
) : BaseAdapter<DeviceItCcsmartOutBinding>() {

    private var roomList = ArrayList<BaseBean>()
    private var type = CCSmartEnum.DEVICE

    override fun getItemCount() = roomList.size

    override fun getViewBinding(parent: ViewGroup) =
        DeviceItCcsmartOutBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<DeviceItCcsmartOutBinding>, position: Int) {
        val data = roomList[position]
        when (type) {
            CCSmartEnum.DEVICE -> {
                holder.binding.icon.load(data.product_image)
                holder.binding.name.text =
                    if (data.nick_name.isNullOrEmpty()) data.product_name else data.nick_name
            }
            CCSmartEnum.SCENE -> {
                if (!data.icon_color.isNullOrEmpty()) {
                    holder.binding.icon.imageTintList =
                        ColorStateList.valueOf(Color.parseColor("#${data.icon_color}"))
                }
                holder.binding.icon.load(data.icon_image)
                holder.binding.name.text = data.name
            }
            CCSmartEnum.GROUP -> {
                holder.binding.icon.load(data.category_group_image)
                holder.binding.name.text = data.name
            }
        }

        holder.binding.ivAdd.setOnClickListener {
            addListener.invoke(data)
        }

    }

    fun notifyList(list: ArrayList<BaseBean>?) {
        list?.apply {
            roomList.clear()
            roomList.addAll(list)
            notifyDataSetChanged()
        }
    }


    fun setListType(type: CCSmartEnum) {
        this.type = type
    }

    fun deleteFromCCS(baseBean: BaseBean) {
        if (!roomList.contains(baseBean)) {
            roomList.add(baseBean)
            notifyDataSetChanged()
        }
    }

    fun addToCCS(bean: BaseBean) {
        if (roomList.contains(bean)) {
            roomList.remove(bean)
            notifyDataSetChanged()
        }
    }
}