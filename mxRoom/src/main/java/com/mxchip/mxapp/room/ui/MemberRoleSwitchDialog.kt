package com.mxchip.mxapp.room.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import coil.load
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.room.databinding.RoomDgMemberRoleSwitchBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/18
 * @desc:
 */
class MemberRoleSwitchDialog(private val context: Context, private var currentRole: Int) {

    private var binding: RoomDgMemberRoleSwitchBinding

    private var switchListener: ((MemberRoleSwitchDialog, role: Int) -> Unit)? = null

    private val dialog = AlertDialog.Builder(context).create().apply {
        binding = RoomDgMemberRoleSwitchBinding.inflate(LayoutInflater.from(context))
        setView(binding.root)
    }

    init {
        initView()
        initEvent()
    }

    private fun initView() {
        dialog.window?.decorView?.background = Color.TRANSPARENT.toDrawable()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val attr = dialog.window?.attributes
        attr?.width = WindowManager.LayoutParams.MATCH_PARENT
        attr?.gravity = Gravity.BOTTOM
        dialog.window?.attributes = attr
        binding.rootView.background = generateShape(
            context.getColor(R.color.core_color_background_dialog),
            ShapeRadius(20, 20, 0, 0)
        )
        binding.ivAdminSelect.setImageResource(
            if (currentRole == 2) R.drawable.core_ic_round_unselected else R.drawable.core_ic_round_selected
        )
        binding.ivUserSelect.setImageResource(
            if (currentRole == 2) R.drawable.core_ic_round_selected else R.drawable.core_ic_round_unselected
        )
    }

    private fun initEvent() {
        binding.admin.setOnClickListener {
            switchListener?.invoke(this, 1)
        }

        binding.user.setOnClickListener {
            switchListener?.invoke(this, 2)
        }
    }

    fun refresh(role: Int) {
        currentRole = role
        binding.ivAdminSelect.setImageResource(
            if (currentRole == 2) R.drawable.core_ic_round_unselected else R.drawable.core_ic_round_selected
        )
        binding.ivUserSelect.setImageResource(
            if (currentRole == 2) R.drawable.core_ic_round_selected else R.drawable.core_ic_round_unselected
        )
    }

    fun setSwitchListener(listener: (MemberRoleSwitchDialog, role: Int) -> Unit) = apply {
        switchListener = listener
    }

    fun show() {
        dialog.show()
    }

    fun cancel() {
        dialog.cancel()
    }
}