package com.mxchip.mxapp.base.device.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.mxchip.mxapp.base.R
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.databinding.DialogWifiGroupDeviceListBinding
import com.mxchip.mxapp.base.device.adapter.WifiGroupDeviceDialogAdapter
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.ToastUtil
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape

/**
 * wifi群组设备列表
 */
class WifiGroupDeviceListDialog : DialogFragment() {

    private lateinit var binding: DialogWifiGroupDeviceListBinding
    private var groupId = 0
    private var _deviceList: List<DeviceBean>? = null
    private var selectedIotIds: List<String>? = null

    private val adapter by lazy { WifiGroupDeviceDialogAdapter() }

    fun setData(groupId: Int, deviceList: List<DeviceBean>?, selectedIotIds: List<String>?) {
        this.groupId = groupId
        _deviceList = deviceList
        this.selectedIotIds = selectedIotIds
        selectedIotIds?.apply {
            adapter.setSelectedIotIdList(this)
        }
        if (deviceList.isNullOrEmpty())
            binding.emptyView.visibility = View.VISIBLE
        else {
            binding.emptyView.visibility = View.GONE
            adapter.setData(deviceList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogWifiGroupDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialog()
        initView()
        initEvent()
    }

    private fun initDialog() {
        dialog?.window?.setBackgroundDrawable(null)
        binding.rootView.background = generateShape(
            requireContext().getColor(com.mxchip.mxapp.base.R.color.core_color_background_main),
            ShapeRadius(20, 20, 0, 0)
        )
        val height = when {
            _deviceList.isNullOrEmpty() -> WindowManager.LayoutParams.WRAP_CONTENT
            _deviceList!!.size == 1 -> 200.dp2px()
            _deviceList!!.size < 5 -> WindowManager.LayoutParams.WRAP_CONTENT
            else -> 600.dp2px()
        }
        val attr = dialog?.window?.attributes
        attr?.width = WindowManager.LayoutParams.MATCH_PARENT
        attr?.height = height
        attr?.gravity = Gravity.BOTTOM
        dialog?.window?.attributes = attr
    }

    private fun initView() {
        binding.title.setText(if (groupId == 0) R.string.mx_select_device else R.string.mx_group_manage)
        binding.deviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.deviceList.adapter = adapter
        val mainColor = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.setIconColor(mainColor)
    }

    private fun initEvent() {
        binding.cancel.setOnClickListener {
            dismiss()
        }

        binding.nextStep.setOnClickListener {
            save()
        }
    }

    private fun save() {
        val selectList = adapter.getSelectedIotIdList()
        if (selectList.isEmpty()) {
            ToastUtil.showLongToast(requireContext(), getString(com.mxchip.mxapp.base.R.string.mx_select_device_hint))
            return
        }
        dismiss()
        val selected = adapter.getData().filter { it.iotid in selectList }
        wifiGroupListener?.invoke(selected)
    }

    private var wifiGroupListener: ((List<DeviceBean>) -> Unit)? = null

    fun setWifiGroupListener(listener: (List<DeviceBean>) -> Unit) {
        wifiGroupListener = listener
    }
}