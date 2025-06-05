package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Device

class DeviceAdapter(
    private val devices: List<Device>,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemClickListener: OnItemClickListener? = null
    private var addClickListener: (() -> Unit)? = null
    private var selectedPosition = 0

    companion object {
        private const val TYPE_DEVICE = 0
        private const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == devices.size) TYPE_ADD else TYPE_DEVICE
    }

    override fun getItemCount(): Int = devices.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DEVICE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_main, parent, false)
            DeviceViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add, parent, false)
            AddViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DeviceViewHolder) {
            val device = devices[position]
            holder.tvName.text = device.name
            holder.tvStatus.text = if (device.room == 1) "(재실)" else "(부재중)"

            val isSelected = position == selectedPosition
            holder.mainView.setCardBackgroundColor(
                if (isSelected) "#BBBBBB".toColorInt() else "#FFFFFF".toColorInt()
            )

            holder.itemView.setOnClickListener {
                itemClickListener?.onItemClick(position)
            }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener {
                addClickListener?.invoke()
            }
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }

    fun setSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    fun setOnAddClickListener(listener: () -> Unit) {
        this.addClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val mainView: CardView = view.findViewById(R.id.mainView)
    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
