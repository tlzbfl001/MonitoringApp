package com.aitronbiz.arron.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import java.time.LocalDate

class DayAdapter(
    private val context: Context,
    private val homeId: String,
    private val days: List<DayItem>,
    private val onDayClick: (DayItem) -> Unit,
    initialSelectedDate: LocalDate? = null
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {
    private var selectedDate: LocalDate? = initialSelectedDate

    fun setSelectedDate(date: LocalDate) {
        selectedDate = date
        notifyDataSetChanged()
    }

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val container: View = view.findViewById(R.id.rootLayout)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)

        fun bind(item: DayItem) {
            val currentDate = LocalDate.of(item.year, item.month + 1, item.day!!)
            tvDate.text = item.day.toString()

            container.setBackgroundResource(
                if(currentDate == selectedDate) R.drawable.selected_bg
                else android.R.color.transparent
            )

            itemView.setOnClickListener {
                onDayClick(item)
                setSelectedDate(currentDate)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size
}

data class DayItem(
    val day: Int?,
    val year: Int,
    val month: Int,
    val isInCurrentMonth: Boolean,
    val isToday: Boolean
)
