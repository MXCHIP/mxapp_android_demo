package com.mxchip.mxapp.base.device.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.bean.DeviceWriteStateBean
import com.mxchip.mxapp.base.databinding.CoreItWriteRuleProcessBinding


/**
 * 对设备同步数据进度弹框的适配器
 */
class WriteRuleProcessDialogAdapter(private val clickSyncFailed: (Int) -> Unit) :
    BaseAdapter<CoreItWriteRuleProcessBinding>() {

    companion object {
        private const val STATE_WRITING = 0
        const val STATE_SUCCESS = 1
        const val STATE_FAILED = 2
    }

    private val list = ArrayList<DeviceWriteStateBean>()

    private val loadingAnim = RotateAnimation(
        0f,
        359f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    ).apply {
        duration = 1000
        repeatCount = Animation.INFINITE
        repeatMode = Animation.RESTART
        interpolator = LinearInterpolator()
    }


    override fun getItemCount() = list.size

    override fun getViewBinding(parent: ViewGroup) =
        CoreItWriteRuleProcessBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

    override fun onBindViewHolder(
        holder: BaseViewHolder<CoreItWriteRuleProcessBinding>,
        position: Int
    ) {
        val data = list[position]
        holder.binding.deviceImage.load(data.product_image)
        holder.binding.deviceName.text =
            if (data.nick_name.isNullOrEmpty()) data.product_name else data.nick_name
        if (data.groupWriteType != null) { //群组
            holder.binding.desc.text = data.writeDescription
        } else {
            holder.binding.desc.text = data.room_name
        }

        when (data.writeState) {
            STATE_WRITING -> {
                holder.binding.ivState.clearAnimation()
                holder.binding.ivState.visibility = View.VISIBLE
                holder.binding.ivState.setImageResource(R.drawable.core_ic_progress_loading)
                holder.binding.ivState.startAnimation(loadingAnim)
                holder.binding.ivFailed.visibility = View.GONE
                holder.binding.tvFailed.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }

            STATE_SUCCESS -> {
                holder.binding.ivState.clearAnimation()
                holder.binding.ivState.visibility = View.VISIBLE
                holder.binding.ivState.setImageResource(R.drawable.core_ic_progress_successful)
                holder.binding.ivFailed.visibility = View.GONE
                holder.binding.tvFailed.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }

            STATE_FAILED -> {
                holder.binding.ivState.clearAnimation()
                holder.binding.ivState.visibility = View.VISIBLE
                holder.binding.ivFailed.visibility = View.VISIBLE
                holder.binding.tvFailed.visibility = View.VISIBLE
                    holder.binding.ivState.setImageResource(R.drawable.core_ic_progress_err)
                    holder.binding.tvFailed.setText(com.mxchip.mxapp.base.R.string.mx_failed)
//                if (data.groupWriteType != null) {
//                    holder.binding.tvFailed.setText(com.mxchip.mxapp.base.R.string.core_failed)
//                    //群组
//                } else {
//                    //同一个image使用colorFilter会出现滤镜效果......
////                    holder.binding.ivState.load(R.drawable.ic_exclamation_point_warning)
//                    holder.binding.tvFailed.setText(com.mxchip.mxapp.base.R.string.core_failed)
//                }
                holder.itemView.setOnClickListener {
                    clickSyncFailed.invoke(position)
                }
            }

            else -> {
                holder.binding.ivState.visibility = View.INVISIBLE
                holder.binding.ivFailed.visibility = View.GONE
                holder.binding.tvFailed.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }
        }
    }

    fun setData(datas: List<DeviceWriteStateBean>) {
        list.clear()
        list.addAll(datas)
        notifyDataSetChanged()
    }

    fun writing(position: Int) {
        list[position].writeState = STATE_WRITING
        notifyItemChanged(position)
    }

    fun success(position: Int) {
        list[position].writeState = STATE_SUCCESS
        notifyItemChanged(position)
    }

    fun failed(position: Int) {
        list[position].writeState = STATE_FAILED
        notifyItemChanged(position)
    }

    fun complete() {
        list.sortByDescending { it.writeState }
        notifyDataSetChanged()
    }

    fun reset() {
        list.clear()
        notifyDataSetChanged()
    }

    fun hasFailed(): Boolean {
        return list.firstOrNull { it.writeState == STATE_FAILED } != null
    }

    fun getDatas() = list
}