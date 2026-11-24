package com.mxchip.mxapp.family.ui.dialog

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import com.mxchip.mxapp.base.consts.HomeConstants.JOIN_HOME_TYPE_INVITE_CODE
import com.mxchip.mxapp.base.consts.HomeConstants.JOIN_HOME_TYPE_QRCODE
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.family.databinding.FamilyDgJoinHomeBinding

/**
 * 加入家庭方式选择弹框
 */
class JoinHomeDialog(context: Context) {

    private var binding : FamilyDgJoinHomeBinding

    private var joinListener: ((AlertDialog, type: Int) -> Unit)? = null

    private val dialog = AlertDialog.Builder(context).create().apply {
        binding = FamilyDgJoinHomeBinding.inflate(LayoutInflater.from(context))
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
        binding.rootView.background = generateShape(dialog.context.getColor(com.mxchip.mxapp.base.R.color.core_color_background_dialog), ShapeRadius(20, 20, 0, 0))
    }

    private fun initEvent() {
        binding.inviteCode.setOnClickListener {
            joinListener?.invoke(dialog, JOIN_HOME_TYPE_INVITE_CODE)
        }

        binding.qrcode.setOnClickListener {
            joinListener?.invoke(dialog, JOIN_HOME_TYPE_QRCODE)
        }

        binding.cancel.setOnClickListener {
            dialog.cancel()
        }
    }

    fun setListener(listener: (AlertDialog, type: Int) -> Unit)= apply {
        joinListener = listener
    }

    fun create() = dialog
}