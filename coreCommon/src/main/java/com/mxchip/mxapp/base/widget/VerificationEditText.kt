package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.*
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatEditText
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.utils.dp2px
import java.util.*
import kotlin.concurrent.timerTask

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/27
 * @desc:
 */
class VerificationEditText: AppCompatEditText, TextWatcher {

    companion object {
        private const val DEFAULT_NUMBER = 6
        private const val DEFAULT_MARGIN = 4f
        private const val DEFAULT_CURSOR_DURATION = 400
        private const val DEFAULT_RADIUS = 4
        private val DEFAULT_BG_COLOR = Color.parseColor("#00000000")
        private val DEFAULT_CURSOR_COLOR = Color.parseColor("#13B7F9")
        private val DEFAULT_STROKE_FOCUS_COLOR = Color.parseColor("#13B7F9")
        private val DEFAULT_STROKE_NORMAL_COLOR = Color.parseColor("#EEEEEE")
    }
    private var number = 6
    private var margin = 0
    private var cursorDuration = DEFAULT_CURSOR_DURATION
    private var bgColor = DEFAULT_BG_COLOR
    private var cursorWidth = 20
    private var cursorColor = DEFAULT_CURSOR_COLOR
    private var radius = DEFAULT_RADIUS
    private var strokeFocusColor = DEFAULT_STROKE_FOCUS_COLOR
    private var strokeNormalColor = DEFAULT_STROKE_NORMAL_COLOR

    private val path = Path()

    private var currentPosition = 0
    private var eachRectLength = 0
    private lateinit var bgPaint: Paint
    private lateinit var cursorPaint: Paint
    private lateinit var focusPathPaint: Paint
    private lateinit var normalPathPaint: Paint

    private var isShowCursor = true
    private lateinit var cursorTimerTask: TimerTask
    private lateinit var cursorTimer: Timer

    private var onInputCompleted: ((String) -> Unit)? = null
    private var textChantedListener: ((String) -> Unit)? = null


    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initAttr(attrs)
        setBackgroundColor(Color.TRANSPARENT)
        initPaint()
        initCursorTimer()
        isFocusableInTouchMode = true
        super.addTextChangedListener(this)
    }

    private fun initAttr(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.VerificationEditText)
        number = ta.getInt(R.styleable.VerificationEditText_number, DEFAULT_NUMBER)
        margin = ta.getDimension(R.styleable.VerificationEditText_inputMargin, DEFAULT_MARGIN).toInt()
        cursorDuration = ta.getInt(R.styleable.VerificationEditText_cursorDuration, DEFAULT_CURSOR_DURATION)
        bgColor = ta.getColor(R.styleable.VerificationEditText_bgColor, DEFAULT_BG_COLOR)
        cursorWidth = ta.getDimension(R.styleable.VerificationEditText_cursorWidth, 1.dp2px(context).toFloat()).toInt()
        cursorColor = ta.getColor(R.styleable.VerificationEditText_cursorColor, DEFAULT_CURSOR_COLOR)
        radius = ta.getDimension(R.styleable.VerificationEditText_inputRadius, DEFAULT_RADIUS.toFloat()).toInt()
        strokeFocusColor = ta.getColor(R.styleable.VerificationEditText_strokeFocusColor, DEFAULT_STROKE_FOCUS_COLOR)
        strokeNormalColor = ta.getColor(R.styleable.VerificationEditText_strokeNormalColor, DEFAULT_STROKE_NORMAL_COLOR)
        ta.recycle()
    }

    private fun initPaint() {
        bgPaint = Paint().apply {
            isAntiAlias = true
            color = bgColor
        }
        cursorPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            color = cursorColor
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = cursorWidth.toFloat()
        }

        focusPathPaint = Paint(cursorPaint).apply {
            style = Paint.Style.STROKE
            color = strokeFocusColor
            strokeWidth = 2.dp2px().toFloat()
        }

        normalPathPaint = Paint(focusPathPaint).apply {
            color = strokeNormalColor
        }
    }

    private fun initCursorTimer() {
        cursorTimerTask = timerTask {
            isShowCursor = !isShowCursor
            postInvalidate()
        }
        cursorTimer = Timer()
    }

    fun showKeyBoard(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_FORCED)
    }

    fun hideKeyBoard(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun setOnInputCompletedListener(listener: (String) -> Unit) {
        onInputCompleted = listener
    }

    fun setOnTextChangedListener(listener: (String) -> Unit) {
        textChantedListener = listener
    }

    fun number(value: Int) = apply {
        number = value
    }

    fun bgColor(@ColorRes res: Int) = apply {
        bgColor = context.getColor(res)
    }

    fun cursorColor(@ColorRes res: Int) = apply {
        cursorColor = context.getColor(res)
    }

    fun inputMargin(value: Int) = apply {
        margin = value
    }

    fun duration(value: Int) = apply {
        cursorDuration = value
    }

    fun radius(value: Int)  = apply {
        radius = value.dp2px(context)
    }

    fun strokeFocusColor(@ColorRes res: Int) = apply {
        strokeFocusColor = context.getColor(res)
    }

    fun strokeNormalColor(@ColorRes res: Int) = apply {
        strokeNormalColor = context.getColor(res)
    }

    fun refresh() {
        postInvalidate()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        currentPosition = text.toString().length
        postInvalidate()
    }

    override fun afterTextChanged(s: Editable?) {
        currentPosition = text.toString().length
        postInvalidate()
        if (text!!.length == number) {
            onInputCompleted?.invoke(text.toString())
        } else if (text!!.length > number) {
            text!!.delete(number, text!!.length)
        }
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        currentPosition = text.toString().length
        postInvalidate()
        textChantedListener?.invoke(text.toString())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cursorTimer.scheduleAtFixedRate(cursorTimerTask, 0L, cursorDuration.toLong())
    }

    override fun onDetachedFromWindow() {
        cursorTimer.cancel()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            requestFocus()
            setSelection(text!!.length)
            showKeyBoard(context)
            return false
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthResult = 0
        var heightResult = 0
        //最终的宽度
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        //每个矩形形的宽度  UI要求 这里先固定宽度和高度
        eachRectLength = 40.dp2px()//(widthResult - margin * (number - 1)) / number
        widthResult = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
//            getScreenWidth(context)
            margin * (number - 1) + number * eachRectLength
        }
//        //每个矩形形的宽度  UI要求 这里先固定宽度和高度
//        eachRectLength = 40.dp2px()//(widthResult - margin * (number - 1)) / number
        //最终的高度
        //最终的高度
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        heightResult = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            //eachRectLength
            50.dp2px()
        }
        setMeasuredDimension(widthResult, heightResult)
    }

    override fun onDraw(canvas: Canvas) {
        currentPosition = text?.length?:0
        val width = eachRectLength - paddingLeft - paddingRight
        val height = measuredHeight - paddingTop - paddingBottom
        drawInputBox(canvas, width, height)
        drawText(canvas, width, height)
        drawCursor(canvas, width, height)
    }

    private fun drawInputBox(canvas: Canvas, width: Int, height: Int) {
        for (i in 0 until number) {
            canvas.save()
            val start = width * i + i * margin
            val end = width + start
            val rectF = RectF(start.toFloat(), 0f, end.toFloat(), height.toFloat())
            canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), bgPaint)
            rectF.apply {
                left += 2
                top += 2
                right -= 2
                bottom -= 2
            }
            drawStroke(canvas, rectF, normalPathPaint)
            canvas.restore()
        }
    }

    private fun drawStroke(canvas: Canvas, rectF: RectF, paint: Paint) {
        canvas.save()
        path.reset()
        path.addRoundRect(rectF, radius.toFloat(), radius.toFloat(), Path.Direction.CCW)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawText(canvas: Canvas, width: Int, height: Int) {
        val value = text.toString()
        for (i in value.indices) {
            canvas.save()
            val start = width * i + i * margin
            val x = (start + width / 2).toFloat()
            val paint = paint
            paint.textAlign = Paint.Align.CENTER
            paint.color = currentTextColor
            paint.textSize = textSize
            val fontMetrics = paint.fontMetrics
            val baseline = ((height - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top)
            canvas.drawText(value[i].toString(), x, baseline, paint)
            canvas.restore()
        }
    }

    private fun drawCursor(canvas: Canvas, width: Int, height: Int) {
        if (currentPosition < number && hasFocus()) {
            val start = width * currentPosition + currentPosition * margin
            val end = width + start
            val rectF = RectF(start.toFloat() + 2, 2f, end.toFloat() - 2, height.toFloat() - 2)
            drawStroke(canvas, rectF, focusPathPaint)

            if (!isShowCursor && isCursorVisible) {
                canvas.save()
                val startX: Int = currentPosition * (width + margin) + width / 2
                val startY = height / 4
                val endY = height - height / 4
                canvas.drawLine(
                    startX.toFloat(),
                    startY.toFloat(),
                    startX.toFloat(),
                    endY.toFloat(),
                    cursorPaint
                )
                canvas.restore()
            }
        }
    }
}