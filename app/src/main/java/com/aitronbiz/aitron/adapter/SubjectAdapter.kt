package com.aitronbiz.aitron.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.aitron.R
import com.aitronbiz.aitron.entity.Subject
import com.aitronbiz.aitron.util.CustomUtil.TAG

class SubjectAdapter(
    private val subjects: List<Subject>
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(subjects[position])
    }

    override fun getItemCount(): Int = subjects.size

    class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_name)
        private val birthdateText: TextView = itemView.findViewById(R.id.text_birthdate)
        private val imageView: ImageView = itemView.findViewById(R.id.image_profile)

        fun bind(subject: Subject) {
            nameText.text = subject.name
            birthdateText.text = subject.birthdate

            if (!subject.image.isNullOrEmpty()) {
                Log.d(TAG, "bind: ${subject.image}")
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
            }
        }
    }
}