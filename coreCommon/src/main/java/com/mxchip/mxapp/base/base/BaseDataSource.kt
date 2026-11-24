package com.mxchip.mxapp.base.base

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mxchip.http.response.NetState
import com.mxchip.http.response.NetStateResponse

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 5/11/21
 * @desc:
 */
abstract class BaseDataSource<Key : Any, Bean : Any>(private val dataSource: suspend (Key) -> NetStateResponse) :
    PagingSource<Key, Bean>() {

    /**
     * 刷新页面时的index
     */
    open fun getRefreshKey(): Key = startKey()

    /**
     * 第一页索引值
     */
    abstract fun startKey(): Key

    /**
     * 当前页的上一页索引值
     */
    abstract fun prev(position: Key): Key?

    /**
     * 当前页的下一页索引值
     */
    abstract fun next(data: List<Bean>?, position: Key): Key?

    final override fun getRefreshKey(state: PagingState<Key, Bean>) = getRefreshKey()

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Bean> {
        val pos = params.key ?: startKey()
        Log.i("MXLog", "BaseDataSource: ------> position: $pos")
        return try {
            val result = dataSource.invoke(pos)
            if (result.state == NetState.SUCCESS) {
                val data = dataConverter(result.data)
                LoadResult.Page(
                    data,
                    prev(pos),
                    next(data, pos)
                )
            } else {
                LoadResult.Error(Throwable("${result.state.code}, ${result.state.message}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    abstract fun dataConverter(data: String?): List<Bean>
}