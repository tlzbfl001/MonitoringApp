package com.aitronbiz.arron.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Room

class RoomsAdapter(
    private val onClick: (Room) -> Unit
) : ListAdapter<RoomsAdapter.Item, RoomsAdapter.VH>(Diff) {

    data class Item(
        val room: Room,
        val isPresent: Boolean,
        val isSelected: Boolean
    )

    private var showBadge: Boolean = true

    fun submit(
        rooms: List<Room>,
        presence: Map<String, Boolean>,
        selectedId: String,
        showBadge: Boolean
    ) {
        this.showBadge = showBadge
        val list = rooms.map { r ->
            Item(
                room = r,
                isPresent = presence[r.id] == true,
                isSelected = (r.id == selectedId)
            )
        }
        submitList(list)
    }

    object Diff : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(old: Item, new: Item) = old.room.id == new.room.id
        override fun areContentsTheSame(old: Item, new: Item) = old == new
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.card)
        private val name = itemView.findViewById<TextView>(R.id.tvName)
        private val badge = itemView.findViewById<TextView>(R.id.tvBadge)
        private val density = itemView.resources.displayMetrics.density

        fun bind(item: Item) {
            val present = item.isPresent
            name.text = item.room.name
            badge.visibility = if (showBadge) View.VISIBLE else View.GONE

            if (showBadge) {
                val isPresent = present
                val bgTintInt = android.graphics.Color.parseColor(
                    if (isPresent) "#3322D3EE" else "#339A9EA8"
                )
                val textColorInt = android.graphics.Color.parseColor(
                    if (isPresent) "#00D0E6" else "#9EA4AE"
                )

                badge.text = if (isPresent) "재실중" else "부재중"
                badge.setTextColor(textColorInt)
                badge.background?.mutate()
                badge.backgroundTintList = ColorStateList.valueOf(bgTintInt)
            }

            card.setCardBackgroundColor("#5A185078".toColorInt())

            val strokePx = (1.4f * density).toInt()
            card.strokeWidth = strokePx
            card.strokeColor = if (item.isSelected) 0xFFFFFFFF.toInt() else 0xFF185078.toInt()

            card.setOnClickListener { onClick(item.room) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_room_cell, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
