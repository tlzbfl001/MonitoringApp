package com.aitronbiz.arron.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aitronbiz.arron.view.MonthFragment
import java.util.Calendar

class CalendarPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun createFragment(position: Int): Fragment {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, position - 1000)
        return MonthFragment.newInstance(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH)
        )
    }
}
