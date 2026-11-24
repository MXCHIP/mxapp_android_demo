package com.mxchip.mxapp.device.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.utils.ShapeRadius
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.device.adapter.DeviceSyncMatterAdapter
import com.mxchip.mxapp.device.databinding.DeviceDgChooseMatterGatewayBinding

class ChooseMatterGatewayDialog(
    private val itemClickListener: (DeviceBean) -> Unit,
    private val list: List<DeviceBean>
) :
    DialogFragment() {

    private lateinit var binding: DeviceDgChooseMatterGatewayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DeviceDgChooseMatterGatewayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialog()
        initView()
    }

    private fun initDialog() {
        dialog?.window?.setBackgroundDrawable(null)
        binding.rootView.background = generateShape(
            requireContext().getColor(com.mxchip.mxapp.base.R.color.core_color_background_dialog), ShapeRadius(20, 20, 0, 0)
        )
        val attr = dialog?.window?.attributes
        attr?.width = WindowManager.LayoutParams.MATCH_PARENT
        attr?.gravity = Gravity.BOTTOM
        dialog?.window?.attributes = attr
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        trimSize()
    }

    private fun trimSize() {
        val height = if (list.size <= 1) 200.dp2px()
        else if (list.size < 5) WindowManager.LayoutParams.WRAP_CONTENT
        else 600.dp2px()
        val attr = dialog?.window?.attributes
        attr?.height = height
        dialog?.window?.attributes = attr
    }

    private fun initView() {
        binding.rvDevice.layoutManager = LinearLayoutManager(requireContext())
        val adapter = DeviceSyncMatterAdapter()
        adapter.setData(list)
        binding.rvDevice.adapter = adapter
        binding.close.setOnClickListener {
            dismiss()
        }
        adapter.setItemClick {
            itemClickListener.invoke(it)
            dismiss()
        }
        val mainColor = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.emptyView.setIconColor(mainColor)
    }
}