package com.aitronbiz.aitron.adapter

import com.aitronbiz.aitron.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.aitron.entity.Device

class DeviceAdapter (
  private val item: ArrayList<Device> = ArrayList()
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: DeviceAdapter.ViewHolder, pos: Int) {
    holder.tvName.text = "${item[pos].serialNumber}"
  }

  override fun getItemViewType(position: Int): Int {
    return position
  }

  override fun getItemCount(): Int {
    return item.count()
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvName: TextView = itemView.findViewById(R.id.tvName)
  }
}