package com.aitronbiz.arron.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Device

class DeviceItemAdapter(
    private var items: MutableList<Device>,
    private val onItemClick: (Device) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<DeviceItemAdapter.DeviceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val isAddButton = position == items.size

        if (isAddButton) {
            holder.ivAdd.visibility = View.VISIBLE
            holder.tvName.text = "디바이스 추가"
            holder.tvName.textSize = 14f
            holder.tvName.setTextColor(Color.parseColor("#CCCCCC"))
            holder.itemView.setOnClickListener { onAddClick() }
        } else {
            val device = items[position]
            holder.ivAdd.visibility = View.GONE
            holder.tvName.text = device.name
            holder.tvName.textSize = 15f
            holder.tvName.setTextColor(Color.WHITE)
            holder.itemView.setOnClickListener { onItemClick(device) }
        }
    }

    override fun getItemCount(): Int = items.size + 1 // +1 -> 뒤에 아이템 추가

    fun updateData(newList: List<Device>) {
        this.items = newList.toMutableList()
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivAdd: ImageView = view.findViewById(R.id.ivAdd)
    }
}
