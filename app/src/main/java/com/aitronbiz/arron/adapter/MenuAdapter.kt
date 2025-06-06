package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.databinding.ItemMenuSectionBinding
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.util.OnStartDragListener

class MenuAdapter(
    private val menuItems: MutableList<MenuItem>,
    private val dragStartListener: OnStartDragListener
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(val binding: ItemMenuSectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMenuSectionBinding.inflate(inflater, parent, false)
        return MenuViewHolder(binding)
    }

    override fun getItemCount(): Int = menuItems.size

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuItems[position]
        holder.binding.tvTitle.text = item.title

        holder.binding.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragStartListener.onStartDrag(holder)
            }
            false
        }
    }

    fun moveItem(from: Int, to: Int) {
        val item = menuItems.removeAt(from)
        menuItems.add(to, item)
        notifyItemMoved(from, to)
    }

    fun getMenuItems(): List<MenuItem> = menuItems.map { it.copy() }
}