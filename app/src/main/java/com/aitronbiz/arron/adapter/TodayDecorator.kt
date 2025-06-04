package com.aitronbiz.arron.adapter

import android.content.Context
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.aitronbiz.arron.R
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class TodayDecorator(
    private val context: Context,
    private var selectedDate: CalendarDay
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == selectedDate
    }

    override fun decorate(view: DayViewFacade) {
        view.setBackgroundDrawable(
            ContextCompat.getDrawable(context, R.drawable.calendar_selected)!!
        )
        view.addSpan(object : ForegroundColorSpan(Color.WHITE) {}) // 텍스트 흰색
    }

    fun updateDate(date: CalendarDay) {
        selectedDate = date
    }
}