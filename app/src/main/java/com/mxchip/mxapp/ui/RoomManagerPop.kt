package com.mxchip.mxapp.ui

import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.databinding.PopwindowRoomManagerBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/7
 * @desc:
 */
class RoomManagerPop(activity: FragmentActivity) : Dialog(activity) {

    private val binding =
        PopwindowRoomManagerBinding.inflate(LayoutInflater.from(activity), null, false)
    private var roomManagerBlock: (() -> Unit)? = null
    private var deviceManagerBlock: (() -> Unit)? = null

    init {
        setContentView(binding.root)
        window?.setGravity(Gravity.TOP or Gravity.END)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val params = window?.attributes
        params?.y = 200.dp2px()
        params?.x = 10.dp2px()
        window?.attributes = params
        initEvent()
    }

    private fun initEvent() {
        binding.roomManage.setOnClickListener {
            roomManagerBlock?.invoke()
        }

        binding.deviceManage.setOnClickListener {
            deviceManagerBlock?.invoke()
        }

//        LiveDataBus.with<RoomBean>(KEY_DEFAULT_ROOM_INFO).observe(activity) {
//            adapter.addDefaultRoomInfo(it)
//        }
    }

    fun roomManageListener(block: () -> Unit) = apply {
        roomManagerBlock = block
    }

    fun deviceManageListener(block: () -> Unit) = apply {
        deviceManagerBlock = block
    }
}