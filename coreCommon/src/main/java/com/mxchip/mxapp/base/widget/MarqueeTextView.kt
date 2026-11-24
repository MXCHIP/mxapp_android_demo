package com.mxchip.mxapp.base.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mxchip.mxapp.base.R

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/8/3
 * @desc: 跑马灯效果 textview
 */
class MarqueeTextView: AppCompatTextView {
    private var isOpenMarquee = true

    constructor(context: Context): this(context, null)

    constructor(context: Context, attr: AttributeSet?): this(context, attr, 0)

    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int): super(context, attr, defStyleAttr) {
        maxLines = 1
        isSingleLine = true
        val ta = context.obtainStyledAttributes(attr, R.styleable.MarqueeTextView)
        isOpenMarquee = ta.getBoolean(R.styleable.MarqueeTextView_isMarquee, true)
        if (isOpenMarquee) {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isFocusable = true
            isFocusableInTouchMode = true
        } else {
            ellipsize = TextUtils.TruncateAt.END
        }
    }

    override fun isFocused(): Boolean {
        return isOpenMarquee
    }
}