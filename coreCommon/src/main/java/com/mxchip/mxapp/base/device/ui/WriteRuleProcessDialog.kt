package com.mxchip.mxapp.base.device.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.MXBusinessApplication
import com.mxchip.mxapp.base.bean.DeviceWriteStateBean
import com.mxchip.mxapp.base.databinding.CoreDgWriteRuleProcessBinding
import com.mxchip.mxapp.base.device.adapter.WriteRuleProcessDialogAdapter
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WriteRuleProcessDialog(
    private val dialogCreated: () -> Unit,
    private val okClick: () -> Unit,
    private val retryClick: (() -> Unit)? = null
) : DialogFragment() {

    private val adapter by lazy {
        WriteRuleProcessDialogAdapter { pos ->
            clickSyncFailed(pos)
        }
    }
    private lateinit var binding: CoreDgWriteRuleProcessBinding

    //数据的标识ID，例：场景id，群组id...
    private var tagId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CoreDgWriteRuleProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialog()
        initView()
        dialogCreated.invoke()
    }

    private fun initDialog() {
        dialog?.window?.setBackgroundDrawable(null)
        binding.rootView.background =
            generateShape(requireContext().getColor(com.mxchip.mxapp.base.R.color.core_color_background_dialog), 20)
        val attr = dialog?.window?.attributes
        attr?.width = WindowManager.LayoutParams.MATCH_PARENT
        attr?.gravity = Gravity.CENTER
        attr?.horizontalMargin = 24.dp2px().toFloat()
        dialog?.window?.attributes = attr
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
    }

    private fun initView() {
        binding.rvDevice.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDevice.adapter = adapter
        binding.ok.setOnClickListener {
            dismiss()
            okClick.invoke()
        }
        if (null == retryClick) {
            binding.retry.visibility = View.GONE
            binding.middleLine.visibility = View.GONE
        } else
            binding.retry.setOnClickListener {
                retryClick.invoke()
            }
    }

    /**
     * 数据的标识ID，用于更新item状态，例：场景id，群组id
     */
    fun setDataTagId(id: Int) {
        tagId = id
    }

    fun getDataTagId(): Int = tagId

    fun getData() = adapter.getDatas()

    fun setDatas(datas: List<DeviceWriteStateBean>) {
        binding.ok.alpha = 0.6f
        binding.ok.isEnabled = false
        adapter.setData(datas)
    }

    fun writeRule(position: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.writing(position)
            binding.rvDevice.scrollToPosition(position)
            binding.ok.text =
                String.format(
                    MXBusinessApplication.mxBusinessApp.getString(com.mxchip.mxapp.base.R.string.mx_syncing),
                    position + 1,
                    adapter.itemCount
                )
        }
    }

    fun success(position: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.success(position)
        }
    }

    fun failed(position: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.failed(position)
        }
    }

    fun reStartState() {
        if (null != retryClick) {
            binding.retry.visibility = View.GONE
            binding.middleLine.visibility = View.GONE
        }
    }

    fun complete() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.rvDevice.scrollToPosition(0)
            binding.ok.alpha = 1f
            binding.ok.isEnabled = true
            binding.ok.setText(com.mxchip.mxapp.base.R.string.mx_ok)
            if (null != retryClick) {
                val syncFailedCount = adapter.getDatas()
                    .filter { it.writeState == WriteRuleProcessDialogAdapter.STATE_FAILED }.size
                if (syncFailedCount > 0) {
                    binding.retry.visibility = View.VISIBLE
                    binding.middleLine.visibility = View.VISIBLE
                    binding.retry.text =
                        MXBusinessApplication.mxBusinessApp.getString(com.mxchip.mxapp.base.R.string.mx_retry) + " ($syncFailedCount)"
                }
            }
            adapter.complete()
        }
    }

    fun reset() {
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.reset()
        }
    }


    fun hasFailed() = adapter.hasFailed()

    fun getFailedData(): List<DeviceWriteStateBean> {
        return adapter.getDatas()
            .filter {
                val oldWriteState = it.writeState
                it.writeState = -1
                oldWriteState == WriteRuleProcessDialogAdapter.STATE_FAILED
            }
    }

    private fun clickSyncFailed(position: Int) {
        val device = adapter.getDatas()[position]
        RouterManager.navigation(
            RouterConstants.DEVICE_SYNC_DATA_FAILED, mapOf(
                "icon" to (device.product_image ?: ""),
                "name" to (device.nick_name ?: device.product_name!!),

                )
        )
    }
}