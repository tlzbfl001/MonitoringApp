package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.NotificationData
import com.aitronbiz.arron.screen.notification.parseTime

sealed class NotificationRow {
    data class DateHeader(val dateText: String) : NotificationRow()
    data class Item(val data: NotificationData) : NotificationRow()
}

class NotificationAdapter(
    private val onItemClick: (NotificationData) -> Unit,
) : ListAdapter<NotificationRow, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        val DIFF = object : DiffUtil.ItemCallback<NotificationRow>() {
            override fun areItemsTheSame(oldItem: NotificationRow, newItem: NotificationRow): Boolean {
                return when {
                    oldItem is NotificationRow.DateHeader && newItem is NotificationRow.DateHeader ->
                        oldItem.dateText == newItem.dateText
                    oldItem is NotificationRow.Item && newItem is NotificationRow.Item ->
                        oldItem.data.id == newItem.data.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: NotificationRow, newItem: NotificationRow): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationRow.DateHeader -> TYPE_HEADER
            is NotificationRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                HeaderVH(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification, parent, false)
                ItemVH(v, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is NotificationRow.DateHeader -> (holder as HeaderVH).bind(row)
            is NotificationRow.Item -> (holder as ItemVH).bind(row.data)
        }
    }

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        fun bind(row: NotificationRow.DateHeader) {
            tvDate.text = row.dateText.replace("-", ".")
        }
    }

    class ItemVH(
        itemView: View,
        private val onItemClick: (NotificationData) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val ivRead: ImageView = itemView.findViewById(R.id.ivRead)

        fun bind(item: NotificationData) {
            tvTitle.text = item.title.orEmpty()
            tvBody.text = item.body.orEmpty()
            tvTime.text = item.createdAt?.let { parseTime(it) }.orEmpty()

            val read = item.isRead == true
            ivRead.setImageResource(if (read) R.drawable.ic_check else R.drawable.ic_unread_dot)
            ivRead.setColorFilter(if (read) 0xFFD3D3D3.toInt() else 0xFF00BFFF.toInt())

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
