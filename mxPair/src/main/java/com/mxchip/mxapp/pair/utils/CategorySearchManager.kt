package com.mxchip.mxapp.pair.utils

import com.tencent.mmkv.MMKV

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/3/3
 * @desc:
 */
object CategorySearchManager {

    private const val SEARCH_RECORD = "category.search.record"

    private val mmkv by lazy {
        MMKV.mmkvWithID("pair_search")
    }

    fun saveProductKeyRecord(productKey: String) {
        val record = mmkv.decodeStringSet(SEARCH_RECORD)
        if (null == record) {
            mmkv.encode(SEARCH_RECORD, mutableSetOf(productKey))
        } else {
            record.add(productKey)
            mmkv.encode(SEARCH_RECORD, record)
        }
    }

    fun getProductKeyRecord(): Set<String> = mmkv.decodeStringSet(SEARCH_RECORD, setOf())!!

    fun clearAll() {
        mmkv.clearAll()
    }
}