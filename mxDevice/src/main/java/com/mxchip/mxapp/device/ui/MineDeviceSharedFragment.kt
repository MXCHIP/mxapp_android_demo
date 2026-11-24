package com.mxchip.mxapp.device.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessFragment
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.device.adapter.ShareDeviceAdapter
import com.mxchip.mxapp.device.bean.ShareDeviceListBean
import com.mxchip.mxapp.device.consts.ShareConstants
import com.mxchip.mxapp.device.databinding.DeviceFrShareDeviceBinding
import com.mxchip.mxapp.device.viewmodel.ShareViewModel
import com.mxchip.utils.LiveDataBus

class MineDeviceSharedFragment : MXBusinessFragment<DeviceFrShareDeviceBinding>() {

    override fun getLayoutBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = DeviceFrShareDeviceBinding.inflate(inflater)

    private val viewModel by viewModels<ShareViewModel>()
    private var selectList: ArrayList<ShareDeviceListBean>? = null

    private val adapter by lazy {
        ShareDeviceAdapter(requireContext(), ShareConstants.TYPE_MINE_DEVICE).apply {
            setItemClickListener { type, isEdit, shareDeviceListBean, arrayList ->
                if (type == ShareConstants.TYPE_MINE_DEVICE) {
                    if (isEdit) {
                        selectList = arrayList
                    } else
                        toMineDeviceDetail(shareDeviceListBean)
                } else {
                    unbindShareDevice(shareDeviceListBean)
                }
            }
        }
    }

    override fun onInit(view: View, savedInstanceState: Bundle?) {
        binding.mineDeviceList.background =
            generateShape(
                requireContext().getColor(com.mxchip.mxapp.base.R.color.core_color_background_list),
                16
            )
        binding.mineDeviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.mineDeviceList.adapter = adapter
        val mainColor = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.content(getString(com.mxchip.mxapp.base.R.string.mx_my_device_no_data))
        binding.emptyView.setIconColor(mainColor)
        initEvent()
        fetchList()
    }

    private fun fetchList() {
        if (RoleManager.isUser()) {
            binding.mineDeviceList.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            viewModel.launchMain {
                viewModel.fetchShareDeviceList(
                    ShareConstants.TYPE_MINE_DEVICE,
                    AppConfigManager.getUseHomeId()
                ).collect {
                    adapter.submitData(it)
                }
            }
        }
    }

    private fun initEvent() {
        adapter.addLoadStateListener {
            when (it.refresh) {
                is LoadState.Error -> {
                    ToastUtil.showToast(
                        requireContext(),
                        (it.refresh as LoadState.Error).error.message.toString()
                    )
                    binding.mineDeviceList.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                }

                is LoadState.NotLoading -> {
                    if (adapter.itemCount == 0) {
                        binding.mineDeviceList.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.mineDeviceList.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                    }
                }

                else -> {}
            }
        }
    }

    fun refresh() {
        adapter.refresh()
    }

    fun edit(edit: Boolean) {
        adapter.notifyEdit(edit)
    }

    fun getSelectList() = selectList

    private fun toMineDeviceDetail(shareDeviceListBean: ShareDeviceListBean) {
        RouterManager.navigation(
            RouterConstants.MINE_DEVICE_DETAIL_ACTIVITY, mapOf(
                CommonConstants.KEY_IOT_ID to shareDeviceListBean.iotid,
                "deviceName" to shareDeviceListBean.name
            )
        )
    }

    private fun unbindShareDevice(shareDeviceListBean: ShareDeviceListBean) {
        MXDialog.Builder(requireContext())
            .title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_del_share_device_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.cancel()
            }
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                alertDialog.cancel()
                showLoading()
                viewModel.unbindShareDevice(arrayListOf(shareDeviceListBean.iotid))
                    .launchAndCollectIn(
                        this, Lifecycle.State.CREATED, loadFlow<Any>(
                            success = {
                                dismissLoading()
                                LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                                    .postValue(
                                        HomeDataRefresh.SHARED_DEVICE
                                    )
                                ToastUtil.showToast(
                                    requireContext(),
                                    getString(com.mxchip.mxapp.base.R.string.mx_del_success)
                                )
                                adapter.refresh()
                            },
                            error = { code, msg, _ ->
                                dismissLoading()
                                val isNetErr = onException(code)
                                if (!isNetErr) ToastUtil.showToast(requireContext(), msg.toString())
                            }
                        ))
            }.create().show()
    }
}