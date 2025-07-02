package com.aitronbiz.arron.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Room

class RoomListAdapter(
    private val items: MutableList<Room>
) : RecyclerView.Adapter<RoomListAdapter.RoomViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        context = parent.context
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.tvName.text = items[position].name
    }

    fun updateData(newItems: List<Room>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class RoomViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    override fun getItemCount(): Int = items.size
}