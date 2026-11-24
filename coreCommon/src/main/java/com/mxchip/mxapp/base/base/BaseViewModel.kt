package com.mxchip.mxapp.base.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.mxchip.http.response.NetStateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 5/8/21
 * @desc:
 */
open class BaseViewModel : ViewModel() {

    /**
     * UI线程
     */
    fun launchMain(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch { block() }

    /**
     * IO线程
     */
    suspend fun <T> launchIO(block: suspend CoroutineScope.() -> T) {
        withContext(Dispatchers.IO) {
            block()
        }
    }

    /**
     * 事件流形式
     * 类似rxjava
     * 可使用操作符，map、flatmap、onEach、catch等扩展功能需求
     */
    fun launchFlow(dataSource: suspend () -> NetStateResponse): Flow<NetStateResponse> {
        return flow {
            emit(dataSource())
        }
    }

//    /**
//     * 事件流形式
//     * 增加了生命周期
//     */
//    fun <T> launchFlow(lifecycleOwner: LifecycleOwner, minActiveState: Lifecycle.State, dataSource: suspend () -> NetStateResponse<T>, action: ((NetStateResponse<T>) -> Unit)?) {
//        val response = flow {
//            emit(NetStateResponse.loading())
//            emit(dataSource())
//        }
//        lifecycleOwner.lifecycleScope.launch {
//            lifecycleOwner.repeatOnLifecycle(minActiveState) {
//                response.collect {
//                    action?.invoke(it)
//                }
//            }
//        }
//    }

    /**
     * 事件流形式
     * 类似rxjava.just方法
     * 可发射多个事件
     * 可使用操作符，map、flatmap、onEach、catch等扩展功能需求
     */
    fun <T> launchFlow(vararg data: T): Flow<T> {
        return flow {
            data.forEach {
                emit(it)
            }
        }
    }

    /**
     * 事件流形式
     * 帮助回调了collect方法，因此不能使用其他操作符扩展功能
     */
    suspend fun launchFlowUnit(
        data: MutableLiveData<NetStateResponse>,
        dataSource: suspend () -> NetStateResponse
    ) {
        val ret = flow {
            emit(dataSource())
        }
        ret.collect {
            data.postValue(it)
        }
    }

    suspend fun launchWithLiveDate(
        data: MutableLiveData<NetStateResponse>,
        dataSource: suspend () -> NetStateResponse
    ) {
        val ret = dataSource()
        data.postValue(ret)
    }

    /**
     * liveData结合flow
     */
    fun launchLiveData(dataSource: suspend () -> NetStateResponse): LiveData<NetStateResponse> {
        return liveData {
            emit(dataSource())
        }
    }

    /**
     * 需要自定义pagingSourceFactory，继承BaseDataSource
     */
    fun <Key : Any, Bean : Any> paging(
        pageSize: Int = 20,
        prefetchRatio: Float = 0.25f,
        pagingSourceFactory: () -> PagingSource<Key, Bean>
    ): Flow<PagingData<Bean>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize,
                prefetchDistance = (pageSize * prefetchRatio).toInt()
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.cachedIn(viewModelScope)
    }
}