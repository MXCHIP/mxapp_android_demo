package com.mxchip.mxapp.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.log.MXLogStoreHelper
import com.mxchip.mxapp.BuildConfig
import com.mxchip.mxapp.R
import com.mxchip.mxapp.account.ui.AgreementDialog
import com.mxchip.mxapp.account.vm.AccountViewModel
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.activity.MXWebViewActivity
import com.mxchip.mxapp.base.base.AppLifecycleCallback
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.dialog.BottomActionDialog
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.MeshAttrUtil
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterConstants.MAIN_ACTIVITY
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.AppUtil
import com.mxchip.mxapp.base.utils.DeviceManage
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.LoginManager
import com.mxchip.mxapp.base.utils.PermissionUtil
import com.mxchip.mxapp.base.utils.ProductInfoManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.databinding.ActivityMainBinding
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.mxapp.pair.bean.FirstCategory
import com.mxchip.mxapp.pair.consts.PairConstants
import com.mxchip.mxapp.ui.fragment.IHomeRoleChange
import com.mxchip.mxapp.ui.fragment.RoomFragment
import com.mxchip.mxapp.utils.CloudMessageUtil
import com.mxchip.mxapp.utils.EnvConfigManager
import com.mxchip.mxapp.viewmodel.MainViewModel
import com.mxchip.router.annotation.Router
import com.mxchip.scanner.MxScan
import com.mxchip.scanner.bean.MxBluetoothDevice
import com.mxchip.scanner.callback.MxScanListener
import com.mxchip.scanner.callback.MxScanType
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.bean.MeshOpcodeConstants
import com.mxchip.sdk.mesh.bean.MeshState
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.utils.LiveDataBus
import com.mxchip.utils.WebSocketMange
import com.mxchip.utils.WebSocketType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.fixedRateTimer
import kotlin.system.exitProcess

@Router(path = MAIN_ACTIVITY)
class MainActivity : MXBusinessActivity<ActivityMainBinding>(), AppLifecycleCallback {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val vm by viewModels<MainViewModel>()
    private val accountVm by viewModels<AccountViewModel>()
    private val homeVM by viewModels<HomeViewModel>()

    private val fragments = mutableListOf<IHomeRoleChange>()
    private var mainTimer: Timer? = null

    private var isSocketOpen: Boolean? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (it.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR
                        )) {
                            BluetoothAdapter.STATE_ON -> {
                                MXLog.d(TAG, "手机蓝牙打开")
                                lifecycleScope.launch(Dispatchers.IO) {
                                    delay(500)
                                    withContext(Dispatchers.Main) {
                                        checkPermissionAndStartMesh()
                                    }
                                }
                            }

                            BluetoothAdapter.STATE_TURNING_OFF -> {
                                if (MeshSDKManage.isConnectProxy) {
                                    MXLog.d(TAG, "手机蓝牙正在关闭")
                                    MxScan.stopScan()
                                    MXMesh.disConnect()
                                }
                            }

                            BluetoothAdapter.STATE_OFF -> {
                                MXLog.d(TAG, "手机蓝牙已关闭")
                                checkPermissionAndStartMesh()
                            }
                        }
                    }

                    LocationManager.PROVIDERS_CHANGED_ACTION -> {
                        checkPermissionAndStartMesh()
                    }
                }
            }
        }
    }

    override fun getLayoutBinding() = ActivityMainBinding.inflate(layoutInflater)
    private val connectProxying = AtomicBoolean(false)
    private val scanProxyListener by lazy {
        object : MxScanListener {

            override fun onScanResultReceived(mxBluetoothDevice: MxBluetoothDevice) {
                if (mxBluetoothDevice.mxScanType == MxScanType.PROXY) {
                    //检查是否允许proxy连接，使用compareAndSet确保原子性
                    if (!connectProxying.compareAndSet(false, true)) {
                        MXLog.d(TAG, "connectProxying already in progress")
                        return
                    }

                    if (!MeshSDKManage.isConnectProxy) {
                        MXLog.d(TAG, "connectProxyMode is false")
                        connectProxying.set(false)
                        return
                    }

                    if (MXMesh.isConnected()) {
                        MXLog.w(TAG, "mesh is connected, do not need connect again")
                        connectProxying.set(false)
                        return
                    }
                    val allNodeSize = MXMesh.getAllNode()?.size ?: 0
                    if (allNodeSize <= 1) {//>1,手机本身也是一个node
                        MXLog.w(TAG, "mesh node count->$allNodeSize")
                        connectProxying.set(false)
                        return
                    }
//                PairManager.getScanRssiThreshold()?.apply {
//                    val rssi = mxBluetoothDevice.getRSSI()
//                    if (rssi < this) {
//                        MXLog.w(TAG, "dump device rssi: $rssi ,set threshold rssi: $this")
//                        connectProxying.set(true)
//                        return
//                    }
//                }
                    MXMesh.connectProxy(mxBluetoothDevice.getDevice())
                }
            }
        }
    }

    /**
     * 告诉网络中其它设备，清除指定单播地址（设备）在mesh网络中的缓存
     */
    private fun clearMeshNetworkCache(unicastAddress: Int) {
        MXMesh.sendGroupMessage(
            0xFFFF,
            MeshOpcodeConstants.VENDOR_MSG_OPCODE_SYNC,
            "03" + MeshSDKManage.stringReversedToLittleEndian(
                String.format("%04X", unicastAddress)
            )
        )
    }

    private var mxJoinHomeDialog: AlertDialog? = null
    val scanQRCodeResultLaunch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (RESULT_OK == it.resultCode) {
                val originalValue = it.data?.getStringExtra("originalValue") ?: ""
                homeVM.bindShareFamilyByCode(0, originalValue).launchAndCollectIn(
                    this@MainActivity, Lifecycle.State.CREATED
                ) { netState ->
                    if (netState.state == NetState.SUCCESS) {
                        netState.data?.apply {
                            val dataJson = JSONObject(this)
                            val newHomeId = dataJson.getInt("home_id")
                            if (null == mxJoinHomeDialog) mxJoinHomeDialog =
                                MXDialog.Builder(this@MainActivity)
                                    .title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                                    .content(getString(com.mxchip.mxapp.base.R.string.mx_join_home_success))
                                    .leftButton(
                                        getString(com.mxchip.mxapp.base.R.string.mx_cancel)
                                    ) { alertDialog, _ ->
                                        alertDialog.dismiss()
                                    }.rightButton(
                                        getString(com.mxchip.mxapp.base.R.string.mx_confirm)
                                    ) { alertDialog, _ ->
                                        alertDialog.dismiss()
                                        setSharedCurrentHome(newHomeId)
                                    }.create()
                            mxJoinHomeDialog?.show()
                        }
                    } else {
                        netState.state.message?.apply {
                            showToast(this)
                        }
                    }
                }
            }
        }

    override fun onInit() {
        //初始化状态栏高度
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutHomeInfo) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top + 10.dp2px(), 0, 0)
            insets
        }
        onBackPressedDispatcher.addCallback {
            // 处理返回键逻辑
        }
        MeshSDKManage.isConnectProxy = true
//        PairManager.setScanRssiThreshold(-70)
        fragments.add(RoomFragment())
        // Ensure fragments are properly initialized
        ensureFragmentState()
        checkViewState()
        inject()
        injectMesh()
        initEvent()
        (application as MXBusinessApplication).registerAppLifecycleCallback(this)
    }

    //应用回到前台
    override fun onAppForeground() {
        LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
            .postValue(HomeDataRefresh.DEVICE)
        //回到前台，如果mqtt断开了，mqtt重连上也不需要再次刷新数据
        if (isSocketOpen == false)
            isSocketOpen = null
    }

    override fun onStart() {
        super.onStart()
        //设置需要扫描为proxy节点
        MeshSDKManage.isConnectProxy = true
        //一些延迟初始化
        if (null == mainTimer) {
            getCurrentHome()
            queryAsyncData()
            timerCheckHeartbeat()
        } else
            checkPermissionAndStartMesh()
    }

    private fun checkViewState() {
        binding.ivNotifications.visibility = View.GONE
        binding.notificationRedPointer.visibility = View.GONE
        if (DeviceManage.isSupportAddDevice() || EnvConfigManager.Home_Scan) {
            binding.ivAddSmall.visibility = View.VISIBLE
        } else binding.ivAddSmall.visibility = View.GONE

        if (BuildConfig.BUILD_TYPE == EnvConfigManager.BUILD_TYPE_DEBUG) {
            binding.envState.visibility = View.VISIBLE
            //debug 包
            binding.envState.text = when (EnvConfigManager.getEnvSwitch()) {
                EnvConfigManager.ENV_DEV -> "当前开发环境"
                EnvConfigManager.ENV_PREV -> "当前预发环境"
                EnvConfigManager.ENV_RELEASE -> "当前生产环境"
                else -> "当前测试环境"
            }

            binding.envState.setOnClickListener {
                envSwitchDialog.create().show()
            }
        }
    }

    private var oldPosition = -1
    private var isFragmentTransactionInProgress = false

    /**
     * 确保Fragment状态正确，防止重叠显示
     */
    private fun ensureFragmentState() {
        try {
            // 清理可能存在的旧Fragment
            val existingFragments = supportFragmentManager.fragments
            existingFragments.forEach { fragment ->
                if (fragment != null && fragment.isAdded && fragment.tag?.startsWith("fragment_") == true) {
                    supportFragmentManager.beginTransaction().remove(fragment)
                        .commitAllowingStateLoss()
                }
            }
        } catch (e: Exception) {
            MXLog.e(TAG, "Error ensuring fragment state: ${e.message}")
        }
    }

    private fun showFragment(position: Int) {
        if (position < 0 || position >= fragments.size || isFinishing || isFragmentTransactionInProgress) return

        isFragmentTransactionInProgress = true

        try {
            val ft = supportFragmentManager.beginTransaction()

            // 隐藏所有Fragment，防止重叠
            fragments.forEachIndexed { index, fragment ->
                if (fragment is Fragment && fragment.isAdded) {
                    ft.hide(fragment)
                }
            }

            val currentFragment = fragments[position] as Fragment

            // 如果Fragment未添加，则添加
            if (!currentFragment.isAdded) {
                ft.add(R.id.frame_container, currentFragment, "fragment_$position")
            }

            // 显示当前Fragment
            ft.show(currentFragment)

            // 优先使用commit()，失败时使用commitAllowingStateLoss()
            try {
                ft.commit()
            } catch (e: IllegalStateException) {
                try {
                    ft.commitAllowingStateLoss()
                } catch (e: Exception) {
                    MXLog.e(TAG, "Failed to commit fragment transaction: ${e.message}")
                }
            }

            oldPosition = position
        } finally {
            isFragmentTransactionInProgress = false
        }
    }

    private fun initEvent() {
        binding.ivAddSmall.setOnClickListener {
            if (RoleManager.isUser()) {
                RoleManager.showDialog(this, RoleManager.RoleType.TYPE_USER)
                return@setOnClickListener
            }
            RouterManager.navigation(RouterConstants.PRODUCT_ACTIVITY)
        }
        showFragment(0)
    }

    private fun queryAsyncData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val findHomeAttr = async {
                vm.findHomeAttrConfig()
            }

            val findProducts = async {
                vm.findProducts()
            }

            val getWebSocket = async {
                vm.getWebSocketInfo()
            }

            val getPrivacyChange = async {
                vm.privacyChange()
            }

            val homeAttr = findHomeAttr.await()
            val productList = findProducts.await()
            val webSocket = getWebSocket.await()
            val privacyChange = getPrivacyChange.await()

            homeAttr.collect {
                val oldAttr = AppConfigManager.getAttrConfig()
                it.data?.apply {
                    //检查attr是否更新
                    val dataJson = JSONObject(this)
                    val newAttr = dataJson.getString("attr_map")
                    if (oldAttr != newAttr && newAttr.isNotEmpty()) {
                        AppConfigManager.saveAttrConfig(newAttr)
                        MeshAttrUtil.parseFillAttrMap(JSONObject(newAttr))
                    } else if (!oldAttr.isNullOrEmpty()) {
                        MeshAttrUtil.parseFillAttrMap(JSONObject(oldAttr))
                    }
                }
            }

            productList.collect {
                JsonUtil.fromJsonMXList(it.data, FirstCategory::class.java)?.forEach { first ->
                    for (category in first.categorys) {
                        for (product in category.products) {
                            ProductInfoManager.saveProductInfo(
                                product.product_key,
                                JsonUtil.toJson(product)
                            )
                            if (product.cloud_platform == PairConstants.CLOUD_PLATFORM_FEIYAN) {
                                //设备广播数据fog为pk，飞燕设备为pid，飞燕设备存储pid-pk对应关系
                                ProductInfoManager.saveProductKeyPid(
                                    product.product_key,
                                    product.feiyan_product_id
                                )
                            }
                        }
                    }
                }
            }

            webSocket.collect {
                it.data?.apply {
                    val json = JSONObject(this)
                    if (json.has("url")) {
                        WebSocketMange.setUrl(json.getString("url"))
                        WebSocketMange.setLoginToken(LoginManager.getToken())
                    }
                }
            }
            privacyChange.collect {
                it.data?.apply {
                    val json = JSONObject(this)
                    val docId = json.optInt("doc_id")
                    if (docId > 0) {
                        val isLogin = intent.getBooleanExtra("isLogin", false)
                        if (isLogin) {
                            vm.readPrivacy(docId)
                        } else
                            showPrivacyChangeTips(docId)
                    }
                }
            }
        }
    }

    private fun setCurrentHome(setHomeBean: HomeBean) {
        parseHomeData(setHomeBean)
        lifecycleScope.launch(Dispatchers.IO) {
            homeVM.setCurrentHome(setHomeBean.home_id).collect {}
        }
    }

    /**
     * 设置分享过来的家庭为当前家庭
     */
    private fun setSharedCurrentHome(sharedHomeId: Int) {
        showLoading()
        homeVM.getHomeInfo(sharedHomeId).launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlow<HomeBean>(
                success = {
                    dismissLoading()
                    it?.apply {
                        parseHomeData(this)
                    }
                }, error = { code, msg, _ ->
                    dismissLoading()
                    val httpErr = onException(code, false)
                    if (!httpErr)
                        showToast("$msg")
                }
            )
        )
        lifecycleScope.launch(Dispatchers.IO) {
            homeVM.setCurrentHome(sharedHomeId).collect {}
        }
    }

    private var queryHomeCount = 2
    private fun getCurrentHome() {
        homeVM.findHomes().launchAndCollectIn(
            this, Lifecycle.State.CREATED,
            loadFlowList<HomeBean>(success = {
                it?.apply {
                    val curt = this.firstOrNull { hb -> hb.current_home }
                    if (curt == null) {
                        if (this.isEmpty()) {
                            RouterManager.navigation(RouterConstants.CREATE_HOME_GUIDE_ACTIVITY)
                        } else {//未设置过当前家庭，直接设置第一个
                            val first = this[0]
                            setCurrentHome(first)
                        }
                    } else {
                        parseHomeData(curt)
                    }
                }
            }, error = { code, _, _ ->
                dismissLoading()
                val httpErr = onException(code, false)
                if (httpErr) {//网络异常，允许自动重试
                    if (queryHomeCount > 0) {
                        queryHomeCount--
                        lifecycleScope.launch(Dispatchers.IO) {
                            delay(1000)
                            getCurrentHome()
                        }
                    } else {
                        showNetErrToast()
                    }
                }
            })
        )
    }

    /**
     * 家庭数据
     */
    private fun parseHomeData(homeBean: HomeBean) {
        binding.familyName.text = homeBean.name
        //新旧家庭数据不一致
        if (homeBean.home_id != AppConfigManager.getUseHomeId()) {
            //重置网络
            MXMesh.resetMeshNetwork()
            //缓存当前home信息
            AppConfigManager.setUseHomeData(
                homeBean.home_id,
                homeBean.name,
                homeBean.network_key,
                homeBean.location_key,
                homeBean.city_name
            )
            //获取家庭mesh信息
            vm.getMeshInfo(homeBean.home_id)
            LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH)
                .postValue(HomeDataRefresh.HOME)
        }
        // 角色在每次解析数据时，要重新设置值，因为家庭拥有者可以更新这个字段
        if (RoleManager.CURRENT_HOME_ROLE != homeBean.role) {
            RoleManager.CURRENT_HOME_ROLE = homeBean.role
            fragments.forEach {
                it.updateHomeRoleState(homeBean.role)
            }
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

        accountVm.docInfoState.observe(this) {
            if (it.state == NetState.SUCCESS) {
                it.data?.apply {
                    val json = JSONObject(this)
                    RouterManager.navigation(
                        RouterConstants.PROTOCOL_ACTIVITY, mapOf(
                            MXWebViewActivity.LOAD_URL to json.optString("url"),
                            MXWebViewActivity.TOP_BAR_TITLE to json.optString("title")
                        )
                    )
                }
            } else {
                it.state.message?.apply { showToast(this) }
            }
        }

        LiveDataBus.with<Int>(CommonConstants.KEY_OTHER_LOGIN).observe(this) {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_other_login)).leftButton(null)
                .rightButton(getString(com.mxchip.mxapp.base.R.string.mx_re_login)) { alterDialog, _ ->
                    alterDialog.dismiss()
                    logout {
                        RouterManager.navigation(RouterConstants.LOGIN_GUIDE_ACTIVITY)
                        MXBusinessApplication.mxBusinessApp.finishOtherActivity(RouterConstants.LOGIN_GUIDE_ACTIVITY)
                    }
                }.create().show()
        }

        vm.importMeshState.launchAndCollectIn(this, Lifecycle.State.CREATED) {
            MXMesh.setUseNetworkKey(AppConfigManager.getUseHomeNetworkKey())
            val networkId = MXMesh.getNetworkKeyId(AppConfigManager.getUseHomeNetworkKey())
            if (networkId == null) {
                MXLog.e(TAG, "networkId is null")
            } else
                MxScan.setUseNetworkKeyId(networkId)
            checkPermissionAndStartMesh()
        }

        //刷新当前家庭数据
        LiveDataBus.with<Any>(LiveDataBusKey.KEY_HOME_REFRESH).observe(this) {
            getCurrentHome()
        }
    }

    private fun logout(out: () -> Unit) {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            vm.logout().collect {
                withContext(Dispatchers.Main) {
                    dismissLoading()
                }
                LoginManager.removeToken()
                out.invoke()
            }
        }
    }
    private fun injectMesh() {
        MxScan.addScanDataListener(scanProxyListener)
        //注册mesh连接监听
        MXMesh.setMeshConnectStateCallback {
            if (isFinishing) {
                return@setMeshConnectStateCallback
            }
            when (it) {
                MeshState.SUCCESS -> {
                    connectProxying.set(false)
                    MeshSDKManage.subscribeGroups { addResult ->
                        if (addResult) {
                            MxScan.stopScan()
                            updateMeshMessage(mapOf(DeviceConstants.KEY_MESH_STATE to true))
                        } else {
                            MXMesh.disConnect()
                        }
                    }
                }

                MeshState.DISCONNECTED, MeshState.DEVICE_NOT_SUPPORTED, MeshState.FAIL -> {
                    updateMeshMessage(mapOf(DeviceConstants.KEY_MESH_STATE to false))
                    MXMesh.disConnect()
                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(300)
                        connectProxying.set(false)
                    }
                }

                else -> {}
            }
        }

        //mesh消息
        MXMesh.setReceiveMeshMessageCallback { resultMap ->
            if (isFinishing) {
                return@setReceiveMeshMessageCallback
            }
            resultMap[MeshConstants.KEY_UUID]?.apply {
                updateMeshMessage(resultMap)
            }
        }
        //监听mesh sequence
        MXMesh.setMeshMessageSequenceUpdatedCallback(50) {
            vm.setMeshSeq(AppConfigManager.getUseHomeId(), it)
        }
        //云端socket数据
        WebSocketMange.subscribeWebSocketMessage { type, msg ->
            if (isFinishing) {
                return@subscribeWebSocketMessage
            }
            MXLog.d(TAG, "WebSocketMange: $type, $msg")
            when (type) {
                WebSocketType.OPEN -> isSocketOpen = true

                WebSocketType.MSG -> {
                    CloudMessageUtil.parseMsg(msg)
                }

                else -> {
                    isSocketOpen = false
                }
            }
        }
    }


    private fun updateMeshMessage(map: Map<String, Any>) {
        //推送Mesh数据
        LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA).postValue(map)
    }

    private fun timerCheckHeartbeat() {
        mainTimer = fixedRateTimer("mainTimer", period = 5000L) {
            if (isSocketOpen != true) WebSocketMange.initSocketAndConnect()
        }
    }

    private val dialog by lazy {
        AgreementDialog().apply {
            setPolicyClick { type ->
                accountVm.docInfo(type)
            }
            setDisagreeClick {
                MXBusinessApplication.mxBusinessApp.finishAllActivity()
                exitProcess(0)
            }
        }
    }

    private fun showPrivacyChangeTips(docId: Int) {
        if (isFinishing) return
        dialog.setAgreementClick {
            vm.readPrivacy(docId)
        }
        lifecycleScope.launch(Dispatchers.Main) {
            dialog.show(supportFragmentManager, "AgreementDialog")
        }
    }

    fun checkPermissionAndStartMesh(userOperation: Boolean = false) {
        PermissionUtil.checkFindBlePermission(applicationContext) { locationPermissionState, locationSwitchState, bluePermissionState, blueSwitchState ->
            if (locationPermissionState && locationSwitchState && bluePermissionState && blueSwitchState) {
                //连接代理
                if (MeshSDKManage.isConnectProxy) {
                    //mesh已连接，停止扫描，否则启动扫描
                    if (MXMesh.isConnected()) {
                        MxScan.stopScan()
                    } else {
                        connectProxying.set(false)
                        MxScan.startScanMeshProxy()
                    }
                }
            } else {
                MxScan.stopScan()
            }
            if (userOperation) { //用户手动点击顶部权限提示
                if (!locationPermissionState || !locationSwitchState) {
                    operationLocalDialog(locationPermissionState)
                } else if (!bluePermissionState || !blueSwitchState) {
                    operationBluetoothDialog(bluePermissionState)
                }
            }
        }
    }

    /**
     * 位置权限、位置开关
     */
    private fun operationLocalDialog(isGrantedLocationPermission: Boolean) {
        val title =
            if (isGrantedLocationPermission) getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off) else getString(
                com.mxchip.mxapp.base.R.string.mx_permission_location_denied
            )
        MXDialog.Builder(this).title(title).content(
            String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off_des),
                getString(com.mxchip.mxapp.base.R.string.mx_app_name)
            )
        ).leftButton(
            getString(com.mxchip.mxapp.base.R.string.mx_cancel)
        ) { alertDialog, _ ->
            alertDialog.dismiss()
        }.rightButton(
            getString(com.mxchip.mxapp.base.R.string.mx_go_setting)
        ) { alertDialog, _ ->
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
                        if (all) checkPermissionAndStartMesh(userOperation = true)
                    }

                    override fun onDenied(
                        permissions: MutableList<String>, never: Boolean
                    ) {
                        if (never) {
                            XXPermissions.startPermissionActivity(
                                this@MainActivity, permissions
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
    private fun operationBluetoothDialog(isGrantedBluePermission: Boolean) {
        val title =
            if (isGrantedBluePermission) getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off) else getString(
                com.mxchip.mxapp.base.R.string.mx_permission_ble_denied
            )
        MXDialog.Builder(this).title(title).content(
            String.format(
                getString(com.mxchip.mxapp.base.R.string.mx_permission_ble_switch_off_des),
                getString(com.mxchip.mxapp.base.R.string.mx_app_name)
            )
        ).leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
            alertDialog.dismiss()
        }.rightButton(
            getString(com.mxchip.mxapp.base.R.string.mx_go_setting)
        ) { alertDialog, _ ->
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
                        if (all) checkPermissionAndStartMesh(userOperation = true)
                    }

                    override fun onDenied(
                        permissions: MutableList<String>, never: Boolean
                    ) {
                        if (never) {
                            XXPermissions.startPermissionActivity(
                                this@MainActivity, permissions
                            )
                        }
                    }
                })
            }
        }.create().show()
    }

    override fun onDestroy() {
        // Clean up fragments to prevent memory leaks and state issues
        try {
            fragments.forEach { fragment ->
                if (fragment is Fragment && fragment.isAdded) {
                    supportFragmentManager.beginTransaction().remove(fragment)
                        .commitAllowingStateLoss()
                }
            }
            fragments.clear()
        } catch (e: Exception) {
            MXLog.e(TAG, "Error cleaning up fragments: ${e.message}")
        }

        AppConfigManager.clearUseHomeData()
        WebSocketMange.closeWebSocket()
        MxScan.stopScan()
        //取消proxy数据扫描监听
        MxScan.removeScanDataListener(scanProxyListener)
        MXMesh.disConnect()
        mainTimer?.cancel()
        unregisterReceiver(broadcastReceiver)
        MXLogStoreHelper.flush()
        (application as MXBusinessApplication).unregisterAppLifecycleCallback(this)
        super.onDestroy()
    }

    private fun switchEnv(env: String) {
        logout {
            EnvConfigManager.switchEnv(env)
            EnvConfigManager.initHostConfig("CHN",true)
            RouterManager.navigation(RouterConstants.LOGIN_GUIDE_ACTIVITY)
            MXBusinessApplication.mxBusinessApp.finishOtherActivity(RouterConstants.LOGIN_GUIDE_ACTIVITY)
        }
    }

    private val envSwitchDialog by lazy {
        BottomActionDialog(this)
            .title("切换环境")
            .addItem("开发环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_DEV)
            }.addItem("测试环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_TEST)
            }
            .addItem("预发环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_PREV)
            }
            .addItem("生产环境") { _, _ ->
                switchEnv(EnvConfigManager.ENV_RELEASE)
            }
    }
}