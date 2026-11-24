package com.mxchip.mxapp.base.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 列表卡片间隔
 */
class RVItemDecorationSpace(
    val left: Int? = null,
    val right: Int? = null,
    val top: Int? = null,
    val bottom: Int? = null

) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        top?.apply {
            outRect.top = this
        }
        bottom?.apply {
            outRect.bottom = this
        }
        left?.apply {
            outRect.left = this
        }
        right?.apply {
            outRect.right = this
        }
    }
}