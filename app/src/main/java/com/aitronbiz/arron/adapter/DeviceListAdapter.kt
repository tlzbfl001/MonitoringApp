package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Device
import com.aitronbiz.arron.api.response.Home
import com.google.android.material.bottomsheet.BottomSheetDialog

class DeviceListAdapter(
    private var items: MutableList<Device>,
    private val onItemClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {
    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val item = items[position]
        val tvName = holder.itemView.findViewById<TextView>(R.id.tvName)
        val btnMenu = holder.itemView.findViewById<ConstraintLayout>(R.id.btnMenu)
        btnMenu.visibility = View.GONE
        tvName.text = item.name

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    fun updateData(newList: List<Device>) {
        this.items = newList.toMutableList()
        notifyDataSetChanged()
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}