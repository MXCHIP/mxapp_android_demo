package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.utils.dp2px

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/23
 * @desc:
 */
class SideBar : View {

    private var onTouchingLetterChangedListener: ((String) -> Unit)? = null

    private var letters = arrayOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I",
        "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
        "W", "X", "Y", "Z"
    )
    private var choose = -1
    private var paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.core_color_text_list_content)
        isAntiAlias = true
        textSize = 12.dp2px().toFloat()
    }
    private var textToast: TextView? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val height = height
        val width = width

        if (letters.isEmpty()) return

        // 计算字母间距（根据字体大小自适应）
        val textSize = paint.textSize
        val letterSpacing = textSize * 1.5f // 字母间距为字体大小的1.5倍

        // 计算总高度（所有字母的总高度）
        val totalHeight = (letters.size - 1) * letterSpacing

        // 从中间开始，计算起始Y坐标
        val centerY = height / 2f
        val startY = centerY - totalHeight / 2f

        letters.forEachIndexed { index, s ->
            if (index == choose) {
                paint.isFakeBoldText = true
            }
            // x坐标等于中间-字符串宽度的一半
            val xPos = width / 2 - paint.measureText(letters[index]) / 2
            // y坐标从中间开始，向上下延伸
            val yPos = startY + index * letterSpacing
            canvas.drawText(letters[index], xPos, yPos, paint)
            if (index == choose) {
                paint.isFakeBoldText = false
            }
        }
    }

    fun setLetters(letters: Array<String>) {
        this.letters = letters
        invalidate()
    }

    fun setTextToast(view: TextView) {
        textToast = view
    }

    fun setOnTouchLetterListener(listener: (String) -> Unit) {
        this.onTouchingLetterChangedListener = listener
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.action
        val y = event?.y ?: 0f
        val oldChoose = choose

        // 根据居中布局计算触摸位置对应的字母索引
        val c = findLetterIndexByTouchY(y)

        when (action) {
            MotionEvent.ACTION_UP -> {
                choose = -1
                invalidate()
                textToast?.visibility = View.INVISIBLE
            }

            else -> {
                if (oldChoose != c) {
                    if (c >= 0 && c < letters.size) {
                        onTouchingLetterChangedListener?.invoke(letters[c])
                        textToast?.text = letters[c]
                        textToast?.visibility = View.VISIBLE
                        choose = c
                        invalidate()
                    }
                }
            }
        }
        return true
    }

    /**
     * 根据触摸的Y坐标，找到对应的字母索引（居中布局）
     */
    private fun findLetterIndexByTouchY(touchY: Float): Int {
        if (letters.isEmpty()) return -1

        val textSize = paint.textSize
        val letterSpacing = textSize * 1.5f
        val totalHeight = (letters.size - 1) * letterSpacing
        val centerY = height / 2f
        val startY = centerY - totalHeight / 2f

        // 找到最接近触摸位置的字母
        var minDistance = Float.MAX_VALUE
        var closestIndex = -1

        letters.forEachIndexed { index, _ ->
            val letterY = startY + index * letterSpacing
            val distance = kotlin.math.abs(touchY - letterY)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        return closestIndex
    }
}