package com.mxchip.mxapp.device.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.device.adapter.ChooseSyncRoomAdapter
import com.mxchip.mxapp.device.databinding.DeviceDgChooseRoomSyncBinding

class ChooseSyncRoomDialog(
    private val created: () -> Unit,
    private val sync: () -> Unit,
) : DialogFragment() {

    private val adapter by lazy { ChooseSyncRoomAdapter() }

    private lateinit var binding: DeviceDgChooseRoomSyncBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DeviceDgChooseRoomSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialog()
        initView()
        adapter.clearSelectedRoom()//清除已选择的房间
        created.invoke()
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
        binding.rvRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRooms.adapter = adapter
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.btnStart.setOnClickListener {
            if (adapter.getSelectedRoomIds().isNotEmpty()) {
                dismiss()
                sync.invoke()
            }
        }
    }

    fun setRoomData(rooms: List<RoomBean>) {
        adapter.setData(rooms)
    }

    fun getSelectedRoomIds() = adapter.getSelectedRoomIds()
}