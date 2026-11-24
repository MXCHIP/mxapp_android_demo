package com.mxchip.mxapp.base.utils

import android.content.Context
import androidx.annotation.IntDef
import com.mxchip.mxapp.base.consts.RoleConstants


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/19
 * @desc:
 */
object RoleManager {
    var CURRENT_HOME_ROLE = RoleConstants.COMMON

    fun isOwner(): Boolean {
        return CURRENT_HOME_ROLE == RoleConstants.OWNER
    }

    fun isNotOwner() = !isOwner()

    fun isAdmin(): Boolean {
        return CURRENT_HOME_ROLE == RoleConstants.MANAGE
    }

    fun isUser(): Boolean {
        return CURRENT_HOME_ROLE == RoleConstants.COMMON
    }

    fun showDialog(
        context: Context,
        @RoleType type: Int = RoleType.TYPE_USER,
        rightBlock: (() -> Unit)? = null
    ) {
//        MXDialog.Builder(context)
//            .title(context.getString(com.mxchip.mxapp.base.R.string.core_tips))
//            .content(
//                if (type == RoleType.TYPE_ADMIN) context.getString(com.mxchip.mxapp.base.R.string.core_role_owner_fun_des) else context.getString(
//                    R.string.core_role_user_fun_des
//                )
//            )
//            .leftButton(null)
//            .rightButton(context.getString(com.mxchip.mxapp.base.R.string.core_confirm)) { alertDialog, _ ->
//                alertDialog.dismiss()
//                rightBlock?.invoke()
//            }.create().show()
    }

    fun showUserDialog(context: Context, rightBlock: (() -> Unit)? = null) {
        showDialog(context, RoleType.TYPE_USER, rightBlock)
    }

    fun showOwnerDialog(context: Context, rightBlock: (() -> Unit)? = null) {
        showDialog(context, RoleType.TYPE_ADMIN, rightBlock)
    }


    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
    @IntDef(RoleType.TYPE_USER, RoleType.TYPE_ADMIN)
    @Retention(AnnotationRetention.SOURCE)
    annotation
    class RoleType {
        companion object {
            // 1禁止普通用户user  2禁止管理员和普通用户
            const val TYPE_USER = 1
            const val TYPE_ADMIN = 2
        }
    }
}