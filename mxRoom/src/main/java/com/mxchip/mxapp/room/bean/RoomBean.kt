package com.mxchip.mxapp.room.bean

import com.mxchip.mxapp.base.bean.RoomBean

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/1
 * @desc:
 */
data class RoomListBean(
    val list: List<RoomBean>,
    val page: Page
)

data class Page(
    val page_no: Int,
    val page_size: Int,
    val total: Int
)