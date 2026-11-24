package com.mxchip.mxapp.adapter

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.utils.RoleManager
import com.mxchip.mxapp.base.utils.generateShape
import com.mxchip.mxapp.bean.SmartHomeRoomBean
import com.mxchip.mxapp.databinding.ItemSmartHomeBinding
import kotlin.math.roundToInt


/**
 *
 */
class SmartHomeAdapter(
    private val clickRoom: (roomId: Int) -> Unit,
    private val clickSensor: (roomId: Int, iotId: String?, identifier: String) -> Unit,
    private val clickScene: (sceneId: Int, vid: Int) -> Unit,
    private val clickDevice: (roomId: Int, identifiers: List<String>, value: Int) -> Unit
) : BaseAdapter<ItemSmartHomeBinding>() {

    private var mData = mutableListOf<SmartHomeRoomBean>()

    fun setData(data: List<SmartHomeRoomBean>) {
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    fun getData() = mData

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun getViewBinding(parent: ViewGroup): ItemSmartHomeBinding {
        return ItemSmartHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ItemSmartHomeBinding>, position: Int) {
        val data = mData[position]
        holder.binding.tvName.text = data.room_name
        holder.binding.tvName.setOnClickListener {
            clickRoom(data.room_id)
        }
        // 传感器栏
        sensorUI(data, holder)
        // 场景栏
        sceneUI(data, holder)
        //底部快捷控制
        controlUI(data, holder)
    }

    private fun controlUI(
        data: SmartHomeRoomBean,
        holder: BaseViewHolder<ItemSmartHomeBinding>
    ) {
        //房间灯
        if (data.light.isEmpty()) {
            holder.binding.ivLightOn.visibility = View.GONE
            holder.binding.ivLightOff.visibility = View.GONE
        } else {
            holder.binding.ivLightOn.visibility = View.VISIBLE
            holder.binding.ivLightOff.visibility = View.VISIBLE
            //LightSwitch： 0-关 1-开
            holder.binding.ivLightOn.setOnClickListener {
                clickDevice(data.room_id, listOf("LightSwitch"), 1)
            }
            holder.binding.ivLightOff.setOnClickListener {
                clickDevice(data.room_id, listOf("LightSwitch"), 0)
            }
        }
        //房间空调
        if (data.air_conditoner.isEmpty()) {
            holder.binding.ivAirOn.visibility = View.GONE
            holder.binding.ivAirOff.visibility = View.GONE
        } else {
            holder.binding.ivAirOn.visibility = View.VISIBLE
            holder.binding.ivAirOff.visibility = View.VISIBLE
            //AirConditionerSwitch： 0-关 1-开
            holder.binding.ivAirOn.setOnClickListener {
                clickDevice(data.room_id, listOf("AirConditionerSwitch"), 1)
            }
            holder.binding.ivAirOff.setOnClickListener {
                clickDevice(data.room_id, listOf("AirConditionerSwitch"), 0)
            }
        }
        //房间窗帘
        if (data.curtain.isEmpty()) {
            holder.binding.ivCurtainOn.visibility = View.GONE
            holder.binding.ivCurtainOff.visibility = View.GONE
        } else {
            holder.binding.ivCurtainOn.visibility = View.VISIBLE
            holder.binding.ivCurtainOff.visibility = View.VISIBLE
            //CurtainSwitch：0-继续 1-暂停 2-打开 3-关闭
            holder.binding.ivCurtainOn.setOnClickListener {
                clickDevice(data.room_id, listOf("CurtainSwitch"), 2)
            }
            holder.binding.ivCurtainOff.setOnClickListener {
                clickDevice(data.room_id, listOf("CurtainSwitch"), 3)
            }
        }
        if (data.light.isEmpty() && data.air_conditoner.isEmpty() && data.curtain.isEmpty()) {
            holder.binding.actionDivider.visibility = View.GONE
        } else {
            holder.binding.actionDivider.visibility = View.VISIBLE
        }
    }

    private fun sceneUI(
        data: SmartHomeRoomBean,
        holder: BaseViewHolder<ItemSmartHomeBinding>
    ) {
        if (data.scenes.isEmpty()) {
            holder.binding.layoutScene.visibility = View.GONE
        } else {
            holder.binding.layoutScene.visibility = View.VISIBLE
            val scene = data.scenes[0]
            val color = Color.parseColor("#${scene.icon_color}")
            holder.binding.sceneIcon.imageTintList = ColorStateList.valueOf(color)
            holder.binding.sceneIcon.load(scene.icon_image)
            holder.binding.sceneName.text = scene.name
            holder.binding.executeAction.background =
                generateShape(Color.parseColor("#1f${scene.icon_color}"), 12)
            holder.binding.sceneActionOne.setOnClickListener {
                executeAction(holder.binding.executeAction, it.width)
                clickScene(scene.scene_id, scene.vid)
            }
            if (data.scenes.size == 1) { //只有一个场景，显示去添加
                holder.binding.executeActionTwo.background = null
                if (RoleManager.isUser()) {
                    holder.binding.tvSceneAdd.visibility = View.GONE
                    holder.binding.sceneActionTwo.visibility = View.INVISIBLE
                    holder.binding.sceneActionTwo.setOnClickListener(null)
                } else {
                    holder.binding.sceneIconTwo.visibility = View.GONE
                    holder.binding.sceneNameTwo.visibility = View.GONE
                    //管理员 显示去添加
                    holder.binding.tvSceneAdd.visibility = View.VISIBLE
                    holder.binding.sceneActionTwo.setOnClickListener {
                        clickScene(-1, -1)//-1 无场景，去添加
                    }
                }
            } else {
                holder.binding.tvSceneAdd.visibility = View.GONE
                holder.binding.sceneIconTwo.visibility = View.VISIBLE
                holder.binding.sceneNameTwo.visibility = View.VISIBLE
                val scene2 = data.scenes[1]
                val color2 = Color.parseColor("#${scene2.icon_color}")
                holder.binding.sceneIconTwo.imageTintList = ColorStateList.valueOf(color2)
                holder.binding.sceneIconTwo.load(scene2.icon_image)
                holder.binding.sceneNameTwo.text = scene2.name
                holder.binding.executeActionTwo.background =
                    generateShape(Color.parseColor("#1f${scene2.icon_color}"), 12)
                holder.binding.sceneActionTwo.setOnClickListener {
                    executeAction(holder.binding.executeActionTwo, it.width)
                    clickScene(scene2.scene_id, scene2.vid)
                }
            }
        }
    }

    private fun sensorUI(
        data: SmartHomeRoomBean,
        holder: BaseViewHolder<ItemSmartHomeBinding>
    ) {
        var showTemp = false
        var showHumidity = false
        var showPm25 = false
        var showPresence = false
        data.properties.forEach { prop ->
            val propValue = prop.value
            when (prop.identifier) {
                "CurrentTemperature" -> {
                    showTemp = true
                    holder.binding.tvSensorTemp.tag = prop.iotid
                    holder.binding.tvSensorTemp.text =
                        if (propValue == null || prop.online.not()) "--" else "${
                            formatValue(
                                propValue
                            )
                        }°C"
                }

                "CurrentHumidity" -> {
                    showHumidity = true
                    holder.binding.tvSensorHumidity.tag = prop.iotid
                    holder.binding.tvSensorHumidity.text =
                        if (propValue == null || prop.online.not()) "--" else "${
                            formatValue(
                                propValue
                            )
                        }%"
                }

                "pm2_5" -> {
                    showPm25 = true
                    holder.binding.tvSensorPm25.tag = prop.iotid
                    holder.binding.tvSensorPm25.text =
                        if (propValue == null || prop.online.not()) "--" else "${
                            formatValue(
                                propValue
                            )
                        }"
                }

                "OccupancyStatus" -> {
                    showPresence = true
                    holder.binding.tvSensorPresence.tag = prop.iotid
                    holder.binding.tvSensorPresence.text =
                        if (propValue == null || prop.online.not()) "--" else {
                            if (propValue is Number) {
                                if (propValue.toInt() == 1)
                                    holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_sensor_presence)
                                else
                                    holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_sensor_absence)
                            } else {
                                holder.itemView.context.getString(com.mxchip.mxapp.base.R.string.mx_sensor_absence)
                            }
                        }
                }
            }
        }

        if (showTemp || showHumidity || showPm25 || showPresence) {
            holder.binding.layoutSensor.visibility = View.VISIBLE
            if (showTemp) {
                holder.binding.tvSensorTemp.visibility = View.VISIBLE
                holder.binding.tvSensorTemp.setOnClickListener {
                    clickSensor(data.room_id, it.tag as? String, "CurrentTemperature")
                }
            } else {
                holder.binding.tvSensorTemp.visibility = View.GONE
                holder.binding.tvSensorTemp.setOnClickListener(null)
            }
            if (showHumidity) {
                holder.binding.tvSensorHumidity.visibility = View.VISIBLE
                holder.binding.tvSensorHumidity.setOnClickListener {
                    clickSensor(data.room_id, it.tag as? String, "CurrentHumidity")
                }
            } else {
                holder.binding.tvSensorHumidity.visibility = View.GONE
                holder.binding.tvSensorHumidity.setOnClickListener(null)
            }
            if (showPm25) {
                holder.binding.tvSensorPm25.visibility = View.VISIBLE
                holder.binding.tvSensorPm25.setOnClickListener {
                    clickSensor(data.room_id, it.tag as? String, "pm2_5")
                }
            } else {
                holder.binding.tvSensorPm25.visibility = View.GONE
                holder.binding.tvSensorPm25.setOnClickListener(null)
            }
            if (showPresence) {
                holder.binding.tvSensorPresence.visibility = View.VISIBLE
                holder.binding.tvSensorPresence.setOnClickListener {
                    clickSensor(data.room_id, it.tag as? String, "OccupancyStatus")
                }
            } else {
                holder.binding.tvSensorPresence.visibility = View.GONE
                holder.binding.tvSensorPresence.setOnClickListener(null)
            }
        } else {
            holder.binding.layoutSensor.visibility = View.GONE
        }

        //        if (showDivider)
        //            holder.binding.viewDivider.visibility = View.VISIBLE
        //        else
        //            holder.binding.viewDivider.visibility = View.GONE
    }

    private fun formatValue(value: Any): Any {
        return if (value is Double) value.roundToInt() else value
    }

    private fun executeAction(view: View, itemWidth: Int) {
        val layoutParams = view.layoutParams
        // 创建一个ValueAnimator来改变宽度
        val animator = ValueAnimator.ofInt(0, itemWidth)
        animator.setDuration(600) // 设置动画持续时间

        // 在动画过程中更新view的宽度
        animator.addUpdateListener { valueAnimator -> // 获取动画的当前值
            val currentValue = valueAnimator.animatedValue as Int
            if (currentValue >= itemWidth) {
                view.visibility = View.GONE
            } else {
                // 更新layoutParams的宽度
                layoutParams.width = currentValue
            }
            // 应用新的layoutParams
            view.layoutParams = layoutParams

        }
        view.visibility = View.VISIBLE
        animator.start()
    }
}