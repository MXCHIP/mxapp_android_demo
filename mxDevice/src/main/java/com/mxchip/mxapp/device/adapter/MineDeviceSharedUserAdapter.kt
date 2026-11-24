package com.mxchip.mxapp.device.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mxchip.mxapp.device.bean.ShareUserListBean
import com.mxchip.mxapp.base.base.BasePagingAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.utils.format
import com.mxchip.mxapp.device.databinding.DeviceItMineDeviceSharedUserBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/17
 * @desc:
 */
class MineDeviceSharedUserAdapter(private val context: Context): BasePagingAdapter<DeviceItMineDeviceSharedUserBinding, ShareUserListBean>(
    itemSame = { old, new ->
        old.user_id == new.user_id
    }
) {

    private var itemClickListener: ((ShareUserListBean) -> Unit)? = null

    override fun getViewBinding(parent: ViewGroup) = DeviceItMineDeviceSharedUserBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItMineDeviceSharedUserBinding>,
        position: Int
    ) {
        val data = getItem(position)
        data?.apply {
            val longTime = share_time * 1000L
            val time = longTime.format("yyyy-MM-dd HH:mm:ss")//"yyyy年MM月dd日aHH:mm:ss秒")
            holder.binding.name.text = username
            holder.binding.desc.text = "$time ${context.getString(com.mxchip.mxapp.base.R.string.mx_share_user)}"
            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(this)
            }
        }
    }

    fun setItemClickListener(listener: (ShareUserListBean) -> Unit) {
        itemClickListener = listener
    }
}