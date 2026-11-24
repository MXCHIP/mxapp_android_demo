package com.mxchip.mxapp.family.ui

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeConstants.JOIN_HOME_TYPE_INVITE_CODE
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.family.HomeManager
import com.mxchip.mxapp.family.adapter.FamilyManageAdapter
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.databinding.FamilyAcHomeManageBinding
import com.mxchip.mxapp.family.ui.dialog.JoinHomeDialog
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.MxRouter
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import org.json.JSONObject

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/8
 * @desc:
 */
@Router(RouterConstants.FAMILY_MANAGE_ACTIVITY)
class HomeManageActivity : MXBusinessActivity<FamilyAcHomeManageBinding>() {

    private val viewModel by viewModels<HomeViewModel>()
    private var isRefreshHome: Boolean? = null

    private val launch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                isRefreshHome = true
                getHomeList()
                val newHomeId = it.data?.getIntExtra("homeId", -1) ?: -1
                if (newHomeId != -1) {
                    showJoinedHomeDialog(newHomeId)
                }
            }
        }

    private val scanLaunch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val originalValue = it.data?.getStringExtra("originalValue") ?: ""
                viewModel.bindShareFamilyByCode(0, originalValue).launchAndCollectIn(
                    this@HomeManageActivity, Lifecycle.State.CREATED
                ) { netState ->
                    if (netState.state == NetState.SUCCESS) {
                        getHomeList()
                        netState.data?.apply {
                            val json = JSONObject(this)
                            val newHomeId = json.getInt("home_id")
                            showJoinedHomeDialog(newHomeId)

                        }
                    } else {
                        netState.state.message?.apply {
                            showToast(this)
                        }
                    }
                }
            }
        }

    private fun showJoinedHomeDialog(newHomeId: Int) {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_join_home_success))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                alertDialog.dismiss()
                setCurrentHome(newHomeId)
            }.create().show()
    }

    private val joinHomeDialog by lazy {
        JoinHomeDialog(this).apply {
            setListener { dialog, type ->
                if (type == JOIN_HOME_TYPE_INVITE_CODE) {
                    joinByInviteCode()
                } else {
                    if (XXPermissions.isGranted(this@HomeManageActivity, Permission.CAMERA)) {
                        MxRouter.build(RouterConstants.QR_SCAN_ACTIVITY)
                            .navigation(this@HomeManageActivity, scanLaunch)
                    } else openPermissionDialog()
                }
                dialog.cancel()
            }
        }.create()
    }

    private val adapter by lazy {
        FamilyManageAdapter(this).apply {
            setItemClickListener {
                RouterManager.navigation(
                    RouterConstants.FAMILY_SETTING_ACTIVITY,
                    mapOf(HomeConstants.KEY_HOME_BEAN to it)
                )
            }
        }
    }

    override fun getLayoutBinding() = FamilyAcHomeManageBinding.inflate(layoutInflater)

    override fun onInit() {
        initView()
        checkViewState()
        initEvent()
    }

    override fun onStart() {
        super.onStart()
        getHomeList()
    }

    private fun initView() {
        binding.familyList.layoutManager = LinearLayoutManager(this)
        binding.familyList.adapter = adapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.ivCreateFamily.setColorFilter(mainColor)
        binding.tvCreateFamily.setTextColor(mainColor)
        binding.ivJoinFamily.setColorFilter(mainColor)
        binding.tvJoinFamily.setTextColor(mainColor)
    }

    private fun checkViewState() {
        if (HomeManager.isSupportHomeShare()) {
            binding.joinFamily.visibility = View.VISIBLE
            binding.line.visibility = View.VISIBLE
        } else {
            binding.joinFamily.visibility = View.GONE
            binding.line.visibility = View.GONE
        }
    }

    private fun initEvent() {
        binding.createFamily.setOnClickListener {
            createFamily()
        }

        binding.toolbar.left { finish() }

        binding.joinFamily.setOnClickListener {
            joinHomeDialog.show()
        }
    }

    private fun getHomeList() {
        viewModel.launchMain {
            viewModel.findHomesPaging().collect {
                adapter.submitData(it)
            }
        }
    }

    private fun createFamily() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_family_name))
            .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))

            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, inputText ->
                val inputName = inputText?.trim()
                if (inputName.isNullOrEmpty()) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
                    return@rightButton
                } else if (inputName.length > MXDialog.maxLength) {
                    showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
                    return@rightButton
                }
                dialog.cancel()
                showLoading()
                viewModel.createFamilyFlow(inputName, listOf())
                    .launchAndCollectIn(
                        this,
                        Lifecycle.State.CREATED,
                        loadFlow<HomeBean>(success = {
                            dismissLoading()
                            getHomeList()
                        }, error = { code, msg, _ ->
                            dismissLoading()
                            val netErr = onException(code)
                            if (!netErr) showToast("$msg")
                        })
                    )
            }.create().show()
    }

    private fun setCurrentHome(homeId: Int) {
        showLoading()
        viewModel.setCurrentHome(homeId).launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlow<Any>(
                success = {
                    dismissLoading()
                    isRefreshHome = true
                },
                error = { code, msg, _ ->
                    dismissLoading()
                    val httpErr = onException(code)
                    if (!httpErr)
                        showToast("$msg")
                }
            )
        )
    }

    private fun joinByInviteCode() {
        RouterManager.navigation(
            RouterConstants.JOIN_HOME_BY_INVITE_CODE_ACTIVITY,
            null,
            this,
            launch
        )
    }

    private fun openPermissionDialog() {
        MXDialog.Builder(this)
            .title(getString(com.mxchip.mxapp.base.R.string.mx_permission_camera_denied))
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_permission_camera_denied_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_disagree)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_agree)) { alertDialog, _ ->
                alertDialog.dismiss()
                XXPermissions.with(this)
                    .permission(Permission.CAMERA)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                            if (all) MxRouter.build(RouterConstants.QR_SCAN_ACTIVITY)
                                .navigation(this@HomeManageActivity, scanLaunch)
                        }

                        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                            if (never) MXLog.i("cxk", "permanentlyDenied")
                        }
                    })
            }.create().show()
    }

    override fun finish() {
        isRefreshHome?.apply {
            LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
        }
        super.finish()
    }
}