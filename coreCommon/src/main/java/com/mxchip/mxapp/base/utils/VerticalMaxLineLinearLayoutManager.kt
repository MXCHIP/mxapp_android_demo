package com.mxchip.mxapp.base.utils

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 垂直线性布局设定最多显示item条目
 */
class VerticalMaxLineLinearLayoutManager : LinearLayoutManager {
    private var mMaxLine = 0

    constructor(context: Context, maxLine: Int) : super(context) {
        this.mMaxLine = maxLine
    }

    constructor(
        context: Context,
        orientation: Int,
        reverseLayout: Boolean,
        maxLine: Int
    ) : super(
        context,
        orientation,
        reverseLayout
    ) {
        this.mMaxLine = maxLine
    }

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        if (itemCount <= mMaxLine || itemCount == 0) {
            super.onMeasure(recycler, state, widthSpec, heightSpec)
            return
        } else {
            val child = recycler.getViewForPosition(0)
            addView(child)
            measureChildWithMargins(child, 0, 0)
            val itemWidth = getDecoratedMeasuredWidth(child)
            val itemHeight = getDecoratedMeasuredHeight(child)
            removeAndRecycleView(child, recycler)
            val widthMode = View.MeasureSpec.getMode(widthSpec)
            val width = if (widthMode == View.MeasureSpec.EXACTLY) {
                View.MeasureSpec.getSize(widthSpec)
            } else {
                itemWidth
            }
            val height = itemHeight * mMaxLine

            setMeasuredDimension(width, height)

        }
    }

    override fun isAutoMeasureEnabled(): Boolean {
        if (itemCount <= mMaxLine)
            return super.isAutoMeasureEnabled()
        return false
    }
}