package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.utils.dp2px
import kotlin.math.max

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/26
 * @desc:
 */
class ProgressButton : View {

    companion object {
        private val DEFAULT_BG_COLOR = Color.parseColor("#1a13B7F9")
        private val DEFAULT_PROGRESS_COLOR = Color.parseColor("#ff13b7f9")
    }

    private var radius = 0f
    private var bgColor = -1
    private var progressColor = -1
    private lateinit var bgPaint: Paint
    private lateinit var progressPaint: Paint
    private lateinit var textPaint: TextPaint
    private var text = ""
    private var textColor = -1
    private var rectF = RectF()
    private var progress = 50

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttr(attrs)
        initPaint()
    }

    private fun initPaint() {
        bgPaint = Paint().apply {
            isDither = true
            isAntiAlias = true
            style = Paint.Style.FILL
            color = bgColor
        }

        progressPaint = Paint(bgPaint).apply {
            color = progressColor
        }

        textPaint = TextPaint().apply {
            color = textColor
            textSize = 17.dp2px().toFloat()
            textAlign = Paint.Align.CENTER
        }
    }

    private fun initAttr(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton)
        radius = ta.getDimension(R.styleable.ProgressButton_pb_radius, 0f)
        bgColor = ta.getColor(R.styleable.ProgressButton_pb_bg_color, DEFAULT_BG_COLOR)
        progressColor =
            ta.getColor(R.styleable.ProgressButton_pb_progress_color, DEFAULT_PROGRESS_COLOR)
        text = ta.getString(R.styleable.ProgressButton_pb_text) ?: ""
        textColor = ta.getColor(
            R.styleable.ProgressButton_pb_text_color,
            context.getColor(R.color.core_color_background_fifth)
        )
        ta.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = suggestedMinimumWidth
        val minHeight = suggestedMinimumHeight
        var width = 0
        var height = 0
        var specMode = MeasureSpec.getMode(widthMeasureSpec)
        var specSize = MeasureSpec.getSize(widthMeasureSpec)
        when (specMode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> width = specSize
            MeasureSpec.UNSPECIFIED -> width = max(specSize, minWidth)
        }
        specMode = MeasureSpec.getMode(heightMeasureSpec)
        specSize = MeasureSpec.getSize(heightMeasureSpec)
        when (specMode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> height = specSize
            MeasureSpec.UNSPECIFIED -> height = max(specSize, minHeight)
        }
        setMeasuredDimension(width, height)
        rectF = RectF(
            paddingStart.toFloat(),
            paddingTop.toFloat(),
            (width - paddingEnd).toFloat(),
            (height - paddingBottom).toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawProgress(canvas)
        drawText(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRoundRect(rectF, radius, radius, bgPaint)
    }

    private fun drawText(canvas: Canvas) {
        val fontMetrics = textPaint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val baseline = rectF.centerY() + distance
        canvas.drawText(text, rectF.centerX(), baseline, textPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        val w = width - paddingStart - paddingEnd
        var progressWidth = (progress / 100f) * w
        if (progressWidth < radius * 2) progressWidth = radius * 2
        val rect = RectF(
            paddingStart.toFloat(),
            paddingTop.toFloat(),
            paddingStart + progressWidth,
            (height - paddingBottom).toFloat()
        )
        canvas.drawRoundRect(rect, radius, radius, progressPaint)
    }

    fun setProgress(progress: Int) = apply {
        when {
            progress < 0 -> this.progress = 0
            progress > 100 -> this.progress = 100
            else -> this.progress = progress
        }
    }

    fun setText(text: String?) = apply {
        this.text = text ?: ""
    }

    fun setProgressColor(@ColorInt color: Int) = apply {
        progressPaint.color = color
    }
}