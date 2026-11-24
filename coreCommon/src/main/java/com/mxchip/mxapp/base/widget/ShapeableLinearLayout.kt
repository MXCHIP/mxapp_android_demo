package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import com.mxchip.mxapp.base.R

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/22
 * @desc:
 */
class ShapeableLinearLayout: LinearLayout {

    private lateinit var paint: Paint
    private var mSolid = Color.WHITE
    private var mRadius = 0
    private val path = Path()

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initAttrs(context, attrs)
        initPaint()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        path.addRoundRect(RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat()), mRadius.toFloat(), mRadius.toFloat(), Path.Direction.CW)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val outRectF = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        canvas.drawRoundRect(outRectF, mRadius.toFloat(), mRadius.toFloat(), paint)
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.Shapeable)
        mSolid = ta.getColor(R.styleable.Shapeable_solid, Color.WHITE)
        mRadius = dp2px(ta.getInt(R.styleable.Shapeable_radius, 0))
    }

    private fun initPaint() {
        setWillNotDraw(false)
        paint = Paint()
        paint.color = mSolid
        paint.isAntiAlias = true
        paint.isDither = true
    }

    private fun dp2px(dp: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}