package com.mxchip.mxapp.base.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.mxchip.mxapp.base.R

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/12/9
 * @desc:
 */
class MaxHeightRecyclerView: RecyclerView {
    private var maxHeight = -1

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightRecyclerView)
        maxHeight = ta.getDimension(R.styleable.MaxHeightRecyclerView_rv_max_height, -1f).toInt()
    }

    fun setMaxHeight(height: Int) {
        maxHeight = height
        requestLayout()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        var spec = heightSpec
        if (maxHeight > 0) {
            spec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthSpec, spec)
    }
}