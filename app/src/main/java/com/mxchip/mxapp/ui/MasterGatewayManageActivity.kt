package com.mxchip.mxapp.ui

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.adapter.MasterGatewayAdapter
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVLinearItemDecorationSpace
import com.mxchip.mxapp.databinding.ActivityMasterGatewayManageBinding
import com.mxchip.mxapp.device.vm.DeviceViewModel
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router

/**
 * 家庭主网关设置管理
 */
@Router(path = RouterConstants.MASTER_GATEWAY_MANAGE_ACTIVITY)
class MasterGatewayManageActivity : MXBusinessActivity<ActivityMasterGatewayManageBinding>() {

    override fun getLayoutBinding() = ActivityMasterGatewayManageBinding.inflate(layoutInflater)

    private val deviceVM by viewModels<DeviceViewModel>()
    private val homeVM by viewModels<HomeViewModel>()

    private val adapter by lazy { MasterGatewayAdapter() }
    private val familyPop by lazy { MasterGatewayFamilyPopupWindow(this) }
    private var useHomeId = 0
    override fun onInit() {
        binding.toolbar.left {
            finish()
        }
        adapter.setItemClick {
            updateMasterGateway(it)
        }
        familyPop.setFamilyExchangeClick { homeBean ->
            familyPop.dismiss()
            if (homeBean.home_id != useHomeId) {
                updateGatewayTitle(homeBean.home_id, homeBean.name)
            }
        }
        binding.tvFamilyName.setOnClickListener {
            familyPop.show()
            findHomes()
        }
        if (RoleManager.isUser().not()) {
            updateGatewayTitle(AppConfigManager.getUseHomeId(), AppConfigManager.getUseHomeName())
        } else {
            findHomes(true)
        }
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.addItemDecoration(RVLinearItemDecorationSpace(10.dp2px()))
        binding.rvDevice.adapter = adapter
    }

    private fun updateGatewayTitle(homeId: Int, name: String) {
        useHomeId = homeId
        binding.tvFamilyName.text = name
        getGatewayList()
    }

    private fun findHomes(isUseFirst: Boolean = false) {
        homeVM.findHomes().launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlowList<HomeBean>(
                success = { homes ->
                    val newList = homes?.filter { it.role != RoleConstants.COMMON }
                    if (newList.isNullOrEmpty().not()) {
                        if (isUseFirst) {
                            updateGatewayTitle(newList!![0].home_id, newList[0].name)
                        }
                        familyPop.setData(newList)
                    }
                }, error = { code: Int, msg: String?, data: Any? ->
                    showToast("$msg")
                })
        )
    }


    private fun getGatewayList() {
        deviceVM.findDevices(
            mapOf(
                "home_id" to useHomeId,
                "is_gateway" to 1,
                "page_size" to 50,
                "page_no" to 1
            )
        ).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlowList<DeviceBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.tvGatewayTitle.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.tvGatewayTitle.visibility = View.VISIBLE
                    }
                    adapter.setData(it)
                }, error = { code: Int, msg: String?, data: Any? ->
                    binding.emptyView.visibility = View.VISIBLE
                    binding.tvGatewayTitle.visibility = View.GONE
                    val netErr = onException(code)
                    if (!netErr) showToast("$msg")
                })
        )
    }

    private fun updateMasterGateway(pos: Int) {
        val deviceBean = adapter.getData()[pos]
        showLoading()
        deviceVM.updateMasterGateway(
            useHomeId,
            deviceBean.iotid
        ).launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlow<Any>(success = {
                dismissLoading()
                showToast(getString(com.mxchip.mxapp.base.R.string.mx_update_success))
                adapter.setMasterGatewayIotId(deviceBean.iotid)
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            })
        )
    }
}