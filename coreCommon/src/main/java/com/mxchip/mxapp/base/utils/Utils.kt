package com.mxchip.mxapp.base.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.TypedValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mxchip.http.response.NetState
import com.mxchip.http.response.NetStateResponse
import com.mxchip.mxapp.base.MXBusinessApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/7/12
 * @desc:
 */
fun String.compareVersion(remoteVersion: String): Int {
    if (this.isEmpty()) return 1 //当前版本为空，直接返回提示升级
    val localArray = this.split(".")
    val remoteArray = remoteVersion.split(".")
    if (localArray.size != remoteArray.size) return 1 //版本号长度不一致，直接返回提示升级
    localArray.forEachIndexed { index, current ->
        val local = current.toInt()
        val remote = remoteArray[index].toInt()
        when {
            local > remote -> {
                return -1
            }

            local < remote -> {
                return 1
            }

            else -> {
                return@forEachIndexed
            }
        }
    }
    return 0
}

fun Int.px2dp(context: Context = MXBusinessApplication.mxBusinessApp): Int {
    val scale = context.resources.displayMetrics.density
    return (this / scale + 0.5f).toInt()
}

fun Int.dp2px(context: Context = MXBusinessApplication.mxBusinessApp): Int {
    val scale = context.resources.displayMetrics.density
    return (this * scale + 0.5f).toInt()
}

fun Float.px2dp(context: Context = MXBusinessApplication.mxBusinessApp): Int {
    val scale = context.resources.displayMetrics.density
    return (this / scale + 0.5f).toInt()
}

fun Float.dp2px(context: Context = MXBusinessApplication.mxBusinessApp): Int {
    val scale = context.resources.displayMetrics.density
    return (this * scale + 0.5f).toInt()
}

fun generateShape(
    colors: IntArray,
    radius: ShapeRadius? = null,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM
): GradientDrawable {
    val context = MXBusinessApplication.mxBusinessApp
    val gradientDrawable = GradientDrawable()
    gradientDrawable.colors = colors
    gradientDrawable.orientation = orientation
    var radiusTmp = radius
    if (radiusTmp == null) {
        radiusTmp = ShapeRadius(0, 0, 0, 0)
    }
    if (unit == TypedValue.COMPLEX_UNIT_DIP) {
        gradientDrawable.cornerRadii = floatArrayOf(
            radiusTmp.leftTop.dp2px(context).toFloat(),
            radiusTmp.leftTop.dp2px(context).toFloat(),
            radiusTmp.rightTop.dp2px(context).toFloat(),
            radiusTmp.rightTop.dp2px(context).toFloat(),
            radiusTmp.rightBottom.dp2px(context).toFloat(),
            radiusTmp.rightBottom.dp2px(context).toFloat(),
            radiusTmp.leftBottom.dp2px(context).toFloat(),
            radiusTmp.leftBottom.dp2px(context).toFloat()
        )
    } else {
        gradientDrawable.cornerRadii = floatArrayOf(
            radiusTmp.leftTop.toFloat(),
            radiusTmp.leftTop.toFloat(),
            radiusTmp.rightTop.toFloat(),
            radiusTmp.rightTop.toFloat(),
            radiusTmp.rightBottom.toFloat(),
            radiusTmp.rightBottom.toFloat(),
            radiusTmp.leftBottom.toFloat(),
            radiusTmp.leftBottom.toFloat()
        )
    }
    return gradientDrawable
}

fun generateShape(
    colors: IntArray,
    radius: Int,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM
): GradientDrawable {
    val shapeRadius = ShapeRadius(radius, radius, radius, radius)
    return generateShape(colors, shapeRadius, unit, orientation)
}


fun generateShape(
    color: Int,
    radius: ShapeRadius? = null,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP
): GradientDrawable {
    val colors = intArrayOf(color, color)
    return generateShape(colors, radius, unit)
}

fun generateShape(
    color: Int,
    radius: Int,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP
): GradientDrawable {
    val shapeRadius = ShapeRadius(radius, radius, radius, radius)
    return generateShape(color, shapeRadius, unit)
}

fun generateStoke(
    width: Int,
    color: Int,
    radius: Int,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    dashWidth: Float = 0f,
    dashGap: Float = 1f
): GradientDrawable {
    val shapeRadius = ShapeRadius(radius, radius, radius, radius)
    return generateStoke(width, color, shapeRadius, unit, dashWidth, dashGap)
}

fun generateStoke(
    width: Int,
    color: Int,
    radius: ShapeRadius,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    dashWidth: Float = 0f,
    dashGap: Float = 1f
): GradientDrawable {
    val context = MXBusinessApplication.mxBusinessApp
    val gradientDrawable = GradientDrawable()
    val w = if (unit == TypedValue.COMPLEX_UNIT_DIP) width.dp2px() else width
    gradientDrawable.setStroke(w, color, dashWidth, dashGap)
    val radiusTmp = radius
    if (unit == TypedValue.COMPLEX_UNIT_DIP) {
        gradientDrawable.cornerRadii = floatArrayOf(
            radiusTmp.leftTop.dp2px(context).toFloat(),
            radiusTmp.leftTop.dp2px(context).toFloat(),
            radiusTmp.rightTop.dp2px(context).toFloat(),
            radiusTmp.rightTop.dp2px(context).toFloat(),
            radiusTmp.rightBottom.dp2px(context).toFloat(),
            radiusTmp.rightBottom.dp2px(context).toFloat(),
            radiusTmp.leftBottom.dp2px(context).toFloat(),
            radiusTmp.leftBottom.dp2px(context).toFloat()
        )
    } else {
        gradientDrawable.cornerRadii = floatArrayOf(
            radiusTmp.leftTop.toFloat(),
            radiusTmp.leftTop.toFloat(),
            radiusTmp.rightTop.toFloat(),
            radiusTmp.rightTop.toFloat(),
            radiusTmp.rightBottom.toFloat(),
            radiusTmp.rightBottom.toFloat(),
            radiusTmp.leftBottom.toFloat(),
            radiusTmp.leftBottom.toFloat()
        )
    }
    return gradientDrawable
}

fun generateShapeAndStoke(
    shapeColor: Int,
    stokeColor: Int,
    radius: Int,
    width: Int,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    dashWidth: Float = 0f,
    dashGap: Float = 1f
): GradientDrawable {
    val context = MXBusinessApplication.mxBusinessApp
    val gradientDrawable = GradientDrawable()
    val w = if (unit == TypedValue.COMPLEX_UNIT_DIP) width.dp2px() else width
    gradientDrawable.setStroke(w, stokeColor, dashWidth, dashGap)
    gradientDrawable.colors = intArrayOf(shapeColor, shapeColor)
    val shapeRadius = ShapeRadius(radius, radius, radius, radius)
    if (unit == TypedValue.COMPLEX_UNIT_DIP) {
        gradientDrawable.cornerRadii = floatArrayOf(
            shapeRadius.leftTop.dp2px(context).toFloat(),
            shapeRadius.leftTop.dp2px(context).toFloat(),
            shapeRadius.rightTop.dp2px(context).toFloat(),
            shapeRadius.rightTop.dp2px(context).toFloat(),
            shapeRadius.rightBottom.dp2px(context).toFloat(),
            shapeRadius.rightBottom.dp2px(context).toFloat(),
            shapeRadius.leftBottom.dp2px(context).toFloat(),
            shapeRadius.leftBottom.dp2px(context).toFloat()
        )
    } else {
        gradientDrawable.cornerRadii = floatArrayOf(
            shapeRadius.leftTop.toFloat(),
            shapeRadius.leftTop.toFloat(),
            shapeRadius.rightTop.toFloat(),
            shapeRadius.rightTop.toFloat(),
            shapeRadius.rightBottom.toFloat(),
            shapeRadius.rightBottom.toFloat(),
            shapeRadius.leftBottom.toFloat(),
            shapeRadius.leftBottom.toFloat()
        )
    }
    return gradientDrawable
}

fun getScreenWidth(context: Context = MXBusinessApplication.mxBusinessApp): Int {
    return context.resources.displayMetrics.widthPixels
}

data class ShapeRadius(
    val leftTop: Int,
    val rightTop: Int,
    val leftBottom: Int,
    val rightBottom: Int
)

fun getPathFromUri(uri: Uri): String? {
    val scheme = uri.scheme
    if (scheme.isNullOrBlank() || scheme.equals(ContentResolver.SCHEME_FILE, true)) return uri.path
    var path: String? = null
    if (scheme.equals(ContentResolver.SCHEME_CONTENT, true)) {
        val cursor =
            MXBusinessApplication.mxBusinessApp.contentResolver.query(uri, null, null, null, null)
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                if (index > -1) {
                    path = cursor.getString(index)
                }
            }
        }
    }
    return path
}

fun getScreenWith(): Int {
    return MXBusinessApplication.mxBusinessApp.resources.displayMetrics.widthPixels
}

fun getScreenHeight(): Int {
    return MXBusinessApplication.mxBusinessApp.resources.displayMetrics.heightPixels
}

inline fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    activityState: Lifecycle.State,
    crossinline action: (T) -> Unit
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(activityState) {
            collect {
                action.invoke(it)
            }
        }
    }
}

fun Long.timestampToDate(): String {
    return SimpleDateFormat("yyyy/MM/dd").format(this)
}

fun Long.timestampToTime(): String {
    return SimpleDateFormat("HH:mm").format(this)
}

fun Long.format(pattern: String): String {
    return SimpleDateFormat(pattern).format(this)
}

fun String.isPhone() = RegexUtil.isPhoneNumber(this)

fun String.isEmail() = RegexUtil.isEmail(this)

fun String.isEmoji() = RegexUtil.isEmoji(this)

fun String.formatPhone(): String {
    return this.substring(0, 3) + "****" + this.substring(this.length - 4, this.length)
}

fun String.formatEmail(): String {
    val atIndex = this.indexOf("@")
    if (atIndex == -1) return this
    //@后面的部分,最多显示9个
    val atSuffix = this.substring(atIndex)
    val suffix = if (atSuffix.length > 9) atSuffix.substring(0, 9) + "..." else atSuffix
    return if (atIndex > 3)
        this.substring(0, 2) + "***" + this.substring(atIndex - 2, atIndex) + suffix
    else {
        this.substring(0, atIndex) + suffix
    }
}

fun <T : Any> T.deepCopy(): T {
    //如果不是数据类，直接返回
    if (!this::class.isData) {
        return this
    }

    //拿到构造函数
    return this::class.primaryConstructor!!.let { primaryConstructor ->
        primaryConstructor.parameters.map { parameter ->
            //转换类型
            //memberProperties 返回非扩展属性中的第一个并将构造函数赋值给其
            //最终value=第一个参数类型的对象
            val value = (this::class as KClass<T>).memberProperties.first {
                it.name == parameter.name
            }.get(this)

            //如果当前类(这里的当前类指的是参数对应的类型，比如说这里如果非基本类型时)是数据类
            if ((parameter.type.classifier as? KClass<*>)?.isData == true) {
                parameter to value?.deepCopy()
            } else {
                parameter to value
            }


            //最终返回一个新的映射map,即返回一个属性值重新组合的map，并调用callBy返回指定的对象
        }.toMap().let(primaryConstructor::callBy)
    }
}

/**
 * 获取唯一标识，app卸载重装会更新
 * 使用aliyun的utdid库
 */
//fun getUtdid(context: Context = MXBusinessApplication.mxBusinessApp): String {
//    return UTDevice.getUtdid(context)
//}


/**
 * 短促的震动
 *
 * @param millis 震动时长
 */
@SuppressLint("MissingPermission")
fun Context.vibrateShot(millis: Long) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (it.hasVibrator()) {
            it.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

inline fun <reified T> loadFlowList(
    crossinline success: (data: List<T>?) -> Unit,
    crossinline error: (code: Int, msg: String?, data: Any?) -> Unit = { code, msg, data -> }
): (NetStateResponse) -> Unit {
    return {
        when (it.state) {

            NetState.SUCCESS -> {
                val entity = JsonUtil.fromJsonMXList(it.data, T::class.java)
                success.invoke(entity)
            }

            else -> {
                error.invoke(it.state.code, it.state.message, it.data)
            }
        }
    }
}

inline fun <reified T> loadFlow(
    crossinline success: (data: T?) -> Unit,
    crossinline error: (code: Int, msg: String?, data: Any?) -> Unit = { code, msg, data -> }
): (NetStateResponse) -> Unit {
    return {
        when (it.state) {
            NetState.SUCCESS -> {
                val entity = if (it.data.isNullOrEmpty()) {
                    null
                } else {
                    JsonUtil.fromJson(it.data!!, T::class.java)
                }
                success.invoke(entity)
            }

            else -> {
                error.invoke(it.state.code, it.state.message, it.data)
            }
        }
    }
}

inline fun <reified T> loadObserver(
    crossinline success: (data: T?) -> Unit,
    crossinline error: (code: Int, msg: String?, data: Any?) -> Unit = { code, msg, data -> }
): Observer<NetStateResponse> {
    return Observer {
        when (it.state) {
            NetState.SUCCESS -> {
                val json = it.data
                val entity = JsonUtil.fromJson(json, T::class.java)
                success.invoke(entity)
            }

            else -> {
                error.invoke(it.state.code, it.state.message, it.data)
            }
        }
    }
}