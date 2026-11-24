package com.mxchip.mxapp.room.ui

import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import coil.load
import com.king.zxing.util.CodeUtils
import com.mxchip.http.response.NetState
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.ClipboardManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.RegexUtil
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.room.R
import com.mxchip.mxapp.room.adapter.RoomPermissionAdapter
import com.mxchip.mxapp.room.bean.RoomListBean
import com.mxchip.mxapp.room.databinding.RoomAcRoomPermissionBinding
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import org.json.JSONObject

/**
家庭邀请，角色（房间权限勾选）
 */
@Router(RouterConstants.ROOM_PERMISSION_ACTIVITY)
class RoomPermissionActivity : MXBusinessActivity<RoomAcRoomPermissionBinding>() {
    private var type = -1
    private var memberRole = 2 //当前选择的用户角色
    private var editHomeId = -1
    private var inviteCode = ""
    private var account = ""
    private var memberId = -1
    private lateinit var homeName: String

    private var selectAll = false
    private var oldRoomIds: List<Int>? = null
    private var memberInCurrentHomeRole = 2 // 用户在当前选择的家庭中的角色，如果是管理员，选择邀请用户角色时，只能选择普通用户

    private lateinit var qrcodeDialog: QrcodeDialog
    private var familyRoomTotal: Int? = null //家庭下房间总数

    private val roomViewModel by viewModels<RoomViewModel>()

    private val adapter by lazy {
        RoomPermissionAdapter().apply {
            setItemClickListener { selectList, roomBean ->
                selectAll = selectList.size == familyRoomTotal
                binding.selectAll.setImageResource(
                    if (selectAll) R.drawable.room_ic_round_selected else R.drawable.room_ic_round_unselected
                )

            }
        }
    }

    override fun getLayoutBinding() = RoomAcRoomPermissionBinding.inflate(layoutInflater)

    override fun onInit() {
        type = intent.getIntExtra(HomeConstants.KEY_ROOM_PERMISSION_TYPE, -1)
        if (type == -1) {
            finish()
            return
        }
        when (type) {
            HomeConstants.TYPE_QRCODE_INVITE -> {
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_invite_qrcode_member))
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_invite)
            }

            HomeConstants.TYPE_CODE_INVITE -> {
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_invite_code_member))
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_invite)
            }

            HomeConstants.TYPE_ACCOUNT_INVITE -> {
                account = intent.getStringExtra("account")!!
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_invite_account_member))
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_invite)
            }

            HomeConstants.TYPE_MEMBER_SET_PERMISSION -> {
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_role_select))
                binding.next.text = getString(com.mxchip.mxapp.base.R.string.mx_confirm)
            }
        }

        editHomeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, -1)
        memberId = intent.getIntExtra(HomeConstants.KEY_MEMBER_ID, -1)
        memberRole = intent.getIntExtra(HomeConstants.KEY_SELECT_MEMBER_ROLE, 2)
        homeName = intent.getStringExtra("home.name") ?: ""
        JsonUtil.fromJsonToArray(intent.getStringExtra("memberInCurrentHomeRoomPermission"), Integer::class.java)?.apply {
            oldRoomIds = this.map { it.toInt() }
        }
        memberInCurrentHomeRole = intent.getIntExtra("memberInCurrentHomeRole", 1)
        initView()
        initEvent()
        queryRoomList()
    }

    private fun initView() {
        binding.roomList.layoutManager = LinearLayoutManager(this)
        binding.roomList.adapter = adapter
        checkRoomShowState()
        binding.next.background = generateShape(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 25)
        qrcodeDialog = QrcodeDialog(editHomeId)
    }

    private fun checkRoomShowState() {
        if (memberRole == 2) {
            binding.role.setEndText(getString(com.mxchip.mxapp.base.R.string.mx_role_user))
            binding.layoutSelectAll.visibility = View.VISIBLE
            binding.roomList.visibility = View.VISIBLE
        } else { //管理员不显示房间，拥有所有
            selectAll(true)
            binding.role.setEndText(getString(com.mxchip.mxapp.base.R.string.mx_role_admin))
            binding.layoutSelectAll.visibility = View.GONE
            binding.roomList.visibility = View.GONE
        }
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }

        binding.role.setOnClickListener {
            if (memberInCurrentHomeRole == 1) {
                return@setOnClickListener
            }
            MemberRoleSwitchDialog(this, memberRole).setSwitchListener { alertDialog, role ->
                memberRole = role
                checkRoomShowState()
                alertDialog.cancel()
            }.show()
        }

        binding.selectAll.setOnClickListener {
            selectAll(!selectAll)
        }

        binding.next.setOnClickListener {
            if (adapter.getSelectedRooms().isEmpty()) {
                return@setOnClickListener
            }
            when (type) {
                HomeConstants.TYPE_QRCODE_INVITE, HomeConstants.TYPE_CODE_INVITE -> getInviteCode()
                HomeConstants.TYPE_ACCOUNT_INVITE -> accountInvite()
                HomeConstants.TYPE_MEMBER_SET_PERMISSION -> modifyPermission()
            }
        }
    }

    private fun queryRoomList() {
        val param = mapOf("home_id" to editHomeId, "page_no" to 1, "page_size" to 199)
        roomViewModel.roomListV4(param)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<RoomListBean>(success = {
                it?.apply {
                    familyRoomTotal = this.list.size
                    adapter.setData(list)
                    if (type != 3
                        || oldRoomIds.isNullOrEmpty()
                        || familyRoomTotal == oldRoomIds?.size
                    )
                        selectAll(true)
                    else if (type == 3) {
                        adapter.select(oldRoomIds as ArrayList)
                    }
                }
            }, error = { code, msg, _ ->
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            }))
    }

    private fun getInviteCode() {
        roomViewModel.generateInviteCode(type, editHomeId, memberRole, adapter.getSelectedRooms())
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED
            ) { netState ->
                if (netState.state == NetState.SUCCESS) {
                    netState.data?.apply {
                        val json = JSONObject(this)
                        val content = json.getString("code")
                        if (content.isNullOrBlank()) {
                            showToast("code is null")
                        } else {
                            inviteCode = content
                            if (type == HomeConstants.TYPE_QRCODE_INVITE) {
                                createQrcodeInviteDialog(content)
                            } else {
                                createCodeInviteDialog(content)
                            }
                        }
                    }
                } else {
                    netState.state.message?.apply {
                        showToast(this)
                    }
                }
            }
    }

    private fun accountInvite() {
        showLoading()
        roomViewModel.shareFamilyByAccount(
            editHomeId,
            account,
            if (RegexUtil.isEmail(account)) null else "86",
            memberRole,
            adapter.getSelectedRooms()
        ).launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
            dismissLoading()
            createAccountInviteSuccessDialog()
            LiveDataBus.with<Int>(LiveDataBusKey.KEY_SHARE_PROGRESS_REFRESH).postValue(0)
        }, error = { code, msg, _ ->
            dismissLoading()
            val netErr = onException(code)
            if (!netErr) showToast("$msg")
        }))
    }

    private fun createCodeInviteDialog(code: String) {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_invite_code_title))
            .content(String.format(getString(com.mxchip.mxapp.base.R.string.mx_invite_code), code))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_copy)) { dialog, _ ->
                dialog.cancel()
                getCopyContent()
            }.create().show()
    }

    private fun getCopyContent() {
        roomViewModel.getUserInfo().launchAndCollectIn(
            this, Lifecycle.State.CREATED
        ) { netState ->
            if (netState.state == NetState.SUCCESS) {
                netState.data?.apply {
                    val json = JSONObject(this)
                    val username = json.getString("username")
                    val content = String.format(
                        getString(com.mxchip.mxapp.base.R.string.mx_invite_code_clipboard),
                        inviteCode,
                        username,
                        homeName,
                        getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                    )
                    ClipboardManager.setTextToClip(this@RoomPermissionActivity, content)
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_copy_clipboard))
                }
            } else {
                netState.state.message?.apply {
                    showToast(this)
                }
            }
        }
    }

    private fun createQrcodeInviteDialog(content: String) {
        val bitmap = CodeUtils.createQRCode(content, 180.dp2px())
        bitmap?.also {
            qrcodeDialog.show(supportFragmentManager, "qrcodeDialog")
            qrcodeDialog.setQrcode(it).setRole(memberRole).setRoomIds(adapter.getSelectedRooms())
        }
    }

    private fun createAccountInviteSuccessDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_send_success))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_invite_phone_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            ).leftButton(null)
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_i_know)) { dialog, _ ->
                dialog.cancel()
                finish()
            }.create().show()
    }

    private fun modifyPermission() {
        val newRoomIds = adapter.getSelectedRooms()
        showLoading()
        roomViewModel.updateMember(editHomeId, memberId, null, memberRole, newRoomIds)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                dismissLoading()
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_update_success))
                setResult(RESULT_OK, Intent().apply {
                    putExtra(HomeConstants.KEY_SELECT_MEMBER_ROLE, memberRole)
                    putIntegerArrayListExtra(HomeConstants.KEY_ROOM_IDS, newRoomIds)
                })
                finish()
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            }))

    }

    private fun selectAll(isSelectAll: Boolean) {
        selectAll = isSelectAll
        binding.selectAll.setImageResource(
            if (selectAll) R.drawable.room_ic_round_selected else R.drawable.room_ic_round_unselected
        )
        adapter.selectAll(isSelectAll)
    }
}