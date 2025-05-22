package com.aitronbiz.arron.util

import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class EventDecorator(
  private val color: Int,
  private val dates: Collection<CalendarDay>
) : DayViewDecorator {
  private val drawable: Drawable = color.toDrawable()

  override fun shouldDecorate(day: CalendarDay): Boolean {
    return dates.contains(day)
  }

  override fun decorate(view: DayViewFacade) {
    view.setBackgroundDrawable(drawable)
  }
}