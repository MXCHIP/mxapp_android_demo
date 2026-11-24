package com.mxchip.mxapp.account.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mxchip.mxapp.account.bean.CountryBean
import com.mxchip.mxapp.base.utils.LanguageUtil
import com.mxchip.mxapp.base.utils.dp2px

/**
 * 国家选择国家列表标题
 */
class RegionTitleItemDecoration(
    private val context: Context,
    private val datas: List<CountryBean>,
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        textSize = 12.dp2px(context).toFloat()
        isDither = true
        isAntiAlias = true
    }
    private var titleBgColor = Color.BLUE
    private var titleTextColor = Color.GRAY
    private var titleHeight = 24.dp2px(context)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
        if (position > -1) {
            val isZh = LanguageUtil.isZh()
            val letter = if (isZh) datas[position].pinyin[0] else datas[position].en_name[0]
            if (position > 0) {
                val lastLetter = if (isZh) datas[position - 1].pinyin[0].toString()
                else datas[position - 1].en_name[0].toString()
                if (letter.uppercase() != lastLetter.uppercase()) {
                    outRect.set(0, titleHeight, 0, 0)
                } else {
                    outRect.set(0, 0, 0, 0)
                }
            } else {
                outRect.set(0, titleHeight, 0, 0)
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        //绘制标题，第一个标题可以不绘制（悬浮标题已绘制）
        for (i in 1 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val position = params.viewLayoutPosition
                val isZh = LanguageUtil.isZh()
                val letter = if (isZh) datas[position].pinyin[0] else datas[position].en_name[0]
                val lastLetter = if (isZh) datas[position - 1].pinyin[0].toString()
                else datas[position - 1].en_name[0].toString()
                if (letter.uppercase() != lastLetter.uppercase()) {
                    drawRegionTitle(c, left, right, child, params, position)
                }
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val position = (parent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position == -1) return //在搜索到没有的索引的时候position可能等于-1，所以在这里判断一下
        val isZh = LanguageUtil.isZh()
        val tag = if (isZh) datas[position].pinyin[0].uppercase()
        else datas[position].en_name[0].uppercase()
        val child = parent.findViewHolderForLayoutPosition(position)!!.itemView
        //Canvas是否位移过的标志
        var flag = false
        if ((position + 1) < datas.size) {
            //当前第一个可见的Item的字母索引，不等于其后一个item的字母索引，说明悬浮的View要切换了
            val nextTag = if (isZh) datas[position + 1].pinyin[0].uppercase()
            else datas[position + 1].en_name[0].uppercase()
            if (tag != nextTag) {
                //当第一个可见的item在屏幕中剩下的高度小于title的高度时，开始悬浮Title的动画
                if (child.height + child.top < titleHeight) {
                    c.save()
                    flag = true
                    /**
                     * 下边的索引把上边的索引顶上去的效果
                     */
                    c.translate(0f, child.height + child.top - titleHeight.toFloat())
                }
            }
        }
        paint.color = titleBgColor
        c.drawRect(
            parent.paddingLeft.toFloat(),
            parent.paddingTop.toFloat(),
            (parent.right - parent.paddingRight).toFloat(),
            parent.paddingTop + titleHeight.toFloat(), paint
        )
        paint.color = titleTextColor
        val bounds = Rect()
        paint.getTextBounds(tag, 0, tag.length, bounds)
        c.drawText(
            tag,
            child.paddingLeft.toFloat(),
            parent.paddingTop.toFloat() + titleHeight - (titleHeight / 2 - bounds.height() / 2),
            paint
        )
        if (flag)
            c.restore() //恢复画布到之前保存的状态

    }

    private fun drawRegionTitle(
        c: Canvas,
        left: Int,
        right: Int,
        child: View,
        params: RecyclerView.LayoutParams,
        position: Int
    ) {
        paint.color = titleBgColor
        paint.textSize = 16.dp2px(context).toFloat()
        c.drawRect(
            left.toFloat(),
            child.top - params.topMargin - titleHeight.toFloat(),
            right.toFloat(),
            child.top.toFloat() - params.topMargin,
            paint
        )
        paint.color = titleTextColor
        val text = if (LanguageUtil.isZh()) datas[position].pinyin[0].uppercase()
        else datas[position].en_name[0].uppercase()
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        c.drawText(
            text,
            child.paddingLeft.toFloat(),
            child.top.toFloat() - params.topMargin - (titleHeight / 2 - bounds.height() / 2),
            paint
        )
    }

    fun backgroundColor(colorId: Int) = apply {
        titleBgColor = colorId
    }

    fun textColor(colorId: Int) = apply {
        titleTextColor = colorId
    }
}