package com.mxchip.mxapp.device.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.GridLayoutManager
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.bean.PropertyBean
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.device.adapter.DeviceSwitchAdapter
import com.mxchip.mxapp.device.databinding.DeviceDgSwitchBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/9
 * @desc:
 */
class DeviceSwitchDialog(
    context: Context,
    val iotId:String,
    deviceName: String,
    list: List<PropertyBean>
) {

    private var binding: DeviceDgSwitchBinding
    private var itemSwitchListener: ((PropertyBean) -> Unit)? = null
    private var detailClickListener: (() -> Unit)? = null

    private val adapter by lazy {
        DeviceSwitchAdapter().apply {
            setData(list)
            setItemSwitchListener { position, properties ->
                itemSwitchListener?.invoke(properties)
            }
        }
    }

    private val dialog = AlertDialog.Builder(context).create().apply {
        binding = DeviceDgSwitchBinding.inflate(LayoutInflater.from(context))
        setView(binding.root)
    }

    init {
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val attr = dialog.window?.attributes
        attr?.gravity = Gravity.BOTTOM
        dialog.window?.attributes = attr
        val count = adjust(list.size)
        binding.rootView.background =
            generateShape(context.getColor(R.color.core_color_background_dialog), 20)
        binding.switchList.layoutManager = GridLayoutManager(context, count)
        binding.switchList.addItemDecoration(RVItemDecorationSpace(top = 16))
        binding.switchList.adapter = adapter
        binding.deviceName.text = deviceName
        binding.ivEdit.setOnClickListener {
            dialog.dismiss()
            detailClickListener?.invoke()
        }
    }

    private fun adjust(size: Int): Int {
        return when (size) {
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 2
            else -> 3
        }
    }

    fun setSwitchListener(listener: ((PropertyBean) -> Unit)?) = apply {
        itemSwitchListener = listener
    }

    fun setClickDetailListener(listener: (() -> Unit)?) = apply {
        detailClickListener = listener
    }

    fun show() {
        dialog.show()
    }

    fun isShowing() = dialog.isShowing

    fun switchChange(switchIndex: Int, newSwitch: Int) {
        adapter.getData()?.get(switchIndex)?.apply {
            this.value = newSwitch
        }
        adapter.notifyItemChanged(switchIndex)
    }
}