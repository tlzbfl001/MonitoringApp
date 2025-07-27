package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.CalendarPagerAdapter
import com.aitronbiz.arron.viewmodel.MainViewModel
import java.util.Calendar

class CalendarFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CalendarPagerAdapter
    private lateinit var viewModel: MainViewModel

    private var homeId = ""
    private var initialPosition = 1000

    companion object {
        fun newInstance(homeId: String, year: Int, month: Int): CalendarFragment {
            val fragment = CalendarFragment()
            val args = Bundle().apply {
                putString("homeId", homeId)
                putInt("year", year)
                putInt("month", month)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        arguments?.let {
            homeId = it.getString("homeId") ?: ""

            val year = it.getInt("year")
            val month = it.getInt("month")

            val now = Calendar.getInstance()
            val base = Calendar.getInstance()
            base.set(year, month, 1)

            val offset = (base.get(Calendar.YEAR) - now.get(Calendar.YEAR)) * 12 +
                    (base.get(Calendar.MONTH) - now.get(Calendar.MONTH))
            initialPosition = 1000 + offset
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        viewPager = view.findViewById(R.id.viewPager)

        adapter = CalendarPagerAdapter(this, homeId)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(initialPosition, false)

        return view
    }
}
