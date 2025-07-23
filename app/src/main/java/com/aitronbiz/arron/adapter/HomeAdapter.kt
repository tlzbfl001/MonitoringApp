package com.aitronbiz.arron.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.api.response.Home
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeAdapter(
    private val list: MutableList<Home>,
    private val onItemClick: (Home) -> Unit,
    private val onEditClick: (Home) -> Unit,
    private val onDeleteClick: (Home) -> Unit
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        context = parent.context
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val data = list[position]
        holder.tvName.text = data.name

        holder.view.setOnClickListener {
            onItemClick(data)
        }

        holder.btnMenu.setOnClickListener { view ->
            val dialog = BottomSheetDialog(view.context)
            val dialogView = LayoutInflater.from(view.context).inflate(R.layout.dialog_home_menu, null)

            val tvEdit = dialogView.findViewById<TextView>(R.id.tvEdit)
            val tvDelete = dialogView.findViewById<TextView>(R.id.tvDelete)

            tvEdit.setOnClickListener {
                onEditClick(data)
                dialog.dismiss()
            }

            tvDelete.setOnClickListener {
                onDeleteClick(data)
                dialog.dismiss()
            }

            dialog.setContentView(dialogView)
            dialog.show()
        }
    }

    inner class HomeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnMenu: ConstraintLayout = view.findViewById(R.id.btnMenu)
    }

    override fun getItemCount(): Int = list.size
}