package com.mxchip.mxapp.base.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.databinding.CoreViewItemBinding

/**
 *自定义组合View（首页侧滑菜单item）
 */
class CommonItemView : ConstraintLayout {
    private var binding: CoreViewItemBinding? = null

    constructor(context: Context) : super(context) {

    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
        initAttrs(context, attrs)
    }

    private fun initView(context: Context) {
        binding = CoreViewItemBinding.inflate(LayoutInflater.from(context), this, true)

    }


    private fun initAttrs(context: Context, attrs: AttributeSet) {
        val mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.NotifyItemView);
        val startIcon = mTypedArray.getResourceId(R.styleable.NotifyItemView_start_icon, -1)
        if (startIcon != -1) {
            val tint = mTypedArray.getColor(R.styleable.NotifyItemView_start_icon_tint, 0)
            if (tint != 0) {
                binding?.ivStart?.setColorFilter(tint)
            }
            binding?.ivStart?.setImageResource(startIcon)
            binding?.ivStart?.visibility = View.VISIBLE
        }

        val text = mTypedArray.getResourceId(R.styleable.NotifyItemView_niv_text, -1)
        if (text != -1) {
            binding?.tvTitle?.setText(text)
        }

        val textColor = mTypedArray.getColor(R.styleable.NotifyItemView_text_color, -1)
        if (textColor != -1) {
            binding?.tvTitle?.setTextColor(textColor)
        }
        val desc = mTypedArray.getResourceId(R.styleable.NotifyItemView_text_describe, -1)
        if (desc != -1) {
            binding?.tvDesc?.setText(desc)
        }
        val endIcon = mTypedArray.getResourceId(R.styleable.NotifyItemView_end_icon, -1)
        if (endIcon != -1) {
            binding?.ivEnd?.setImageResource(endIcon)
        }
        mTypedArray.recycle()
    }

    fun setShowRedPoint(isShowRedPoint: Boolean) = apply {
        if (isShowRedPoint)
            binding?.viewRedPoint?.visibility = View.VISIBLE
        else
            binding?.viewRedPoint?.visibility = View.GONE
    }

    fun setStartIconColor(@ColorInt color: Int) = apply {
        binding?.ivStart?.setColorFilter(color)
    }

    fun setEndText(desc: String?) = apply {
        binding?.tvDesc?.text = desc
    }

    fun setEndIcon(@DrawableRes iconRes: Int?) = apply {
        if (iconRes == null)
            binding?.ivEnd?.setImageIcon(null)
        else
            binding?.ivEnd?.setImageResource(iconRes)
    }

    fun setEndIconGone() = apply {
        binding?.ivEnd?.visibility = View.GONE
    }

    fun getEndText(): String = binding?.tvDesc?.text?.toString() ?: ""
}