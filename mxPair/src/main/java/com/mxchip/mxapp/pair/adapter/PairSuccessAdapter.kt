package com.mxchip.mxapp.pair.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.google.android.flexbox.FlexboxLayoutManager
import com.mxchip.mxapp.base.base.BaseAdapter
import com.mxchip.mxapp.base.base.BaseViewHolder
import com.mxchip.mxapp.base.bean.RoomBean
import com.mxchip.mxapp.pair.R
import com.mxchip.mxapp.pair.bean.PairSuccessDevice
import com.mxchip.mxapp.pair.databinding.PairItPairSuccessBinding
import com.mxchip.mxapp.pair.utils.PairManager


/**
 *添加完成设备列表
 */
class PairSuccessAdapter : BaseAdapter<PairItPairSuccessBinding>() {

    private val datas = mutableListOf<PairSuccessDevice>()

    private val rooms = mutableListOf<RoomBean>()

    fun setRoomList(list: List<RoomBean>) {
        rooms.clear()
        rooms.addAll(list)
    }

    fun appendRoom(roomBean: RoomBean) {
        val inset = rooms.size
        rooms.add(roomBean)
        notifyItemInserted(inset)
        notifyItemRangeChanged(inset, rooms.size)
    }

    fun getRoomList(): List<RoomBean> = rooms

    fun getData(): List<PairSuccessDevice> {
        return datas
    }

    fun addAll(list: List<PairSuccessDevice>) {
        datas.clear()
        datas.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun getViewBinding(parent: ViewGroup) =
        PairItPairSuccessBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun onBindViewHolder(holder: BaseViewHolder<PairItPairSuccessBinding>, position: Int) {
        val pairSuccessBean = datas[position]
        holder.binding.icon.load(pairSuccessBean.icon)
        //检查mesh设备才可控制
        if (null != pairSuccessBean.uuid && pairSuccessBean.uuid!!.length > 20) holder.binding.icon.setOnClickListener {
            iconClick?.invoke(pairSuccessBean.uuid!!)
        }
        holder.binding.name.text =
            if (pairSuccessBean.nick_name.isNullOrEmpty()) pairSuccessBean.productName else pairSuccessBean.nick_name
        holder.binding.name.setOnClickListener {
            nicknameClickListener?.invoke(position)
        }
        if (PairManager.isSupportSetCommonly()) {
            if (pairSuccessBean.show)
                holder.binding.ivFavorite.setImageResource(R.drawable.pair_ic_password_show)
            else
                holder.binding.ivFavorite.setImageResource(R.drawable.pair_ic_password_hide)
            holder.binding.ivFavorite.setOnClickListener {
                pairSuccessBean.show = !pairSuccessBean.show
                notifyItemChanged(position)
            }
        } else {
            holder.binding.layoutCommonly.visibility = View.GONE
        }

        //房间列表
        holder.binding.rvRoom.layoutManager = FlexboxLayoutManager(holder.itemView.context)
        val roomAdapter = PairSuccessRoomAdapter()
        roomAdapter.setRoomList(rooms)
        roomAdapter.setSelectedRoomId(pairSuccessBean.room_id)
        holder.binding.rvRoom.adapter = roomAdapter

        roomAdapter.setItemClickListener { roomId ->
            if (roomId != -1) {
                pairSuccessBean.room_id = roomId
                roomAdapter.setSelectedRoomId(roomId)
            } else {
                addRoomClick?.invoke()
            }
        }
    }

    private var iconClick: ((String) -> Unit)? = null
    fun setIconClick(click: (String) -> Unit) {
        iconClick = click
    }

    private var addRoomClick: (() -> Unit)? = null
    fun setAddRoomClick(click: () -> Unit) {
        addRoomClick = click
    }

    private var nicknameClickListener: ((Int) -> Unit)? = null
    fun setNicknameClickListener(nicknameClick: (Int) -> Unit) {
        nicknameClickListener = nicknameClick
    }

}