package com.aitronbiz.arron.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aitronbiz.arron.view.MonthFragment
import java.util.Calendar

class CalendarPagerAdapter(
    fragment: Fragment,
    private val deviceId: Int
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun createFragment(position: Int): Fragment {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, position - 1000)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        return MonthFragment.newInstance(year, month, deviceId)
    }
}