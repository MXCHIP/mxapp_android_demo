package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.device.databinding.DeviceAcDeviceShareListBinding
import com.mxchip.router.annotation.Router

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2021/9/16
 * @desc:
 */
@Router(RouterConstants.DEVICE_SHARE_LIST_ACTIVITY)
class DeviceShareListActivity : MXBusinessActivity<DeviceAcDeviceShareListBinding>() {
    override fun getLayoutBinding() = DeviceAcDeviceShareListBinding.inflate(layoutInflater)

    private val fragments = arrayListOf(MineDeviceSharedFragment(), ShareDeviceFragment())
    private val tabItems by lazy(LazyThreadSafetyMode.NONE) {
        arrayOf(getString(com.mxchip.mxapp.base.R.string.mx_my_device), getString(com.mxchip.mxapp.base.R.string.mx_shared_device))
    }

    private var isEdit = false

    private val adapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }
        }
    }

    override fun onInit() {
        initView()
        initEvent()
    }

    private fun initView() {
        binding.viewpager.offscreenPageLimit = 2
        binding.viewpager.adapter = adapter
        val tabLayoutMediator = TabLayoutMediator(
            binding.deviceShareTab,
            binding.viewpager,
            true
        ) { tab, position ->
            tab.text = tabItems[position]
        }
        tabLayoutMediator.attach()
        val mainColor = ContextCompat.getColor(this, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
        binding.deviceShareTab.setSelectedTabIndicatorColor(mainColor)
        binding.deviceShareTab.setTabTextColors(
            ContextCompat.getColor(
                this,
                com.mxchip.mxapp.base.R.color.core_color_text_list_content
            ), mainColor
        )
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }.right {
            if (!isEdit) {
                edit(true)
            } else {
                shareDevice()
            }
        }


        binding.tvLeft.setOnClickListener {
            edit(false)
        }

        binding.deviceShareTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                edit(false)
                if (tab?.position == 1) {
                    binding.toolbar.rightText(null)
                } else {
                    binding.toolbar.rightText(getString(com.mxchip.mxapp.base.R.string.mx_select))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
    }

    private fun edit(edit: Boolean) {
        isEdit = edit
        if (edit) {
            binding.tvLeft.visibility = View.VISIBLE
            binding.toolbar.showBack(false).rightText(getString(com.mxchip.mxapp.base.R.string.mx_next))
        } else {
            binding.tvLeft.visibility = View.GONE
            binding.toolbar.showBack(true).rightText(getString(com.mxchip.mxapp.base.R.string.mx_select))
            (fragments[0] as MineDeviceSharedFragment).getSelectList()?.clear()
        }
        (fragments[0] as MineDeviceSharedFragment).edit(edit)
    }

    private fun shareDevice() {
        val list = (fragments[0] as MineDeviceSharedFragment).getSelectList()
        if (list.isNullOrEmpty()) {
            return
        }
        if (RoleManager.isUser()) {
            RoleManager.showDialog(this)
            return
        }
        val iotIds = ArrayList<String>()
        list.forEach {
            iotIds.add(it.iotid)
        }
        RouterManager.navigation(
            RouterConstants.DEVICE_SHARE_ACCOUNT_ACTIVITY, mapOf(
                "iotids" to iotIds
            )
        )
    }
}