package com.mxchip.mxapp.pair.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.bean.PairDeviceBean
import com.mxchip.mxapp.pair.consts.PairState
import com.mxchip.mxapp.pair.databinding.PairItPairingBinding

/**
 *
 */
class PairingAdapter : BaseAdapter<PairItPairingBinding>() {

    private var datas = mutableListOf<PairDeviceBean>()

    fun setData(list: List<PairDeviceBean>) {
        datas.clear()
        datas.addAll(list)
        notifyDataSetChanged()
    }

    fun getData(): List<PairDeviceBean> = datas

    private var mItemClick: ((Int) -> Unit)? = null

    fun setItemClick(itemClick: (Int) -> Unit) {
        mItemClick = itemClick
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun getViewBinding(parent: ViewGroup) =
        PairItPairingBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<PairItPairingBinding>, position: Int) {
        val pairBean = datas[position]
        holder.binding.icon.load(pairBean.icon)
        holder.binding.name.text = pairBean.name
        if (null == pairBean.pairState) {
            holder.binding.ivState.setImageDrawable(null)
            holder.itemView.setOnClickListener(null)
        } else {
            //设备item状态
            when (pairBean.pairState) {
                PairState.PAIR_STATE_SUCCESS -> {
                    holder.binding.ivState.clearAnimation()
                    holder.binding.ivState.setImageResource(R.drawable.pair_ic_successful)
                    holder.binding.tvFailed.visibility = View.GONE
                    holder.binding.ivFailed.visibility = View.GONE
                }

                PairState.PAIR_STATE_FAIL -> {
                    holder.binding.ivState.clearAnimation()
                    holder.binding.tvFailed.visibility = View.VISIBLE
                    holder.binding.ivFailed.visibility = View.VISIBLE
                    holder.binding.ivState.setImageResource(R.drawable.pair_ic_exclamation_point)
                    holder.itemView.setOnClickListener {
                        mItemClick?.invoke(position)
                    }
                }

                else -> {
                    holder.binding.tvFailed.visibility = View.GONE
                    holder.binding.ivFailed.visibility = View.GONE
                    //设置设备item动画
                    holder.binding.ivState.setImageResource(R.drawable.pair_ic_loading)
                    if (holder.binding.ivState.tag is Animation) {
                        holder.binding.ivState.startAnimation(holder.binding.ivState.tag as Animation)
                    } else {
                        val a =
                            AnimationUtils.loadAnimation(
                                holder.itemView.context,
                                R.anim.pair_rotation
                            )
                        holder.binding.ivState.animation = a
                        //缓存动画
                        holder.binding.ivState.tag = a
                    }
                }
            }
        }
    }
}