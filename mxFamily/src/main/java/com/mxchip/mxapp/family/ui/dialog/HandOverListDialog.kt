package com.mxchip.mxapp.family.ui.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.family.bean.MemberBean
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.family.adapter.HandOverMemberListAdapter
import com.mxchip.mxapp.family.databinding.FamilyDgHandOverListBinding

/**
 * 移交家庭->选择家庭成员
 */
class HandOverListDialog(
    private val context: Context,
    list: List<MemberBean>,
    private var sendVerification: ((MemberBean) -> Unit)? = null
) {

    private var binding : FamilyDgHandOverListBinding
    private var filter = list.filter { it.role != 0 }

    private val dialog = AlertDialog.Builder(context).create().apply {
        binding = FamilyDgHandOverListBinding.inflate(LayoutInflater.from(context))
        setView(binding.root)
    }

    private val adapter by lazy {
        HandOverMemberListAdapter().apply {
            setItemClickListener {
                dialog.cancel()
                sendVerification?.invoke(it)
            }
        }
    }

    init {
        initView()
        initEvent()
    }

    private fun initView() {
        val count = filter.size
        val height = if (count < 5) {
            (count * 80 + 67).dp2px()
        } else {
            467.dp2px()
        }
        dialog.window?.decorView?.background = ColorDrawable(Color.TRANSPARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val attr = dialog.window?.attributes
        attr?.width = WindowManager.LayoutParams.MATCH_PARENT
        attr?.height = height
        attr?.gravity = Gravity.BOTTOM
        dialog.window?.attributes = attr
        binding.rootView.background = generateShape(context.getColor(R.color.core_color_background_dialog), ShapeRadius(20, 20, 0, 0))

        binding.rvMembers.layoutManager = LinearLayoutManager(context)
        binding.rvMembers.adapter = adapter
        adapter.setMembers(filter)
    }

    private fun initEvent() {
        binding.close.setOnClickListener {
            dialog.cancel()
        }
    }

    fun create() = dialog
}