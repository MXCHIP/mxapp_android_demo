package com.mxchip.mxapp.base.base

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 5/11/21
 * @desc:
 */

/**
 * @param itemSame 判断两个实例是不是同一个实例，一般比较两个实例的id
 */
abstract class BasePagingAdapter<Binding: ViewBinding, Bean: Any>(itemSame: (old: Bean, new: Bean) -> Boolean) : PagingDataAdapter<Bean, BaseViewHolder<Binding>>(
    BaseDiffUtil.createDiffItemCallback(itemSame)
) {

    private var listAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>? = null

    /**
     * 强制使用ViewBinding
     */
    abstract fun getViewBinding(parent: ViewGroup): Binding


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Binding> {
        return BaseViewHolder(getViewBinding(parent))
    }

    fun addFooter(footer: RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
        listAdapter = ConcatAdapter(this, footer)
    }

    fun getListAdapter(): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
        return listAdapter ?: this
    }
}

object BaseDiffUtil {
    fun <Bean : Any> createDiffItemCallback(itemSame: (old: Bean, new: Bean) -> Boolean): DiffUtil.ItemCallback<Bean> {
        return object : DiffUtil.ItemCallback<Bean>() {
            override fun areItemsTheSame(oldItem: Bean, newItem: Bean): Boolean {
                return itemSame.invoke(oldItem, newItem)
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Bean, newItem: Bean): Boolean {
                return oldItem == newItem
            }
        }
    }
}

