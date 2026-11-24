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
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.PermissionUtil
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.widget.RVLinearItemDecorationSpace
import com.mxchip.mxapp.pair.adapter.AutoFindAdapter
import com.mxchip.mxapp.pair.consts.PairConstants
import com.mxchip.mxapp.pair.databinding.PairAcPairScanBinding
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairUtils
import com.mxchip.mxapp.pair.viewmodel.PairViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.scanner.MxScan
import com.mxchip.sdk.mesh.MXMesh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 *附近设备，扫码指定pk内设备
 */
@Router(RouterConstants.PAIR_SCAN_ACTIVITY)
class PairScanActivity : MXBusinessActivity<PairAcPairScanBinding>() {
    companion object {
        private const val TAG = "PairScanActivity"
        private const val deviceCount = 30
    }

    private val vm by viewModels<PairViewModel>()

    private val locationManager by lazy {
        MXBusinessApplication.mxBusinessApp.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
    }
    private var locationListener: LocationListener? = null

    private val adapter by lazy { AutoFindAdapter(this) }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (it.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR
                        )) {
                            BluetoothAdapter.STATE_TURNING_ON -> {
                                MXLog.d(TAG, "手机蓝牙正在打开")
                            }

                            BluetoothAdapter.STATE_ON -> {
                                MXLog.d(TAG, "手机蓝牙打开")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    delay(500)
                                    withContext(Dispatchers.Main) {
                                        checkPermission()
                                    }
                                }
                            }

                            BluetoothAdapter.STATE_TURNING_OFF -> {
                                MXLog.d(TAG, "手机蓝牙正在关闭")
                                MxScan.stopScan()
                                MXMesh.disConnect()
                            }

                            BluetoothAdapter.STATE_OFF -> {
                                MXLog.d(TAG, "手机蓝牙已关闭")
                                checkPermission()
                            }
                        }
                    }

                    LocationManager.PROVIDERS_CHANGED_ACTION -> {
                        checkPermission()
                    }
                }
            }
        }
    }

    private var productKeyArray: List<String>? = null
    private var replacedIotId: String? = null
    override fun getLayoutBinding(): PairAcPairScanBinding {
        return PairAcPairScanBinding.inflate(layoutInflater)
    }


    override fun onInit() {
        //配网,禁止proxy连接
        MeshSDKManage.isConnectProxy = false
        PairDataCenter.pairSelectedDeviceList.clear()
        initView()
        initData()
        inject()
    }

    private fun initView() {
        binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_found_device))
        binding.rvDevice.layoutManager = LinearLayoutManager(this)
        binding.rvDevice.addItemDecoration(RVLinearItemDecorationSpace(12.dp2px()))
        binding.rvDevice.adapter = adapter
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvStateNofound.setBtnStokeColor(mainColor)
        binding.tvStateNofound.setTextColor(mainColor)

        binding.tvOneKeySelect.setBtnStokeColor(mainColor)
        binding.tvOneKeySelect.setTextColor(mainColor)

        binding.tvAdd.setBtnBackgroundColor(mainColor)
        binding.tvStateTip.setTextColor(mainColor)
        binding.waterWave.setColor(mainColor)
    }

    private fun initData() {
        //指定品类
        val pkListStr = intent.getStringExtra(ProductConstants.KEY_PRODUCT_KEY_LIST)
        productKeyArray = JsonUtil.fromJsonToArray(pkListStr, String::class.java)
        replacedIotId = intent.getStringExtra("replaced.iotId")
        if (replacedIotId.isNullOrEmpty()) { //设备配网逻辑
            //每次启动app清除记录的位置信息
            PairDataCenter.removeLocationCurrent()
            PairDataCenter.clearReplaceInfo()
        } else {//设备替换逻辑
            binding.layoutSelect.visibility = View.GONE//隐藏底部操作，一键多选、添加设备
            val name = intent.getStringExtra("replaced.name")!!
            val roomName = intent.getStringExtra("replaced.roomName")!!
            val roomId = intent.getIntExtra("replaced.roomId", -1)
            //一键替换设备模式
            PairDataCenter.setReplacedDeviceInfo(
                replacedIotId!!, name, roomName, if (roomId == -1) null else roomId
            )
            adapter.setReplaceMode(true)
        }
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
        registerReceiver(broadcastReceiver, intentFilter)
        //指定pk不为空，则设置过滤器
        if (!productKeyArray.isNullOrEmpty()) {
            vm.setFilter { productInfo, compositeProductKey ->
                productKeyArray?.contains(productInfo.product_key) == true
                        || if (compositeProductKey != null) {
                    productKeyArray?.contains(compositeProductKey) == true
                } else {
                    false
                }
            }
        }

        vm.autoFindDevices.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            it?.apply {
                adapter.append(it)
                val newPosition = adapter.itemCount - 1
                adapter.notifyItemInserted(newPosition)
                adapter.notifyItemChanged(newPosition)
                if (binding.tvSearchTip.visibility != View.GONE) {
                    binding.tvSearchTip.visibility = View.GONE
                    binding.tvStateTip.visibility = View.GONE
                    if (replacedIotId.isNullOrEmpty()) {//设备配网逻辑，允许底部操作，多选
                        binding.layoutSelect.visibility = View.VISIBLE
                        binding.tvOneKeySelect.text = String.format(
                            getString(com.mxchip.mxapp.base.R.string.mx_provision_select_all),
                            0,
                            deviceCount
                        )
                    } else {
                        binding.tvStateNofound.visibility = View.GONE
                        replaceModeFoundTimeoutRun?.apply {
                            handler.removeCallbacks(this)
                        }
                    }
                }
            }
        }

        //点击item选择
        adapter.setItemClick { pos ->
            adapter.getData()?.get(pos)?.apply {
                if (replacedIotId.isNullOrEmpty()) {
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
                        deviceCount
                    )
                } else { //一键替换模式
                    PairDataCenter.pairDeviceList.clear()
                    PairDataCenter.pairDeviceList.add(this)
                    //是否需要Wi-Fi设置
                    if (PairUtils.includeWIFI(linkTypeId)) {
                        RouterManager.navigation(
                            RouterConstants.PAIR_WIFI_ACTIVITY, mapOf(
                                "replaced.iotId" to replacedIotId!!,
                                PairConstants.KEY_WIFI_REQUIRED to PairUtils.includeWIFIRequired(
                                    linkTypeId
                                )
                            )
                        )
                    } else {
                        RouterManager.navigation(RouterConstants.ONE_KEY_REPLACE_ACTIVITY)
                    }
                    finish()
                }
            }
        }

        binding.toolbar.left {
            finish()
        }

        //请求权限
        binding.layoutOpenLocation.setOnClickListener {
            operationLocalDialog()
        }
        binding.layoutOpenBle.setOnClickListener {
            operationBluetoothDialog()
        }
        //配网逻辑，一键选择
        binding.tvOneKeySelect.setOnClickListener {
            var newAdd = deviceCount - PairDataCenter.pairSelectedDeviceList.size
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
                deviceCount
            )
            it.isEnabled = true
        }

        //对已选设备进行配网
        binding.tvAdd.setOnClickListener {
            if (PairDataCenter.pairSelectedDeviceList.isNotEmpty()) {
                //待配网设备是否包含wifi
                var isIncludeWifi = false
                //wifi是否为必须设置
                var isWifiRequired = false
                //移除暂存Wi-Fi信息
                PairDataCenter.setCacheWifi(null, null)
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

        binding.tvStateNofound.setOnClickListener {
            if (productKeyArray.isNullOrEmpty()) {
                return@setOnClickListener
            }
            binding.tvStateNofound.visibility = View.GONE
            //一键替换设备，超时未发现设备，允许点击去设备配网引导页面
            RouterManager.navigation(
                RouterConstants.PAIR_GUIDE_ACTIVITY, mapOf(
                    ProductConstants.KEY_PRODUCT_KEY to productKeyArray!![0],
                    ProductConstants.KEY_PRODUCT_KEY_LIST to productKeyArray!!,
                    "replaced.iotId" to replacedIotId!!
                )
            )
        }
    }

    /**
     * 检查必要权限
     */
    @SuppressLint("MissingPermission")
    private fun checkPermission() {
        //清除列表数据
        clearDataAndNotify()
        PermissionUtil.checkFindBlePermission(applicationContext) { locationPermissionState, locationSwitchState, bluePermissionState, blueSwitchState ->
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
                if (PermissionUtil.isLocationEnable(applicationContext)) {
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
        if (!replacedIotId.isNullOrEmpty()) {//替换设备逻辑
            if (null == replaceModeFoundTimeoutRun) replaceModeFoundTimeoutRun = Runnable {
                //检查，显示未发现设备按钮，允许跳转手动引导页面
                binding.tvStateNofound.visibility =
                    if (adapter.itemCount > 0) View.GONE else View.VISIBLE
            }
            else handler.removeCallbacks(replaceModeFoundTimeoutRun!!)
            handler.postDelayed(replaceModeFoundTimeoutRun!!, 15 * 1000)
        }
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var replaceModeFoundTimeoutRun: Runnable? = null

    @SuppressLint("MissingPermission")
    private fun initLocationInfo() {
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (null == location) {
            if (null == locationListener) {
                locationListener = LocationListener {
                    updateLocation(it)
                }
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 3000, 1000f, locationListener!!
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

    private fun clearDataAndNotify() {
        PairDataCenter.pairSelectedDeviceList.clear()
        //清空发现列表
        adapter.clearData()
        vm.clearFoundDeviceFilterData()
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
        vm.registerScanProvisioningData()
    }

    override fun onStop() {
        //停止发现
        MxScan.stopScan()
        vm.stopDiscoveryDevice()
        binding.waterWave.pause()
        binding.waterWave.visibility = View.GONE
        super.onStop()
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
        MXDialog.Builder(this).title(title)
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
                    PermissionUtil.remindStartLocateService(this)
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
                                    this@PairScanActivity, permissions
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

        MXDialog.Builder(this).title(title)
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
                    PermissionUtil.remindStartBlueService(this)
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
                                    this@PairScanActivity, permissions
                                )
                            }
                        }
                    })
                }
            }.create().show()
    }

    override fun onDestroy() {
        binding.waterWave.stop()
        unregisterReceiver(broadcastReceiver)
        PairDataCenter.pairSelectedDeviceList.clear()
        super.onDestroy()
    }
}