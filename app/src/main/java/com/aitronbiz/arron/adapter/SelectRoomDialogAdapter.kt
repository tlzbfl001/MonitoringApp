package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Room

class SelectRoomDialogAdapter(
    private val items: List<Room>,
    private val onItemClick: (Room) -> Unit
) : RecyclerView.Adapter<SelectRoomDialogAdapter.ViewHolder>() {
    private var selectedPosition = 0 // 처음에 첫 번째 아이템을 선택된 상태로 초기화

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvName)
        val checkIcon: ImageView = view.findViewById(R.id.checkIcon)

        init {
            view.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = adapterPosition

                // 선택된 아이템 변경 시 notifyItemChanged로 이전, 현재 아이템의 상태를 업데이트
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)

                onItemClick(items[selectedPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context).inflate(R.layout.item_dialog_selected, parent, false)
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = items[position].name

        // checkIcon 보이기/숨기기 처리
        holder.checkIcon.visibility = if (position == selectedPosition) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount() = items.size
}
