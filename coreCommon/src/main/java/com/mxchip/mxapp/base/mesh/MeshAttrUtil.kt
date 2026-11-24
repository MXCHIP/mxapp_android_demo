package com.mxchip.mxapp.base.mesh

import com.mxchip.log.MXLog
import com.mxchip.mesh.rule.utils.MeshTranscodeUtil
import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import org.json.JSONArray
import org.json.JSONObject
import kotlin.experimental.and

object MeshAttrUtil {
    private const val TAG = "MeshAttrUtil"
    private val attrConfMap by lazy { HashMap<String, Int>() }

    fun parseFillAttrMap(attrJson: JSONObject) {
        attrConfMap[MeshSDKManage.ATTR_TYPE_WIFI_LINK_STATUS] =
            MeshSDKManage.ATTR_TYPE_WIFI_LINK_STATUS_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_GET_VERSION] =
            MeshSDKManage.ATTR_TYPE_GET_VERSION_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_GROUP_ADD] = MeshSDKManage.ATTR_TYPE_WRITE_RESULT_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_GROUP_DEL] = MeshSDKManage.ATTR_TYPE_WRITE_RESULT_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_GROUP_RESET] =
            MeshSDKManage.ATTR_TYPE_WRITE_RESULT_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_RULE] = MeshSDKManage.ATTR_TYPE_WRITE_RESULT_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_RULE_NEW] = MeshSDKManage.ATTR_TYPE_WRITE_RESULT_LENGTH
        attrConfMap[MeshSDKManage.ATTR_TYPE_CEVT_CONFIG] =
            MeshSDKManage.ATTR_TYPE_WRITE_RESULT_LENGTH

        val keyIterator = attrJson.keys()
        //解析attrType 和value 字节长度
        while (keyIterator.hasNext()) {
            val attrType = keyIterator.next()
            when (val attrContent = attrJson[attrType]) {
                is JSONObject -> {
                    val valueType = attrContent.optString("value_type")
                    if (valueType == "array") {
                        attrConfMap[attrType] = -1
                    } else if (attrContent.has("value_length"))
                        attrConfMap[attrType] = attrContent.getInt("value_length")
                }

                is JSONArray -> {
                    val child = attrContent.getJSONObject(0)
                    if (child.has("value_length"))
                        attrConfMap[attrType] = child.getInt("value_length")
                }
            }
        }
    }

    /**
     * 从mesh转码配置中解析字节长度
     * @return Pair<字节配置map,未配置Pair<attr/hex>>
     */
    fun parseData(dataByteArray: ByteArray): Map<String, ByteArray> {
        return parseMeshDataByAttrMap(attrConfMap, dataByteArray)
    }

    /**
     * 通过产品独立的mesh转码配置解析字节长度
     * @return Pair<字节配置map,未配置Pair<attr/hex>>
     */
    fun parseDataByProductAttr(
        attrJson: JSONObject,
        dataByteArray: ByteArray,
    ): Map<String, ByteArray> {
        //4 表示attrType的长度，例："0108"
        val attrMap = mutableMapOf<String, Int>()
        val keyIterator = attrJson.keys()
        //解析attrType 和value 字节长度
        while (keyIterator.hasNext()) {
            val attrType = keyIterator.next()
            when (val attrContent = attrJson[attrType]) {
                is JSONObject -> {
                    val valueType = attrContent.optString("value_type")
                    if (valueType == "array") {
                        attrMap[attrType] = -1
                    } else if (attrContent.has("value_length"))
                        attrMap[attrType] = attrContent.getInt("value_length")
                }

                is JSONArray -> {
                    val child = attrContent.getJSONObject(0)
                    if (child.has("value_length"))
                        attrMap[attrType] = child.getInt("value_length")
                }
            }
        }
        return parseMeshDataByAttrMap(attrMap, dataByteArray)
    }

    //attr type占两个字节
    private const val ATTR_LEN = 2

    private fun parseMeshDataByAttrMap(
        attrMap: Map<String, Int>,
        dataByteArray: ByteArray
    ): Map<String, ByteArray> {
        val resultMap = mutableMapOf<String, ByteArray>()
        var i = 0
        while (i + 1 < dataByteArray.size) {
            //attrType 2字节，协议文档和实际传输，字节反转
            val at = attrTypeDecode(dataByteArray[i + 1], dataByteArray[i])
            var valueLength = attrMap[at]
            if (valueLength == -1) { //-1不定长，解析array等
                valueLength = dataByteArray.size - ATTR_LEN
            } else if (valueLength == null) {
                val atPair = MeshTranscodeUtil.getLowServiceAttrTypePair(at)
                val newLength = attrMap[atPair.first]
                if (newLength == null) {
                    MXLog.w(TAG, "attrType: $at, value length is null")
                    break
                } else {
                    valueLength = newLength
                }
            }
            val valueByteArray = ByteArray(valueLength)
            System.arraycopy(
                dataByteArray,
                ATTR_LEN + i,
                valueByteArray,
                0,
                valueByteArray.size
            )
            MXLog.d(TAG, "attrType: $at, value: ${ByteUtil.bytesToHexString(valueByteArray)}")
            resultMap[at] = valueByteArray
            i += ATTR_LEN + valueLength
        }
        return resultMap
    }

    private fun attrTypeDecode(byte1: Byte, byte2: Byte): String {
        //将byte1 高位置0
        val byte = (1 shl 7).inv().let {
            byte1 and it.toByte()
        }
        return MeshParserUtils.bytesToHex(byteArrayOf(byte, byte2), false)
    }
}