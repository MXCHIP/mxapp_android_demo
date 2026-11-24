package com.mxchip.mxapp.ui.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mxchip.http.response.NetState
import com.mxchip.log.MXLog
import com.mxchip.mxapp.R
import com.mxchip.mxapp.base.base.MXBusinessFragment
import com.mxchip.mxapp.base.bean.DeviceBean
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.base.consts.DeviceTypeConstants
import com.mxchip.mxapp.base.consts.HomeConstants
import com.mxchip.mxapp.base.consts.HomeDataRefresh
import com.mxchip.mxapp.base.consts.LiveDataBusKey
import com.mxchip.mxapp.base.consts.RoleConstants
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.router.RouterManager
import com.mxchip.mxapp.base.utils.AppConfigManager
import com.mxchip.mxapp.base.utils.JsonUtil
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.bean.WeatherBean
import com.mxchip.mxapp.consts.SmartHomeConstants
import com.mxchip.mxapp.databinding.FragmentRoomBinding
import com.mxchip.mxapp.family.bean.HomeBean
import com.mxchip.mxapp.family.bean.LocationBean
import com.mxchip.mxapp.family.vm.HomeViewModel
import com.mxchip.mxapp.room.consts.RoomConstants
import com.mxchip.mxapp.room.consts.RoomConstants.KEY_ORDER_ROOM
import com.mxchip.mxapp.room.viewmodel.RoomViewModel
import com.mxchip.mxapp.ui.MainActivity
import com.mxchip.mxapp.ui.RoomManagerPop
import com.mxchip.mxapp.utils.AppDataCenter
import com.mxchip.mxapp.utils.DeviceUIStateUtil
import com.mxchip.mxapp.viewmodel.MainViewModel
import com.mxchip.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


/**
 * 首页房间列表
 */
class RoomFragment : MXBusinessFragment<FragmentRoomBinding>(), IHomeRoleChange {

    private val vm by viewModels<MainViewModel>()
    private val roomVM by viewModels<RoomViewModel>()
    private val homeVM by viewModels<HomeViewModel>()
    private val fragments = ArrayList<IRoomRefresh>()
    private var mRoomList: List<RoomBean>? = null
    private var selectedTabPosition = 0
    private var tabLayoutMediator: TabLayoutMediator? = null

    private val adapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                val fragment = fragments[position]
                return fragment as Fragment
            }

            // 实现 getItemId 方法，帮助 ViewPager2 识别 fragment 的变化
            // 基于 room_id 生成唯一 ID，确保 ViewPager2 能正确刷新
            override fun getItemId(position: Int): Long {
                if (position < 0 || position >= fragments.size) {
                    return position.toLong()
                }
                // 根据 room_id 生成唯一 ID
                val roomId = when {
                    mRoomList != null && position < mRoomList!!.size -> {
                        mRoomList!![position].room_id
                    }

                    else -> position
                }
                // 使用 room_id 作为唯一标识，0 表示全屋，-1 表示分享设备
                return roomId.toLong()
            }

            // 重写 containsItem 方法，确保 ViewPager2 能正确识别 item 是否相同
            override fun containsItem(itemId: Long): Boolean {
                if (mRoomList == null) return false
                val roomId = itemId.toInt()
                return mRoomList!!.any { it.room_id == roomId }
            }
        }
    }

    override fun getLayoutBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentRoomBinding {
        return FragmentRoomBinding.inflate(inflater)
    }

    override fun onInit(view: View, savedInstanceState: Bundle?) {
        binding.viewPager2.isUserInputEnabled = false
        updateHomeRoleState(RoleManager.CURRENT_HOME_ROLE)
        initData()
        inject()
        initEvent()
        queryData()
    }

    private fun queryData() {
        if (AppConfigManager.getUseHomeId() > 0) {
            lifecycleScope.launch(Dispatchers.IO) {
                val sharedDeviceWait = async { vm.findSharedDevices(1) }
                val roomListWait = async {
                    roomVM.roomListV4(
                        mapOf(
                            "home_id" to AppConfigManager.getUseHomeId(),
                            "page_no" to 1,
                            "page_size" to 199
                        )
                    )
                }
                val roomListResp = roomListWait.await()
                val sharedDeviceResp = sharedDeviceWait.await()
                roomListResp.collect {
                    binding.refresh.isRefreshing = false
                    if (it.state == NetState.SUCCESS) {
                        JsonUtil.fromJsonMXList(it.data, RoomBean::class.java)?.apply {
                            val newRoomList = mutableListOf<RoomBean>()
                            newRoomList.addAll(this)
                            sharedDeviceResp.collect { sharedNet ->//账号分享设备
                                val sharedDeviceList = JsonUtil.fromJsonMXList(
                                    sharedNet.data,
                                    DeviceBean::class.java
                                )
                                if (sharedDeviceList.isNullOrEmpty().not()) {
                                    newRoomList.add(
                                        RoomBean(
                                            -1,
                                            getString(com.mxchip.mxapp.base.R.string.mx_shared_device)
                                        )
                                    )
                                }
                            }
                            // 检查是否有任何变化（room_id 或 name 变化，或者数量变化）
                            val oldRoomList = mRoomList
                            val hasAnyChange =
                                if (oldRoomList == null || oldRoomList.size != newRoomList.size) {
                                    // 数量变化，需要全部重新创建
                                    true
                                } else {
                                    // 检查每个房间的 room_id 和 name 是否都相同
                                    newRoomList.any { newRoom ->
                                        oldRoomList.find { old -> old.room_id == newRoom.room_id } == null
                                    }
                                }

                            if (hasAnyChange) {
                                // 有变化，全部重新创建 fragment
                                mRoomList = newRoomList
                                fragments.clear()
                                newRoomList.forEach { roomBean ->
                                    if (roomBean.room_id == 0) {//全屋
                                        fragments.add(SmartHomeFragment())
                                    } else {
                                        val room = RoomInfoFragment()
                                        room.arguments = Bundle().apply {
                                            putInt(RoomConstants.KEY_ROOM_ID, roomBean.room_id)
                                        }
                                        fragments.add(room)
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    // 更新 ViewPager2 和 TabLayout
                                    initViewPaging()

                                    // 使用 postDelayed 延迟刷新，确保 ViewPager2 已经创建了 fragment
                                    // 刷新当前可见的 fragment 和附近的 fragment（因为 ViewPager2 懒加载）
                                    binding.viewPager2.postDelayed({
                                        val currentPos = binding.viewPager2.currentItem
                                        val offscreenPageLimit =
                                            binding.viewPager2.offscreenPageLimit

                                        // 刷新当前可见的 fragment 和附近的 fragment
                                        val startPos =
                                            (currentPos - offscreenPageLimit).coerceAtLeast(0)
                                        val endPos =
                                            (currentPos + offscreenPageLimit).coerceAtMost(fragments.size - 1)

                                        for (i in startPos..endPos) {
                                            if (i >= 0 && i < fragments.size) {
                                                val fragment = fragments[i]
                                                // 检查 fragment 是否已经被创建（isAdded）
                                                if (fragment is Fragment && fragment.isAdded) {
                                                    fragment.onRefresh(null)
                                                }
                                            }
                                        }
                                    }, 100) // 延迟 100ms，确保 fragment 已经被创建
                                }
                            } else {
                                // 没有任何变化，只更新 mRoomList（用于 TabLayout 文本更新）和刷新数据
                                mRoomList = newRoomList

                                withContext(Dispatchers.Main) {
                                    // 只刷新所有 fragment 的数据，不重新创建
                                    fragments.forEach { fragment ->
                                        fragment.onRefresh(null)
                                    }

                                    // 通知 TabLayout 更新（TabLayoutMediator 会自动从 mRoomList 读取最新文本）
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    } else {
                        val netErr = onException(it.state.code)
                        withContext(Dispatchers.Main) {
                            if (!netErr) showToast("${it.state.message}")
                        }
                    }
                }
            }
            getWeather(
                AppConfigManager.getUseHomeLocationKey(),
                AppConfigManager.getUseHomeLocationCity()
            )
        } else {
            LiveDataBus.with<Int>(LiveDataBusKey.KEY_HOME_REFRESH).postValue(1)
            binding.refresh.isRefreshing = false
        }
    }

    private fun initViewPaging() {
        if (fragments.isEmpty()) {
            MXLog.w("RoomFragment", "fragments list is empty, skip initViewPaging")
            return
        }

        // 设置或更新 adapter
        binding.viewPager2.adapter = adapter

        // 设置或更新 TabLayoutMediator
        tabLayoutMediator?.detach()
        tabLayoutMediator = TabLayoutMediator(
            binding.tabLayoutRoom, binding.viewPager2, true
        ) { tab, position ->
            tab.text =
                if (mRoomList != null && position < mRoomList!!.size) mRoomList!![position].name
                else ""
        }
        tabLayoutMediator?.attach()

        // 更新选中的位置
        val newPos =
            if (selectedTabPosition < fragments.size) selectedTabPosition else 0
        selectedTabPosition = newPos
        binding.viewPager2.setCurrentItem(newPos, false)
    }

    private fun checkSharedDeviceFragment() {
        vm.findSharedDevices(1)
            .launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlowList<DeviceBean>(
                    success = {
                        //是否已经存在分享设备fragment
                        val exist = mRoomList?.firstOrNull { roomBean -> roomBean.room_id == -1 }
                        if (exist != null) {
                            //已存在分享fragment
                            if (it.isNullOrEmpty()) {
                                //账号下已无分享设备，需要移除
                                mRoomList = mRoomList!!.filter { old -> old.room_id != -1 }
                                if (fragments.isNotEmpty()) {
                                    fragments.removeAt(fragments.size - 1)
                                    // 调用 initViewPaging() 确保 ViewPager2 和 TabLayout 同步更新
                                    initViewPaging()
                                }
                            } else {
                                //直接刷新分享设备fragment
                                if (fragments.isNotEmpty()) {
                                    fragments[fragments.size - 1].onRefresh(null)
                                }
                            }
                        } else {
                            //不存在分享fragment
                            if (it.isNullOrEmpty().not()) {
                                //账号下有分享设备
                                val newRoomList = mutableListOf<RoomBean>()
                                newRoomList.addAll(mRoomList!!)
                                newRoomList.add(
                                    RoomBean(
                                        -1,
                                        getString(com.mxchip.mxapp.base.R.string.mx_shared_device)
                                    )
                                )
                                mRoomList = newRoomList
                                val room = RoomInfoFragment()
                                room.arguments =
                                    Bundle().apply { putInt(RoomConstants.KEY_ROOM_ID, -1) }
                                fragments.add(room)
                                initViewPaging()
                            }
                        }
                    }
                ))
    }

    private fun inject() {
        //设备mesh数据
        LiveDataBus.with<Map<String, Any>>(LiveDataBusKey.KEY_DEVICE_MESH_DATA).observe(this) {
            meshMessageData(it)
        }
        //云端socket数据
        LiveDataBus.with<String>(LiveDataBusKey.KEY_DEVICE_CLOUD_DATA).observe(this) {
            val json = JSONObject(it)
            ilopMessageData(json)
        }

        LiveDataBus.with<Int>(KEY_ORDER_ROOM).observe(this) {
            queryData()
        }

        LiveDataBus.with<HomeDataRefresh>(LiveDataBusKey.KEY_HOME_DATA_REFRESH).observe(this) {
            when (it) {
                HomeDataRefresh.HOME, HomeDataRefresh.ROOM_LIST, HomeDataRefresh.DEVICE, HomeDataRefresh.SCENE -> {
                    queryData()
                }

                HomeDataRefresh.SHARED_DEVICE -> {
                    //分享设备变化
                    checkSharedDeviceFragment()
                }

                else -> {}
            }
        }

        LiveDataBus.with<Int>(SmartHomeConstants.KEY_SWITCH_ROOM_TAB).observe(this) { pos ->
            binding.viewPager2.setCurrentItem(pos, false)
        }

        //家庭位置更新
        LiveDataBus.with<LocationBean>(HomeConstants.LOCATION_SELECTED)
            .observe(this) { locationBean ->
                AppConfigManager.updateUseHomeLocation(
                    locationBean.location_key,
                    locationBean.city_name
                )
                getWeather(locationBean.location_key, locationBean.city_name)
            }
    }

    private fun initData() {
        binding.viewPager2.offscreenPageLimit = 4
        // adapter 和 TabLayoutMediator 在 initViewPaging() 中设置，确保 fragments 和 mRoomList 已填充
    }

    private fun initEvent() {

//        binding.bleState.setOnClickListener {
//            val activity = requireActivity()
//            if (activity is MainActivity)
//                activity.checkPermissionAndStartMesh(userOperation = true)
//        }
        binding.tabLayoutRoom.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTabPosition = tab.position
                // 获取选中Tab的TextView，并设置加粗
                val tabTextView = (tab.view as LinearLayout).getChildAt(1) as TextView
                tabTextView.setTypeface(null, Typeface.BOLD)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // 获取未选中Tab的TextView，并恢复正常字体
                val tabTextView = (tab.view as LinearLayout).getChildAt(1) as TextView
                tabTextView.setTypeface(null, Typeface.NORMAL)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })

        binding.refresh.setOnRefreshListener {
            if (fragments.isEmpty()) {
                queryData()
                return@setOnRefreshListener
            }
            //产品要求，每次下拉刷新，所有房间都需要刷新
//            fragments[selectedTabPosition].onRefresh {
//                binding.refresh.isRefreshing = false
//            }
            fragments.forEach {
                it.onRefresh(null)
            }
            apiGtWeather()
            binding.refresh.isRefreshing = false
        }

        binding.ivEdit.setOnClickListener {
            showPop()
        }
        binding.ivEdit.setOnLongClickListener {
            DeviceUIStateUtil.isDebugH5 = true
            true
        }
        //去设置家庭位置
        binding.layoutLocation.setOnClickListener {
            if (RoleManager.isOwner()) { // 家庭所有者
                RouterManager.navigation(
                    RouterConstants.LOCATION_ACTIVITY, mapOf(
                        HomeConstants.KEY_HOME_ID to AppConfigManager.getUseHomeId()
                    )
                )
            }
        }
    }

    override fun updateHomeRoleState(role: Int) {
        if (isAdded.not()) return
        if (RoleConstants.COMMON == role) {
            binding.ivEdit.visibility = View.GONE
        } else {
            binding.ivEdit.visibility = View.VISIBLE
        }
    }

    private fun showPop() {
        val pop = RoomManagerPop(requireActivity())
        pop.roomManageListener {
            pop.dismiss()
            RouterManager.navigation(
                RouterConstants.ROOM_MANAGE_ACTIVITY, mapOf(
                    HomeConstants.KEY_HOME_ID to AppConfigManager.getUseHomeId()
                )
            )

        }.deviceManageListener {
            pop.dismiss()
            RouterManager.navigation(
                RouterConstants.DEVICE_MANAGER_ACTIVITY, mapOf(
                    DeviceConstants.KEY_DEVICE_TYPE to DeviceTypeConstants.DEVICE
                )
            )

        }
        pop.show()
    }

    fun readSmartHomeGuideShowListData() {
        val smartHomeFragment = fragments[0]
        if (smartHomeFragment is SmartHomeFragment && smartHomeFragment.isAdded)
            smartHomeFragment.onRefresh(null)
    }

    private fun meshMessageData(dtMap: Map<String, Any>) {
        fragments.forEach {
            it.meshMessageData(dtMap)
        }
    }

    private fun ilopMessageData(ilopData: JSONObject) {
        fragments.forEach {
            it.ilopMessageData(ilopData)
        }
    }

    private fun apiGtWeather() {
        lifecycleScope.launch(Dispatchers.IO) {
            homeVM.findHomes().collect {
                val home = JsonUtil.fromJsonMXList(it.data, HomeBean::class.java)
                if (home != null) {
                    val cut = home.firstOrNull { hb -> hb.current_home }
                    if (cut != null) {
                        withContext(Dispatchers.Main) {
                            getWeather(cut.location_key, cut.city_name)
                        }
                    }
                }
            }
        }
    }

    private fun getWeather(locationKey: String?, city: String?) {
        binding.tvWeatherCity.text = city
        if (!locationKey.isNullOrEmpty()) {
            binding.layoutLocation.visibility = View.VISIBLE
            homeVM.getWeatherOneHour(locationKey).launchAndCollectIn(
                this, Lifecycle.State.CREATED, loadFlow<WeatherBean>(
                    success = { weather ->
                        weather?.apply {//未设置定位
                            binding.tvGetLocation.visibility = View.GONE
                            binding.ivWeatherIcon.visibility = View.VISIBLE
                            binding.layoutCity.visibility = View.VISIBLE
                            when (weather.weather_icon) {
                                1, 2 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_1_2)
                                3, 4, 5, 6 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_3_4_5_6)
                                7, 8 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_7_8)
                                11, 25 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_11_25)
                                12, 13, 14, 16, 17 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_12_13_14_16_17)
                                15, 18, 26, 29 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_15_18_26_29)
                                19, 22 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_19_22)
                                20, 21, 23 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_20_21_23)
                                32 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_32)
                                33, 34 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_33_34)
                                35, 36, 37, 38 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_35_36_37_38)
                                39, 40, 41, 42 -> binding.ivWeatherIcon.setImageResource(R.mipmap.weather_39_40_41_42)
                                else -> binding.ivWeatherIcon.load(weather.icon_image)
                            }
                            binding.tvWeatherTemperature.text = "${weather.temperature}°C"
                        }
                    })
            )
        } else if (RoleManager.isOwner()) {
            binding.layoutLocation.visibility = View.VISIBLE
            binding.tvGetLocation.visibility = View.VISIBLE
            binding.ivWeatherIcon.visibility = View.GONE
            binding.layoutCity.visibility = View.GONE
        } else { // 非家主权限不能设置
            binding.layoutLocation.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        // 清理 TabLayoutMediator，避免内存泄漏
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        super.onDestroyView()
    }
}