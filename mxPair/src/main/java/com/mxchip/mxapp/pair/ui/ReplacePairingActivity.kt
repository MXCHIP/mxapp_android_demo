package com.mxchip.mxapp.pair.ui

import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.bean.PairDeviceBean
import com.mxchip.mxapp.pair.consts.PairState
import com.mxchip.mxapp.pair.databinding.PairAcOneKeyReplaceBinding
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairManager
import com.mxchip.mxapp.pair.viewmodel.AppPairBusiness
import com.mxchip.mxapp.pair.viewmodel.PairSuccessViewModel
import com.mxchip.mxapp.pair.viewmodel.PairViewModel
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *一键替换，配网、同步信息
 */
@Router(RouterConstants.ONE_KEY_REPLACE_ACTIVITY)
class ReplacePairingActivity : MXBusinessActivity<PairAcOneKeyReplaceBinding>() {

    private val vm by viewModels<PairViewModel>()
    private val pairSuccessVM by viewModels<PairSuccessViewModel>()

    private var pairBean: PairDeviceBean? = null

    private var pairState = PairState.PAIR_STATE_START
    private val anima by lazy { AnimationUtils.loadAnimation(this, R.anim.pair_rotation) }

    override fun getLayoutBinding(): PairAcOneKeyReplaceBinding =
        PairAcOneKeyReplaceBinding.inflate(layoutInflater)

    override fun onInit() {
        pairBean = PairDataCenter.pairDeviceList[0]
        initView()
        inject()
        initData()
    }

    private fun initView() {
        binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_replace))
        binding.layoutNew.icon.load(pairBean!!.icon)
        binding.layoutNew.name.text = pairBean!!.name
        val descColor = ContextCompat.getColor(
            this, com.mxchip.mxapp.base.R.color.core_color_text_list_content
        )
        binding.layoutNew.tvFailed.text = pairBean!!.mac
        binding.layoutNew.tvFailed.setTextColor(descColor)
        binding.layoutNew.tvState.setText(com.mxchip.mxapp.base.R.string.mx_replace_ready)

        binding.layoutOld.icon.load(pairBean!!.icon)
        binding.layoutOld.name.text = PairDataCenter.getReplacedName()
        binding.layoutOld.tvFailed.text = PairDataCenter.getReplacedRoomName()
        binding.layoutOld.tvFailed.setTextColor(descColor)
        binding.layoutOld.tvState.setText(com.mxchip.mxapp.base.R.string.mx_replace_ready)
        updateProgress(100)
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
                        pairBean?.pairState = it
                        //记录设备配网状态
                        if (PairState.PAIR_STATE_FAIL != it)
                            pairState = it
                        when {
                            PairState.PAIR_STATE_START == it -> {
                                binding.btnReplace.setText(getString(com.mxchip.mxapp.base.R.string.mx_replace_doing))
                                    .postInvalidate()
                                //设置设备item动画
                                binding.layoutNew.ivState.setImageResource(R.drawable.pair_ic_loading)
                                binding.layoutNew.ivState.animation = anima
                            }

                            PairState.PAIR_STATE_FAIL == it -> {
                                //执行断开，避免proxy连接成功，配网后续步骤失败，造成mesh连接状态显示异常
                                MXMesh.disConnect()
                                binding.btnReplace.setProgress(100)
                                    .setText(getString(com.mxchip.mxapp.base.R.string.mx_back))
                                    .postInvalidate()
                                binding.layoutNew.ivState.clearAnimation()
                                binding.layoutNew.ivState.setImageResource(R.drawable.pair_ic_exclamation_point)
                                binding.layoutNew.root.setOnClickListener {
                                    toFailedPage()
                                }
                                binding.layoutNew.tvFailed.setText(com.mxchip.mxapp.base.R.string.mx_sync_fail)
                                binding.layoutNew.tvFailed.setTextColor(
                                    ContextCompat.getColor(
                                        this@ReplacePairingActivity,
                                        com.mxchip.mxapp.base.R.color.core_color_error
                                    )
                                )
                            }

                            PairState.PAIR_STATE_SUCCESS == it -> {
                                val uuid = pairBean?.extBluetoothDevice?.uuid
                                if (uuid.isNullOrEmpty() || !MXMesh.isConnected()) {
                                    successful()
                                } else {
                                    val node = MXMesh.getNode(uuid)
                                    if (node != null) {
                                        MXMesh.getSupportProxyState(uuid) { meshState, _ ->
                                            successful()
                                            if (meshState != MeshState.SUCCESS) {
                                                MXMesh.disConnect()
                                            }
                                        }
                                    } else {
                                        MXMesh.disConnect()
                                    }
                                }
                            }

                            else -> {
                                updateProgress((it.ordinal + 1) * 15 + 15)
                            }
                        }
                    }
                }
            }
        }
        binding.btnReplace.setOnClickListener {
            pairBean?.apply {
                if (null == this.pairState) {
                    binding.layoutOld.tvState.visibility = View.GONE
                    binding.layoutOld.ivState.setImageResource(R.drawable.pair_ic_successful)
                    binding.layoutNew.tvState.visibility = View.GONE
                    vm.starPair(this)
                } else backPressed()
            }
        }
    }

    private fun successful() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.layoutNew.ivState.clearAnimation()
            binding.layoutNew.ivState.setImageResource(R.drawable.pair_ic_successful)
            binding.btnReplace.setProgress(100)
                .setText(getString(com.mxchip.mxapp.base.R.string.mx_done))
                .postInvalidate()
        }
    }


    private fun initData() {
        //智家精灵获取替换设备规则时，不会返回所在房间的mesh地址，需要单独处理
        val roomId = PairDataCenter.getReplacedRoomId()
        if (roomId != null) {
            if (AppConfigManager.getUseHomeId() != 0) {
                pairSuccessVM.roomListV4(
                    AppConfigManager.getUseHomeId(),
                    pageNo = 1,
                    pageSize = 199
                ).launchAndCollectIn(
                    this,
                    Lifecycle.State.CREATED,
                    loadFlowList<RoomBean>(success = { roomsData ->
                        roomsData?.firstOrNull { it.room_id == roomId }?.apply {
                            PairManager.setFromRoomInfo(roomId, this.room_address)
                        }
                    })
                )
            }
        }
    }

    private fun toFailedPage() {
        RouterManager.navigation(
            RouterConstants.PAIR_FAILED_ACTIVITY, mapOf(
                "icon" to pairBean!!.icon,
                "name" to pairBean!!.name,
                "state.ordinal" to pairState.ordinal,
            )
        )
    }

    private fun updateProgress(progress: Int) {
        binding.btnReplace.setProgress(progress).postInvalidate()
    }

    private fun backDialog() {
        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
            .content(getString(com.mxchip.mxapp.base.R.string.mx_replace_cancel_des))
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.cancel()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                alertDialog.dismiss()
                MXMesh.disConnect()
                finish()
            }.create().show()
    }

    private fun checkGroupSubscribe() {
        if (MXMesh.isConnected())
            MeshSDKManage.subscribeGroups {}
    }

    private fun backPressed() {
        when (pairBean?.pairState) {
            null, PairState.PAIR_STATE_FAIL -> {
                MXMesh.disConnect()
                finish()
            }

            PairState.PAIR_STATE_SUCCESS -> {
                checkGroupSubscribe()
                //未设置配网，默认mxapp配置
                val pariBusiness = PairManager.getPairBusiness()
                if (pariBusiness == null)
                    PairManager.setPairBusiness(AppPairBusiness())
                RouterManager.navigation(
                    PairManager.getPairBusiness()!!.getReplaceSuccessfulRoute()
                )
            }

            else -> backDialog()
        }
    }

    override fun onDestroy() {
        if (MXMesh.isConnected()) {
            LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA).postValue(
                mapOf(
                    DeviceConstants.KEY_MESH_STATE to true
                )
            )
        }
        //清除mesh配网产生的数据
        MXMesh.destroyProvisioningData()
        PairManager.setFromRoomInfo(null, null)
        super.onDestroy()
    }
}