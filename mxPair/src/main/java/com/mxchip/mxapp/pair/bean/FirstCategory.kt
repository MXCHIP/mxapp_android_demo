package com.mxchip.mxapp.pair.bean

import com.mxchip.mxapp.base.bean.ProductInfo

/**
 *第一大类
 */
data class FirstCategory(
    val category_id: Int,
    val name: String,
    val categorys: List<SecondCategory>,
    var isSelected: Boolean = false//ui字段
) 

/**
 *第二类
 */
data class SecondCategory(
    val category_id: Int,
    val name: String,
    val products: List<ProductInfo>
) 
