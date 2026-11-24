package com.mxchip.mxapp.device.ui

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mxchip.router.annotation.Router
import com.mxchip.mxapp.device.bean.BaseBean
import com.mxchip.mxapp.device.consts.CCSmartEnum
import com.mxchip.mxapp.device.vm.CCSmartViewModel
import com.mxchip.mxapp.base.base.MXBusinessActivity
import com.mxchip.mxapp.base.consts.CommonConstants
import com.mxchip.mxapp.base.device.consts.DeviceConstants
import com.mxchip.mxapp.base.dialog.MXDialog
import com.mxchip.mxapp.base.router.RouterConstants
import com.mxchip.mxapp.base.utils.launchAndCollectIn
import com.mxchip.mxapp.base.utils.loadFlow
import com.mxchip.mxapp.base.utils.loadFlowList
import com.mxchip.mxapp.base.utils.vibrateShot
import com.mxchip.mxapp.device.adapter.ccsmart.CCSmartInAdapter
import com.mxchip.mxapp.device.adapter.ccsmart.CCSmartOutAdapter
import com.mxchip.mxapp.device.databinding.DeviceAcCcsmartDeviceListBinding
import java.util.Collections

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 2022/2/14
 * @desc:
 */
@Router(RouterConstants.CENTER_CONTROL_SMART_ACTIVITY)
class CenterControlSmartListActivity : MXBusinessActivity<DeviceAcCcsmartDeviceListBinding>() {
    override fun getLayoutBinding() = DeviceAcCcsmartDeviceListBinding.inflate(layoutInflater)

    private val vm by viewModels<CCSmartViewModel>()
    private lateinit var iotId: String
    private lateinit var listType: String
    private var moved = false
    private var modify = false
    private val inAdapter by lazy {
        CCSmartInAdapter(this) {
            showRemoveDialog(it)
        }
    }

    private fun showRemoveDialog(baseBean: BaseBean) {
        val title: String
        val content: String
        when (listType) {
            DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> {
                title = getString(com.mxchip.mxapp.base.R.string.ccs_del_group_title)
                content = getString(com.mxchip.mxapp.base.R.string.ccs_del_group_des)
            }

            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> {
                title = getString(com.mxchip.mxapp.base.R.string.ccs_del_scene_title)
                content = getString(com.mxchip.mxapp.base.R.string.ccs_del_scene_des)
            }

            else -> {
                title = getString(com.mxchip.mxapp.base.R.string.ccs_del_device_title)
                content = getString(com.mxchip.mxapp.base.R.string.ccs_del_device_des)
            }
        }
        MXDialog.Builder(this).title(title).content(content)
            .leftButton(getString(com.mxchip.mxapp.base.R.string.mx_cancel)) { alertDialog, _ ->
                alertDialog.dismiss()
            }.rightButton(getString(com.mxchip.mxapp.base.R.string.mx_confirm), { alertDialog, _ ->
                alertDialog.dismiss()
                modify = true
                binding.clOut.visibility = View.VISIBLE
                inAdapter.deleteCCS(baseBean)
                outAdapter.deleteFromCCS(baseBean)
                checkInEmpty()
            }).create().show()
    }

    private val outAdapter by lazy {
        CCSmartOutAdapter(this) {
            modify = true
            addToCCS(it)
        }
    }

    private fun addToCCS(bean: BaseBean) {
        binding.ccsmartInEmpty.visibility = View.GONE
        binding.ccsmartInList.visibility = View.VISIBLE
        outAdapter.addToCCS(bean)
        inAdapter.addCCS(bean)
    }

    private fun checkInEmpty() {
        if (inAdapter.getList().isEmpty()) {
            binding.ccsmartInEmpty.visibility = View.VISIBLE
            binding.ccsmartInList.visibility = View.GONE
        }
    }

    override fun onInit() {
        listType = intent.getStringExtra("listType") ?: ""
        iotId = intent.getStringExtra(CommonConstants.KEY_IOT_ID) ?: ""
        initView()
        initEvent()
        setAdapterType(listType)
        initData()
    }

    private fun initEvent() {
        binding.toolbar.left { finish() }
            .title(
                when (listType) {
                    DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> getString(com.mxchip.mxapp.base.R.string.mx_device_list)
                    DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> getString(com.mxchip.mxapp.base.R.string.mx_group_list)
                    DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> getString(com.mxchip.mxapp.base.R.string.mx_scene_list)
                    else -> "列表"
                }
            )
            .right {
                if (modify || moved) {
                    val updateType = when (listType) {
                        DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> CCSmartEnum.DEVICE
                        DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> CCSmartEnum.SCENE
                        DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> CCSmartEnum.GROUP
                        else -> CCSmartEnum.DEVICE
                    }
                    val ids = arrayListOf<Any>()
                    inAdapter.getList().forEach {
                        when (listType) {
                            DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> ids.add(it.iotid!!)
                            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> ids.add(it.scene_id!!)
                            DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> ids.add(it.group_id!!)
                        }
                    }
                    vm.update(updateType, iotId, ids).launchAndCollectIn(
                        this, Lifecycle.State.CREATED, loadFlow<Any>(
                            success = {
                                dismissLoading()
                                finish()
                            }, error = { code, msg, data ->
                                dismissLoading()
                                val netErr = onException(code)
                                if (!netErr) showToast("$msg")
                            }
                        )
                    )
                } else {
                    finish()
                }
            }
    }

    private fun initView() {
        binding.ccsmartIn.text = when (listType) {
            DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> getString(com.mxchip.mxapp.base.R.string.ccs_device_show)
            DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> getString(com.mxchip.mxapp.base.R.string.ccs_group_show)
            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> getString(com.mxchip.mxapp.base.R.string.ccs_scene_show)
            else -> ""
        }
        binding.ccsmartInList.layoutManager = LinearLayoutManager(this)
        binding.ccsmartInList.adapter = inAdapter

        binding.ccsmartOutList.layoutManager = LinearLayoutManager(this)
        binding.ccsmartOutList.adapter = outAdapter

        getItemTouchHelper().attachToRecyclerView(binding.ccsmartInList)
    }

    private fun initData() {
        if (listType == DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE) {
            getDeviceList(iotId)
        } else if (listType == DeviceConstants.ROUTER_CENTER_CONTROL_SCENE) {
            getSceneList(iotId)
        } else {
            getGroupList(iotId)
        }
    }

    private fun getDeviceList(iotId: String) {
        vm.getDeviceList(iotId, 0)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<BaseBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.ccsmartInEmpty.visibility = View.VISIBLE
                        binding.ccsmartInList.visibility = View.GONE
                    } else {
                        binding.ccsmartInEmpty.visibility = View.GONE
                        binding.ccsmartInList.visibility = View.VISIBLE
                        inAdapter.notifyList(it as ArrayList)
                    }
                }, error = { _, _, _ ->
                    binding.ccsmartInEmpty.visibility = View.VISIBLE
                    binding.ccsmartInList.visibility = View.GONE
                }
            ))


        vm.getDeviceList(iotId, 1)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<BaseBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.clOut.visibility = View.GONE
                    } else {
                        binding.clOut.visibility = View.VISIBLE
                        binding.ccsmartOut.text = when (listType) {
                            DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> getString(com.mxchip.mxapp.base.R.string.ccs_device_hide)
                            DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> getString(com.mxchip.mxapp.base.R.string.ccs_group_hide)
                            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> getString(com.mxchip.mxapp.base.R.string.ccs_scene_hide)
                            else -> ""
                        }
                        outAdapter.notifyList(it as ArrayList)
                    }
                }, error = { _, _, _ ->
                    binding.clOut.visibility = View.GONE
                }
            ))
    }

    private fun getSceneList(iotId: String) {
        vm.getSceneList(iotId, 0)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<BaseBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.ccsmartInEmpty.visibility = View.VISIBLE
                        binding.ccsmartInList.visibility = View.GONE
                    } else {
                        binding.ccsmartInEmpty.visibility = View.GONE
                        binding.ccsmartInList.visibility = View.VISIBLE
                        inAdapter.notifyList(it as ArrayList)
                    }
                }, error = { _, _, _ ->
                    binding.ccsmartInEmpty.visibility = View.VISIBLE
                    binding.ccsmartInList.visibility = View.GONE
                }
            ))


        vm.getSceneList(iotId, 1)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<BaseBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.clOut.visibility = View.GONE
                    } else {
                        binding.clOut.visibility = View.VISIBLE
                        binding.ccsmartOut.text = when (listType) {
                            DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> getString(com.mxchip.mxapp.base.R.string.ccs_device_hide)
                            DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> getString(com.mxchip.mxapp.base.R.string.ccs_group_hide)
                            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> getString(com.mxchip.mxapp.base.R.string.ccs_scene_hide)
                            else -> ""
                        }
                        outAdapter.notifyList(it as ArrayList)
                    }
                }, error = { _, _, _ ->
                    binding.clOut.visibility = View.GONE
                }
            ))
    }

    private fun getGroupList(iotId: String) {
        vm.getGroupList(iotId, 0)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<BaseBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.ccsmartInEmpty.visibility = View.VISIBLE
                        binding.ccsmartInList.visibility = View.GONE
                    } else {
                        binding.ccsmartInEmpty.visibility = View.GONE
                        binding.ccsmartInList.visibility = View.VISIBLE
                        inAdapter.notifyList(it as ArrayList)
                    }
                }, error = { _, _, _ ->
                    binding.ccsmartInEmpty.visibility = View.VISIBLE
                    binding.ccsmartInList.visibility = View.GONE
                }
            ))


        vm.getGroupList(iotId, 1)
            .launchAndCollectIn(this, Lifecycle.State.CREATED, loadFlowList<BaseBean>(
                success = {
                    if (it.isNullOrEmpty()) {
                        binding.clOut.visibility = View.GONE
                    } else {
                        binding.clOut.visibility = View.VISIBLE
                        binding.ccsmartOut.text = when (listType) {
                            DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> getString(com.mxchip.mxapp.base.R.string.ccs_device_hide)
                            DeviceConstants.ROUTER_CENTER_CONTROL_GROUP -> getString(com.mxchip.mxapp.base.R.string.ccs_group_hide)
                            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> getString(com.mxchip.mxapp.base.R.string.ccs_scene_hide)
                            else -> ""
                        }
                        outAdapter.notifyList(it as ArrayList)
                    }
                }, error = { _, _, _ ->
                    binding.clOut.visibility = View.GONE
                }
            ))
    }

    private fun setAdapterType(listType: String) {
        when (listType) {
            DeviceConstants.ROUTER_CENTER_CONTROL_DEVICE -> {
                inAdapter.setListType(CCSmartEnum.DEVICE)
                outAdapter.setListType(CCSmartEnum.DEVICE)
            }

            DeviceConstants.ROUTER_CENTER_CONTROL_SCENE -> {
                inAdapter.setListType(CCSmartEnum.SCENE)
                outAdapter.setListType(CCSmartEnum.SCENE)
            }

            else -> {
                inAdapter.setListType(CCSmartEnum.GROUP)
                outAdapter.setListType(CCSmartEnum.GROUP)
            }
        }
    }

    private fun getItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {

                if (viewHolder.adapterPosition == inAdapter.getList().size) {
                    return makeMovementFlags(0, 0)
                }
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = 0
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                //得到当拖拽的viewHolder的Position
                val fromPosition = viewHolder.adapterPosition
                //拿到当前拖拽到的item的viewHolder
                var toPosition: Int = target.adapterPosition
                if (fromPosition < toPosition) {
                    if (toPosition >= inAdapter.getList().size) {
                        toPosition = inAdapter.getList().size - 1
                    }
                    for (i in fromPosition until toPosition) {
                        if (i + 1 < inAdapter.getList().size) {
                            Collections.swap(inAdapter.getList(), i, i + 1)
                        }
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(inAdapter.getList(), i, i - 1)
                    }
                }
                inAdapter.notifyItemMoved(fromPosition, toPosition)
                moved = true
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }


            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    //震动
                    applicationContext.vibrateShot(70)
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return inAdapter.getList().size >= 2
            }
        })
    }
}