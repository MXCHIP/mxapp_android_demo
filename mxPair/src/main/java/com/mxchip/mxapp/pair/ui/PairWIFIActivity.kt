package com.mxchip.mxapp.pair.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mxchip.log.MXLog
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.consts.ProductConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.mesh.CallbackMsg
import com.mxchip.mxapp.base.mesh.MapCallback
import com.mxchip.mxapp.base.mesh.MeshSDKManage
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.adapter.WiFiRecordAdapter
import com.mxchip.mxapp.pair.consts.PairConstants
import com.mxchip.mxapp.pair.databinding.PairAcPairWifiBinding
import com.mxchip.mxapp.pair.ui.dialog.WiFiRecordPopupWindow
import com.mxchip.mxapp.pair.utils.PairDataCenter
import com.mxchip.mxapp.pair.utils.PairWiFiSPManage
import com.mxchip.router.annotation.Router
import com.mxchip.sdk.mesh.MXMesh
import com.mxchip.sdk.mesh.util.MeshConstants
import com.mxchip.sdk.mesh.util.MeshMessageUtil
import com.mxchip.utils.ByteUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *Wi-Fi信息输入
 */
@Router(RouterConstants.PAIR_WIFI_ACTIVITY)
class PairWIFIActivity : MXBusinessActivity<PairAcPairWifiBinding>() {
    private var productKey: String? = null

    private var isChangeWifi = false
    private var changeWifiDeviceUUID: String? = null
    private var replacedIotId: String? = null

    //    private var productImage: String? = null
    private var pwdVisibility = false

    private val wifiManager by lazy { applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val connectivityManager by lazy { applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    //    private var wifiFrequency: Int? = null
    private var wifiRecordPop: WiFiRecordPopupWindow? = null

    override fun getLayoutBinding(): PairAcPairWifiBinding {
        return PairAcPairWifiBinding.inflate(layoutInflater)
    }

    private val launch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // 立即获取当前WiFi信息（用户从系统设置返回时需要）
            requestPermission {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getCurrentWifiInfoImmediately()
                } else {
                    fallbackToTraditionalWifiInfo()
                }
            }
        }

    override fun onInit() {
        productKey = intent.getStringExtra(ProductConstants.KEY_PRODUCT_KEY)
        changeWifiDeviceUUID = intent.getStringExtra(CommonConstants.KEY_UUID)
        isChangeWifi = intent.getBooleanExtra("change_wifi", false)
        replacedIotId = intent.getStringExtra("replaced.iotId")
        initView()
        inject()
        requestPermission {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getCurrentWifi()
            } else {
                fallbackToTraditionalWifiInfo()
            }
        }
    }

    override fun onResume() {
        //每次Wi-Fi页获得焦点，移除暂存Wi-Fi信息
        PairDataCenter.setCacheWifi(null, null)
        super.onResume()
    }

    private fun requestPermission(block: () -> Unit) {
        XXPermissions.with(this)
            .permission(Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION)
            .request(object : OnPermissionCallback {
                override fun onGranted(
                    permissions: MutableList<String>, all: Boolean
                ) {
                    if (all) block.invoke()
                }

                override fun onDenied(
                    permissions: MutableList<String>, never: Boolean
                ) {
                    if (never) {
                        MXDialog.Builder(this@PairWIFIActivity)
                            .title(getString(com.mxchip.mxapp.base.R.string.mx_permission_location_denied))
                            .content(
                                String.format(
                                    getString(com.mxchip.mxapp.base.R.string.mx_permission_location_switch_off_des),
                                    getString(com.mxchip.mxapp.base.R.string.mx_app_name)
                                )
                            )
                            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                                alertDialog.dismiss()
                            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_go_setting)) { alertDialog, _ ->
                                alertDialog.dismiss()
                                XXPermissions.startPermissionActivity(
                                    this@PairWIFIActivity, permissions
                                )
                            }.create().show()
                    }
                }
            })
    }

    private fun initView() {
        if (isChangeWifi) {
            binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_wifi_change))
        } else {
            binding.toolbar.title(getString(com.mxchip.mxapp.base.R.string.mx_set_network))
            val wifiRequired = intent.getBooleanExtra(PairConstants.KEY_WIFI_REQUIRED, false)
            if (wifiRequired.not()) { //wifi 非必须
                binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_skip))
                binding.tvSkipTips.visibility = View.VISIBLE
            }
        }
        val stoke =
            generateStoke(
                1,
                ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_dividers),
                25
            )
        binding.layoutWifi.background = stoke
        binding.layoutWifiPassword.background = stoke
        binding.tvNext.setBtnBackgroundColor(ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9))
    }

    private fun inject() {
        binding.toolbar.left {
            finish()
        }.right {
            MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
                .content(getString(com.mxchip.mxapp.base.R.string.mx_skip_set_wifi_des))
                .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                    alertDialog.dismiss()
                }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm)) { alertDialog, _ ->
                    alertDialog.dismiss()
                    toPage(true)
                }.create().show()
        }
        binding.ivWifiExchange.setOnClickListener {
            requestPermission {
                launch.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }
        binding.ivPwdHide.setOnClickListener {
            pwdVisibility = !pwdVisibility
            if (pwdVisibility) {
                binding.ivPwdHide.setImageResource(R.drawable.pair_ic_password_show)
                binding.etWifiPwd.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.ivPwdHide.setImageResource(R.drawable.pair_ic_password_hide)
                binding.etWifiPwd.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.etWifiPwd.setSelection(binding.etWifiPwd.text.length)
        }
        binding.etWifiSsid.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.etWifiSsid.post {
                    showWiFiRecord()
                }
            } else {
                checkInputWifi5G()
                wifiRecordPop?.dismiss()
            }
        }
        binding.etWifiSsid.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val ssid = binding.etWifiSsid.text.toString()
                if (ssid.isBlank()) {
                    binding.tvNext.isEnabled = false
                    binding.tvNext.alpha = 0.5f
                } else {
                    binding.tvNext.isEnabled = true
                    binding.tvNext.alpha = 1f
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.tvNext.setOnClickListener {
            //检查当前Wi-Fi频率是否为5G
//            if (null != wifiFrequency && PairUtil.is5GHz(wifiFrequency!!)) {
//                show5GDialog()
//            } else
            toPage()
        }
    }

    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun getCurrentWifi() {
        val request =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        wifiNetworkCallback = object : ConnectivityManager.NetworkCallback(
            FLAG_INCLUDE_LOCATION_INFO
        ) {
            override fun onCapabilitiesChanged(
                network: Network, networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo?
                wifiInfo?.apply {
                    inputWifiInfo(wifiInfo.ssid)
                }
            }
        }
        // 使用 registerNetworkCallback 来监听网络状态变化
        // 这样当WiFi连接状态变化时会自动回调
        connectivityManager.registerNetworkCallback(request, wifiNetworkCallback!!)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun getCurrentWifiInfoImmediately() {
        try {
            // 立即获取当前WiFi信息，不依赖回调
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    val wifiInfo = networkCapabilities.transportInfo as WifiInfo?
                    wifiInfo?.apply {
                        inputWifiInfo(wifiInfo.ssid)
                    }
                }
            }
        } catch (e: Exception) {
            MXLog.e("PairWiFi", "Failed to get current WiFi info immediately: ${e.message}")
            // 如果获取失败，回退到传统方式
            fallbackToTraditionalWifiInfo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToTraditionalWifiInfo() {
        try {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null && wifiInfo.ssid != null) {
                inputWifiInfo(wifiInfo.ssid)
            } else {
                MXLog.d("PairWiFi", "No WiFi connection or SSID is null")
            }
        } catch (e: Exception) {
            MXLog.e("PairWiFi", "Failed to get WiFi info: ${e.message}")
        }
    }

//    private fun is24GHz(freqMhz: Int): Boolean {
//        //2.4 GHz band frequency of first channel in MHz
//        //BAND_24_GHZ_START_FREQ_MHZ = 2412
//
//        // 2.4 GHz band frequency of last channel in MHz
//        //BAND_24_GHZ_END_FREQ_MHZ = 2484
//        return freqMhz >= 2412 && freqMhz <= 2484
//    }
//
//    private fun checkWifi5G(wifiInfo: WifiInfo) {
//        if (is24GHz(wifiInfo.frequency)) {
//            binding.wifi5gTips.visibility = View.GONE
//            inputWifiInfo(wifiInfo.ssid)
//        } else {
//            binding.wifi5gTips.visibility = View.VISIBLE
//        }
//    }

    private fun checkInputWifi5G() {
        val ssid = binding.etWifiSsid.text.toString()
        if (ssid.endsWith("_5G") || ssid.endsWith("_5g")
            || ssid.endsWith("-5G") || ssid.endsWith("-5g")
        ) {
            binding.wifi5gTips.visibility = View.VISIBLE
        } else {
            binding.wifi5gTips.visibility = View.GONE
        }
    }

    private fun inputWifiInfo(ssid: String) {
        MXLog.d("PairWiFi", "input ssid: $ssid")
        val finalSsid = ssid.replace("\"", "")
        MXLog.d("PairWiFi", "replaced ssid: $finalSsid")
        val unknown = "<unknown ssid>"
        if (finalSsid.isEmpty() || finalSsid.equals(unknown, true)) {
            MXLog.d("PairWiFi", "SSID is empty or unknown, skipping input")
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            binding.etWifiSsid.setText(finalSsid)
            checkInputWifi5G()
            val pwd = PairWiFiSPManage.getWifiPwd(finalSsid)
            if (pwd.isNotEmpty()) {
                binding.etWifiPwd.setText(pwd)
            } else {
                binding.etWifiPwd.requestFocus()
            }
        }
    }

    private fun toPage(skip: Boolean = false) {
        if (isChangeWifi) {
            sendMeshWiFi()
        } else {
            if (!skip) {
                val ssid = binding.etWifiSsid.text.toString()
                val password = binding.etWifiPwd.text.toString()
                //暂存Wi-Fi信息
                PairDataCenter.setCacheWifi(ssid, password)
            }
            if (replacedIotId.isNullOrEmpty()) {
                MXLog.d("go to pairing")
                //正常配网，自动发现页，用户已选待配网设备
                RouterManager.navigation(RouterConstants.PAIRING_ACTIVITY)
            } else {
                //一键替换
                RouterManager.navigation(RouterConstants.ONE_KEY_REPLACE_ACTIVITY)
            }
            finish()
        }
    }

    private fun sendMeshWiFi() {
        if (null == changeWifiDeviceUUID) return
        if (!MXMesh.isConnected()) {
            //mesh 未连接
            ToastUtil.showToast(this, getString(com.mxchip.mxapp.base.R.string.mx_sync_unconnect_title))
            return
        }
        showLoading()
        val ssid = binding.etWifiSsid.text.toString()
        val password = binding.etWifiPwd.text.toString()
        val wifiHex = ByteUtil.bytesToHexString(ssid.toByteArray())
        val pwdHex = ByteUtil.bytesToHexString(password.toByteArray())
        //根据实际协议文档
        val attrType = "0011"
        //组装wifi hex
        //外部指定消息TID，并暂存，用于自行移除callback ，msgId的生成必须使用sdk提供的generateTid()
        val msgId = MeshMessageUtil.getNextMeshMessageTid()
        val hex =
            MeshSDKManage.stringReversedToLittleEndian(attrType) + wifiHex + "00" + pwdHex + "00"
        MeshSDKManage.setWiFi(changeWifiDeviceUUID!!, hex, msgId, object : MapCallback() {
            override fun onResult(resultMap: Map<String, Any>) {
                if (resultMap.containsKey(MeshConstants.KEY_DATA)) {
                    val dataArray = resultMap[MeshConstants.KEY_DATA] as ByteArray
                    val valueArray = ByteArray(dataArray.size - 2)
                    System.arraycopy(dataArray, 2, valueArray, 0, valueArray.size)
                    val rest = ByteUtil.bytesToInt(valueArray)
                    if (4 == rest) {
                        //wifi成功,手动移除messageCallback
                        MeshMessageUtil.removeMessageCallback(msgId, changeWifiDeviceUUID!!)
                        //存储使用过的Wi-Fi信息
                        PairWiFiSPManage.saveWifi(ssid, password)
                        lifecycleScope.launch(Dispatchers.Main) {
                            dismissLoading()
                            setResult(Activity.RESULT_OK)
                            finish()
                            ToastUtil.showToast(
                                this@PairWIFIActivity, getString(com.mxchip.mxapp.base.R.string.mx_wifi_change_success)
                            )
                        }
                    }
                }
            }

            override fun onError(callbackMsg: CallbackMsg) {
                //超时手动移除messageCallback
                MeshMessageUtil.removeMessageCallback(msgId, changeWifiDeviceUUID!!)
                super.onError(callbackMsg)
                MeshSDKManage.setUnAckMsg(
                    changeWifiDeviceUUID!!, listOf(Pair(MeshSDKManage.ATTR_TYPE_REBOOT_GATEWAY, ""))
                )
                lifecycleScope.launch(Dispatchers.Main) {
                    dismissLoading()
                    setResult(Activity.RESULT_CANCELED)
                    ToastUtil.showToast(
                        this@PairWIFIActivity, getString(com.mxchip.mxapp.base.R.string.mx_wifi_change_fail)
                    )
                }
            }
        })
    }

//    private fun show5GDialog() {
//        MXDialog.Builder(this).title(getString(com.mxchip.mxapp.base.R.string.mx_tips))
//            .content(getString(com.mxchip.mxapp.base.R.string.current_5g_wifi_desc))
//            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel), { dialog, _ ->
//                dialog.dismiss()
//            }).rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm), { dialog, _ ->
//                dialog.dismiss()
//                toPage()
//            }).create().show()
//    }

    private fun showWiFiRecord() {
        if (null == wifiRecordPop) {
            val allRecords = PairWiFiSPManage.getWifiSSIDArray() ?: return
            val records = if (allRecords.size > 5) {
                allRecords.copyOfRange(0, 5)
            } else allRecords
            wifiRecordPop = WiFiRecordPopupWindow(this)
            wifiRecordPop?.anchorView = binding.etWifiSsid
            val adapter = WiFiRecordAdapter(this, records).setItemClick {
                wifiRecordPop?.dismiss()
                binding.etWifiSsid.setText(it)
                binding.etWifiSsid.requestFocus()
                binding.etWifiSsid.setSelection(it.length)
                binding.etWifiPwd.setText(PairWiFiSPManage.getWifiPwd(it))
            }
            wifiRecordPop?.setAdapter(adapter)
        }
        wifiRecordPop?.show()
    }

    override fun onDestroy() {
        wifiNetworkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                MXLog.d("PairWiFi", "Unregister network callback in onDestroy failed: ${e.message}")
            }
        }
        wifiNetworkCallback = null
        super.onDestroy()
    }
}