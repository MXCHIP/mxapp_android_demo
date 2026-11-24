package com.mxchip.mxapp.base.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.databinding.CoreViewTopBarBinding
import com.mxchip.mxapp.base.utils.AppUtil

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/26
 * @desc:
 */
class TopBarView : ConstraintLayout {
    private var binding: CoreViewTopBarBinding
    private var rightClick: ((TopBarView) -> Unit)? = null
    private var leftClick: ((TopBarView) -> Unit)? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TopBarView)
        val isShowBack = typedArray.getInt(R.styleable.TopBarView_back_icon_visibility, 0)
        val rightText = typedArray.getString(R.styleable.TopBarView_right_text)
        val rightTextColor = typedArray.getColor(
            R.styleable.TopBarView_right_text_color,
            context.getColor(R.color.core_color_text_dialog_content)
        )
        val titleText = typedArray.getString(R.styleable.TopBarView_title_text)
        val textAndIconColor = typedArray.getColor(
            R.styleable.TopBarView_tint_color,
            context.getColor(R.color.core_color_text_title)
        )
        typedArray.recycle()

        binding = CoreViewTopBarBinding.inflate(LayoutInflater.from(context), this, true)
        binding.ivBack.setOnClickListener { leftClick?.invoke(this) }
        binding.tvRight.setOnClickListener { rightClick?.invoke(this) }

        val setTop =
            if (this.paddingTop > 0) this.paddingTop else AppUtil.getStatusBarHeight()
        setPadding(0, setTop, 0, this.paddingBottom)

        if (isShowBack == 1) {
            binding.ivBack.visibility = GONE
        }
        // 获取是否设置了android:background
        val androidTa = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.background))
        val hasBackground = typedArray.hasValue(0)
        androidTa.recycle()

        if (hasBackground.not()) {//未设置，使用默认
            setBackgroundColor(context.getColor(R.color.core_color_background_tab))
        }
        if (rightText.isNullOrBlank()) {
            binding.tvRight.visibility = GONE
        } else {
            binding.tvRight.visibility = VISIBLE
            binding.tvRight.text = rightText
        }
        binding.tvRight.setTextColor(rightTextColor)
        if (titleText.isNullOrEmpty()) {
            binding.tvTitle.visibility = GONE
        } else {
            binding.tvTitle.text = titleText
        }

        binding.tvTitle.setTextColor(textAndIconColor)
    }

    fun left(click: ((TopBarView) -> Unit)?) = apply {
        this.leftClick = click
    }

    fun showBack(isShow: Boolean) = apply {
        binding.ivBack.visibility = if (isShow) VISIBLE else INVISIBLE
    }

    fun title(text: String) = apply {
        binding.tvTitle.text = text
        binding.tvTitle.visibility = View.VISIBLE
    }

    fun right(text: String?, onClick: ((TopBarView) -> Unit)?) = apply {
        binding.tvRight.visibility = if (text.isNullOrEmpty()) GONE else VISIBLE
        binding.tvRight.text = text
        rightClick = onClick
    }

    fun right(onClick: ((TopBarView) -> Unit)?) {
        rightClick = onClick
    }

    fun rightText(text: String?, colorId: Int? = null) = apply {
        if (text.isNullOrEmpty()) {
            binding.tvRight.visibility = GONE
        } else {
            binding.tvRight.visibility = VISIBLE
            binding.tvRight.text = text
            colorId?.apply {
                binding.tvRight.setTextColor(this)
            }
        }
    }
}