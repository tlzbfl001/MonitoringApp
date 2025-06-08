package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Device

class DeviceListAdapter(
    private var items: MutableList<Device>,
    private val onAddClick: () -> Unit,
    private val onItemClick: (Device) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DEVICE = 0
        private const val TYPE_ADD_BUTTON = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < items.size) TYPE_DEVICE else TYPE_ADD_BUTTON
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DEVICE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            DeviceViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_device, parent, false)
            AddViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DeviceViewHolder && position < items.size) {
            val item = items[position]
            holder.bind(item)

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener { onAddClick() }
        }
    }

    fun updateData(newItems: List<Device>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(device: Device) {
            itemView.findViewById<TextView>(R.id.textView).text = device.name
        }
    }

    class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}