package com.aitronbiz.arron.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Device

class DeviceItemAdapter(
    private val list: MutableList<Device>,
    private val onItemClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceItemAdapter.HomeViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
        context = parent.context
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val data = list[position]
        holder.tvName.text = data.name

        holder.view.setOnClickListener {
            onItemClick(data)
        }
    }

    inner class HomeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    override fun getItemCount(): Int = list.size
}