package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.mxchip.mxapp.base.R

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/12
 * @desc:
 */
class CustomizeSwitchView : View {

    companion object {
        private const val DEFAULT_WIDTH = 44
        private const val DEFAULT_HEIGHT = 26
        private val NORMAL_OUTER_BG_COLOR = Color.parseColor("#DEDEDE")
        private const val NORMAL_INNER_BG_COLOR = Color.WHITE
        private val CHECKED_OUTER_BG_COLOR = Color.parseColor("#13B7F9")
        private const val CHECKED_INNER_BG_COLOR = Color.WHITE
    }

    private var outRadius = 0
    private var innerRadius = 0
    private var normalOutBgColor = NORMAL_OUTER_BG_COLOR
    private var normalInnerBgColor = NORMAL_INNER_BG_COLOR
    private var checkedOutBgColor = CHECKED_OUTER_BG_COLOR
    private var checkedInnerBgColor = CHECKED_INNER_BG_COLOR
    private var innerDx = 0
    private var innerCheckedDx = 0
    private var innerDy = 0
    private var checked = false
    private var checkChangeListener: ((Boolean, View) -> Unit)? = null

    private var outerPaint = Paint().apply {
        isDither = true
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var innerPaint = Paint().apply {
        isDither = true
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, def: Int) : super(
        context,
        attributeSet,
        def
    ) {
        initAttr(context, attributeSet)
        inject()
    }

    private fun initAttr(context: Context, attributeSet: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.CustomizeSwitchView)
        normalOutBgColor =
            ta.getColor(R.styleable.CustomizeSwitchView_cs_normal_outer_bg, NORMAL_OUTER_BG_COLOR)
        normalInnerBgColor =
            ta.getColor(R.styleable.CustomizeSwitchView_cs_normal_inner_bg, NORMAL_INNER_BG_COLOR)
        checkedOutBgColor =
            ta.getColor(R.styleable.CustomizeSwitchView_cs_checked_outer_bg, CHECKED_OUTER_BG_COLOR)
        checkedInnerBgColor =
            ta.getColor(R.styleable.CustomizeSwitchView_cs_checked_inner_bg, CHECKED_INNER_BG_COLOR)
        ta.recycle()
    }

    private fun inject() {
        setOnClickListener {
            checked = !checked
            invalidate()
            checkChangeListener?.invoke(checked, it)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 0
        var height = 0
        var specMode = MeasureSpec.getMode(widthMeasureSpec)
        var specSize = MeasureSpec.getSize(widthMeasureSpec)
        when (specMode) {
            MeasureSpec.EXACTLY -> width = specSize
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> width = dp2px(DEFAULT_WIDTH)
        }
        specMode = MeasureSpec.getMode(heightMeasureSpec)
        specSize = MeasureSpec.getSize(heightMeasureSpec)
        when (specMode) {
            MeasureSpec.EXACTLY -> height = specSize
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> height = dp2px(DEFAULT_HEIGHT)
        }
        outRadius = height / 2
        innerRadius = (height - dp2px(8)) / 2
        innerDx = dp2px(4) + innerRadius
        innerCheckedDx = width - dp2px(4) - innerRadius
        innerDy = outRadius
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawOuter(canvas)
        drawInner(canvas)
    }

    final override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }

    private fun drawOuter(canvas: Canvas) {
        outerPaint.color = if (checked) checkedOutBgColor else normalOutBgColor
        canvas.drawRoundRect(
            0f,
            0f,
            measuredWidth.toFloat(),
            measuredHeight.toFloat(),
            outRadius.toFloat(),
            outRadius.toFloat(),
            outerPaint
        )
    }

    private fun drawInner(canvas: Canvas) {
        innerPaint.color = if (checked) checkedInnerBgColor else normalInnerBgColor
        val dx = if (checked) innerCheckedDx else innerDx
        canvas.drawCircle(dx.toFloat(), innerDy.toFloat(), innerRadius.toFloat(), innerPaint)
    }

    private fun dp2px(dpValue: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dpValue * density + 0.5f).toInt()
    }

    fun setOnCheckChangeListener(listener: (Boolean, View) -> Unit) {
        checkChangeListener = listener
    }

    fun setChecked(value: Boolean) = apply {
        checked = value
        invalidate()
        checkChangeListener?.invoke(value, this)
    }

    fun isChecked() = checked

    fun normalOuterBgColorId(colorId: Int) = apply {
        normalOutBgColor = colorId
    }

    fun normalInnerBgColorId(colorId: Int) = apply {
        normalInnerBgColor = colorId
    }

    fun checkedOuterBgColorId(colorId: Int) = apply {
        checkedOutBgColor = colorId
    }

    fun checkedInnerBgColorId(colorId: Int) = apply {
        checkedInnerBgColor = colorId
    }
}