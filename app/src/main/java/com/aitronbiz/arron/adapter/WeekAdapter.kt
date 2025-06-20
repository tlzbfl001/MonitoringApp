package com.aitronbiz.arron.adapter

import com.aitronbiz.arron.R
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

class WeekAdapter(
    private val context: Context,
    private val baseDate: LocalDate,
    private var selectedDate: LocalDate,
    private val onDateSelected: (LocalDate) -> Unit
) : RecyclerView.Adapter<WeekAdapter.WeekViewHolder>() {
    private val startPage = 1000

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            weightSum = 7f
        }
        return WeekViewHolder(layout)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        val weekStart = baseDate.plusWeeks((position - startPage).toLong())
        holder.bindWeek(weekStart)
    }

    override fun getItemCount(): Int = Int.MAX_VALUE

    inner class WeekViewHolder(private val container: LinearLayout) :
        RecyclerView.ViewHolder(container) {

        fun bindWeek(weekStartDate: LocalDate) {
            container.removeAllViews()
            val sunday = weekStartDate.with(DayOfWeek.SUNDAY)
            val baseMonth = sunday.month  // 기준 월(해당 주 시작일의 월)

            for (i in 0..6) {
                val date = sunday.plusDays(i.toLong())
                val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()

                val itemView = LayoutInflater.from(container.context)
                    .inflate(R.layout.item_week_day, container, false) as ConstraintLayout

                val tvWeek = itemView.findViewById<TextView>(R.id.tvWeek)
                val tvDate = itemView.findViewById<TextView>(R.id.tvDate)

                tvWeek.text = dayOfWeek
                tvDate.text = date.dayOfMonth.toString()

                val isOtherMonth = date.month != baseMonth
                val textColor = if (isOtherMonth) android.R.color.darker_gray else android.R.color.black
                tvWeek.setTextColor(context.getColor(textColor))
                tvDate.setTextColor(context.getColor(textColor))

                itemView.setBackgroundResource(
                    if (date == selectedDate)
                        R.drawable.selected_bg
                    else
                        android.R.color.transparent
                )

                itemView.layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )

                itemView.setOnClickListener {
                    selectedDate = date
                    onDateSelected(date)
                    notifyDataSetChanged()
                }

                container.addView(itemView)
            }
        }
    }
}