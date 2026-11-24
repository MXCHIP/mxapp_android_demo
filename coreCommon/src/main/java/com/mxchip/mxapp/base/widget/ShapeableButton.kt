package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.generateStoke

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/12/3
 * @desc:
 */
class ShapeableButton : AppCompatButton {

    private var mSolid = 0
    private var mRadius = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(context, attrs)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ShapeableButton)
        mSolid = ta.getColor(R.styleable.ShapeableButton_sbBgColor, Color.WHITE)
        mRadius = ta.getInt(R.styleable.ShapeableButton_sbRadius, 0)
        setBtnBackgroundColor(mSolid, mRadius)
        gravity = Gravity.CENTER
    }

    override fun setBackgroundColor(color: Int) {
        setBtnBackgroundColor(color, mRadius)
    }

    fun setBtnBackgroundColor(color: Int, radius: Int) {
        mRadius = radius
        mSolid = color
        background = generateShape(mSolid, mRadius)
    }

    fun setBtnBackgroundColor(color: Int) {
        background = generateShape(color, mRadius)
    }

    fun setBtnStokeColor(color: Int) {
        background = generateStoke(1, color, mRadius)
    }

    fun setRadius(radius: Int) {
        setBtnBackgroundColor(mSolid, radius)
    }

    override fun setGravity(gravity: Int) {
        super.setGravity(Gravity.CENTER)
    }
}