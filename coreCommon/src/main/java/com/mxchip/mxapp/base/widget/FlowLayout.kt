package com.mxchip.mxapp.base.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/22
 * @desc:
 */
class FlowLayout : ViewGroup {

    companion object {
        private const val TAG = "FlowLayout"
    }

    private val mChildrenPositionList = ArrayList<Rect>() // 记录各子 View 的位置

    private var mMaxLines = Int.MAX_VALUE // 最多显示的行数，默认无限制

    private var mVisibleItemCount = 0 // 可见的 item 数

    constructor(context: Context) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 清除之前的位置
        mChildrenPositionList.clear()
        // 测量所有子元素（这样 child.getMeasuredXXX 才能获取到值）
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val a: IntArray = helper(widthSize)
        var measuredHeight = 0
        // EXACTLY 模式：对应指定大小和 match_parent
        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = a[0]
        }
        var measuredWidth = 0
        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = a[1]
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun helper(widthSize: Int): IntArray {
        var isOneRow = true // 是否是单行
        var width = paddingLeft // 记录当前行已有的宽度
        var height = paddingTop // 记录当前行已有的高度
        var maxHeight = 0 // 记录当前行的最大高度
        var currLine = 1 // 记录当前行数

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // 获取当前子元素的 margin
            val params: LayoutParams = child.layoutParams
            var mlp: MarginLayoutParams = if (params is MarginLayoutParams) {
                params
            } else {
                MarginLayoutParams(params)
            }
            // 记录子元素所占宽度和高度
            val childWidth: Int = mlp.leftMargin + child.measuredWidth + mlp.rightMargin
            val childHeight: Int = mlp.topMargin + child.measuredHeight + mlp.bottomMargin
            maxHeight = max(maxHeight, childHeight)

            // 判断是否要换行
            if (width + childWidth + paddingRight > widthSize) {
                // 加上该行的最大高度
                height += maxHeight
                // 重置 width 和 maxHeight
                width = paddingLeft
                maxHeight = childHeight
                isOneRow = false
                currLine++
                if (currLine > mMaxLines) {
                    break
                }
            }
            // 存储该子元素的位置，在 onLayout 时设置
            val rect = Rect(
                width + mlp.leftMargin,
                height + mlp.topMargin,
                width + childWidth - mlp.rightMargin,
                height + childHeight - mlp.bottomMargin
            )
            mChildrenPositionList.add(rect)
            // 加上该子元素的宽度
            width += childWidth
        }

        val res = IntArray(2)
        res[0] = height + maxHeight + paddingBottom
        res[1] = if (isOneRow) width + paddingRight else widthSize

        return res
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 布置子 View 的位置
        val n = Math.min(childCount, mChildrenPositionList.size)
        for (i in 0 until n) {
            val child = getChildAt(i)
            val rect = mChildrenPositionList[i]
            child.layout(rect.left, rect.top, rect.right, rect.bottom)
        }
        mVisibleItemCount = n
    }

    /**
     * 设置 Adapter
     */
    fun <VH : ViewHolder> setAdapter(adapter: Adapter<VH>) {
        // 移除之前的视图
        removeAllViews()
        // 添加 item
        val n: Int = adapter.getItemCount()
        for (i in 0 until n) {
            val holder = adapter.onCreateViewHolder(this)
            adapter.onBindViewHolder(holder, i)
            val child = holder.itemView
            addView(child)
        }
    }

    /**
     * 设置最多显示的行数
     */
    fun setMaxLines(maxLines: Int) {
        mMaxLines = maxLines
    }

    /**
     * 获取显示的 item 数
     */
    fun getVisibleItemCount(): Int {
        return mVisibleItemCount
    }

    abstract class Adapter<VH : ViewHolder?> {
        abstract fun onCreateViewHolder(parent: ViewGroup?): VH
        abstract fun onBindViewHolder(holder: VH, position: Int)
        abstract fun getItemCount(): Int
    }

    abstract class ViewHolder(itemView: View?) {
        val itemView: View

        init {
            requireNotNull(itemView) { "itemView may not be null" }
            this.itemView = itemView
        }
    }
}