package com.mxchip.mxapp.base.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * 网格RecyclerView的item间距（spanSize不区分的布局）
 */
class RVGridItemDecorationSpace(
    private val itemHorizontalSpace: Int,
    private val itemVerticalSpace: Int = itemHorizontalSpace
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val spanCount=   if (parent.layoutManager is GridLayoutManager ) {
          (parent.layoutManager as GridLayoutManager).spanCount
        }else if (parent.layoutManager is StaggeredGridLayoutManager){
          (parent.layoutManager as StaggeredGridLayoutManager).spanCount
        }else null
        spanCount?.apply {
            val position = parent.getChildAdapterPosition(view)
            if (position < spanCount) {
                outRect.top = itemVerticalSpace
            }
            if (position % spanCount == 0) {
                outRect.left = itemHorizontalSpace
                outRect.right = itemHorizontalSpace
            } else {
//                outRect.left = itemHorizontalSpace
                outRect.right = itemHorizontalSpace
            }
            outRect.bottom = itemVerticalSpace
        }
    }
}