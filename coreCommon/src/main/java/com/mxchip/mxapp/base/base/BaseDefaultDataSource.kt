package com.mxchip.mxapp.base.base

import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.utils.JsonUtil
import org.json.JSONObject

/**
 * 分页索引默认为Int类型时可直接使用
 */
class BaseDefaultDataSource<Bean : Any>(private val beanClass: Class<Bean>, dataSource: suspend (Int) -> NetStateResponse) :
    BaseDataSource<Int, Bean>(dataSource) {

    override fun startKey() = 1

    override fun prev(position: Int) = if (position <= startKey()) null else position - 1

    override fun next(data: List<Bean>?, position: Int) =
        if (data.isNullOrEmpty()) null else position + 1


    override fun dataConverter(data: String?): List<Bean> {
        return if (data.isNullOrEmpty())
            listOf()
        else{
            val listJson = JSONObject(data).getJSONArray("list").toString()
            JsonUtil.fromJsonToArray(listJson,beanClass) ?: listOf()
        }
    }
}