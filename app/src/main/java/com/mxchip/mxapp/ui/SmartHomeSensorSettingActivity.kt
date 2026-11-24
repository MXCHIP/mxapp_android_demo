package com.mxchip.mxapp.ui

import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.R
import com.mxchip.mxapp.adapter.SmartHomeSensorSettingAdapter
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.widget.RVItemDecorationSpace
import com.mxchip.mxapp.bean.SmartHomePropertiesBean
import com.mxchip.mxapp.consts.SmartHomeConstants
import com.mxchip.mxapp.databinding.ActivitySmarthomeSensorSettingBinding
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.viewmodel.MainViewModel
import com.mxchip.router.MxRouter
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus

/**
 * 全屋房间内 各类传感器设置
 */
@Router(RouterConstants.SMART_HOME_SENSOR_SETTING_ACTIVITY)
class SmartHomeSensorSettingActivity : MXBusinessActivity<ActivitySmarthomeSensorSettingBinding>() {

    override fun getLayoutBinding() = ActivitySmarthomeSensorSettingBinding.inflate(layoutInflater)

    private val mainVM by viewModels<MainViewModel>()
    private val adapter by lazy { SmartHomeSensorSettingAdapter() }

    override fun onInit() {
        val roomId = intent.getIntExtra(RoomConstants.KEY_ROOM_ID, -1)
        if (roomId == -1) {
            finish()
            return
        }
        binding.toolbar.left {
            finish()
        }
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.addItemDecoration(RVItemDecorationSpace(bottom = 10.dp2px()))
        binding.rvDevice.adapter = adapter

        val identifier = intent.getStringExtra("identifier")!!
        //类型：0-温度；1 - 湿度；2 - pm2.5;  3 - 人存
        val type = when (identifier) {
            "CurrentTemperature" -> {
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_whole_house_temperature))
                binding.tvTypeTitle.setText(com.mxchip.mxapp.base.R.string.mx_whole_house_temperature_header_title)
                binding.tvTypeDesc.setText(com.mxchip.mxapp.base.R.string.mx_whole_house_temperature_header_des)
                0
            }

            "CurrentHumidity" -> {
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_whole_house_humidity))
                binding.tvTypeTitle.setText(com.mxchip.mxapp.base.R.string.mx_whole_house_humidity_header_title)
                binding.tvTypeDesc.setText(com.mxchip.mxapp.base.R.string.mx_whole_house_humidity_header_des)
                1
            }

            "OccupancyStatus" -> {
                binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_whole_house_occupancy))
                binding.tvTypeTitle.setText(com.mxchip.mxapp.base.R.string.mx_whole_house_occupancy_header_title)
                binding.tvTypeDesc.setText(com.mxchip.mxapp.base.R.string.mx_whole_house_occupancy_header_des)
                3
            }

            else -> -1
        }
        adapter.setItemClick {
            val iotId = adapter.getData()[it].iotid
            adapter.setSelectedIotId(iotId)
            adapter.notifyDataSetChanged()
            settingUseSensor(roomId, type, iotId)
        }
        val iotId = intent.getStringExtra(CommonConstants.KEY_IOT_ID)
        adapter.setSelectedIotId(iotId)

        initData(roomId, type)
        binding.tvAdd.setOnClickListener {
            MxRouter.build(RouterConstants.PRODUCT_ACTIVITY).navigation()
        }
    }

    private fun initData(roomId: Int, type: Int) {
        mainVM.findRoomSensorList(roomId, type)
            .launchAndCollectIn(this,
                Lifecycle.State.CREATED,
                loadFlowList<SmartHomePropertiesBean>(
                    success = {
                        if (it.isNullOrEmpty())
                            showEmptyView()
                        else {
                            adapter.setData(it)
                        }
                    },
                    error = { code, msg, _ ->
                        val netErr = onException(code)
                        if (!netErr) showToast("$msg")
                        showEmptyView()
                    }
                ))
    }

    private fun settingUseSensor(roomId: Int, type: Int, iotId: String) {
        showLoading()
        mainVM.settingRoomUseSensor(roomId, type, iotId).launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlow<Any>(success = {
                dismissLoading()
                LiveDataBus.with<Int>(SmartHomeConstants.KEY_REFRESH_SMART_HOME).postValue(0)
            }, error = { code, msg, _ ->
                dismissLoading()
                val netErr = onException(code)
                if (!netErr) showToast("$msg")
            })
        )
    }

    private fun showEmptyView() {
        binding.emptyView.visibility = View.VISIBLE
        binding.tvAdd.visibility = View.VISIBLE
        binding.tvAdd.setBtnBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.app_main_color
            )
        )
    }
}