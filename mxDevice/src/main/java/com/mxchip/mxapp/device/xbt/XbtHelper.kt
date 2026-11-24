package com.mxchip.mxapp.device.xbt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mxchip.link.pair.ble.BleConfigure
import com.mxchip.link.pair.ble.BleManager
import com.mxchip.link.pair.ble.BleState
import com.mxchip.link.utils.LinkByteUtil
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.device.xbt.transport.XbtTransportLayer
import com.mxchip.scanner.MxScan
import com.mxchip.scanner.bean.MxBluetoothDevice
import com.mxchip.scanner.callback.MxScanListener
import com.mxchip.scanner.callback.MxScanType
import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.utils.ByteUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("MissingPermission")
object XbtHelper {
    const val TAG = "XbtHelper"


    private val mMessageSequence = AtomicInteger(0)

    private var appRandom: ByteArray? = null
    private var nonce: ByteArray? = null
    private var mps = 0
    private var mtu = 0

    private val mxBleManager by lazy {
        BleManager(
            BleConfigure.Builder()
                .setServiceUUID(UUID.fromString("00000922-0000-1000-8000-00805F9B34FB"))
                .setWriteUUID(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
                .setNotifyUUID(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")).build()
        )
    }

    private lateinit var encryptKey: ByteArray
    private var isXbtSuccessful = false

    private var mXbtResult: ((Boolean) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRun by lazy {
        Runnable {
            if (isXbtSuccessful)
                return@Runnable
            MXLog.e(TAG, "xbt timeout")
            MxScan.stopScan()
            mxBleManager.disconnect()
            mXbtResult?.invoke(false)
        }
    }

    fun startXbt(
        context: Context,
        networkKey: String,
        deviceMac: String,
        content: String,
        xbtResult: (Boolean) -> Unit
    ) {
        mXbtResult = xbtResult
        encryptKey = LinkByteUtil.hexStringToBytes(networkKey)
        val arrayDeque = ArrayDeque<ByteArray>()
        mxBleManager.setDataCallback { resultData ->
            MXLog.d(TAG, "received data: ${LinkByteUtil.bytesToHexString(resultData.second)}")
            handler.removeCallbacks(timeoutRun)
            deCodeData(resultData.second, encryptKey) { receiverState ->
                when (receiverState) {
                    0 -> { //xbt业务认证成功，开始发送设备数据
                        MXLog.i(TAG, "xbt business authentication successful")
                        fileXbtContentData(content)
                        sendXbtData(arrayDeque.first())
                    }

                    1 -> {
                        handler.removeCallbacks(timeoutRun)
                        arrayDeque.removeFirstOrNull() //移除已发送的数据
                        val first = arrayDeque.firstOrNull()
                        if (first != null) {
                            sendXbtData(first)
                        } else {
                            MXLog.i(TAG, "all data sent, sending gateway restart command")
                            val payload =
                                LinkByteUtil.hexStringToBytes("0200D0070000")//延迟2000ms,0x07D0反转字节
                            sendXbtData(payload)
                            isXbtSuccessful = true
                            mXbtResult?.invoke(true)
                        }
                    }

                    else -> {
                        MXLog.e(TAG, "XBT failed with receiver state: $receiverState")
                        mxBleManager.disconnect()
                        mXbtResult?.invoke(false)
                    }
                }
            }
        }
        var isMatch = false
        MXLog.i(TAG, "xbt gateway mac: $deviceMac")
        MxScan.addScanDataListener(object : MxScanListener {
            override fun onScanResultReceived(mxBluetoothDevice: MxBluetoothDevice) {
                if (mxBluetoothDevice.mxScanType == MxScanType.MX && isMatch.not()) {
                    val address = mxBluetoothDevice.getAddress()
                    isMatch = address.equals(deviceMac, false)
                    if (isMatch) {
                        MxScan.stopScan()
                        handler.removeCallbacks(timeoutRun)
                        mxBleManager.connect(
                            context.applicationContext,
                            mxBluetoothDevice.getDevice()
                        ) { bleState ->
                            MXLog.i(TAG, "bleState: $bleState")
                            when (bleState) {
                                BleState.STATE_SUCCESS -> {
                                    // 认证超时设置为10秒
                                    handler.postDelayed(timeoutRun, 10_000)
                                    sendStartData()
                                }

                                BleState.STATE_DISCONNECTED, BleState.STATE_FAILED -> {
                                    handler.removeCallbacks(timeoutRun)
                                    if (!isXbtSuccessful)
                                        mXbtResult?.invoke(false)
                                }
                            }
                        }
                    }
                }
            }
        })
        // 扫描超时设置为30秒，与网关广播时间一致
        handler.postDelayed(timeoutRun, 30_000)
        MXLog.i(TAG, "starting device scan with 30s timeout")
        MxScan.startScan(listOf(MxScan.SERVICE_MX))
    }

    private fun getSequenceNumber(): Int {
        val sequence = mMessageSequence.getAndIncrement()
        return if (sequence >= 0xFFFF) {
            mMessageSequence.set(0)
            0
        } else
            sequence
    }

    private fun sendStartData() {
        isXbtSuccessful = false
        nonce = null//重置
        mMessageSequence.set(0)
        val startByte = mutableListOf<Byte>()
        startByte.add(0x00) //Type << 2 | SAR
        startByte.add(0x00)//Version
        startByte.add(0x00)//Share Secrect Type

        // ATT OpCode	UINT8	0x52 = ATT_WRITE_CMD
        // ATT Handle	UINT16	目标 Characteristic 的 handle
        mtu = mxBleManager.getMaximumPacketSize() - 3 - 1
        // MPS字段：2字节，小端序
        startByte.add((mtu and 0xFF).toByte())        // 低字节
        startByte.add(((mtu shr 8) and 0xFF).toByte()) // 高字节
        appRandom = XbtEncrypt.generateRandomNumber(4)
        startByte.addAll(appRandom!!.toList())
        val startData = startByte.toByteArray()
        mxBleManager.sendData(startData)
    }

    private fun fileXbtContentData(content: String): List<ByteArray> {
        val mApplicationByteArrays = mutableListOf<ByteArray>()
        val crc = XbtEncrypt.doCRC16_XMODEM(content.toByteArray())
        val crcHex = MeshSDKManage.stringReversedToLittleEndian(String.format("%04X", crc))
        //xbt传输 文件名固定 "home_model.json"
        // 格式：2000 + 文件长度(4字节) + 文件类型(4字节) + CRC(2字节) + 文件名 + 00
        val fileInfo = "2000" + MeshSDKManage.stringReversedToLittleEndian(
            String.format(
                "%08X", content.length
            )
        ) + "00000000" + crcHex + LinkByteUtil.bytesToHexString("home_model.json".toByteArray()) + "00"
        MXLog.i(TAG, "file info")
        val fileInfoData = enCodeData(LinkByteUtil.hexStringToBytes(fileInfo))
        mApplicationByteArrays.add(fileInfoData)

        // 文件数据分包处理
        MXLog.i(TAG, "file content: $content")
        val contextByteArray = content.toByteArray()
        //mps -opcode(2) -offset(4)
        val contentSegSize = mps - 2 - 4
        val numberOfSegments = (contextByteArray.size + contentSegSize - 1) / contentSegSize
        MXLog.i(
            TAG,
            "file size: ${contextByteArray.size}, contentSegSize: $contentSegSize, numberOfSegments: $numberOfSegments"
        )

        // 按contentSegSize分包处理文件数据
        for (segmentIndex in 0 until numberOfSegments) {
            val startOffset = segmentIndex * contentSegSize
            val endOffset = minOf(startOffset + contentSegSize, contextByteArray.size)
            val segmentData = contextByteArray.copyOfRange(startOffset, endOffset)
            val fileOffset = startOffset

            MXLog.d(
                TAG,
                "creating segment $segmentIndex: offset=$fileOffset, size=${segmentData.size}"
            )

            val fileDataPayload =
                ByteBuffer.allocate(6 + segmentData.size).order(ByteOrder.BIG_ENDIAN)
            fileDataPayload.put(byteArrayOf(0x21, 0x00))  // OpCode: 0x0021 (FTP文件数据) - 大端序
            fileDataPayload.put(MeshParserUtils.intToBytes(fileOffset))  // File Offset - 大端序
            fileDataPayload.put(segmentData)

            val fileData = enCodeData(fileDataPayload.array())
            mApplicationByteArrays.add(fileData)
        }

        //● 2200: 0x0022, FTP file Valid FTP 文件验证
        MXLog.i(TAG, "file valid")
        val fileValidData = enCodeData(LinkByteUtil.hexStringToBytes("2200$crcHex"))
        mApplicationByteArrays.add(fileValidData)
        return mApplicationByteArrays
    }

    private fun enCodeData(payload: ByteArray): ByteArray {
        val sequenceNumber = getSequenceNumber()
        //确保sequence number正确格式化为2字节小端序
        val sequenceByteArray = ByteArray(2)
        sequenceByteArray[0] = (sequenceNumber and 0xFF).toByte()
        sequenceByteArray[1] = ((sequenceNumber shr 8) and 0xFF).toByte()

        MXLog.d(
            TAG,
            "sequence: $sequenceNumber, payload: ${LinkByteUtil.bytesToHexString(payload)}"
        )

        val ccmByteArray = XbtEncrypt.encryptCCM(payload, encryptKey, nonce!!, sequenceByteArray, 4)
        val mic = XbtEncrypt.getMic(ccmByteArray)
        val encryptedData = XbtEncrypt.getEncryptData(ccmByteArray)
        // 构建完整的加密PDU：Sequence(2) + MIC(4) + EncryptedData
        val encryptPDUSize = 2 + 4 + encryptedData.size
        val encryptPDU = ByteArray(encryptPDUSize)
        System.arraycopy(sequenceByteArray, 0, encryptPDU, 0, 2)
        System.arraycopy(mic, 0, encryptPDU, 2, 4)  // MIC
        System.arraycopy(encryptedData, 0, encryptPDU, 6, encryptedData.size)  // 加密数据
        MXLog.d(TAG, "encrypted PDU: ${LinkByteUtil.bytesToHexString(encryptPDU)}")
        return encryptPDU
    }

    fun deCodeData(dtBytes: ByteArray, encryptKey: ByteArray, dateResult: (Int) -> Unit) {
        MXLog.d(TAG, "deCodeData: received ${dtBytes.size} bytes")
        if (dtBytes.size == 9 && 0x00.toByte() == dtBytes[0]) {
            //设备回复的 start data
            MXLog.d(TAG, "processing start response: ${LinkByteUtil.bytesToHexString(dtBytes)}")
            val random = ByteArray(4)
            System.arraycopy(dtBytes, 5, random, 0, random.size)
            mps = MeshParserUtils.unsignedBytesToInt(dtBytes[3], dtBytes[4])
            MXLog.d(TAG, "MPS: $mps")
            //104-bit nonce，0x00 || Random(APP) || Random(Device) || 0x00000000
            val nonceHex =
                "00" + LinkByteUtil.bytesToHexString(appRandom!!) + LinkByteUtil.bytesToHexString(
                    random
                ) + "00000000"
            nonce = LinkByteUtil.hexStringToBytes(nonceHex)
            MXLog.d(TAG, "encryption key: " + LinkByteUtil.bytesToHexString(encryptKey))
            MXLog.d(TAG, "nonce: " + LinkByteUtil.bytesToHexString(nonce!!))
            //认证成功，回复上层连接可用，可进行应用层数据交互
            dateResult.invoke(0)
        } else {
            // 处理application响应
            MXLog.d(
                TAG,
                "processing application response: ${LinkByteUtil.bytesToHexString(dtBytes)}"
            )

            if (dtBytes.size < 7) {
                MXLog.e(TAG, "application response too short: ${dtBytes.size}")
                dateResult.invoke(-1)
                return
            }

            // 检查是否是加密的application响应
            val headerByte = dtBytes[0]
            val type = (headerByte.toInt() shr 2) and 0x03
            val sar = headerByte.toInt() and 0x03

            MXLog.d(TAG, "response header: type=$type, sar=$sar")

            if (type == 1) { // Application message
                val sequence = byteArrayOf(dtBytes[1], dtBytes[2])
                val encryptBuffer =
                    ByteBuffer.allocate(dtBytes.size - 3).order(ByteOrder.LITTLE_ENDIAN)
                val micData = dtBytes.copyOfRange(3, 7)
                MXLog.d(TAG, "data receive mic: ${LinkByteUtil.bytesToHexString(micData)}")
                val encryptedPDU = dtBytes.copyOfRange(7, dtBytes.size)
                encryptBuffer.put(encryptedPDU)
                encryptBuffer.put(micData)

                try {
                    val dataBytes = XbtEncrypt.decryptCCM(
                        encryptBuffer.array(), encryptKey,
                        nonce!!,
                        sequence,
                        4
                    )
                    MXLog.d(TAG, "decrypted data=>${ByteUtil.bytesToHexString(dataBytes)}")

                    // 检查数据长度是否足够
                    if (dataBytes.size < 4) {
                        MXLog.e(TAG, "decrypted data too short: ${dataBytes.size}")
                        dateResult.invoke(-1)
                        return
                    }

                    // 状态码应该是最后4字节
                    val stateBytes = dataBytes.copyOfRange(dataBytes.size - 4, dataBytes.size)
                    val state = LinkByteUtil.bytesToInt(stateBytes)
                    MXLog.d(TAG, "state=>$state (0x${String.format("%08X", state)})")

                    if (state == 0) {
                        dateResult.invoke(1)
                    } else {
                        MXLog.e(
                            TAG,
                            "device returned error state: $state (0x${
                                String.format(
                                    "%08X",
                                    state
                                )
                            })"
                        )
                        dateResult.invoke(-1)
                    }
                } catch (e: Exception) {
                    MXLog.e(TAG, "decryption failed", e)
                    dateResult.invoke(-1)
                }
            } else {
                MXLog.e(TAG, "unexpected message type: $type")
                dateResult.invoke(-1)
            }
        }
    }

    private fun sendXbtData(data: ByteArray) {
        if (data.size > mtu) {
            val segArray = XbtTransportLayer.createSegmentedAppData(mtu, data)
            MXLog.d(TAG, "sending segmented data with ${segArray.size} segments")
            segArray.forEachIndexed { index, segment ->
                mxBleManager.sendData(segment)
            }
            // 继续传输数据，超时设置为
            val delayTime = segArray.size * 3000L
            handler.postDelayed(timeoutRun, delayTime)
        } else {
            val unsegmentedPDU = XbtTransportLayer.createUnsegmentedAppData(data)
            mxBleManager.sendData(unsegmentedPDU)
            // 数据传输超时设置为5秒
            handler.postDelayed(timeoutRun, 3000)
        }
    }
}