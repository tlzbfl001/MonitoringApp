package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Subject
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubjectAdapter(
    private val list: MutableList<Subject>,
    private val onItemClick: (Subject) -> Unit,
    private val onEditClick: (Subject) -> Unit,
    private val onDeleteClick: (Subject) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = list[position]
        holder.tvName.text = subject.name

        holder.view.setOnClickListener {
            onItemClick(subject)
        }

        holder.btnMenu.setOnClickListener { view ->
            val dialog = BottomSheetDialog(view.context)
            val dialogView = LayoutInflater.from(view.context).inflate(R.layout.dialog_home_menu, null)

            val tvEdit = dialogView.findViewById<TextView>(R.id.tvEdit)
            val tvDelete = dialogView.findViewById<TextView>(R.id.tvDelete)

            tvEdit.setOnClickListener {
                onEditClick(subject)
                dialog.dismiss()
            }

            tvDelete.setOnClickListener {
                onDeleteClick(subject)
                dialog.dismiss()
            }

            dialog.setContentView(dialogView)
            dialog.show()
        }
    }

    fun updateData(newItems: List<Subject>) {
        list.clear()
        list.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class SubjectViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnMenu: ConstraintLayout = view.findViewById(R.id.btnMenu)
    }

    override fun getItemCount(): Int = list.size
}