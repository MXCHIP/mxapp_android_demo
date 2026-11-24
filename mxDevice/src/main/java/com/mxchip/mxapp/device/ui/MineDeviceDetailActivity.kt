package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.device.adapter.MineDeviceSharedUserAdapter
import com.mxchip.mxapp.device.databinding.DeviceAcMineDeviceDetailBinding
import com.mxchip.mxapp.device.viewmodel.ShareViewModel
import com.mxchip.router.annotation.Router

/**
 * 设备分享，我的设备列表
 */
@Router(RouterConstants.MINE_DEVICE_DETAIL_ACTIVITY)
class MineDeviceDetailActivity : MXBusinessActivity<DeviceAcMineDeviceDetailBinding>() {
    override fun getLayoutBinding() = DeviceAcMineDeviceDetailBinding.inflate(layoutInflater)

    private val viewModel by viewModels<ShareViewModel>()
    private lateinit var iotId: String
    private lateinit var deviceName: String

    private val adapter by lazy {
        MineDeviceSharedUserAdapter(this).apply {
            setItemClickListener {
                MXDialog.Builder(this@MineDeviceDetailActivity)
                    .title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                    .content(getString(com.mxchip.mxapp.base.R.string.mx_unbind_user_des))
                    .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                        dialog.cancel()
                    }.rightButton(
                        getString(com.mxchip.mxapp.base.R.string.mx_confirm),
                        { dialog, _ ->
                            dialog.cancel()
                            dismissLoading()
                            viewModel.mineDeviceRemoveSharedUsers(iotId, arrayListOf(it.user_id))
                                .launchAndCollectIn(
                                    this@MineDeviceDetailActivity,
                                    Lifecycle.State.CREATED,
                                    loadFlow<Any>(
                                        success = {
                                            dismissLoading()
                                            this.refresh()
                                        },
                                        error = { code, msg, data ->
                                            dismissLoading()
                                            showToast(msg.toString())
                                        })
                                )
                        }).create().show()
            }
        }
    }

    override fun onInit() {
        iotId = intent.getStringExtra(CommonConstants.KEY_IOT_ID)
            ?: throw NullPointerException("iotId is null")
        deviceName =
            intent.getStringExtra("deviceName") ?: throw NullPointerException("deviceName is null")
        initView()
        initEvent()
        fetchSharedUserList(iotId)
    }

    private fun initView() {
        binding.mineDeviceUserList.layoutManager = LinearLayoutManager(this)
        binding.mineDeviceUserList.addItemDecoration(RVItemDecorationSpace(top = 8.dp2px()))
        binding.mineDeviceUserList.adapter = adapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.setIconColor(mainColor)
        binding.addShare.setBtnBackgroundColor(mainColor)
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }.title(deviceName)

        adapter.addLoadStateListener {
            if (it.refresh is LoadState.NotLoading || it.refresh is LoadState.Error) {
                if (adapter.itemCount == 0) {
                    binding.mineDeviceUserList.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.mineDeviceUserList.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                }
            }
        }

        binding.addShare.setOnClickListener {
            if (RoleManager.isUser()) {
                RoleManager.showDialog(this)
                return@setOnClickListener
            }

            RouterManager.navigation(
                RouterConstants.DEVICE_SHARE_ACCOUNT_ACTIVITY, mapOf(
                    "iotids" to arrayListOf(iotId)
                )
            )
        }
    }

    private fun fetchSharedUserList(iotId: String) {
        viewModel.launchMain {
            viewModel.fetchSharedUserList(iotId).collect {
                adapter.submitData(it)
            }
        }
    }
}