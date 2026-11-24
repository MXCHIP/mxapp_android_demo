package com.mxchip.mxapp.account.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.account.bean.CountryBean
import com.mxchip.mxapp.account.databinding.AccountItRegionCodeBinding
import com.mxchip.mxapp.base.utils.LanguageUtil

/**
 * 国家选择适配器
 */
class RegionAdapter : BaseAdapter<AccountItRegionCodeBinding>() {

    private val countryList by lazy { mutableListOf<CountryBean>() }

    fun setData(datas: List<CountryBean>?) {
        countryList.clear()
        datas?.apply {
            countryList.addAll(this)
        }
        notifyDataSetChanged()
    }

    private var itemClick: ((position: Int, CountryBean) -> Unit)? = null

    fun setItemClickListener(itemClick: (position: Int, CountryBean) -> Unit) {
        this.itemClick = itemClick
    }

    override fun getItemCount(): Int {
        return countryList.size
    }

    override fun getViewBinding(parent: ViewGroup) =
        AccountItRegionCodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<AccountItRegionCodeBinding>, position: Int) {
        val data = countryList[position]
        holder.binding.regionName.text = if (LanguageUtil.isZh()) data.zh_name else data.en_name
        holder.binding.regionName.setOnClickListener {
            itemClick?.invoke(position, data)
        }
    }

    fun getList() = countryList
}