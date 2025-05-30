package com.aitronbiz.arron.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import java.io.File
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Subject

class SubjectAdapter(
    private val subjects: List<Subject>
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private lateinit var dataManager: DataManager
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        dataManager = DataManager(parent.context)
        dataManager.open()
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.nameText.text = subjects[position].name

        // 이미지 설정
        if(!subjects[position].image.isNullOrEmpty()) {
            holder.imageView.setImageURI(Uri.fromFile(File(subjects[position].image!!)))
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // 위치가 selectedPosition 값과 같으면 true
        val isSelected = position == selectedPosition

        holder.checkIcon.visibility = if(isSelected) View.VISIBLE else View.GONE
        holder.nameText.setTypeface(null, if(isSelected) Typeface.BOLD else Typeface.NORMAL)
        holder.nameText.setTextColor(if(isSelected) Color.BLACK else "#AAAAAA".toColorInt())

        // 콜백을 통해 클릭 이벤트 전달
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int = subjects.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }

    class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val nameText: TextView = view.findViewById(R.id.tvName)
        val checkIcon: ImageView = view.findViewById(R.id.checkIcon)
    }

    fun setSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }
}