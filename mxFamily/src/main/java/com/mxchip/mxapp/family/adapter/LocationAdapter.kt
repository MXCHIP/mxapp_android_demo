package com.mxchip.mxapp.family.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mxchip.mxapp.family.bean.LocationBean
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.family.databinding.FamilyItLocationBinding

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/24
 * @desc:
 */
class LocationAdapter(private val context: Context) : BaseAdapter<FamilyItLocationBinding>() {
    private var currentLocationState = false

    private val mList = mutableListOf<LocationBean>()

    private var itemClickListener: ((LocationBean?) -> Unit)? = null

    override fun getItemCount(): Int = mList.size

    override fun getViewBinding(parent: ViewGroup) =
        FamilyItLocationBinding.inflate(LayoutInflater.from(context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<FamilyItLocationBinding>, position: Int) {
        val locationBean = mList[position]
        holder.binding.iconLocation.visibility =
            if (currentLocationState) View.VISIBLE else View.GONE
        holder.binding.tvStart.visibility = View.VISIBLE
        holder.binding.tvStart.text =
            "${locationBean.province_name} ${locationBean.city_name}"
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(locationBean)
        }
    }

    fun locationSuccess(locationBean: LocationBean) {
        currentLocationState = true
        mList.add(0, locationBean)
        notifyDataSetChanged()
    }

    fun searched(list: List<LocationBean>) {
        val location = if (currentLocationState) mList.firstOrNull() else null
        mList.clear()
        location?.apply { mList.add(location) }
        mList.addAll(list)
        notifyDataSetChanged()
    }

    fun setItemClickListener(listener: (LocationBean?) -> Unit) {
        itemClickListener = listener
    }
}