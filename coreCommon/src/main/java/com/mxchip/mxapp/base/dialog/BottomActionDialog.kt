package com.mxchip.mxapp.base.dialog

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.adapter.BottomDialogAdapter
import com.mxchip.mxapp.base.databinding.CoreDgBottomActionBinding
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.generateShape

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/17
 * @desc:
 */
class BottomActionDialog(private val context: Context) {

    private var binding : CoreDgBottomActionBinding

    private val adapter by lazy {
        BottomDialogAdapter(context).apply {
            setItemClickListener { s, i ->
                eventMap[s]?.apply {
                    this.invoke(s, i)
                    dialog.cancel()
                }
            }
        }
    }

    private val eventMap = HashMap<String, ((String, Int) -> Unit)?>()

    private val dialog = AlertDialog.Builder(context).create().apply {
        binding = CoreDgBottomActionBinding.inflate(LayoutInflater.from(context))
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
        binding.rootView.background = generateShape(context.getColor(R.color.core_color_background_dialog), ShapeRadius(20, 20, 0, 0))

        binding.rvActions.layoutManager = LinearLayoutManager(context)
        binding.rvActions.adapter = adapter
    }

    private fun initEvent() {
        binding.cancel.setOnClickListener {
            dialog.cancel()
        }
    }

    fun addItem(content: String, clickListener: ((String, Int) -> Unit)?) = apply {
        eventMap[content] = clickListener
        adapter.appendData(content)
    }

    fun title(title: String?) = apply {
        binding.title.text = title
    }

    fun create() = dialog
}