package com.aitronbiz.arron.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Room

class RoomItemAdapter(
    private val items: MutableList<Room>,
    private val onItemClick: (Room) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RoomItemAdapter.HomeViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
        context = parent.context
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val isAddButton = position == items.size

        if (isAddButton) {
            holder.ivAdd.visibility = View.VISIBLE
            holder.tvName.text = "추가하기"
            holder.tvName.setTextColor("#CCCCCC".toColorInt())
            holder.itemView.setOnClickListener { onAddClick() }
        } else {
            val data = items[position]
            holder.ivAdd.visibility = View.GONE
            holder.tvName.text = data.name
            holder.tvName.setTextColor(Color.WHITE)
            holder.itemView.setOnClickListener { onItemClick(data) }
        }
    }

    inner class HomeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivAdd: ImageView = view.findViewById(R.id.ivAdd)
    }

    override fun getItemCount(): Int = items.size + 1
}