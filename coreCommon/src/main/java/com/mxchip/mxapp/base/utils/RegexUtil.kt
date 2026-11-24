package com.mxchip.mxapp.base.utils

import java.util.regex.Pattern

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/8/23
 * @desc:
 */
object RegexUtil {

    /**
     * 正则：手机号（简单）
     */
    const val REGEX_MOBILE_SIMPLE = "^[1]\\d{10}$"

    /**
     * 正则：手机号（精确）
     * <p>移动：134(0-8)、135、136、137、138、139、147、150、151、152、157、158、159、178、182、183、184、187、188</p>
     * <p>联通：130、131、132、145、155、156、171、175、176、185、186</p>
     * <p>电信：133、153、173、177、180、181、189</p>
     * <p>全球星：1349</p>
     * <p>虚拟运营商：170</p>
     */
    const val REGEX_MOBILE_EXACT =
        "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(17[0,1,3,5-8])|(18[0-9])|(147))\\d{8}$"


    /**
     * 正则：手机号
     * 适配2021年号段
     */
    const val REGEX_MOBILE_2021 = "^1[3-9]\\d{9}\$"

    const val REGEX_NUMBER = "^[0-9]*\$"

    /**
     * 正则：电话号码
     */
    const val REGEX_TEL = "^0\\d{2,3}[- ]?\\d{7,8}"

    /**
     * 正则：邮箱
     */
    const val REGEX_EMAIL = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"
    /**
     * 密码规则:6–16位，数字、字母、字符至少包含两种
     */
    const val REGEX_PWD = "^(?![0-9]+\$)(?![A-Za-z]+\$)(?![^0-9A-Za-z]+\$)[0-9A-Za-z\\W]{6,16}\$"

    /**
     * 字符{中文、英文、数字、+、-、_、/}
     */
    const val REGEX_CHAR = "^[\\u4e00-\\u9fa5a-zA-Z0-9_+-/]+\$"

    /**
     * emoji表情过滤
     */
    const val REGEX_EMOJI ="[\\u2600-\\u26FF\\u2700-\\u27BF]|" +  // 杂项符号和装饰符号
            "[\\uD83C\\uDC04-\\uD83C\\uDFFF]|" +  // 特殊标记
            "[\\uD83D\\uDC00-\\uD83D\\uDEFF]|" +  // 表情符号
            "[\\uD83E\\uDD00-\\uD83E\\uDEFF]|" +  // 补充符号和象形文字
            "[\\u203C\\u2049\\u2122\\u2139\\u2194-\\u2199\\u21A9\\u21AA]|" +  // 单字符符号
            "[\\u231A\\u231B\\u2328\\u23CF\\u23E9-\\u23F3\\u23F8-\\u23FA]|" +  // 时钟和其他符号
            "[\\u24C2\\u25AA\\u25AB\\u25B6\\u25C0\\u25FB-\\u25FE]|" +  // 几何图形
            "[\\u2600-\\u2604\\u260E\\u2611\\u2614\\u2615\\u2618\\u261D\\u2620]|" +  // 天气和符号
            "[\\u2622\\u2623\\u2626\\u262A\\u262E\\u262F\\u2638-\\u263A]|" +  // 宗教符号
            "[\\u2640\\u2642\\u2648-\\u2653\\u2660\\u2663\\u2665\\u2666\\u2668\\u267B]|" +  // 性别、星座、扑克牌
            "[\\u267E\\u267F\\u2692-\\u2697\\u2699\\u269B\\u269C\\u26A0\\u26A1]|" +  // 警告和工具
            "[\\u26AA\\u26AB\\u26B0\\u26B1\\u26BD\\u26BE\\u26C4\\u26C5\\u26C8]|" +  // 圆形、体育
            "[\\u26CE\\u26CF\\u26D1\\u26D3\\u26D4\\u26E9\\u26EA\\u26F0-\\u26F5]|" +  // 建筑、自然
            "[\\u26F7-\\u26FA\\u26FD\\u2702\\u2705\\u2708-\\u270D\\u270F\\u2712\\u2714]|" +  // 工具、手势
            "[\\u2716\\u271D\\u2721\\u2728\\u2733\\u2734\\u2744\\u2747\\u274C\\u274E]|" +  // 标记、符号
            "[\\u2753-\\u2755\\u2757\\u2763\\u2764\\u2795-\\u2797\\u27A1\\u27B0\\u27BF]|" +  // 问号、心形、箭头
            "[\\u2934\\u2935\\u2B05-\\u2B07\\u2B1B\\u2B1C\\u2B50\\u2B55]|" +  // 箭头、星星
            "[\\u3030\\u303D\\u3297\\u3299]|" +  // 波浪线、标记
            "[\\uFE00-\\uFE0F\\u200D\\u20E3]|" +  // 变体选择器、零宽连接符、组合封闭标记
            "[\\u00A9\\u00AE]"  // 版权和注册商标

    fun isPhoneNumber(phone: String):Boolean{
//        return Pattern.matches(REGEX_NUMBER, phone)
        return Pattern.matches(REGEX_MOBILE_2021,phone)
    }

    fun isNumber(phone: String):Boolean{
        return Pattern.matches(REGEX_NUMBER, phone)
    }

    fun isEmail(email: String) =
        Pattern.matches(REGEX_EMAIL, email)//Pattern.compile(REGEX_EMAIL).matcher(email).matches()

    fun isValidPwd(pwd: String) = Pattern.matches(REGEX_PWD, pwd)

    fun isEmoji(content: String) = Pattern.matches(REGEX_EMOJI, content)

    /**
     * 检查字符串中是否包含emoji
     * 用于InputFilter等场景，检查字符串中是否包含任何emoji字符
     */
    fun containsEmoji(text: CharSequence?): Boolean {
        if (text == null || text.isEmpty()) return false
        val pattern = Pattern.compile(REGEX_EMOJI)
        val matcher = pattern.matcher(text)
        return matcher.find()
    }
}