package com.mxchip.mxapp.family.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.location.LocationHelper
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.family.adapter.LocationAdapter
import com.mxchip.mxapp.family.bean.LocationBean
import com.mxchip.mxapp.family.databinding.FamilyAcLocationBinding
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.utils.LiveDataBus

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/11/24
 * @desc:
 */
@Router(RouterConstants.LOCATION_ACTIVITY)
class LocationActivity : MXBusinessActivity<FamilyAcLocationBinding>() {
    override fun getLayoutBinding() = FamilyAcLocationBinding.inflate(layoutInflater)

    private val viewModel by viewModels<HomeViewModel>()
    private var homeId = -1

    @SuppressLint("MissingPermission")
    private val adapter = LocationAdapter(this).apply {
        setItemClickListener { locationBean ->
            locationBean?.apply {
                viewModel.updateHomeLocation(
                    homeId,
                    locationBean.country_name,
                    locationBean.province_name,
                    locationBean.city_name,
                    locationBean.location_key
                ).launchAndCollectIn(
                    this@LocationActivity, Lifecycle.State.CREATED, loadFlow<Any>(success = {
                        LiveDataBus.with<LocationBean>(HomeConstants.LOCATION_SELECTED)
                            .postValue(this)
                    })
                )
                finish()
            }
        }
    }

    override fun onInit() {
        initView()
        initEvent()
        initLocation()
    }

    private val block: (Location?) -> Unit = {
        it?.apply { geoToLocation(this.latitude.toString(), this.longitude.toString()) }
    }

    @SuppressLint("MissingPermission")
    private fun initLocation() {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) || PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_permission_location_denied))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_permission_location_denied_des))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                    alertDialog.cancel()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_go_setting), { alertDialog, _ ->
                    alertDialog.cancel()
                    XXPermissions.with(this).permission(
                        Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION
                    ).request(object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>, all: Boolean
                        ) {
                            if (all) LocationHelper.location(block)
                        }

                        override fun onDenied(
                            permissions: MutableList<String>, never: Boolean
                        ) {
                            if (never) {
                                XXPermissions.startPermissionActivity(
                                    this@LocationActivity, permissions
                                )
                            }
                        }
                    })
               }).create().show()
        } else {
            LocationHelper.location(block)
        }
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }

        binding.keyword.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                fetchLocation(binding.keyword.text.toString())
            }
            false
        }
    }

    private fun initView() {
        homeId = intent.getIntExtra(HomeConstants.KEY_HOME_ID, -1)
        binding.keyword.background = generateStoke(2, getColor(com.mxchip.mxapp.base.R.color.core_color_dividers), 20)
        binding.locationList.layoutManager = LinearLayoutManager(this)
        binding.locationList.adapter = adapter
    }

    private fun geoToLocation(lat: String, lon: String) {
        viewModel.geoToLocation(lat, lon)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlow<LocationBean>(success = {
                it?.apply {
                    adapter.locationSuccess(it)
                }
            }, error = { code, msg, data ->
                showToast(msg.toString())
            }))
    }

    private fun fetchLocation(keyword: String) {
        if (keyword.isBlank()) {
            return
        }
        showLoading()
        viewModel.fetchLocationByKeyword(keyword.trim()).launchAndCollectIn(
            this, Lifecycle.State.CREATED, loadFlowList<LocationBean>(success = {
                dismissLoading()
                it?.apply {
                    adapter.searched(this)
                }
            }, error = {_,_,_->
                dismissLoading()
            })
        )
    }
}