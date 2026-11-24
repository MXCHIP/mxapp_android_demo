package com.mxchip.mxapp.pair.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.base.MXBusinessFragment
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.PermissionUtil
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.widget.RVLinearItemDecorationSpace
import com.mxchip.mxapp.pair.adapter.AutoFindAdapter
import com.mxchip.mxapp.pair.consts.PairConstants
import com.mxchip.mxapp.pair.databinding.PairFrAutoFindBinding
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairUtils
import com.mxchip.mxapp.pair.viewmodel.PairViewModel
import com.mxchip.scanner.MxScan
import com.mxchip.sdk.mesh.MXMesh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


/**
 *自动发现设备
 */
class AutoFindFragment : MXBusinessFragment<PairFrAutoFindBinding>() {
    companion object {
        private const val TAG = "AutoFindFragment"
        private const val ALLOWED_DEVICES_TOTAL = 30
    }

    private val adapter by lazy { AutoFindAdapter(requireContext()) }

    private val locationManager by lazy {
        MXBusinessApplication.mxBusinessApp.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
    }
    private var locationListener: LocationListener? = null

    private val vm by viewModels<PairViewModel>()

    private var activityStarted = true

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (it.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR
                        )) {
                            BluetoothAdapter.STATE_ON -> {
                                if (activityStarted) {
                                    MXLog.d(TAG, "手机蓝牙打开")
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        delay(500)
                                        withContext(Dispatchers.Main) {
                                            checkPermission()
                                        }
                                    }
                                }
                            }

                            BluetoothAdapter.STATE_TURNING_OFF -> {
                                if (activityStarted) {
                                    MXLog.d(TAG, "手机蓝牙正在关闭")
                                    MxScan.stopScan()
                                    MXMesh.disConnect()
                                }
                            }

                            BluetoothAdapter.STATE_OFF -> {
                                if (activityStarted) {
                                    MXLog.d(TAG, "手机蓝牙已关闭")
                                    checkPermission()
                                }
                            }
                        }
                    }

                    LocationManager.PROVIDERS_CHANGED_ACTION -> {
                        if (activityStarted) checkPermission()
                    }
                }
            }
        }

    }

    override fun getLayoutBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = PairFrAutoFindBinding.inflate(layoutInflater)

    override fun onInit(view: View, savedInstanceState: Bundle?) {
        initView()
        inject()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                activityStarted = true
                checkPermission()
                vm.registerScanProvisioningData()
            }
        }
    }

    override fun onStop() {
        activityStarted = false
        //停止发现
        MxScan.stopScan()
        vm.stopDiscoveryDevice()
        binding.waterWave.pause()
        binding.waterWave.visibility = View.GONE
        super.onStop()
    }

    private fun clearDataAndNotify() {
        PairDataCenter.pairSelectedDeviceList.clear()
        //清空发现列表
        if (!adapter.getData().isNullOrEmpty()) {
            adapter.clearData()
        }
        vm.clearFoundDeviceFilterData()
        changeAddDeviceUIState()
    }

    /**
     * 检查必要权限、蓝牙开关
     */
    @SuppressLint("MissingPermission")
    private fun checkPermission() {
        //清除列表数据
        clearDataAndNotify()
        PermissionUtil.checkFindBlePermission(requireContext().applicationContext) { locationPermissionState, locationSwitchState, bluePermissionState, blueSwitchState ->
            if (bluePermissionState && blueSwitchState) {
                binding.layoutOpenBle.visibility = View.GONE
            } else {
                binding.layoutOpenBle.visibility = View.VISIBLE
                isGrantedBluePermission = bluePermissionState
            }
            if (locationPermissionState && locationSwitchState) {
                binding.layoutOpenLocation.visibility = View.GONE
            } else {
                binding.layoutOpenLocation.visibility = View.VISIBLE
                isGrantedLocationPermission = locationPermissionState
            }

            if (locationPermissionState && locationSwitchState && bluePermissionState && blueSwitchState) {
                permissionGranted()
                //扫描额外的fog、阿里蓝牙设备
                MxScan.startScan(
                    listOf(
                        MxScan.SERVICE_MESH_PROVISIONING,
                        MxScan.SERVICE_MX,
                        UUID.fromString("000001A8-0000-1000-8000-00805F9B34FB")
                    )
                )
            } else {
                permissionDenied()
                MxScan.stopScan()
            }
            binding.tvSearchTip.visibility = View.VISIBLE
            binding.tvStateTip.visibility = View.VISIBLE
            binding.layoutSelect.visibility = View.GONE
        }
    }

    /**
     * 权限被允许
     */
    private fun permissionGranted() {
        binding.tvSearchTip.text = getString(com.mxchip.mxapp.base.R.string.mx_provision_searching)
        binding.tvStateTip.text =
            getString(com.mxchip.mxapp.base.R.string.mx_provision_ensure_device_state)
        binding.waterWave.setColor(ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9))
        val tag = binding.waterWave.tag
        if (tag == 1) binding.waterWave.resume()
        else {
            binding.waterWave.start()
            binding.waterWave.tag = 1
        }
        binding.waterWave.visibility = View.VISIBLE
        //android12可能未获取到位置权限，需要单独检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionState = XXPermissions.isGranted(
                MXBusinessApplication.mxBusinessApp,
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_COARSE_LOCATION
            )
            if (permissionState) {
                if (PermissionUtil.isLocationEnable(MXBusinessApplication.mxBusinessApp)) {
                    //检查初始化经纬度
                    val lat = PairDataCenter.getLocationLatitude()
                    if (lat.isNullOrEmpty()) initLocationInfo()
                }
            }
        } else {
            //检查初始化经纬度
            val lat = PairDataCenter.getLocationLatitude()
            if (lat.isNullOrEmpty()) initLocationInfo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationInfo() {
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (null == location) {
            if (null == locationListener) {
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        updateLocation(location)
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 3000, 500f, locationListener!!
                )
            }
        } else {
            updateLocation(location)
        }
    }

    private fun updateLocation(location: Location) {
        locationListener?.apply {
            locationManager.removeUpdates(this)
            locationListener = null
        }
        if (location.latitude > 0) {
            PairDataCenter.saveLocationCurrent(location.latitude, location.longitude)
        }
    }

    private fun permissionDenied() {
        binding.tvSearchTip.text =
            getString(com.mxchip.mxapp.base.R.string.mx_provision_search_prepare)
        binding.tvStateTip.text =
            getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_location)
        binding.waterWave.pause()
        binding.waterWave.visibility = View.GONE
    }

    private var isGrantedLocationPermission = false
    private var isGrantedBluePermission = false

    /**
     * 位置权限、位置开关
     */
    private fun operationLocalDialog() {
        val title = if (isGrantedLocationPermission) {
            getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off)
        } else {
            getString(com.mxchip.mxapp.base.R.string.mx_permission_location_denied)
        }
        MXDialog.Builder(requireContext()).title(title)
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_go_setting)) { alertDialog, _ ->
                alertDialog.dismiss()
                if (isGrantedLocationPermission) { //已授权，未开启位置开关
                    PermissionUtil.remindStartLocateService(requireContext())
                } else { //未授权
                    XXPermissions.with(this).permission(
                        Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION
                    ).request(object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>, all: Boolean
                        ) {
                            if (all) checkPermission()
                        }

                        override fun onDenied(
                            permissions: MutableList<String>, never: Boolean
                        ) {
                            if (never) {
                                XXPermissions.startPermissionActivity(
                                    requireActivity(), permissions
                                )
                            }
                        }
                    })
                }
            }.create().show()
    }

    /**
     * 蓝牙权限、蓝牙开关
     */
    @SuppressLint("MissingPermission")
    private fun operationBluetoothDialog() {
        val title = if (isGrantedBluePermission) {
            getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off)
        } else {
            getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_denied)
        }

        MXDialog.Builder(requireContext()).title(title)
            .content(
                String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off_des),
                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                )
            )
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }
            .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_go_setting)) { alertDialog, _ ->
                alertDialog.dismiss()
                if (isGrantedBluePermission) { //已授权，未开启蓝牙开关
                    PermissionUtil.remindStartBlueService(requireContext())
                } else {
                    XXPermissions.with(this).permission(
                        Permission.BLUETOOTH_SCAN,
                        Permission.BLUETOOTH_CONNECT,
                    ).request(object : OnPermissionCallback {
                        override fun onGranted(
                            permissions: MutableList<String>, all: Boolean
                        ) {
                            if (all) checkPermission()
                        }

                        override fun onDenied(
                            permissions: MutableList<String>, never: Boolean
                        ) {
                            if (never) {
                                XXPermissions.startPermissionActivity(
                                    requireActivity(), permissions
                                )
                            }
                        }
                    })
                }
            }.create().show()
    }

    private fun initView() {
        binding.rvDevice.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDevice.addItemDecoration(RVLinearItemDecorationSpace(12.dp2px()))
        binding.rvDevice.adapter = adapter
        val mainColor = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvOneKeySelect.setBtnStokeColor(mainColor)
        binding.tvOneKeySelect.setTextColor(mainColor)
        binding.tvAdd.setBtnBackgroundColor(mainColor)
        binding.tvStateTip.setTextColor(mainColor)
    }

    private fun inject() {
        val intentFilter = IntentFilter()
        // 设置过滤蓝牙开关状态
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        //设置过滤位置开关状态
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || AppUtil.isHarmonyOs()) intentFilter.addAction(
            LocationManager.PROVIDERS_CHANGED_ACTION
        )
        //注册广播
        requireContext().registerReceiver(broadcastReceiver, intentFilter)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.autoFindDevices.collect {
                    if (null != it) {
                        adapter.append(it)
                        val newPosition = adapter.itemCount - 1
                        adapter.notifyItemInserted(newPosition)
                        adapter.notifyItemChanged(newPosition)
                        if (binding.tvSearchTip.visibility != View.GONE) {
                            binding.tvSearchTip.visibility = View.GONE
                            binding.tvStateTip.visibility = View.GONE
                            binding.layoutSelect.visibility = View.VISIBLE
                            binding.tvOneKeySelect.text = String.format(
                                getString(com.mxchip.mxapp.base.R.string.mx_provision_select_all),
                                0,
                                ALLOWED_DEVICES_TOTAL
                            )
                        }
                    }
                }
            }
        }

        //请求权限
        binding.layoutOpenLocation.setOnClickListener {
            operationLocalDialog()
        }

        binding.layoutOpenBle.setOnClickListener {
            operationBluetoothDialog()
        }

        //点击item选择
        adapter.setItemClick { pos ->
            adapter.getData()?.get(pos)?.apply {
                if (isSelected) {
                    PairDataCenter.pairSelectedDeviceList.remove(this)
                    isSelected = false
                    adapter.notifyItemChanged(pos)
                } else if (PairDataCenter.pairSelectedDeviceList.size < 30) {
                    isSelected = true
                    adapter.notifyItemChanged(pos)
                    if (!PairDataCenter.pairSelectedDeviceList.contains(this)) PairDataCenter.pairSelectedDeviceList.add(
                        this
                    )
                }
                binding.tvOneKeySelect.text = String.format(
                    getString(com.mxchip.mxapp.base.R.string.mx_provision_select_all),
                    PairDataCenter.pairSelectedDeviceList.size,
                    ALLOWED_DEVICES_TOTAL
                )
                changeAddDeviceUIState()
            }
        }

        //一键选择
        binding.tvOneKeySelect.setOnClickListener {
            var newAdd = ALLOWED_DEVICES_TOTAL - PairDataCenter.pairSelectedDeviceList.size
            if (newAdd == 0) return@setOnClickListener
            it.isEnabled = false
            run forBreak@{
                adapter.getData()?.forEach { pairBean ->
                    if (newAdd == 0) return@forBreak
                    if (!pairBean.isSelected) {
                        if (!PairDataCenter.pairSelectedDeviceList.contains(pairBean)) {
                            pairBean.isSelected = true
                            PairDataCenter.pairSelectedDeviceList.add(pairBean)
                            newAdd--
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged()
            binding.tvOneKeySelect.text = String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_provision_select_all),
                PairDataCenter.pairSelectedDeviceList.size,
                ALLOWED_DEVICES_TOTAL
            )
            it.isEnabled = true
            changeAddDeviceUIState()
        }

        //对已选设备进行配网
        binding.tvAdd.setOnClickListener {
            //待配网设备是否包含wifi
            var isIncludeWifi = false
            //wifi是否为必须设置
            var isWifiRequired = false
            adapter.getData()?.forEach {
                if (it.isSelected) {//缓存选中的配网设备
                    if (PairUtils.includeWIFI(it.linkTypeId)) {
                        isIncludeWifi = true
                        if (PairUtils.includeWIFIRequired(it.linkTypeId)) {
                            isWifiRequired = true
                        }
                    }
                }
            }
            activityStarted = false
            //将选择设备交给配网列表
            PairDataCenter.pairDeviceList.clear()
            PairDataCenter.pairDeviceList.addAll(PairDataCenter.pairSelectedDeviceList)
            //包含wifi，去设置
            if (isIncludeWifi) {
                RouterManager.navigation(
                    RouterConstants.PAIR_WIFI_ACTIVITY, mapOf(
                        PairConstants.KEY_WIFI_REQUIRED to isWifiRequired
                    )
                )
            } else {
                //去配网页面
                RouterManager.navigation(RouterConstants.PAIRING_ACTIVITY)
            }
        }
    }

    private fun changeAddDeviceUIState() {
        if (PairDataCenter.pairSelectedDeviceList.isEmpty()) {
            binding.tvAdd.isEnabled = false
            binding.tvAdd.alpha = 0.6f
        } else {
            binding.tvAdd.isEnabled = true
            binding.tvAdd.alpha = 1f
        }
    }

    override fun onDestroyView() {
        PairDataCenter.pairSelectedDeviceList.clear()
        binding.waterWave.stop()
        requireContext().unregisterReceiver(broadcastReceiver)
        super.onDestroyView()
    }
}