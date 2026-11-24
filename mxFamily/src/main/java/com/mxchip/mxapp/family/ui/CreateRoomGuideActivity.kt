package com.mxchip.mxapp.family.ui

import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.widget.RVGridItemDecorationSpace
import com.mxchip.mxapp.family.R
import com.mxchip.mxapp.family.adapter.CreateRoomGuideAdapter
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.databinding.FamilyAcCreateRoomGuideBinding
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus

@Router(RouterConstants.CREATE_ROOM_GUIDE_ACTIVITY)
class CreateRoomGuideActivity : MXBusinessActivity<FamilyAcCreateRoomGuideBinding>() {
    private val viewModel by viewModels<HomeViewModel>()
    override fun getLayoutBinding() = FamilyAcCreateRoomGuideBinding.inflate(layoutInflater)
    private lateinit var homeName: String

    private val adapter by lazy {
        CreateRoomGuideAdapter(this).apply {
            addRoomListener { ->
                MXDialog.Builder(this@CreateRoomGuideActivity)
                    .title(getString(com.mxchip.mxapp.base.R.string.mx_room_name))
                    .edittextHint(getString(com.mxchip.mxapp.base.R.string.mx_input_name))
                    
                    .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { dialog, _ ->
                        dialog.cancel()
                    }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { dialog, inputText ->
                        val roomName = inputText?.trim()
                        insertRoom(dialog, roomName)
                    }.create().show()
            }
        }
    }

    private fun insertRoom(dialog: AlertDialog, roomName: String?) {
        if (roomName.isNullOrEmpty()) {
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_no_input))
            return
        } else if (roomName.length > MXDialog.maxLength) {
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_invalid))
            return
        } else if (adapter.getList().contains(roomName)) {
            showToast(getString(com.mxchip.mxapp.base.R.string.mx_name_duplicate))
            return
        }
        dialog.cancel()
        adapter.add(roomName)
    }

    override fun onInit() {
        initView()
        binding.btnNext.setBtnBackgroundColor(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9), 44.dp2px())
        binding.btnNext.setOnClickListener {
            createHomeSetDefaultRoom()
        }
        homeName = intent.getStringExtra("home_name")!!
    }

    private fun initView() {
        binding.roomList.layoutManager = GridLayoutManager(this, 2)
        binding.roomList.addItemDecoration(RVGridItemDecorationSpace(16.dp2px()))
        binding.roomList.adapter = adapter
        adapter.updateList(resources.getStringArray(R.array.family_default_rooms).toList())
    }

    private fun createHomeSetDefaultRoom() {
        showLoading()
        viewModel.createFamilyFlow(homeName, adapter.getRoom())
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<HomeBean>(success = {
                dismissLoading()
                LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
                RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
                finish()
            }, error = { code, msg, _ ->
                val netErr = onException(code)
                if (!netErr) {
                    if (10021 == code) {//家庭已创建成功，重复code
                        LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
                        RouterManager.navigation(RouterConstants.MAIN_ACTIVITY)
                        finish()
                    } else showToast("$msg")
                }
            }))
    }
}