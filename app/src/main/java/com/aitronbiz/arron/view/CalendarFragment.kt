package com.aitronbiz.arron.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.CalendarPagerAdapter

class CalendarFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CalendarPagerAdapter
    private var deviceId: Int = 0

    companion object {
        fun newInstance(deviceId: Int): CalendarFragment {
            val fragment = CalendarFragment()
            val args = Bundle().apply {
                putInt("deviceId", deviceId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceId = it.getInt("deviceId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        viewPager = view.findViewById(R.id.viewPager)

        adapter = CalendarPagerAdapter(this, deviceId)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(1000, false)

        return view
    }
}
