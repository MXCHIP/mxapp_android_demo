package com.mxchip.mxapp.device.xbt

import com.mxchip.link.utils.LinkByteUtil
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.modes.CCMBlockCipher
import org.spongycastle.crypto.params.AEADParameters
import org.spongycastle.crypto.params.KeyParameter
import java.security.SecureRandom


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2023/3/7
 * @desc:
 */
object XbtEncrypt {
    private const val TAG = "XbtEncrypt"
    private const val TAG_SIZE = 4

    fun generateRandomNumber(byteSize: Int): ByteArray {
        val random = SecureRandom()
        val randomBytes = ByteArray(byteSize)
        random.nextBytes(randomBytes)
        return randomBytes
    }

    fun encryptCCM(
        data: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        additionalData: ByteArray,
        micSize: Int
    ): ByteArray {
        val ccm = ByteArray(data.size + micSize)
        val ccmBlockCipher = CCMBlockCipher(AESEngine())
        val aeadParameters = AEADParameters(KeyParameter(key), micSize * 8, nonce, additionalData)
        ccmBlockCipher.init(true, aeadParameters)
        ccmBlockCipher.processBytes(data, 0, data.size, ccm, data.size)
        ccmBlockCipher.doFinal(ccm, 0)
        return ccm
//        val encryptData = ccm.copyOfRange(0, ccm.size - TAG_SIZE)
//        val micData = ccm.copyOfRange(ccm.size - TAG_SIZE, ccm.size)
//        return Pair(encryptData, micData)
    }

    fun getMic(ccmByteArray: ByteArray): ByteArray {
        return ccmByteArray.copyOfRange(ccmByteArray.size - TAG_SIZE, ccmByteArray.size)
    }

    fun getEncryptData(ccmByteArray: ByteArray): ByteArray {
        return ccmByteArray.copyOfRange(0, ccmByteArray.size - TAG_SIZE)
    }


    fun decryptCCM(
        data: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        additionalData: ByteArray,
        micSize: Int
    ): ByteArray {
        val ccm = ByteArray(data.size - micSize)
        val ccmBlockCipher = CCMBlockCipher(AESEngine())
        val aeadParameters = AEADParameters(KeyParameter(key), micSize * 8, nonce, additionalData)
        ccmBlockCipher.init(false, aeadParameters)
        ccmBlockCipher.processBytes(data, 0, data.size, ccm, 0)
        ccmBlockCipher.doFinal(ccm, 0)
        return ccm
    }

    fun doCRC16_XMODEM(buffer: ByteArray): Int {
        var wCRCin = 0x0000 // initial value 65535
        val wCPoly = 0x1021 // 0001 0000 0010 0001 (0, 5, 12)
        for (b in buffer) {
            for (i in 0..7) {
                val bit = b.toInt() shr 7 - i and 1 == 1
                val c15 = wCRCin shr 15 and 1 == 1
                wCRCin = wCRCin shl 1
                if (c15 xor bit) wCRCin = wCRCin xor wCPoly
            }
        }
        wCRCin = wCRCin and 0xffff
        return 0x0000.let { wCRCin = wCRCin xor it; wCRCin }
    }
}

fun main() {
    val KEY = "69696969696969696969696969696969"
    val NONCE = "00DEADBEEF6969696900000000"
    val data =
        "210000000000313233343536373839"
    val ccmByteArray = XbtEncrypt.encryptCCM(
        LinkByteUtil.hexStringToBytes(data),
        LinkByteUtil.hexStringToBytes(KEY),
        LinkByteUtil.hexStringToBytes(NONCE),
        byteArrayOf(0x01,0x00),
        4
    )
    val encryptDate = XbtEncrypt.getEncryptData(ccmByteArray)
    val mic = XbtEncrypt.getMic(ccmByteArray)
    println(
        "encryptData: ${LinkByteUtil.bytesToHexString(encryptDate)} \n mic: ${
            LinkByteUtil.bytesToHexString(mic)
        }"
    )
    println("hex=>${LinkByteUtil.bytesToHexString("123456789".toByteArray())}")
    val crc = XbtEncrypt.doCRC16_XMODEM("123456789".toByteArray())
    println("CRC=>${String.format("%02X", crc)}")
}