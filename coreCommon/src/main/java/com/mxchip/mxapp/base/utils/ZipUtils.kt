package com.mxchip.mxapp.base.utils

import com.mxchip.log.MXLog
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File


/**
 * <pre>
 * author: Blankj
 * blog  : http://blankj.com
 * time  : 2016/08/27
 * desc  : utils about zip
</pre> *
 */
object ZipUtils {

    fun zip(
        sourceFolder: String,
        zipFilePath: String,
        password: String? = null
    ): Boolean {
        try {
            val zipParameters = ZipParameters()
            val zipFile: ZipFile
            if (!password.isNullOrEmpty()) {
                // 设置加密参数
                zipParameters.isEncryptFiles = true
                zipParameters.encryptionMethod = EncryptionMethod.AES
                // 使用 AES 256 加密
                zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                // 创建加密的 ZIP 文件
                zipFile = ZipFile(zipFilePath, password.toCharArray())
            } else {
                // 不加密
                zipParameters.isEncryptFiles = false
                zipFile = ZipFile(zipFilePath)
            }

            val targetFile = File(sourceFolder)
            if (targetFile.isDirectory) {
                zipFile.addFolder(targetFile, zipParameters)
            } else {
                zipFile.addFile(targetFile, zipParameters)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * Unzip the file by keyword.
     *
     * @param zipFilePath The path of ZipFile.
     * @param destDirPath The path of destination directory.
     * @param keyword     The keyboard.
     * @return the unzipped files
     */
    fun unzipFile(
        zipFilePath: String,
        destDirPath: String,
        keyword: String? = null
    ): Boolean {
        val zipFile = ZipFile(zipFilePath)
        return unzipFile(zipFile, File(destDirPath), keyword)
    }

    /**
     * Unzip the file by keyword.
     *
     * @param zipFile The ZipFile.
     * @param destDir The destination directory.
     * @param keyword The keyboard.
     * @return the unzipped files
     */
    fun unzipFile(
        zipFile: ZipFile,
        destDir: File,
        keyword: String? = null
    ): Boolean {
        try {
            if (!keyword.isNullOrEmpty()) {
                if (zipFile.isEncrypted()) {
                    zipFile.setPassword(keyword.toCharArray());
                }
            }
            // 确保输出目录存在
            if (destDir.exists().not()) {
                destDir.mkdirs()
            }
            zipFile.extractAll(destDir.absolutePath)
        } catch (e: ZipException) {
            e.printStackTrace()
            MXLog.e("ZIP", "unzipFile failed: ${e.message}")
            return false
        }
        return true
    }
}