package com.mxchip.mxapp.base.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import coil.load
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.databinding.CoreViewEmptyBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/23
 * @desc:
 */
class EmptyView : FrameLayout {

    private lateinit var binding: CoreViewEmptyBinding

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init()
        initAttrs(attrs)
    }

    private fun init() {
        binding = CoreViewEmptyBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.EmptyView)
        val content = ta.getString(R.styleable.EmptyView_ev_content)
        binding.content.text = content
        ta.getDrawable(R.styleable.EmptyView_ev_icon)?.apply {
            binding.icon.load(this)
        }
        val color = ta.getColor(R.styleable.EmptyView_ev_icon_color, -1)
        if (color != -1) binding.iconLayer.setColorFilter(color)
    }

    fun content(content: String) = apply {
        binding.content.text = content
    }

    fun icon(@DrawableRes icon: Int) = apply {
        binding.icon.load(icon)
    }

    fun setIconColor(@ColorInt color: Int) = apply {
        binding.iconLayer.setColorFilter(color)
    }
}