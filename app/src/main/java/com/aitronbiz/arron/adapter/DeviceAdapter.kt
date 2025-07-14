package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.Room
import com.google.android.material.bottomsheet.BottomSheetDialog

class DeviceAdapter(
    private var items: MutableList<Device>,
    private val onItemClick: (Device) -> Unit,
    private val onEditClick: (Device) -> Unit,
    private val onDeleteClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val data = items[position]
        holder.tvName.text = data.name

        holder.view.setOnClickListener {
            onItemClick(data)
        }

        holder.btnMenu.setOnClickListener { view ->
            val dialog = BottomSheetDialog(view.context)
            val dialogView = LayoutInflater.from(view.context).inflate(R.layout.dialog_home_menu, null)

            val tvEdit = dialogView.findViewById<TextView>(R.id.tvEdit)
            val tvDelete = dialogView.findViewById<TextView>(R.id.tvDelete)

            tvEdit.setOnClickListener {
                onEditClick(data)
                dialog.dismiss()
            }

            tvDelete.setOnClickListener {
                onDeleteClick(data)
                dialog.dismiss()
            }

            dialog.setContentView(dialogView)
            dialog.show()
        }
    }

    fun updateData(newItems: List<Device>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnMenu: ConstraintLayout = view.findViewById(R.id.btnMenu)
    }

    override fun getItemCount(): Int = items.size

}