package com.mxchip.mxapp.base.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 线性（水平/垂直）RecyclerView的item间间距
 */
class RVLinearItemDecorationSpace(
    private val itemDecorationSpace: Int,
    private var orientation: Int = RecyclerView.VERTICAL
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (RecyclerView.HORIZONTAL == orientation) {
//            outRect.left = itemDecorationSpace
//            val position = parent.getChildAdapterPosition(view)
//            if (position == parent.childCount - 1)
            outRect.right = itemDecorationSpace
        } else {
//            outRect.top = itemDecorationSpace
//            val position = parent.getChildAdapterPosition(view)
//            if (position == parent.childCount - 1)
            outRect.bottom = itemDecorationSpace
        }
    }
}