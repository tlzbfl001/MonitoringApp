package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.DayAdapter
import com.aitronbiz.arron.adapter.DayItem
import java.time.LocalDate
import java.util.Calendar

class MonthFragment : Fragment() {
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: DayAdapter
    private var year = 0
    private var month = 0
    private var deviceId = 0

    companion object {
        fun newInstance(year: Int, month: Int, deviceId: Int): MonthFragment {
            val fragment = MonthFragment()
            val args = Bundle().apply {
                putInt("year", year)
                putInt("month", month)
                putInt("deviceId", deviceId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            year = it.getInt("year")
            month = it.getInt("month")
            deviceId = it.getInt("deviceId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_month, container, false)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        tvTitle.text = "${year}년 ${month + 1}월"

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 7)

        val selectedDate = viewModel.selectedDate.value ?: LocalDate.now()
        val dayList = generateMonthDays(year, month)

        adapter = DayAdapter(
            context = requireContext(),
            deviceId = deviceId,
            days = dayList,
            onDayClick = { dayItem ->
                val newDate = LocalDate.of(dayItem.year, dayItem.month + 1, dayItem.day!!)
                viewModel.updateSelectedDate(newDate)
                adapter.setSelectedDate(newDate)
            },
            initialSelectedDate = selectedDate
        )

        recyclerView.adapter = adapter

        return view
    }

    private fun generateMonthDays(year: Int, month: Int): List<DayItem> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        val result = mutableListOf<DayItem>()

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        // 이전 달 날짜
        val prevMonthCal = Calendar.getInstance()
        prevMonthCal.set(year, month, 1)
        prevMonthCal.add(Calendar.MONTH, -1)
        val prevYear = prevMonthCal.get(Calendar.YEAR)
        val prevMonth = prevMonthCal.get(Calendar.MONTH)
        val prevDaysInMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for(i in (firstDayOfWeek - 1) downTo 0) {
            val day = prevDaysInMonth - i
            result.add(
                DayItem(
                    day = day,
                    year = prevYear,
                    month = prevMonth,
                    isToday = (prevYear == todayYear && prevMonth == todayMonth && day == todayDay),
                    isInCurrentMonth = false
                )
            )
        }

        // 현재 달 날짜
        for(day in 1..daysInMonth) {
            result.add(
                DayItem(
                    day = day,
                    year = year,
                    month = month,
                    isToday = (year == todayYear && month == todayMonth && day == todayDay),
                    isInCurrentMonth = true
                )
            )
        }

        // 다음 달 날짜
        val nextMonthCal = Calendar.getInstance()
        nextMonthCal.set(year, month, 1)
        nextMonthCal.add(Calendar.MONTH, 1)
        val nextYear = nextMonthCal.get(Calendar.YEAR)
        val nextMonth = nextMonthCal.get(Calendar.MONTH)

        var nextDay = 1
        while(result.size < 42) {
            result.add(
                DayItem(
                    day = nextDay,
                    year = nextYear,
                    month = nextMonth,
                    isToday = (nextYear == todayYear && nextMonth == todayMonth && nextDay == todayDay),
                    isInCurrentMonth = false
                )
            )
            nextDay++
        }

        return result
    }
}