package com.mxchip.mxapp.pair.ui

import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.link.utils.LinkLogger
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.adapter.PairingAdapter
import com.mxchip.mxapp.pair.consts.PairState
import com.mxchip.mxapp.pair.databinding.PairAcPairingBinding
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.mxapp.pair.utils.PairWiFiSPManage
import com.mxchip.mxapp.pair.viewmodel.AppPairBusiness
import com.mxchip.mxapp.pair.viewmodel.PairViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.scanner.MxScan
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshState
import com.mxchip.sdk.nordicmesh.utils.MeshParserUtils
import com.mxchip.sdk.nordicmesh.utils.SecureUtils
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.isVisible

/**
 *配网中
 */
@Router(RouterConstants.PAIRING_ACTIVITY)
class PairingActivity : MXBusinessActivity<PairAcPairingBinding>() {
    companion object {
        private const val TAG = "PairingActivity"
    }

    private val vm by viewModels<PairViewModel>()

    private val adapter by lazy { PairingAdapter() }

    private var currentPairPosition = 0

    private var bindSuccessCount = 0

    //key=position
    private val pairStateMap by lazy { mutableMapOf<Int, PairState>() }

    override fun getLayoutBinding(): PairAcPairingBinding =
        PairAcPairingBinding.inflate(layoutInflater)

    override fun onInit() {
        if (PairDataCenter.pairDeviceList.isEmpty()) {
            MXLog.w(TAG, "没有设备可以配网")
            finish()
            return
        }
        MXMesh.disConnect()
        //将linkSDK的日志打印
        LinkLogger.setLogHandler { priority, tag, message ->
            MXLog.println(priority, tag, message)
        }
        binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_add_device))
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.adapter = adapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvContinueAdd.setBtnStokeColor(mainColor)
        binding.tvContinueAdd.setTextColor(mainColor)
        binding.tvNext.setBtnBackgroundColor(mainColor)
        inject()
        adapter.setData(PairDataCenter.pairDeviceList)
        binding.tvStatusDec.text =
            String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_provisioning_des),
                adapter.itemCount,
                0
            )
        vm.starPair(adapter.getData()[currentPairPosition])
    }

    private fun inject() {
        onBackPressedDispatcher.addCallback {
            backPressed()
        }
        binding.toolbar.left {
            backPressed()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.pairState.collect {
                    it?.apply {
                        val pairBean = adapter.getData()[currentPairPosition]
                        pairBean.pairState = it
                        adapter.notifyItemChanged(currentPairPosition)
                        //记录设备配网状态
                        if (PairState.PAIR_STATE_FAIL != it) {
                            pairStateMap[currentPairPosition] = it
                        }
                        when {
                            PairState.PAIR_STATE_START == it -> {
                                binding.rvDevice.scrollToPosition(currentPairPosition)
                            }

                            PairState.PAIR_STATE_SUCCESS == it -> {
                                bindSuccessCount++
                                binding.tvStatusDec.text = String.format(
                                    getString(com.mxchip.mxapp.base.R.string.mx_provisioning_des),
                                    adapter.itemCount,
                                    bindSuccessCount
                                )
                                nextDevicePair()
                            }

                            PairState.PAIR_STATE_FAIL == it -> {
                                //执行断开，避免proxy连接成功，配网后续步骤失败，造成mesh连接状态显示异常
                                MXMesh.disConnect()
                                nextDevicePair()
                            }
                        }
                    }
                }
            }
        }
        adapter.setItemClick { pos ->
            if (pairStateMap.containsKey(pos)) {
                val pairBean = adapter.getData()[pos]
                RouterManager.navigation(
                    RouterConstants.PAIR_FAILED_ACTIVITY, mapOf(
                        "icon" to pairBean.icon,
                        "name" to pairBean.name,
                        "state.ordinal" to pairStateMap[pos]!!.ordinal
                    )
                )
            }
        }
        binding.tvContinueAdd.setOnClickListener {
            //点击继续添加后，再次设置不允许连接proxy
            MeshSDKManage.isConnectProxy = false
            backPressed()
        }
        binding.tvNext.setOnClickListener {
            if (bindSuccessCount > 0) {
                //未设置配网，默认mxapp配置
                val pariBusiness = PairManager.getPairBusiness()
                if (pariBusiness == null)
                    PairManager.setPairBusiness(AppPairBusiness())
                val pairSuccessful = PairManager.getPairBusiness()!!.getPairSuccessfulRoute()
                RouterManager.navigation(pairSuccessful)
            }
            finish()
        }
    }

    private fun nextDevicePair() {
        if (isFinishing) return
        if (adapter.itemCount > currentPairPosition + 1) {
            currentPairPosition++
            vm.starPair(adapter.getData()[currentPairPosition])
        } else {
            //配网结束
            MeshSDKManage.isConnectProxy = true
            if (MXMesh.isConnected()) {
                val mxble = adapter.getData().lastOrNull()?.extBluetoothDevice
                val uuid = mxble?.uuid
                if (null != uuid) {
                    MXMesh.getSupportProxyState(uuid) { meshState, _ ->
                        if (meshState == MeshState.SUCCESS) {
                            //将最后一个配网的mesh设备mac发出去
                            LiveDataBus.with<String>(LiveDataBusKey.KEY_MESH_PAIR_LAST_MAC)
                                .postValue(mxble.getDevice().address)
                            CoroutineScope(Dispatchers.IO).launch {
                                MeshSDKManage.subscribeGroups {
                                    if (it)
                                        sendConnectState(true)
                                    else {
                                        sendConnectState(false)
                                        MXMesh.disConnect()
                                        MxScan.startScanMeshProxy()
                                    }
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        binding.layoutAddedAction.visibility = View.VISIBLE
                                    }
                                }
                            }
                        } else {
                            sendConnectState(false)
                            MXMesh.disConnect()
                            MxScan.startScanMeshProxy()
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.layoutAddedAction.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    sendConnectState(false)
                    MXMesh.disConnect()
                    MxScan.startScanMeshProxy()
                    binding.layoutAddedAction.visibility = View.VISIBLE
                }
            } else {
                MxScan.startScanMeshProxy()
                binding.layoutAddedAction.visibility = View.VISIBLE
            }
        }
    }

    private fun sendConnectState(state: Boolean) {
        LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA).postValue(
            mapOf(
                DeviceConstants.KEY_MESH_STATE to state
            )
        )
    }

    private fun backDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_provision_back_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                alertDialog.dismiss()
                backNotifyData()
                MXMesh.disConnect()
                finish()
            }.create().show()
    }

    private fun backNotifyData() {
        if (bindSuccessCount > 0) {
            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                .postValue(HomeDataRefresh.DEVICE)
        }
    }

    private fun backPressed() {
        if (binding.layoutAddedAction.isVisible) {
            backNotifyData()
            finish()
        } else {
            backDialog()
        }
    }

    override fun onDestroy() {
        PairDataCenter.pairDeviceList.clear() // 清除配网数据
        pairStateMap.clear()
        //清除mesh配网产生的数据
        MXMesh.destroyProvisioningData()
        vm.stopPair()
        super.onDestroy()
    }
}