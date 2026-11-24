package com.mxchip.mxapp.base.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mxchip.apicore.utils.MXApiConfigUtil
import com.tencent.mmkv.MMKV
import java.util.Locale

object LanguageUtil {

    const val ENGLISH = "en"
    const val CHINESE = "zh-Hans-CN"

    private val mmkv by lazy {
        MMKV.mmkvWithID("mx_language")
    }

    fun saveLanguage(language: String) {
        mmkv.encode("language", language)
        setLanguage(language)
    }

    fun setLanguage(language: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        MXApiConfigUtil.setLanguage(language) // 设置API
    }

    fun getSettingLanguage() = mmkv.decodeString("language")

    fun isZh(): Boolean {
        val cut = Locale.getDefault().toLanguageTag()
        //有的手机返回zh-CN，有的返回zh-Hans-CN
        return cut.contains("zh-Hans") || cut.contains("-CN")
    }

    fun getCurrentLanguage(): String {
        val setting = getSettingLanguage()
        return if (setting.isNullOrEmpty()) {
            val default = Locale.getDefault().toLanguageTag()
            when {
                default.contains("zh-Hans")
                        || default.contains("-CN") -> {
                    CHINESE
                }

                else -> {
                    ENGLISH
                }
            }
        } else {
            setting
        }
    }
}