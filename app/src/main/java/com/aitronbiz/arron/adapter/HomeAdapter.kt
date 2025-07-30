package com.aitronbiz.arron.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.util.CustomUtil.TAG

class HomeAdapter(
    private var items: MutableList<Home>,
    private val onItemClick: (Home) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
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

    override fun getItemCount(): Int = items.size + 1 // +1 -> 뒤에 아이템 추가

    inner class HomeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivAdd: ImageView = view.findViewById(R.id.ivAdd)
    }
}