package com.mxchip.mxapp.room.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.king.zxing.util.CodeUtils
import com.mxchip.http.response.NetState
import com.mxchip.mxapp.base.utils.dp2px
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.room.databinding.RoomDgQrcodeBinding
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/5/20
 * @desc:
 */
class QrcodeDialog(private val homeId: Int) : DialogFragment() {

    private lateinit var binding: RoomDgQrcodeBinding
    private val roomViewModel by viewModels<RoomViewModel>()
    private var role = 2
    private val roomIds = arrayListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RoomDgQrcodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog?.window
        val params = window?.attributes
        params?.gravity = Gravity.CENTER
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = params
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        binding.root.setBackgroundColor(Color.TRANSPARENT)
        binding.close.background =
            generateStoke(
                2,
                requireContext().getColor(com.mxchip.mxapp.base.R.color.core_color_background_dialog),
                14
            )
        isCancelable = false
        binding.tvScanUse.text = String.format(
            getString(com.mxchip.mxapp.base.R.string.mx_join_family_scan_use),
            getString(com.mxchip.mxapp.base.R.string.mx_app_name)
        )
        binding.close.setOnClickListener {
            dismiss()
        }
        val mainColor = ContextCompat.getColor(requireContext(), com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.tvRefresh.setTextColor(mainColor)
        binding.tvRefresh.setOnClickListener {
            getInviteCode(homeId)
        }
    }

    fun setQrcode(bitmap: Bitmap) = apply {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(500)
            binding.ivQrcode.setImageBitmap(bitmap)
        }
    }

    fun setRole(role: Int) = apply {
        this.role = role
    }

    fun setRoomIds(roomIds: List<Int>) = apply {
        this.roomIds.clear()
        this.roomIds.addAll(roomIds)
    }

    private fun getInviteCode(homeId: Int) {
        roomViewModel.generateInviteCode(0, homeId, role, roomIds)
            .launchAndCollectIn(this, Lifecycle.State.CREATED) { netState ->
                if (netState.state == NetState.SUCCESS) {
                    netState.data?.apply {
                        val json = JSONObject(this)
                        val content = json.getString("code")
                        val bitmap = CodeUtils.createQRCode(content, 180.dp2px())
                        bitmap?.apply {
                            binding.ivQrcode.setImageBitmap(this)
                        }
                    }
                }
            }
    }
}