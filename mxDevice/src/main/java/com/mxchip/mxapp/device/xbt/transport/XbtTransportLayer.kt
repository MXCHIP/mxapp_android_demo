package com.mxchip.mxapp.device.xbt.transport

import com.mxchip.link.utils.LinkByteUtil
import com.mxchip.log.MXLog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object XbtTransportLayer {
    private const val TAG = "XbtTransportLayer"
    private const val MESSAGE_HEADER_LENGTH = 1

    fun createUnsegmentedAppData(encryptedPDU: ByteArray): ByteArray {
        val lowerTransportBuffer =
            ByteBuffer.allocate(MESSAGE_HEADER_LENGTH + encryptedPDU.size)
                .order(ByteOrder.BIG_ENDIAN)
        // Type=1 (Application), SAR=0 (完整消息)
        //Type << 2 | SAR [0 Data 是一个完整的消息,1 Data 首个分片,2 Data 剩余分片,3 Data 最后分片]
        val headerByte = (1 shl 2).toByte()
        lowerTransportBuffer.put(headerByte)
        lowerTransportBuffer.put(encryptedPDU, 0, encryptedPDU.size)
//        MXLog.d(TAG, "Type=1,SAR=0, header=0x${String.format("%02X", headerByte.toInt() and 0xFF)}")
        MXLog.d(
            TAG,
            "unsegmented Lower transport PDU: " + LinkByteUtil.bytesToHexString(lowerTransportBuffer.array())
        )
        return lowerTransportBuffer.array()
    }

    fun createSegmentedAppData(
        mtu: Int,
        encryptedPDU: ByteArray
    ): List<ByteArray> {
        val numberOfSegments = (encryptedPDU.size + mtu - 1) / mtu
        MXLog.d(
            TAG,
            "creating segments: PDU size=${encryptedPDU.size}, mtu=$mtu, segments=$numberOfSegments"
        )

        val lowerTransportPduList = mutableListOf<ByteArray>()
        var offset = 0
        var length: Int
        for (segO in 0 until numberOfSegments) {
            length = min(encryptedPDU.size - offset, mtu)
            val lowerTransportBuffer =
                ByteBuffer.allocate(MESSAGE_HEADER_LENGTH + length).order(ByteOrder.BIG_ENDIAN)
            val sar = when (segO) {
                0 -> 1  // 首个分片
                numberOfSegments - 1 -> 3  // 最后分片
                else -> 2  // 剩余分片
            }
            //Type << 2 | SAR [0 Data 是一个完整的消息,1 Data 首个分片,2 Data 剩余分片,3 Data 最后分片]
            // Type=1 (Application), SAR=0 (完整消息)
            val headerByte = (1 shl 2 or sar).toByte()
            lowerTransportBuffer.put(headerByte)
            lowerTransportBuffer.put(encryptedPDU, offset, length)
            offset += length
            val lowerTransportPDU = lowerTransportBuffer.array()
//            MXLog.d(TAG, "Type=1,SAR=$sar, header=0x${String.format("%02X", headerByte.toInt() and 0xFF)}")
            MXLog.d(
                TAG,
                "Segmented Lower transport PDU: " + LinkByteUtil.bytesToHexString(lowerTransportPDU) + " " + (segO + 1) + " of " + numberOfSegments
            )
            lowerTransportPduList.add(lowerTransportPDU)
        }
        return lowerTransportPduList
    }
}