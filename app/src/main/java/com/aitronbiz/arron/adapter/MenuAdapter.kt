package com.aitronbiz.arron.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.MenuItem

class MenuAdapter(
    private val menuList: MutableList<MenuItem>,
    private val onVisibilityChanged: (Int, Boolean) -> Unit,
    private val dragStartListener: OnStartDragListener
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubTitle: TextView = view.findViewById(R.id.tvSubTitle)
        val switch: SwitchCompat = view.findViewById(R.id.visibilitySwitch)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]
        holder.tvTitle.text = item.title
        holder.tvSubTitle.text = "${item.title} 메뉴 표시"
        holder.switch.isChecked = item.visible
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onVisibilityChanged(position, isChecked)
        }

        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragStartListener.onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = menuList.size

    fun moveItem(from: Int, to: Int) {
        val item = menuList.removeAt(from)
        menuList.add(to, item)
        notifyItemMoved(from, to)
    }

    fun getMenuItems() = menuList
}

interface OnStartDragListener {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}