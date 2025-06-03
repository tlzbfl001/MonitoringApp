package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Subject

class SelectSubjectDialogAdapter(
    private val items: List<Subject>,
    private val onItemClick: (Subject) -> Unit // 클릭 콜백
) : RecyclerView.Adapter<SelectSubjectDialogAdapter.ViewHolder>() {
    private var selectedPosition = RecyclerView.NO_POSITION

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
        val textView = LayoutInflater.from(parent.context).inflate(R.layout.item_select_subject, parent, false)
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = items[position].name

        // checkIcon 보이기/숨기기 처리
        holder.checkIcon.visibility = if (position == selectedPosition) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount() = items.size
}
