package com.mxchip.mxapp.family.ui

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import coil.load
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_HOME_ID
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_ACCOUNT
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_AVATAR
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_ID
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_NAME
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_ROOM_IDS
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_SELECT_HOME_OWNER_ROLE
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_SELECT_MEMBER_ROLE
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.family.R
import com.mxchip.mxapp.family.databinding.FamilyAcHomeMemberDetailBinding
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/18
 * @desc:
 */
@Router(RouterConstants.HOME_MEMBER_DETAIL_ACTIVITY)
class HomeMemberDetailActivity : MXBusinessActivity<FamilyAcHomeMemberDetailBinding>() {
    override fun getLayoutBinding() = FamilyAcHomeMemberDetailBinding.inflate(layoutInflater)

    private val launch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val data = it.data
                data?.apply {
                    val role = getIntExtra(KEY_SELECT_MEMBER_ROLE, memberRole)
                    memberRole = role
                    binding.role.setEndText(
                        if (memberRole == RoleConstants.COMMON) getString(com.mxchip.mxapp.base.R.string.mx_role_user) else getString(
                            com.mxchip.mxapp.base.R.string.mx_role_admin
                        )
                    )
                    val roomids = this.getIntegerArrayListExtra(KEY_ROOM_IDS)
                    roomids?.apply {
                        roomIds.clear()
                        roomIds.addAll(this)
                    }
                }
            }
        }

    private var memberRole = -1
    private var homeId = -1
    private var memberId = -1
    private var cutAccountHomeRole = -1
    private var roomIds = arrayListOf<Int>()
    private val viewModel by viewModels<HomeViewModel>()

    override fun onInit() {
        initData()
        initEvent()
    }

    private fun initData() {
        homeId = intent.getIntExtra(KEY_HOME_ID, -1)
        memberId = intent.getIntExtra(KEY_MEMBER_ID, -1)
        val name = intent.getStringExtra(KEY_MEMBER_NAME)
        val account = intent.getStringExtra(KEY_MEMBER_ACCOUNT)
        val avatar = intent.getStringExtra(KEY_MEMBER_AVATAR)
        memberRole = intent.getIntExtra(KEY_SELECT_MEMBER_ROLE, -1)
        cutAccountHomeRole = intent.getIntExtra(KEY_SELECT_HOME_OWNER_ROLE, -1)
        binding.name.setEndText(name)
        binding.account.setEndText(account)
        binding.role.setEndText(
            when (memberRole) {
                RoleConstants.OWNER -> getString(com.mxchip.mxapp.base.R.string.mx_role_owner)
                RoleConstants.MANAGE -> getString(
                    com.mxchip.mxapp.base.R.string.mx_role_admin
                )
                else -> getString(com.mxchip.mxapp.base.R.string.mx_role_user)
            }
        )
        binding.removeMember.visibility =
            if (cutAccountHomeRole > -1 && memberRole > -1 && cutAccountHomeRole < memberRole && (cutAccountHomeRole == RoleConstants.OWNER || cutAccountHomeRole == RoleConstants.MANAGE)) {
                binding.role.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
                View.VISIBLE
            } else {
                View.GONE
            }
        if (avatar.isNullOrBlank())
            binding.shapIvPhoto.setImageResource(R.drawable.family_ic_default_avatar)
        else binding.shapIvPhoto.load(avatar)
        JsonUtil.fromJsonToArray(intent.getStringExtra(KEY_ROOM_IDS), Integer::class.java)?.apply {
            roomIds.addAll(this.map { it.toInt() })
        }
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }
        if (cutAccountHomeRole == RoleConstants.COMMON) {
        } else {
            binding.name.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
            binding.name.setOnClickListener {
                MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_name))
                    .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
                    .edittextText(binding.name.getEndText())
                    
                    .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                        alertDialog.cancel()
                    }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, inputText ->
                        if (inputText.isNullOrBlank()) {
                            showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                            return@rightButton
                        } else if (inputText.length > MXDialog.maxLength) {
                            showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
                            return@rightButton
                        }
                        alertDialog.cancel()
                        val finalInput = inputText.trim()
                        updateMember(finalInput, null, null) {
                            binding.name.setEndText(finalInput)
                        }
                    }.create().show()
            }
        }

        if (cutAccountHomeRole < memberRole) {
            binding.removeMember.setOnClickListener {
                createRemoveMemberDialog()
            }

            binding.role.setOnClickListener {
                AppConfigManager.getUseHomeName()
                RouterManager.navigation(
                    RouterConstants.ROOM_PERMISSION_ACTIVITY, mapOf(
                        HomeConstants.KEY_ROOM_PERMISSION_TYPE to 3,
                        KEY_HOME_ID to homeId,
                        KEY_MEMBER_ID to memberId,
                        KEY_SELECT_MEMBER_ROLE to memberRole,
                        "memberInCurrentHomeRole" to cutAccountHomeRole,
                        "memberInCurrentHomeRoomPermission" to roomIds,
                    ), this, launch
                )
            }
        }
    }

    private fun createRemoveMemberDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_role_del_member_title))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_role_del_member_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                if (homeId <= 0 || memberId <= -1) {
                    showToast("homeId or memberId is null")
                    return@rightButton
                }
                dialog.cancel()
                showLoading()
                viewModel.removeMember(homeId, memberId)
                    .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                        dismissLoading()
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_del_success))
                        finish()
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }))
            }.create().show()
    }

    private fun updateMember(
        name: String?, role: Int?, roomList: List<Int>?, success: (() -> Unit)? = null
    ) {
        showLoading()
        viewModel.updateMember(homeId, memberId, name, role, roomList)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                dismissLoading()
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_update_success))
                success?.invoke()
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            }))
    }

    override fun finish() {
        setResult(RESULT_OK)
        super.finish()
    }
}