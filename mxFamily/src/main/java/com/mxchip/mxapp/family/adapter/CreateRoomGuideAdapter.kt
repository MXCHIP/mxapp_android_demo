package com.mxchip.mxapp.family.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mxchip.mxapp.base.utils.generateStoke
import com.mxchip.mxapp.family.databinding.FamilyItCreateRoomGuideAddRoomBinding
import com.mxchip.mxapp.family.databinding.FamilyItCreateRoomGuideBinding

class CreateRoomGuideAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedRoom by lazy { mutableListOf<String>() }

    fun getRoom(): List<String> = selectedRoom

    private val list = ArrayList<String>()
    private var addListener: (() -> Unit)? = null

    override fun getItemCount(): Int {
        return list.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1)
            AddRoomViewHolder(
                FamilyItCreateRoomGuideAddRoomBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        else
            RoomViewHolder(
                FamilyItCreateRoomGuideBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RoomViewHolder) {
            val data = list[position]
            holder.binding.root.text = data
            if (selectedRoom.contains(data)) {
                val mainColor = ContextCompat.getColor(context, com.mxchip.mxapp.base.R.color.core_color_13b7f9)
                holder.binding.root.setTextColor(mainColor)
                holder.binding.root.background = generateStoke(2, mainColor, 8)
            } else {
                holder.binding.root.setTextColor(context.getColor(com.mxchip.mxapp.base.R.color.core_color_text_dialog_content))
                holder.binding.root.background =
                    generateStoke(
                        1,
                        context.getColor(com.mxchip.mxapp.base.R.color.core_color_border),
                        8
                    )
            }
            holder.itemView.setOnClickListener {
                if (selectedRoom.contains(data)) {
                    selectedRoom.remove(data)
                } else {
                    selectedRoom.add(data)
                }
                notifyItemChanged(position)
            }
        } else {
            holder.itemView.setOnClickListener {
                addListener?.invoke()
            }
        }
    }

    fun addRoomListener(listener: () -> Unit) = apply {
        addListener = listener
    }

    fun updateList(list: List<String>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }

    fun add(name: String) {
        val insertIndex = list.size
        list.add(name)
        //追加item
        notifyItemInserted(insertIndex)
        notifyItemRangeChanged(insertIndex, 1)
    }

    fun getList() = list
}

class RoomViewHolder(val binding: FamilyItCreateRoomGuideBinding) :
    RecyclerView.ViewHolder(binding.root)

class AddRoomViewHolder(val binding: FamilyItCreateRoomGuideAddRoomBinding) :
    RecyclerView.ViewHolder(binding.root)