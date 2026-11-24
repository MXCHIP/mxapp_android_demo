package com.mxchip.mxapp.family.ui

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_HOME_BEAN
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_HOME_ID
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_ACCOUNT
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_AVATAR
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_ID
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_MEMBER_NAME
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_ROOM_IDS
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_SELECT_HOME_OWNER_ROLE
import com.mxchip.mxapp.base.consts.HomeConstants.KEY_SELECT_MEMBER_ROLE
import com.mxchip.mxapp.base.consts.HomeConstants.LOCATION_SELECTED
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.family.HomeManager
import com.mxchip.mxapp.family.adapter.FamilySettingMemberAdapter
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.bean.LocationBean
import com.mxchip.mxapp.family.bean.MemberBean
import com.mxchip.mxapp.family.databinding.FamilyAcFamilySettingBinding
import com.mxchip.mxapp.family.ui.dialog.HandOverListDialog
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus

/**
 * 家庭设置
 */
@Router(RouterConstants.FAMILY_SETTING_ACTIVITY)
class FamilySettingActivity : MXBusinessActivity<FamilyAcFamilySettingBinding>() {

    private val launch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val data = it.data
                val roomCount = data?.getIntExtra("roomCount", 0)
                binding.roomManager.setEndText("$roomCount")
            }
        }
    private val memberLaunch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                fetchMemberList()
            }
        }

    private val viewMode by viewModels<HomeViewModel>()

    private var _editHomeId: Int = 0

    // 当前账号在选择的家庭中的角色,0 - 家庭拥有者（管理员）；1 - 管理员； 2 -  普通成员
    private var cutAccountHomeRole = -1

    private val adapter by lazy {
        FamilySettingMemberAdapter(this, cutAccountHomeRole).apply {
            setItemClickListener { type, memberBean ->
                if (type == FamilySettingMemberAdapter.INVITE) {
                    itemInvite()
                } else {
                    if (memberBean == null) {
                        return@setItemClickListener
                    }
                    RouterManager.navigation(
                        RouterConstants.HOME_MEMBER_DETAIL_ACTIVITY,
                        mapOf(
                            KEY_MEMBER_ID to memberBean.member_id,
                            KEY_HOME_ID to _editHomeId,
                            KEY_MEMBER_NAME to memberBean.name,
                            KEY_MEMBER_ACCOUNT to memberBean.account,
                            KEY_MEMBER_AVATAR to memberBean.avatar_url,
                            KEY_SELECT_HOME_OWNER_ROLE to cutAccountHomeRole,
                            KEY_SELECT_MEMBER_ROLE to memberBean.role,
                            KEY_ROOM_IDS to (memberBean.room_ids ?: listOf()),
                        ),
                        this@FamilySettingActivity,
                        memberLaunch,
                    )
                }
            }
        }
    }

    override fun getLayoutBinding() = FamilyAcFamilySettingBinding.inflate(layoutInflater)

    private val sendVerificationCallback: (MemberBean) -> Unit = {
        showLoading()
        viewMode.handOverCode(_editHomeId, it.member_id)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<Any>(
                    success = { code ->
                        dismissLoading()
                        RouterManager.navigation(
                            RouterConstants.HAND_OVER_VERIFICATION_ACTIVITY, mapOf(
                                KEY_MEMBER_ID to it.member_id,
                                KEY_HOME_ID to _editHomeId,
                                KEY_MEMBER_NAME to it.name,
                            )
                        )
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    })
            )
    }

    override fun onInit() {
        initData()
        initView()
        checkViewState()
        initEvent()
        fetchMemberList()
    }

    private fun initView() {
        binding.memberList.layoutManager = LinearLayoutManager(this)
        binding.memberList.adapter = adapter
    }

    private fun checkViewState() {
        if (cutAccountHomeRole == RoleConstants.OWNER) {
            binding.familyName.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
            binding.roomManager.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
            binding.familyLocation.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
        } else if (cutAccountHomeRole == RoleConstants.MANAGE) {
            binding.roomManager.setEndIcon(com.mxchip.mxapp.base.R.drawable.core_ic_arrow_right)
        }

        if (HomeManager.isSupportHomeShare()) {
            binding.tvMember.visibility = View.VISIBLE
            binding.memberList.visibility = View.VISIBLE
        } else {
            binding.tvMember.visibility = View.GONE
            binding.memberList.visibility = View.GONE
        }
        if (HomeManager.isSupportHomeLocation()) {
            binding.familyLocation.visibility = View.VISIBLE
        } else {
            binding.familyLocation.visibility = View.GONE
        }
    }

    private fun initData() {
        val homeBeanStr = intent.getStringExtra(KEY_HOME_BEAN)
        val homeBean = JsonUtil.fromJson(homeBeanStr, HomeBean::class.java)
        if (homeBean == null) {
            finish()
            return
        }
        _editHomeId = homeBean.home_id
        val familyRoomCount = homeBean.room_count
        val familyDeviceCount = homeBean.device_count
        cutAccountHomeRole = homeBean.role
        binding.familyName.setEndText(homeBean.name)
        binding.roomManager.setEndText("$familyRoomCount")
        binding.deviceCount.setEndText("$familyDeviceCount")
        val country = homeBean.country_name
        val province = homeBean.province_name
        val city = homeBean.city_name
        binding.familyLocation.setEndText("$country $province $city")
        binding.handOver.visibility =
            if (cutAccountHomeRole == RoleConstants.OWNER) View.VISIBLE else View.GONE
    }

    private fun initEvent() {
        val rightText = if (cutAccountHomeRole == RoleConstants.OWNER) //所有者
            getString(com.mxchip.mxapp.base.R.string.mx_delete)
        else
            getString(com.mxchip.mxapp.base.R.string.mx_exit)
        binding.toolbar.rightText(rightText)
        binding.toolbar.left { finish() }.right {
            deleteFamily(_editHomeId)
        }
        if (cutAccountHomeRole == RoleConstants.OWNER) {
            binding.familyName.setOnClickListener {
                MXDialog.Builder(this)
                    .title(getString(com.mxchip.mxapp.base.R.string.mx_family_name))
                    .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
                    .edittextText(binding.familyName.getEndText())

                    .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
                    .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, inputText ->
                        if (inputText.isNullOrBlank()) {
                            showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                            return@rightButton
                        } else if (inputText.length > MXDialog.maxLength) {
                            showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
                            return@rightButton
                        }
                        dialog.cancel()
                        val familyName = inputText.trim()
                        showLoading()
                        viewMode.updateHomeName(_editHomeId, familyName)
                            .launchAndCollectIn(
                                this,
                                Lifecycle.State.CREATED,
                                loadFlow<Any>(success = {
                                    dismissLoading()
                                    binding.familyName.setEndText(familyName)
                                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_update_success))
                                }, error = { code, msg, _ ->
                                    dismissLoading()
                                    val netErr = onException(code)
                                    if (!netErr) showToast("$msg")
                                })
                            )
                    }.create().show()
            }
            binding.familyLocation.setOnClickListener {
                RouterManager.navigation(
                    RouterConstants.LOCATION_ACTIVITY, mapOf(
                        KEY_HOME_ID to _editHomeId
                    )
                )
            }
        }
        if (cutAccountHomeRole != RoleConstants.COMMON)
            binding.roomManager.setOnClickListener {
                RouterManager.navigation(
                    RouterConstants.ROOM_MANAGE_ACTIVITY, mapOf(
                        KEY_HOME_ID to _editHomeId
                    ), this, launch
                )
            }

        binding.handOver.setOnClickListener {
            if (HomeManager.isSupportHomeShare()) {
                //支持分享模式，家庭成员方式移交
                if (adapter.getItems().size <= 1) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_role_no_member))
                    return@setOnClickListener
                }
                val dialog =
                    HandOverListDialog(this, adapter.getItems(), sendVerificationCallback).create()
                dialog.show()
            } else {
                showAccountHandOverDialog()
            }
        }

        LiveDataBus.with<LocationBean>(LOCATION_SELECTED).observe(this) {
            binding.familyLocation.setEndText("${it.country_name} ${it.province_name} ${it.city_name}")
        }
    }

    private fun itemInvite() {
        if (cutAccountHomeRole == RoleConstants.COMMON) {
            RoleManager.showUserDialog(this@FamilySettingActivity)
            return
        }
        RouterManager.navigation(
            RouterConstants.INVITEE_MEMBER_ACTIVITY, mapOf(
                KEY_HOME_ID to _editHomeId,
                "home.name" to binding.familyName.getEndText(),
                "memberInCurrentHomeRole" to cutAccountHomeRole
            )
        )
    }

    private fun fetchMemberList() {
        showLoading()
        viewMode.memberList(_editHomeId).launchAndCollectIn(
            this@FamilySettingActivity,
            Lifecycle.State.CREATED,
            loadFlowList<MemberBean>(success = {
                dismissLoading()
                it?.apply {
                    adapter.setData(this)
                }
            }, error = { _, _, _ ->
                dismissLoading()
            })
        )
        viewMode.launchMain {}
    }

    private fun deleteFamily(homeId: Int) {
        val title: String
        val content: String
        if (cutAccountHomeRole == 0) {
            title = getString(com.mxchip.mxapp.base.R.string.mx_del_home_title)
            content = getString(com.mxchip.mxapp.base.R.string.mx_family_del_des)
        } else {
            title = getString(com.mxchip.mxapp.base.R.string.mx_tips)
            content = getString(com.mxchip.mxapp.base.R.string.mx_family_exit_des)
        }
        MXDialog.Builder(this).title(title)
            .content(content)
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, _ ->
                dialog.cancel()
                showLoading()
                viewMode.deleteFamily(homeId)
                    .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                        dismissLoading()
                        if (homeId == AppConfigManager.getUseHomeId()) {
                            //当前使用家庭被删除
                            LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
                        }
                        finish()
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }))
            }.create().show()
    }

    private fun showAccountHandOverDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_role_hand_over))
            .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_account_phone_hint))

            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, input ->
                if (input.isNullOrEmpty()) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                    return@rightButton
                }
                dialog.dismiss()
                showLoading()
                viewMode.handOverByAccount(AppConfigManager.getUseHomeId(), input)
                    .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                        dismissLoading()
                        showToast(getString(com.mxchip.mxapp.base.R.string.mx_role_hand_over_successful))
                        AppConfigManager.clearUseHomeData()
                        RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
                    }, error = { code, msg, _ ->
                        dismissLoading()
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                    }))
            }.create().show()
    }
}