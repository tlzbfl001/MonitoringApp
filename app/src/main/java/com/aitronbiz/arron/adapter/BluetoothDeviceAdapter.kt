package com.aitronbiz.arron.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import androidx.core.graphics.toColorInt

class BluetoothDeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onClick: (BluetoothDevice) -> Unit,
    private var connectedDevice: BluetoothDevice? = null
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth, parent, false)
        return DeviceViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device.name ?: "Unknown Device"

        if (device.address == connectedDevice?.address) {
            holder.tvStatus.text = "연결됨"
            holder.tvStatus.setTextColor("#3F51B5".toColorInt())
        } else {
            holder.tvStatus.text = "연결 안됨"
            holder.tvStatus.setTextColor("#CCCCCC".toColorInt())
        }

        holder.view.setOnClickListener { onClick(device) }
    }

    fun updateConnectedDevice(device: BluetoothDevice?) {
        connectedDevice = device
        notifyDataSetChanged()
    }

    fun getConnectedDevice(): BluetoothDevice? = connectedDevice

    inner class DeviceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun getItemCount(): Int = devices.size
}