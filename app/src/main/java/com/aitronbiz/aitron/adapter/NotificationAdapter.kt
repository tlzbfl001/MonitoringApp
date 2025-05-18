package com.aitronbiz.aitron.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.aitron.R
import com.aitronbiz.aitron.entity.Subject

class NotificationAdapter(
    private val subjects: List<Subject>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val subject = subjects[position]

        // 이름, 생일, 이미지 설정
        holder.nameTextView.text = subject.name
        holder.birthdateTextView.text = subject.birthdate
        if (subject.image != null) {
            holder.avatarImageView.setImageResource(R.drawable.ic_launcher_foreground)
        } else {
            holder.avatarImageView.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // 시간과 상태 설정
        holder.timeTextView.text = "오후 02:13"
        holder.statusTextView.apply {
            text = if (position % 2 == 0) "위급" else "주의"
            setBackgroundColor(if (position % 2 == 0) Color.RED else Color.YELLOW)
        }
    }

    override fun getItemCount(): Int = subjects.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val birthdateTextView: TextView = itemView.findViewById(R.id.birthdateTextView)
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
    }
}