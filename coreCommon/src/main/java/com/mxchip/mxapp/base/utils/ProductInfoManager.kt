package com.mxchip.mxapp.base.utils

import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.bean.ProductInfo
import com.mxchip.sdk.mesh.MXMesh
import com.tencent.mmkv.MMKV

/**
 *产品Attr转码配置
 */
object ProductInfoManager {

    private val mmkv by lazy {
        MMKV.mmkvWithID("mx_product")
    }

    fun saveProductInfo(productKey: String, productInfo: String) {
        mmkv.encode(productKey, productInfo)
    }

    fun getProductInfo(productKey: String): String? = mmkv.decodeString(productKey)

    fun clearAll() {
        mmkv.clearAll()
        pidMmkv.clearAll()
    }

    private val pidMmkv by lazy {
        MMKV.mmkvWithID("mx_product_pid")
    }

    fun saveProductKeyPid(productKey: String, productId: String) {
        pidMmkv.encode("pid_$productId", productKey)
    }

    fun getProductPkByPid(productId: String): String? = pidMmkv.decodeString("pid_$productId")

    fun getProductByDevice(meshAddress: Int): ProductInfo? {
        val node = MXMesh.getNode(meshAddress)
        if (node == null) {
            MXLog.e("Product", "node is null, meshAddress: $meshAddress")
            return null
        }
        val uuid = node.uuid
        return getProductByDevice(uuid)
    }

    fun getProductByDevice(uuid: String): ProductInfo? {
        val fogPk = DeviceManage.getPkByUUID(uuid)
        //先检查pk（fog以pk为key）
        var pairInfoBean = getProductInfo(fogPk)
        if (pairInfoBean.isNullOrEmpty()) {
            val pid = DeviceManage.getFeiYanPidByUUID(uuid).toString()
            MXLog.d("Product", "find product pid: $pid")
            //pid换pk
            getProductPkByPid(pid)?.apply {
                pairInfoBean = getProductInfo(this)
            }
        }
        val product =
            JsonUtil.fromJson(pairInfoBean, ProductInfo::class.java)
        return product
    }
}