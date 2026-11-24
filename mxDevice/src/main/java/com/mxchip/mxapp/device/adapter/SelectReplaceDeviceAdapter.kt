package com.mxchip.mxapp.device.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.device.databinding.DeviceItReplaceDeviceBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/10/20
 * @desc: 适配选择设备修改为activity
 */
class SelectReplaceDeviceAdapter(private val context: Context) :
    BaseAdapter<DeviceItReplaceDeviceBinding>() {

    private var itemClickListener: ((DeviceBean) -> Unit)? = null
    private val Start = 0
    private val Mid = 1
    private val End = 2
    private val Full = 3

    private lateinit var selectFlagDrawable: Drawable
    private lateinit var normalDrawable: Drawable
    private var clickPos = -1

    private val devices = arrayListOf<DeviceBean>()

    override fun getViewBinding(parent: ViewGroup): DeviceItReplaceDeviceBinding {
        selectFlagDrawable = generateStoke(3, ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 10)
        normalDrawable = generateShape(parent.context.getColor(R.color.core_color_dividers), 10)
        return DeviceItReplaceDeviceBinding.inflate(LayoutInflater.from(context), parent, false)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<DeviceItReplaceDeviceBinding>,
        position: Int
    ) {
        val item = devices[position]
        val content = item.nick_name.ifEmpty { item.product_name }
        val url = item.product_image.ifEmpty { "null" }
        holder.binding.ivIcon.load(url)
        holder.binding.deviceName.text = content
        holder.binding.room.text = item.room_name
        if (shouldShowRoomName(position)) {
            holder.binding.room.visibility = View.VISIBLE
        } else {
            holder.binding.room.visibility = View.GONE
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

        if (clickPos == position) {
            holder.binding.selectFlag.background = selectFlagDrawable
        } else {
            holder.binding.selectFlag.background = normalDrawable
        }

        holder.itemView.setOnClickListener {
            clickPos = position
            notifyDataSetChanged()
            itemClickListener?.invoke(item)
        }
    }

    private fun shouldShowRoomName(position: Int): Boolean {
        if (position == 0) return true
        val prevRoomName = devices[position - 1].room_name
        val currentRoomName = devices[position].room_name
        return prevRoomName != currentRoomName
    }

    private fun calPosition(position: Int): Int {
        val currentRoomName = devices[position].room_name
        val nextRoomName = if (position + 1 < itemCount) devices[position + 1].room_name else null
        if (position == 0) {
            return if (nextRoomName == currentRoomName) Start
            else Full
        }
        val prevRoomName = devices[position - 1].room_name
        return if (currentRoomName == prevRoomName && nextRoomName == currentRoomName) {
            Mid
        } else {
            if (prevRoomName == currentRoomName) End
            else if (currentRoomName == nextRoomName) Start
            else Full
        }
    }

    fun setItemClickListener(listener: ((DeviceBean) -> Unit)?) {
        itemClickListener = listener
    }

    fun setData(list: List<DeviceBean>) {
        devices.clear()
        devices.addAll(list)
        notifyDataSetChanged()
    }
}