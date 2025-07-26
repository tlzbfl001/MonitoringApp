package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Room

class SelectRoomDialogAdapter(
    private val items: List<Room>,
    private val onItemClick: (Room) -> Unit,
    private var selectedPosition: Int = 0
) : RecyclerView.Adapter<SelectRoomDialogAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvName)
        val checkIcon: ImageView = view.findViewById(R.id.checkIcon)

        init {
            view.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onItemClick(items[selectedPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialog_selected, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = items[position].name
        holder.checkIcon.visibility = if (position == selectedPosition) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount() = items.size
}
