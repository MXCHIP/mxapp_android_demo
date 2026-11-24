package com.mxchip.mxapp.pair.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.databinding.PairItWifiRecordBinding


/**
 *Wi-Fi历史列表弹窗
 */
class WiFiRecordAdapter(context: Context, val datas: Array<String>) :
    ArrayAdapter<String>(context, R.layout.pair_it_wifi_record, datas) {

    private var itemClick: ((String) -> Unit)? = null
    fun setItemClick(click: (String) -> Unit) = apply {
        itemClick = click
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = PairItWifiRecordBinding.inflate(LayoutInflater.from(context), parent, false)
        val text = datas[position]
        binding.root.text = text
        binding.root.setOnClickListener {
            itemClick?.invoke(text)
        }
        return binding.root
    }
}