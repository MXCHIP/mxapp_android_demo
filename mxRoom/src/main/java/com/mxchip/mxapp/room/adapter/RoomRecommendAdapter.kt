package com.mxchip.mxapp.room.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import com.mxchip.mxapp.room.bean.RoomRecommendBean
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.widget.FlowLayout
import com.mxchip.mxapp.room.R


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/22
 * @desc:
 */
class RoomRecommendAdapter: FlowLayout.Adapter<RoomRecommendAdapter.FlowViewHolder>() {
    private var itemClickListener: ((RoomRecommendBean, Int) -> Unit)? = null
    private val mList = arrayListOf<RoomRecommendBean>()

    override fun onCreateViewHolder(parent: ViewGroup?): RoomRecommendAdapter.FlowViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.room_it_room_recommend, parent, false)
        // 给 View 设置 margin
        val mlp = MarginLayoutParams(view.layoutParams)
        val margin = 5.dp2px()
        mlp.setMargins(margin, margin, margin, margin)
        view.layoutParams = mlp
        return FlowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomRecommendAdapter.FlowViewHolder, position: Int) {
        holder.content.text = mList[position].name
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(mList[position], position)
        }
    }

    override fun getItemCount() = mList.size

    inner class FlowViewHolder(itemView: View): FlowLayout.ViewHolder(itemView) {
        var content: TextView = itemView.findViewById(R.id.content)
    }

    fun setItemClickListener(click: (RoomRecommendBean, Int) -> Unit) {
        itemClickListener = click
    }

    fun notifyList(list: List<RoomRecommendBean>?) {
        list?.apply {
            mList.clear()
            mList.addAll(list)
        }
    }
}