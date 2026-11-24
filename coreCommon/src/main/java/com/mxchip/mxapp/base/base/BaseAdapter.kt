package com.mxchip.mxapp.base.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/19
 * @desc:
 */
abstract class BaseAdapter<Binding: ViewBinding>: RecyclerView.Adapter<BaseViewHolder<Binding>>() {

    /**
     * 强制使用ViewBinding
     */
    abstract fun getViewBinding(parent: ViewGroup): Binding


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Binding> {
        return BaseViewHolder(getViewBinding(parent))
    }
}

class BaseViewHolder<Binding: ViewBinding>(val binding: Binding): RecyclerView.ViewHolder(binding.root)