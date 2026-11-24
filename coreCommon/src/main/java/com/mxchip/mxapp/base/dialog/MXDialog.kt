package com.mxchip.mxapp.base.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.method.NumberKeyListener
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.viewbinding.ViewBinding
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.databinding.CoreDgAlterBinding
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.generateStoke

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/29
 * @desc:
 */
class MXDialog {

    companion object {
        const val TYPE_INPUT = 1
        const val TYPE_NORMAL = 0
        const val maxLength = 30
    }

    class Builder(val context: Context, private val inputFilter: InputFilter? = null) {

        private var type = TYPE_NORMAL
        private var binding: ViewBinding

        private fun getInternalBinding() = binding as CoreDgAlterBinding

        private val dialog = AlertDialog.Builder(context).create().apply {
            binding = CoreDgAlterBinding.inflate(LayoutInflater.from(context))
            setView(binding.root)
        }

        init {
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.setCancelable(false)
            getInternalBinding().rootView.background =
                generateShape(context.getColor(R.color.core_color_background_dialog), 20)
            //编辑框 默认背景
            getInternalBinding().dialogEdittext.background =
                generateStoke(2, ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 8)
            getInternalBinding().edittextClear.setOnClickListener {
                getInternalBinding().dialogEdittext.text = null
            }
        }

        fun <Binding : ViewBinding> setCustomizeView(binding: Binding): CustomizeView<Binding> {
            this.binding = binding
            dialog.setView(binding.root)
            return CustomizeView(binding, dialog)
        }

        fun title(title: String, @ColorInt colorInt: Int? = null) = apply {
            getInternalBinding().dialogTitle.text = title
            colorInt?.apply {
                getInternalBinding().dialogTitle.setTextColor(colorInt)
            }
        }

        fun content(content: String) = apply {
            getInternalBinding().dialogContent.text = content
        }

        fun content(content: String, gravity: Int) = apply {
            getInternalBinding().dialogContent.text = content
            getInternalBinding().dialogContent.gravity = gravity
        }

        fun edittextHint(hint: String) = apply {
            this.type = TYPE_INPUT
            getInternalBinding().dialogEdittext.visibility = View.VISIBLE
            getInternalBinding().edittextClear.visibility = View.VISIBLE
            getInternalBinding().dialogEdittext.hint = hint
        }

        fun edittextBg(drawable: Drawable) = apply {
            getInternalBinding().dialogEdittext.background = drawable
        }

        fun edittextText(text: String?) = apply {
            this.type = TYPE_INPUT
            getInternalBinding().dialogEdittext.visibility = View.VISIBLE
            getInternalBinding().edittextClear.visibility = View.VISIBLE
            getInternalBinding().dialogEdittext.setText(text)
        }

        fun edittextInputType(type: Int) = apply {
            this.type = TYPE_INPUT
            getInternalBinding().dialogEdittext.inputType = type
        }

        fun edittextKeyListener(listener: NumberKeyListener) = apply {
            getInternalBinding().dialogEdittext.keyListener = listener
        }

//        fun edittextMaxLength(len: Int) = apply {
//            this.type = TYPE_INPUT
//            maxLength = len
//        }

        fun leftButton(
            text: String?,
            click: ((AlertDialog, String?) -> Unit)? = null,
        ) = apply {
            if (text.isNullOrBlank()) getInternalBinding().btnLeft.visibility = View.GONE
            else {
                getInternalBinding().btnLeft.visibility = View.VISIBLE
                getInternalBinding().btnLeft.text = text
                getInternalBinding().btnLeft.setOnClickListener {
                    val input =
                        if (type == TYPE_INPUT) getInternalBinding().dialogEdittext.text.toString() else null
                    click?.invoke(dialog, input)
                }
            }
        }

        fun rightButton(
            text: String?,
            click: ((AlertDialog, String?) -> Unit)? = null,
        ) = apply {
            if (text.isNullOrBlank()) getInternalBinding().btnRight.visibility = View.GONE
            else {
                getInternalBinding().btnRight.visibility = View.VISIBLE
                getInternalBinding().btnRight.text = text
                getInternalBinding().btnRight.setOnClickListener {
                    val input =
                        if (type == TYPE_INPUT) getInternalBinding().dialogEdittext.text.toString() else null
                    click?.invoke(dialog, input)
                }
                getInternalBinding().btnRight.setBtnBackgroundColor(
                    ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
                )
            }
        }

        fun create(): AlertDialog {
            if (inputFilter == null) {
                val defaultInputFilter = InputFilter { source, _, _, _, _, _ ->
                    if (RegexUtil.containsEmoji(source)) "" else null
                }
                //InputFilter.LengthFilter(maxLength) 长度需要toast
                getInternalBinding().dialogEdittext.filters = arrayOf(defaultInputFilter)
            } else {
                getInternalBinding().dialogEdittext.filters = arrayOf(inputFilter)
            }
            if (getInternalBinding().dialogContent.text.isEmpty()) getInternalBinding().dialogContent.visibility =
                View.GONE
            return dialog
        }
    }

    class CustomizeView<Binding : ViewBinding>(
        private val binding: Binding, private val dialog: AlertDialog
    ) {

        fun getViewBinding(): Binding = binding

        fun create(): AlertDialog {
            return dialog
        }
    }
}