package com.aitronbiz.arron.adapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.Device
import com.aitronbiz.arron.entity.EnumData

class DeviceAdapter(
    private val devices: List<Device>
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private lateinit var dataManager: DataManager
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        dataManager = DataManager(parent.context)
        dataManager.open()
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = devices[position].name

        if(devices[position].status == EnumData.PRESENT.name) {
            holder.tvStatus.text = "재실"
        }else {
            holder.tvStatus.text = "부재중"
        }

        // 위치가 selectedPosition 값과 같으면 true
        val isSelected = position == selectedPosition
        holder.checkIcon.visibility = if(isSelected) View.VISIBLE else View.GONE

        if(devices[position].sign == EnumData.NORMAL.name) {
            holder.signLabel.visibility = View.GONE
        }else if(devices[position].sign == EnumData.CAUTION.name) {
            holder.signLabel.text = "주의"
            holder.signLabel.setBackgroundColor("#FFA500".toColorInt())
            blinkAnimation(holder.signLabel)
        }else {
            holder.signLabel.text = "경고"
            holder.signLabel.setBackgroundColor(Color.RED)
            blinkAnimation(holder.signLabel)
        }

        // 콜백을 통해 클릭 이벤트 전달
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int = devices.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: CardView = view.findViewById(R.id.container)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val signLabel: TextView = view.findViewById(R.id.signLabel)
        val checkIcon: ImageView = view.findViewById(R.id.checkIcon)
    }

    fun setSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    private fun blinkAnimation(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        animator.start()
    }
}