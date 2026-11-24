package com.mxchip.mxapp.pair.ui.dialog

import android.content.Context
import android.view.ViewGroup
import android.widget.ListPopupWindow
import androidx.core.content.ContextCompat


/**
 *Wi-Fi历史列表弹窗
 */
class WiFiRecordPopupWindow(context: Context) : ListPopupWindow(context) {

    init {
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                com.mxchip.mxapp.base.R.drawable.core_bg_solid_white_r100
            )
        )
//        setOnItemClickListener { _, _, position, _ ->
//
//        }
//        val records = listOf("mxchip", "mxchip-guest")
//        setAdapter(ArrayAdapter(context, R.layout.item_wifi_record, records))
    }
}