package com.mxchip.mxapp.bean

import com.mxchip.mxapp.base.bean.ItemsBean
import com.mxchip.mxapp.base.bean.PropertyBean

data class RecommendSceneBean(
    val actions: List<Action>?,
    val description: String,
    val device_range: Int,
    val enable: Boolean,
    val icon_color: String,
    val icon_image: String,
    val name: String,
    val scene_id: Int,
    val type: String
)


data class Action(
    val categorys: List<Category>?,
    val icon_image: String?,
    val name: String?,
    val products: List<Product>?,
    val properties: List<PropertyBean>
)  {

    @Transient
    var itemsBean: List<ItemsBean>? = null
}


data class Category(
    val category_id: Int,
    val name: String
) 


data class Product(
    val name: String,
    val product_key: String
) 